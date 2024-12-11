package com.liphium.elfhunt.game.state;

import com.liphium.core.Core;
import com.liphium.core.util.ItemStackBuilder;
import com.liphium.elfhunt.Elfhunt;
import com.liphium.elfhunt.game.GameState;
import com.liphium.elfhunt.game.team.Team;
import com.liphium.elfhunt.game.team.impl.ElfTeam;
import com.liphium.elfhunt.game.team.impl.HunterTeam;
import com.liphium.elfhunt.listener.machines.MachineManager;
import com.liphium.elfhunt.listener.machines.impl.ItemShop;
import com.liphium.elfhunt.listener.machines.impl.PresentReceiver;
import com.liphium.elfhunt.screens.ItemShopScreen;
import com.liphium.elfhunt.util.LocationAPI;
import com.liphium.elfhunt.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
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
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class IngameState extends GameState {
    private final ArrayList<DroppableTrap> traps = new ArrayList<>();

    private Runnable runnable;

    public IngameState() {
        super("In game", 30);
    }

    private int presentsLeft = 0;
    private final HashMap<String, PresentReceiver> receivers = new HashMap<>();
    private final HashMap<Location, Boolean> placedBlocks = new HashMap<>();
    private final HashMap<Player, String> currentDelivery = new HashMap<>();

    @Override
    public void start() {

        // Change the amount of presents based on team size
        final var hunterSize = Elfhunt.getInstance().getGameManager().getTeamManager().getTeam("Elves").getPlayers().size();
        int maxPresents = hunterSize * 10; // 10 per member of the team seems fine for 15 minutes
        presentsLeft = maxPresents;

        // Give every single present receiver a random name
        for(PresentReceiver receiver : Elfhunt.getInstance().getMachineManager().getMachines(PresentReceiver.class)) {
            var name = PresentReceiver.randomName();
            while(receivers.containsKey(name)) {
                name = PresentReceiver.randomName();
            }
            receiver.assignName(name);
            receivers.put(name, receiver);
        }

        for (Team team : Elfhunt.getInstance().getGameManager().getTeamManager().getTeams()) {
            team.sendStartMessage();

            for (Player player : team.getPlayers()) {
                player.getInventory().clear();
                team.giveKit(player, true);
            }
        }

        Elfhunt.getInstance().getTaskManager().inject(runnable = new Runnable() {
            int tickCount = 0;
            int countdown = 20 * 60 * 15; // 15 minutes in ticks

            @Override
            public void run() {
                Elfhunt.getInstance().getGameManager().getTeamManager().tick();
                Elfhunt.getInstance().getMachineManager().tick();

                if (tickCount++ >= 20) {
                    tickCount = 0;

                    // Check if the win condition for the hunters is met
                    if(countdown == 0) {
                        handleWin(Elfhunt.getInstance().getGameManager().getTeamManager().getTeam("Hunters"));
                        return;
                    }

                    Messages.actionBar(Component.text(maxPresents, NamedTextColor.GREEN)
                            .append(Component.text("/", NamedTextColor.GRAY))
                            .append(Component.text(presentsLeft, NamedTextColor.GREEN))
                            .appendSpace()
                            .append(Component.text("remaining", NamedTextColor.GRAY))
                            .appendSpace()
                            .append(Component.text("|", NamedTextColor.DARK_GRAY))
                            .appendSpace()
                            .append(Component.text(formatTicks(countdown)).appendSpace()
                                    .append(Component.text("left", NamedTextColor.GRAY)))
                    );

                    countdown--;
                }

            }
        });
    }

    private String formatTicks(int ticks) {
        int totalSeconds = ticks / 20; // 20 ticks = 1 second
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format("§a%02d§7:§a%02d", minutes, seconds);
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
            if (usedItem.getType().equals(Material.TRIPWIRE_HOOK) && event.getClickedBlock() != null) {
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

    private final ArrayList<String> messages = new ArrayList<>(List.of(
            "§7How dare you deliver this to me? This present is for §a%player%§7!",
            "§7Are you too stupid to read? The name on the present clearly says §a%player%§7!",
            "§7Why are you giving me this? It’s clearly for §a%player%§7!",
            "§7I don’t want this! This belongs to §a%player%§7!",
            "§7Is there something wrong with your eyes? This is meant for §a%player%§7!",
            "§7I’m not §a%player%§7! Take this to the right person!",
            "§7Seriously? This is for §a%player%§7, not me!",
            "§7You’ve got the wrong person! This is for §a%player%§7!",
            "§7Don’t waste my time! This clearly says it’s for §a%player%§7!",
            "§7I think you’re lost — this is meant for §a%player%§7!",
            "§7Stop being careless! This is for §a%player%§7, not me!",
            "§7Do I look like §a%player%§7 to you? Are you blind?",
            "§7This isn’t mine — it’s for §a%player%§7!",
            "§7Take a closer look. This belongs to §a%player%§7!",
            "§7How can you mix this up? It’s clearly for §a%player%§7!",
            "§7Not my name on the present—it’s §a%player%§7’s!",
            "§7This isn’t funny. Give this to §a%player%§7!",
            "§7Read the label! It’s for §a%player%§7!",
            "§7Stop bothering me and give this to §a%player%§7!",
            "§7I’m not the recipient! This is meant for §a%player%§7!",
            "§7You’ve made a mistake—this belongs to §a%player%§7!",
            "§7Clearly, you didn’t read the tag. This is for §a%player%§7!"
    ));


    public void onReceiverClicked(Player player, String name) {
        if(Elfhunt.getInstance().getGameManager().getTeamManager().getTeam(player) instanceof ElfTeam team) {
            if(Objects.equals(currentDelivery.get(player), name)) {
                player.getInventory().remove(Material.RED_WOOL);
                presentsLeft -= 1;
                currentDelivery.remove(player);

                // Send an announcement that a present has been delivered
                Bukkit.broadcast(Component.text(" "));
                Bukkit.broadcast(Component.text("§a§l" + player.getName() + " §7delivered a §apresent§7!"));
                Bukkit.broadcast(Component.text(" "));

                // Check if the win condition for the elves is met
                if(presentsLeft <= 0) {
                    handleWin(team);
                }
            } else {
                var message = messages.get(ThreadLocalRandom.current().nextInt(messages.size() - 1));
                message = message.replace("%player%", player.getName());
                player.sendMessage(Component.text("§f§l" + name + "§7: " + message));
            }
        }
    }

    public void onGiverClicked(Player player) {
        if(Elfhunt.getInstance().getGameManager().getTeamManager().getTeam(player) instanceof ElfTeam) {
            if(currentDelivery.containsKey(player)) {
                Bukkit.broadcast(Component.text("§c§lSanta§7: I already gave you a present!"));
                return;
            }

            // Get a random receiver to bring the present to
            final var randomInt = ThreadLocalRandom.current().nextInt(receivers.size() - 1);
            final var receiver = receivers.keySet().stream().toList().get(randomInt);

            // Assign that receiver for the player
            currentDelivery.put(player, receiver);
            final var present = new ItemStackBuilder(Material.RED_WOOL)
                    .withName(Component.text("Present", NamedTextColor.RED))
                    .buildStack();
            player.getInventory().addItem(present);
            Bukkit.broadcast(Elfhunt.PREFIX.append(Component.text("§c§lSanta§7: Bring this present to §c" + receiver + "§7!")));
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
        final var player = event.getPlayer();

        event.setKeepInventory(true);
        event.deathMessage(null);
        event.setKeepLevel(true);

        player.setGameMode(GameMode.SPECTATOR);

        if (player.getKiller() != null) {
            Bukkit.broadcast(Elfhunt.PREFIX.append(Component.text("§c" + player.getName() + " §7was killed by §c§l" + player.getKiller().getName() + "§7!")));
        } else
            Bukkit.broadcast(Elfhunt.PREFIX.append(Component.text("§c§l" + player.getKiller().getName() + " §7died!")));

        // Make sure the player isn't still delivering
        currentDelivery.remove(player);

        Elfhunt.getInstance().getTaskManager().inject(new Runnable() {
            int tickCount = 0;
            @Override
            public void run() {
                if(tickCount++ >= 1) {
                    if(player.isDead()) {
                        player.spigot().respawn();
                        player.getInventory().clear();
                        player.setHealth(20);
                    }
                    Elfhunt.getInstance().getTaskManager().uninject(this);
                }
            }
        });
    }

    @Override
    public void onRespawn(PlayerRespawnEvent event) {
        final var team = Elfhunt.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());
        if(team instanceof HunterTeam) {
            event.setRespawnLocation(Objects.requireNonNull(LocationAPI.getLocation("Hunters")));
        } else {
            event.setRespawnLocation(Objects.requireNonNull(LocationAPI.getLocation("Elves")));
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
