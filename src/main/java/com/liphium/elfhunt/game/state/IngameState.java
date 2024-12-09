package com.liphium.elfhunt.game.state;

import com.liphium.core.particle.ParticleBuilder;
import com.liphium.core.util.ItemStackBuilder;
import com.liphium.elfhunt.Elfhunt;
import com.liphium.elfhunt.game.GameState;
import com.liphium.elfhunt.game.team.Team;
import com.liphium.elfhunt.game.team.impl.HunterTeam;
import com.liphium.elfhunt.game.team.impl.ElfTeam;
import com.liphium.elfhunt.listener.machines.impl.ItemShop;
import com.liphium.elfhunt.screens.ItemShopScreen;
import com.liphium.elfhunt.util.LocationAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class IngameState extends GameState {

    private final ParticleBuilder beetroot;

    private final ArrayList<BeetrootData> beetroots = new ArrayList<>();
    private final ArrayList<DroppableTrap> traps = new ArrayList<>();

    private Runnable runnable;

    public IngameState() {
        super("In game", 30);

        beetroot = new ParticleBuilder().withColor(Color.PURPLE);
    }

    private final HashMap<Location, Boolean> placedBlocks = new HashMap<>();

    @Override
    public void start() {

        Objects.requireNonNull(LocationAPI.getLocation("Camp")).getWorld().setDifficulty(Difficulty.HARD);

        for (Team team : Elfhunt.getInstance().getGameManager().getTeamManager().getTeams()) {
            team.sendStartMessage();

            for (Player player : team.getPlayers()) {
                player.getInventory().clear();
                team.giveKit(player, true);
            }
        }

        Elfhunt.getInstance().getTaskManager().inject(runnable = new Runnable() {
            int tickCount = 0;

            @Override
            public void run() {
                Elfhunt.getInstance().getGameManager().getTeamManager().tick();
                Elfhunt.getInstance().getMachineManager().tick();

                if (tickCount++ >= 20) {
                    tickCount = 0;

                    ArrayList<BeetrootData> toRemove = new ArrayList<>();

                    for (BeetrootData data : beetroots) {
                        if (data.start + 60000 <= System.currentTimeMillis()) {
                            data.item.remove();
                            toRemove.add(data);
                        }

                        for (Player all : Bukkit.getOnlinePlayers()) {
                            beetroot.renderCircle(all, data.location.clone().add(0, 0.3, 0), 3);
                        }
                    }

                    for (BeetrootData rem : toRemove) {
                        beetroots.remove(rem);
                    }
                }
            }
        });
    }

    private boolean placeItemShop = false;

    @Override
    public void onSpawn(EntitySpawnEvent event) {
        if (event.getEntityType().equals(EntityType.WIND_CHARGE) || event.getEntityType().equals(EntityType.BREEZE_WIND_CHARGE)
                || event.getEntityType().equals(EntityType.BOAT)) {
            return;
        }
        if (event.getEntityType() == EntityType.ARMOR_STAND && placeItemShop) {
            final var itemShop = new ItemShop((ArmorStand) event.getEntity());
            Elfhunt.getInstance().getMachineManager().addMachine(itemShop);
            placeItemShop = false;
        }
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.WIND_CHARGE) {
            return;
        }
        Elfhunt.getInstance().getMachineManager().onInteract(event);

        if (event.getClickedBlock() != null && event.getItem() != null && event.getItem().getType().equals(Material.ARMOR_STAND)) {
            placeItemShop = true;
            return;
        }

        if (event.getPlayer().getCooldown(Material.STICK) > 0
                && event.getItem() != null && event.getItem().getType().equals(Material.STICK)) {
            event.setCancelled(true);
        }

        if (event.getItem() != null) {
            Team team = Elfhunt.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());
            ItemStack usedItem = event.getItem();
            if (usedItem.getType().equals(Material.BEETROOT) && event.getClickedBlock() != null) {
                beetroots.add(new BeetrootData(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5)));
                reduceMainHandItem(event.getPlayer());
            } else if (usedItem.getType().equals(Material.TRIPWIRE_HOOK) && event.getClickedBlock() != null) {
                traps.add(new SlowTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
                reduceMainHandItem(event.getPlayer());
            } else if (usedItem.getType().equals(Material.GLOWSTONE_DUST) && event.getClickedBlock() != null) {
                traps.add(new GlowTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
                reduceMainHandItem(event.getPlayer());
            } else if (usedItem.getType().equals(Material.FEATHER) && event.getPlayer().getCooldown(Material.FEATHER) <= 0) {
                event.getPlayer().setVelocity(event.getPlayer().getLocation().getDirection().normalize().multiply(event.getPlayer().isOnGround() ? 1.8 : 1.1));
                event.getPlayer().setCooldown(Material.FEATHER, 50);
                ItemShopScreen.removeAmountFromInventory(event.getPlayer(), Material.FEATHER, 1);
            }
        }
    }

    void reduceMainHandItem(Player player) {
        int amount = player.getInventory().getItemInMainHand().getAmount();
        if (amount == 1) {
            player.getInventory().setItemInMainHand(null);
        } else player.getInventory().getItemInMainHand().setAmount(amount - 1);
    }

    @Override
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Elfhunt.getInstance().getMachineManager().onInteractAtEntity(event);

        if (event.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        Team team = Elfhunt.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());

        // Check if they wandered into a trap
        ArrayList<DroppableTrap> toRemove = new ArrayList<>();
        for (DroppableTrap trap : traps) {
            if (trap.location.distance(event.getPlayer().getLocation()) <= 3 && !team.getName().equals(trap.team.getName())) {
                toRemove.add(trap);
                trap.onEnter(event.getPlayer());
                break;
            }
        }
        for (DroppableTrap rem : toRemove) {
            rem.item.remove();
            traps.remove(rem);
        }

    }

    @Override
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            Team team = Elfhunt.getInstance().getGameManager().getTeamManager().getTeam(player);

            if (team instanceof HunterTeam) {
                event.setDamage(0);
            }
        } else event.setCancelled(true);
    }

    @Override
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType().equals(Material.REDSTONE_TORCH)) {
            event.setCancelled(true);
            return;
        }


        // Place a machine if it is one
        final var machine = Elfhunt.getInstance().getMachineManager().newMachineByMaterial(event.getBlockPlaced().getType(), event.getBlockPlaced().getLocation());
        if (machine != null) {
            Elfhunt.getInstance().getMachineManager().addMachine(machine);
        }
    }

    final List<Material> grassTypes = Arrays.asList(
            Material.TALL_GRASS, Material.SHORT_GRASS, Material.CORNFLOWER,
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
            Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
            Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY, Material.SUNFLOWER,
            Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
            Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE
    );

    @Override
    public void onBreak(BlockBreakEvent event) {
        if (Elfhunt.getInstance().getMachineManager().breakLocation(event.getBlock().getLocation())) {
            event.setDropItems(false);
            return;
        }

        // Only let placed blocks be broken again
        if(placedBlocks.get(event.getBlock().getLocation()) != null) {
            placedBlocks.remove(event.getBlock().getLocation());
            return;
        }

        // Let grass blocks be removed permanently (for PvP)
        if(grassTypes.contains(event.getBlock().getType())) {
            event.setDropItems(false);
            event.setCancelled(false);
            return;
        }

        event.setCancelled(true);
    }

    @Override
    public void onDeath(PlayerDeathEvent event) {
        event.setKeepInventory(true);
        event.setDeathMessage(null);
        event.setKeepLevel(true);
        handleDeath(event.getEntity());
    }

    @Override
    public void handleDeath(Player player) {
        player.setGameMode(GameMode.SPECTATOR);

        if (player.getKiller() != null) {
            Bukkit.broadcast(Elfhunt.PREFIX.append(Component.text("§c" + player.getName() + " §7was killed by §c§l" + player.getKiller().getName() + "§7!")));
        } else
            Bukkit.broadcast(Elfhunt.PREFIX.append(Component.text("§c§l" + player.getKiller().getName() + " §7died!")));

        player.getInventory().clear();
        player.setHealth(20);
        player.spigot().respawn();
        player.getInventory().clear();

        Team team = Elfhunt.getInstance().getGameManager().getTeamManager().getTeam(player);
        team.getPlayers().remove(player);

        if (team.getPlayers().isEmpty()) {
            handleWin(Elfhunt.getInstance().getGameManager().getTeamManager().getTeam("Humans"));
        }
    }

    public void handleWin(Team team) {
        team.handleWin();
        Elfhunt.getInstance().getTaskManager().uninject(runnable);
        Elfhunt.getInstance().getGameManager().setCurrentState(new EndState());
    }

    @Override
    public void quit(Player player) {
        Team team = Elfhunt.getInstance().getGameManager().getTeamManager().getTeam(player);
        team.getPlayers().remove(player);
    }

    public static class BeetrootData {

        public final Location location;
        public final Item item;
        public final long start;

        public BeetrootData(Location location) {
            this.location = location;

            item = (Item) location.getWorld().spawnEntity(location.clone().add(0, 1, 0), EntityType.ITEM);
            item.setItemStack(new ItemStackBuilder(Material.BEETROOT).buildStack());
            item.setVelocity(new Vector(0, 0, 0));
            item.setPickupDelay(1000000000);
            item.setCanPlayerPickup(false);
            item.setCanMobPickup(false);
            item.setUnlimitedLifetime(true);
            item.setCustomNameVisible(true);
            item.customName(Component.text("Blood Garlic", NamedTextColor.RED, TextDecoration.BOLD));
            start = System.currentTimeMillis();
        }

    }

    public static abstract class DroppableTrap {
        public final Location location;
        public final Team team;
        public final Item item;
        public final long start;

        public DroppableTrap(Location location, Team team, Material material) {
            this.location = location;
            this.team = team;

            item = (Item) location.getWorld().spawnEntity(location.clone().add(0, 1, 0), EntityType.ITEM);
            item.setItemStack(new ItemStackBuilder(material).buildStack());
            item.setVelocity(new Vector(0, 0, 0));
            item.setPickupDelay(1000000000);
            item.setCanPlayerPickup(false);
            item.setCanMobPickup(false);
            item.setUnlimitedLifetime(true);
            start = System.currentTimeMillis();
        }

        public abstract void onEnter(Player player);
    }

    public static class SlowTrap extends DroppableTrap {

        SlowTrap(Location location, Team team) {
            super(location, team, Material.TRIPWIRE_HOOK);
        }

        @Override
        public void onEnter(Player player) {

            // Notify all players in the team that planted the trap
            final var x = player.getLocation().getBlockX();
            final var y = player.getLocation().getBlockY();
            final var z = player.getLocation().getBlockZ();
            for (Player human : team.getPlayers()) {
                human.sendMessage(Component.text(" "));
                human.sendMessage(Component.text("     §c§l" + player.getName() + " §7walked into a slow trap."));
                human.sendMessage(Component.text("     §7Location: §c" + x + " " + y + " " + z));
                human.sendMessage(Component.text(" "));
            }

            // Give the player slowness
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 2));
        }
    }

    public static class GlowTrap extends DroppableTrap {

        GlowTrap(Location location, Team team) {
            super(location, team, Material.GLOWSTONE_DUST);
        }

        @Override
        public void onEnter(Player player) {

            // Notify all players in the team that planted the trap
            final var x = player.getLocation().getBlockX();
            final var y = player.getLocation().getBlockY();
            final var z = player.getLocation().getBlockZ();
            for (Player human : team.getPlayers()) {
                human.sendMessage(Component.text(" "));
                human.sendMessage(Component.text("     §c§l" + player.getName() + " §7walked into a glow trap."));
                human.sendMessage(Component.text("     §7Location: §c" + x + " " + y + " " + z));
                human.sendMessage(Component.text(" "));
            }

            // Give the player slowness
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
        }
    }
}