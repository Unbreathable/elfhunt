package com.liphium.elfhunt.game.state;

import com.liphium.core.util.ItemStackBuilder;
import com.liphium.elfhunt.Elfhunt;
import com.liphium.elfhunt.game.GameState;
import com.liphium.elfhunt.game.team.Team;
import com.liphium.elfhunt.game.team.impl.ElfTeam;
import com.liphium.elfhunt.game.team.impl.HunterTeam;
import com.liphium.elfhunt.listener.machines.impl.PresentReceiver;
import com.liphium.elfhunt.screens.ItemShopScreen;
import com.liphium.elfhunt.util.LocationAPI;
import com.liphium.elfhunt.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
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

        // World cleanup
        for (Entity entity : Objects.requireNonNull(Bukkit.getWorld("elfhunt")).getEntities()) {
            if (entity.getType() != EntityType.ARMOR_STAND && entity.getType() != EntityType.PLAYER) entity.remove();
        }

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
                player.setHealth(20);
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
                    if(countdown <= 0) {
                        handleWin(Elfhunt.getInstance().getGameManager().getTeamManager().getTeam("Hunters"));
                        return;
                    }

                    Messages.actionBar(Component.text(presentsLeft, NamedTextColor.GREEN)
                            .append(Component.text("/", NamedTextColor.GRAY))
                            .append(Component.text(maxPresents, NamedTextColor.GREEN))
                            .appendSpace()
                            .append(Component.text("remaining", NamedTextColor.GRAY))
                            .appendSpace()
                            .append(Component.text("|", NamedTextColor.DARK_GRAY))
                            .appendSpace()
                            .append(Component.text(formatTicks(countdown)).appendSpace()
                                    .append(Component.text("left", NamedTextColor.GRAY)))
                    );
                }

                countdown--;
            }
        });
    }

    private String formatTicks(int ticks) {
        int totalSeconds = ticks / 20; // 20 ticks = 1 second
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format("§a%02d§7:§a%02d", minutes, seconds);
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.WIND_CHARGE) {
            return;
        }
        Elfhunt.getInstance().getMachineManager().onInteract(event);

        if (event.getItem() != null) {
            Team team = Elfhunt.getInstance().getGameManager().getTeamManager().getTeam(event.getPlayer());
            ItemStack usedItem = event.getItem();
            if (usedItem.getType().equals(Material.GRAY_DYE) && event.getClickedBlock() != null) {
                traps.add(new SlowTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
                reduceMainHandItem(event.getPlayer());
            } else if (usedItem.getType().equals(Material.GREEN_DYE) && event.getClickedBlock() != null) {
                traps.add(new PoisonTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
                reduceMainHandItem(event.getPlayer());
            } else if (usedItem.getType().equals(Material.FEATHER) && event.getClickedBlock() != null) {
                traps.add(new FlyTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
                reduceMainHandItem(event.getPlayer());
            } else if (usedItem.getType().equals(Material.LIGHT_BLUE_DYE) && event.getClickedBlock() != null) {
                traps.add(new FreezeTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
                reduceMainHandItem(event.getPlayer());
            }  else if (usedItem.getType().equals(Material.WHITE_DYE) && event.getClickedBlock() != null) {
                traps.add(new WebTrap(event.getClickedBlock().getLocation().clone().add(0.5, 1, 0.5), team));
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

            // Make sure the guy actually has a present
            if(!currentDelivery.containsKey(player)) {
                player.sendMessage(Component.text("§f§l" + name + "§7: You don't even have a present! Get one from §cSanta §7first."));
                return;
            }

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
                var message = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
                message = message.replace("%player%", currentDelivery.get(player));
                player.sendMessage(Component.text("§f§l" + name + "§7: " + message));
            }
        }
    }

    public void onGiverClicked(Player player) {
        if(Elfhunt.getInstance().getGameManager().getTeamManager().getTeam(player) instanceof ElfTeam) {
            if(currentDelivery.containsKey(player)) {
                player.sendMessage(Component.text("§c§lSanta§7: I already gave you a present!"));
                return;
            }

            // Get a random receiver to bring the present to
            final var randomInt = ThreadLocalRandom.current().nextInt(receivers.size());
            final var receiver = receivers.keySet().stream().toList().get(randomInt);

            // Assign that receiver for the player
            currentDelivery.put(player, receiver);
            final var present = new ItemStackBuilder(Material.RED_WOOL)
                    .withName(Component.text("Present", NamedTextColor.RED))
                    .buildStack();
            player.getInventory().addItem(present);
            player.sendMessage(Component.text("§c§lSanta§7: Bring this present to §c" + receiver + "§7!"));
        }
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Kill the player in case they fell down
        if(event.getPlayer().getLocation().getY() <= 180) {
            event.getPlayer().setHealth(0);
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
        event.setCancelled(false);
    }

    @Override
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if(event.getEntity().getType() == EntityType.ARMOR_STAND) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().clear();
    }

    @Override
    public void onPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType().equals(Material.REDSTONE_TORCH)) {
            event.setCancelled(true);
            return;
        }

        if(event.getBlockPlaced().getLocation().getY() >= 220) {
            event.setCancelled(true);
            return;
        }

        // Place a machine if it is one
        final var machine = Elfhunt.getInstance().getMachineManager().newMachineByMaterial(event.getBlockPlaced().getType(), event.getBlockPlaced().getLocation());
        if (machine != null) {
            Elfhunt.getInstance().getMachineManager().addMachine(machine);
        } else {

            // Add the block as placed otherwise
            placedBlocks.put(event.getBlock().getLocation(), true);
        }
    }

    final List<Material> grassTypes = Arrays.asList(
            Material.TALL_GRASS, Material.SHORT_GRASS, Material.CORNFLOWER,
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
            Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
            Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY, Material.SUNFLOWER,
            Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
            Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE, Material.COBWEB
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

        if (player.getKiller() != null) {
            Bukkit.broadcast(Elfhunt.PREFIX.append(Component.text("§a" + player.getName() + " §7was killed by §a§l" + player.getKiller().getName() + "§7!")));
        } else
            Bukkit.broadcast(Elfhunt.PREFIX.append(Component.text("§a§l" + player.getName() + " §7died!")));

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
        team.giveKit(event.getPlayer(), false);
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

        // Make sure the team loses if there are no players left
        if(team.getPlayers().isEmpty()) {
            if(team instanceof HunterTeam) {
                handleWin(Elfhunt.getInstance().getGameManager().getTeamManager().getTeam("Elves"));
            } else {
                handleWin(Elfhunt.getInstance().getGameManager().getTeamManager().getTeam("Hunters"));
            }
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
            super(location, team, Material.GRAY_DYE);
        }

        @Override
        public void onEnter(Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 4));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
        }
    }

    public static class PoisonTrap extends DroppableTrap {

        PoisonTrap(Location location, Team team) {
            super(location, team, Material.GREEN_DYE);
        }

        @Override
        public void onEnter(Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
        }
    }

    public static class FreezeTrap extends DroppableTrap {

        FreezeTrap(Location location, Team team) {
            super(location, team, Material.LIGHT_BLUE_DYE);
        }

        @Override
        public void onEnter(Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 255, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 128, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
        }
    }

    public static class FlyTrap extends DroppableTrap {

        FlyTrap(Location location, Team team) {
            super(location, team, Material.FEATHER);
        }

        @Override
        public void onEnter(Player player) {
            player.setVelocity(new Vector(0, 3, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
        }
    }

    public static class WebTrap extends DroppableTrap {

        WebTrap(Location location, Team team) {
            super(location, team, Material.WHITE_DYE);
        }

        @Override
        public void onEnter(Player player) {
            // Place 5 blocks of webs around the location
            final var main = location.clone().getBlock();
            main.setType(Material.COBWEB);
            main.getRelative(BlockFace.EAST).setType(Material.COBWEB);
            main.getRelative(BlockFace.WEST).setType(Material.COBWEB);
            main.getRelative(BlockFace.NORTH).setType(Material.COBWEB);
            main.getRelative(BlockFace.SOUTH).setType(Material.COBWEB);
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 300, 0));
        }
    }
}
