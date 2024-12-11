package com.liphium.elfhunt.screens;

import com.liphium.core.inventory.CClickEvent;
import com.liphium.core.inventory.CItem;
import com.liphium.core.inventory.CScreen;
import com.liphium.core.util.ItemStackBuilder;
import com.liphium.elfhunt.Elfhunt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class ItemShopScreen extends CScreen {

    public ItemShopScreen() {
        super(3, Component.text("Item shop", NamedTextColor.GOLD, TextDecoration.BOLD), 4, false);
    }

    @Override
    public void init(Player player, Inventory inventory) {
        background(player);

        // Add all the categories
        for (ShopCategory category : ShopCategory.values()) {
            setItemNotCached(player, 10 + category.ordinal(), new CItem(category.getStack())
                    .onClick(event -> openCategory(event, category, inventory)));
        }
    }

    public void openCategory(CClickEvent event, ShopCategory category, Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            if (category.getItems().size() <= i) {
                setItemNotCached(event.getPlayer(), 18 + i, ShopCategory.spacer(), inventory);
            } else {
                setItemNotCached(event.getPlayer(), 18 + i, category.getItems().get(i), inventory);
            }
        }
    }

    public static void removeAmountFromInventory(Player player, Material material, int amount) {
        int count = amount;
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == material) {
                int sub = Math.min(item.getAmount(), count);
                int newAmount = item.getAmount() - sub;
                item.setAmount(newAmount);
                count -= sub;
                if (count <= 0) {
                    break;
                }
            }
        }
    }

    public enum ShopCategory {
        WEAPONS(
                new ItemStackBuilder(Material.IRON_SWORD)
                        .withName(Component.text("Weapons", NamedTextColor.RED, TextDecoration.BOLD))
                        .withLore(Component.text("Swords, bows, and more.", NamedTextColor.GRAY))
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.MACE, "Mace", NamedTextColor.RED, 25, 1),
                        itemWithPrice(Material.WIND_CHARGE, "Wind charge", NamedTextColor.RED, 20, 5),
                        itemWithPrice(Material.BOW, "Bow", NamedTextColor.RED, 10, 1),
                        itemWithPriceCustom(new ItemStackBuilder(Material.BOW)
                                .withName(Component.text("Punch Bow", NamedTextColor.RED))
                                .withEnchantments(Map.of(Enchantment.PUNCH, 1))
                                .buildStack(), 20
                        ),
                        itemWithPriceCustom(new ItemStackBuilder(Material.BOW)
                                .withName(Component.text("More Punch Bow", NamedTextColor.RED))
                                .withEnchantments(Map.of(Enchantment.PUNCH, 2, Enchantment.INFINITY, 1))
                                .buildStack(), 40
                        ),
                        itemWithPrice(Material.ARROW, "Arrow", NamedTextColor.RED, 15, 3),
                        itemWithPrice(Material.BOW, "Crossbow", NamedTextColor.RED, 20, 1),
                        itemWithPriceCustom(new ItemStackBuilder(Material.FIREWORK_ROCKET)
                                .withName(Component.text("Rocket", NamedTextColor.RED))
                                .withAmount(3)
                                .buildStack(), 30
                        )
                )
        ),
        DEFENSE(
                new ItemStackBuilder(Material.SHIELD)
                        .withName(Component.text("Defense", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .withLore(Component.text("Turrets and traps.", NamedTextColor.GRAY))
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.TORCH, "Torch", NamedTextColor.GREEN, 4, 1),
                        spacer(),
                        itemWithPrice(Material.IRON_INGOT, "Iron ingot", NamedTextColor.GREEN, 5, 1),
                        itemWithPrice(Material.DIAMOND, "Diamond", NamedTextColor.GREEN, 10, 1),
                        itemWithPrice(Material.MACE, "Mace", NamedTextColor.WHITE, 25, 1),
                        spacer(),
                        itemWithPrice(Material.RED_WOOL, "Blood Garlic dropper", NamedTextColor.GREEN, 40, 1),
                        itemWithPrice(Material.BEACON, "Torch dropper", NamedTextColor.GREEN, 50, 1)
                )
        ),
        TOOLS(
                new ItemStackBuilder(Material.IRON_PICKAXE)
                        .withName(Component.text("Tools & weapons", NamedTextColor.WHITE, TextDecoration.BOLD))
                        .withLore(Component.text("To aid you in the fight.", NamedTextColor.GRAY))
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.IRON_SHOVEL, "Iron shovel", NamedTextColor.WHITE, 4, 1),
                        itemWithPriceCustom(
                                new ItemStackBuilder(Material.GOLDEN_SHOVEL)
                                        .withName(Component.text("Golden shovel", NamedTextColor.WHITE))
                                        .withEnchantments(Map.of(Enchantment.EFFICIENCY, 2))
                                        .buildStack(), 10
                        )
                        //itemWithPrice(Material.WIND_CHARGE, "Wind charge", NamedTextColor.WHITE, 20, 5)
                )
        ),
        BUILDING(
                new ItemStackBuilder(Material.STONE)
                        .withName(Component.text("Building", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .withLore(Component.text("Upgrade your base.", NamedTextColor.GRAY))
                        .buildStack(),
                List.of(
                        itemWithPrice(Material.PUMPKIN, "Pumpkin dropper", NamedTextColor.GOLD, 25, 1),
                        itemWithPrice(Material.BREWING_STAND, "Brewer", NamedTextColor.GOLD, 100, 1),
                        itemWithPrice(Material.ARMOR_STAND, "Pop-up item shop", NamedTextColor.GOLD, 50, 1),
                        spacer(),
                        itemWithPrice(Material.TRIPWIRE_HOOK, "Alarm trap", NamedTextColor.GOLD, 20, 1),
                        itemWithPrice(Material.GLOWSTONE_DUST, "Glow trap", NamedTextColor.GOLD, 30, 1)
                )
        );

        final ItemStack stack;
        final List<CItem> items;

        ShopCategory(ItemStack stack, List<CItem> items) {
            this.stack = stack;
            this.items = items;
        }

        public ItemStack getStack() {
            return stack;
        }

        public List<CItem> getItems() {
            return items;
        }

        private static ItemStack item = new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).withName(Component.text("§r")).buildStack();

        public static CItem spacer() {
            return new CItem(item).notClickable();
        }

        public static CItem itemWithPrice(Material material, String name, NamedTextColor color, int price, int amount) {
            return new CItem(new ItemStackBuilder(material).withName(Component.text(name, color))
                    .withLore(Component.text("Price: ", NamedTextColor.GRAY).append(Component.text(price, NamedTextColor.GOLD)))
                    .buildStack()
            ).onClick(event -> buyFunction(event, new ItemStackBuilder(material).withName(Component.text(name, color)).withAmount(amount).buildStack(), price));
        }

        public static CItem itemWithPriceCustom(ItemStack sold, int price) {
            return new CItem(new ItemStackBuilder(sold.getType()).withName(sold.getItemMeta().displayName())
                    .withLore(Component.text("Price: ", NamedTextColor.GRAY).append(Component.text(price, NamedTextColor.GOLD)))
                    .withEnchantments(sold.getEnchantments())
                    .buildStack()
            ).onClick(event -> buyFunction(event, sold, price));
        }

        public static void buyFunction(CClickEvent event, ItemStack stack, int price) {
            // Get the amount of pumpkins in the inventory
            int count = 0;
            for (ItemStack item : event.getPlayer().getInventory()) {
                if (item != null && item.getType() == Material.CARVED_PUMPKIN) {
                    count += item.getAmount();
                }
            }

            if (count < price) {
                event.getPlayer().sendMessage(Elfhunt.PREFIX.append(Component.text("You don't have enough pumpkins to purchase this item.", NamedTextColor.RED)));
                event.getPlayer().closeInventory();
                return;
            }

            // Remove the pumpkins from the players inventory
            removeAmountFromInventory(event.getPlayer(), Material.CARVED_PUMPKIN, price);

            event.getPlayer().getInventory().addItem(stack);
        }
    }
}
