package com.ommods.openmodularturrets.content.item;

import com.ommods.openmodularturrets.content.turret.AmmoType;
import com.ommods.openmodularturrets.content.turret.TurretUpgrades;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OMT items. Ammo is a plain item consumed per shot from the base's ammo slots; a head's
 * {@link AmmoType} selects which one. The potato cannon uses vanilla potatoes and the disposable
 * turret throws whatever ammo is loaded ({@link AmmoType#ANY}), so those are resolved specially rather
 * than through a dedicated item.
 */
public final class OMTItems {

    public static ItemEntry<Item> AMMO_BULLET;
    public static ItemEntry<Item> AMMO_GRENADE;
    public static ItemEntry<Item> AMMO_ROCKET;
    public static ItemEntry<Item> AMMO_BLAZING_CLAY;
    public static ItemEntry<Item> AMMO_FERRO_SLUG;

    // 1.12 usable_meta 0 and 1. Inert items with no recipe and no use of their own; they exist only so
    // the projectile renderers have a model to draw a round in flight with. Registered but kept out of
    // the creative tab, unlike 1.12, because here they are pure render props.
    public static ItemEntry<Item> THROWABLE_BULLET;
    public static ItemEntry<Item> THROWABLE_GRENADE;

    // Upgrades (base slots 11, 12).
    public static ItemEntry<UpgradeItem> UPGRADE_ACCURACY;
    public static ItemEntry<UpgradeItem> UPGRADE_EFFICIENCY;
    public static ItemEntry<UpgradeItem> UPGRADE_FIRE_RATE;
    public static ItemEntry<UpgradeItem> UPGRADE_RANGE;
    public static ItemEntry<UpgradeItem> UPGRADE_SCATTER_SHOT;

    // Addons (base slots 9, 10). Serial port stays unported (it needs a computer mod); potentia was
    // dead content in 1.12 (no recipe, no logic) and is left out with it.
    public static ItemEntry<AddonItem> ADDON_CONCEALER;
    public static ItemEntry<AddonItem> ADDON_DAMAGE_AMP;
    public static ItemEntry<AddonItem> ADDON_RECYCLER;
    public static ItemEntry<AddonItem> ADDON_REDSTONE_REACTOR;
    public static ItemEntry<AddonItem> ADDON_SOLAR_PANEL;
    public static ItemEntry<AddonItem> ADDON_FAKE_DROPS;

    // The Turret Memory Card (1.12 usable_meta 2), copying settings between bases.
    public static ItemEntry<MemoryCardItem> MEMORY_CARD;

    // Crafting intermediates, faithful to 1.12 IntermediateProductRegular/Tiered: the IO Bus plus a
    // sensor, chamber and barrel for each of the five tiers. Pure crafting components with no behavior.
    public static ItemEntry<Item> IO_BUS;
    public static final Map<Integer, ItemEntry<Item>> SENSORS = new HashMap<>();
    public static final Map<Integer, ItemEntry<Item>> CHAMBERS = new HashMap<>();
    public static final Map<Integer, ItemEntry<Item>> BARRELS = new HashMap<>();

    private OMTItems() {
    }

    public static void register(Registrate registrate) {
        AMMO_BULLET = registrate.item("ammo_bullet",
                (Item.Properties p) -> (Item) new AmmoItem(p, "A simple bullet for use with the machine gun turret."))
                .lang("Ammo - Bullet").register();
        AMMO_GRENADE = registrate.item("ammo_grenade",
                (Item.Properties p) -> (Item) new AmmoItem(p, "Nice grenade for the grenade launcher."))
                .lang("Ammo - Grenade").register();
        AMMO_ROCKET = registrate.item("ammo_rocket",
                (Item.Properties p) -> (Item) new AmmoItem(p, "Rockets are being used by the rocket launcher, obviously!"))
                .lang("Ammo - Rocket").register();
        AMMO_BLAZING_CLAY = registrate.item("ammo_blazing_clay",
                (Item.Properties p) -> (Item) new AmmoItem(p, "Blazing clay for the Incendiary Turret"))
                .lang("Ammo - Blazing Clay").register();
        AMMO_FERRO_SLUG = registrate.item("ammo_ferro_slug",
                (Item.Properties p) -> (Item) new AmmoItem(p, "This advanced looking slug is for the railgun."))
                .lang("Ammo - Ferro-Magnetic Slug").register();

        THROWABLE_BULLET = registrate.item("throwable_bullet", Item::new).lang("Bullet").register();
        THROWABLE_GRENADE = registrate.item("throwable_grenade", Item::new).lang("Grenade").register();

        UPGRADE_ACCURACY = upgrade(registrate, "upgrade_accuracy", TurretUpgrades.UP_ACCURACY, "Upgrade - Accuracy");
        UPGRADE_EFFICIENCY = upgrade(registrate, "upgrade_efficiency", TurretUpgrades.UP_EFFICIENCY, "Upgrade - Efficiency");
        UPGRADE_FIRE_RATE = upgrade(registrate, "upgrade_fire_rate", TurretUpgrades.UP_FIRE_RATE, "Upgrade - Fire Rate");
        UPGRADE_RANGE = upgrade(registrate, "upgrade_range", TurretUpgrades.UP_RANGE, "Upgrade - Range");
        UPGRADE_SCATTER_SHOT = upgrade(registrate, "upgrade_scatter_shot", TurretUpgrades.UP_SCATTER, "Upgrade - Scatter Shot");

        ADDON_CONCEALER = addon(registrate, "addon_concealer", TurretUpgrades.AD_CONCEALER, "Addon - Turret Concealer");
        ADDON_DAMAGE_AMP = addon(registrate, "addon_damage_amp", TurretUpgrades.AD_DAMAGE_AMP, "Addon - Damage Boost");
        ADDON_RECYCLER = addon(registrate, "addon_recycler", TurretUpgrades.AD_RECYCLER, "Addon - Recycler");
        ADDON_REDSTONE_REACTOR = addon(registrate, "addon_redstone_reactor", TurretUpgrades.AD_REDSTONE, "Addon - Redstone Reactor");
        ADDON_SOLAR_PANEL = addon(registrate, "addon_solar_panel", TurretUpgrades.AD_SOLAR, "Addon - Solar Panel");
        ADDON_FAKE_DROPS = addon(registrate, "addon_fake_drops", TurretUpgrades.AD_FAKEDROPS, "Addon - Fake Drops");

        MEMORY_CARD = registrate.item("memory_card", MemoryCardItem::new)
                .lang("Turret Memory Card").register();

        IO_BUS = registrate.item("io_bus", Item::new).lang("IO Bus").register();
        for (int tier = 1; tier <= 5; tier++) {
            SENSORS.put(tier, intermediate(registrate, "sensor", tier));
            CHAMBERS.put(tier, intermediate(registrate, "chamber", tier));
            BARRELS.put(tier, intermediate(registrate, "barrel", tier));
        }
    }

    // 1.12 named these "Sensor (Tier 1)" etc.; keep the display names verbatim.
    private static ItemEntry<Item> intermediate(Registrate registrate, String kind, int tier) {
        String label = Character.toUpperCase(kind.charAt(0)) + kind.substring(1) + " (Tier " + tier + ")";
        return registrate.item(kind + "_tier" + tier, Item::new).lang(label).register();
    }

    private static ItemEntry<UpgradeItem> upgrade(Registrate registrate, String name, int meta, String label) {
        return registrate.item(name, props -> new UpgradeItem(props, meta)).lang(label).register();
    }

    private static ItemEntry<AddonItem> addon(Registrate registrate, String name, int meta, String label) {
        return registrate.item(name, props -> new AddonItem(props, meta)).lang(label).register();
    }

    /** The registered upgrade and addon items, in creative-tab order. */
    public static List<ItemEntry<? extends Item>> modifiers() {
        return List.of(
                UPGRADE_ACCURACY, UPGRADE_EFFICIENCY, UPGRADE_FIRE_RATE, UPGRADE_RANGE, UPGRADE_SCATTER_SHOT,
                ADDON_CONCEALER, ADDON_DAMAGE_AMP, ADDON_RECYCLER, ADDON_REDSTONE_REACTOR,
                ADDON_SOLAR_PANEL, ADDON_FAKE_DROPS);
    }

    /** The crafting intermediates in creative-tab order: IO Bus, then sensor/chamber/barrel per tier. */
    public static List<ItemEntry<Item>> intermediates() {
        List<ItemEntry<Item>> all = new ArrayList<>();
        all.add(IO_BUS);
        for (int tier = 1; tier <= 5; tier++) {
            all.add(SENSORS.get(tier));
            all.add(CHAMBERS.get(tier));
            all.add(BARRELS.get(tier));
        }
        return all;
    }

    /**
     * The specific item an ammo type consumes, or null when there is no single item: NONE (energy
     * only) and ANY (the disposable turret, which takes whatever is loaded).
     */
    @Nullable
    public static Item itemFor(AmmoType ammo) {
        return switch (ammo) {
            case BULLET -> AMMO_BULLET.get();
            case GRENADE -> AMMO_GRENADE.get();
            case ROCKET -> AMMO_ROCKET.get();
            case BLAZING_CLAY -> AMMO_BLAZING_CLAY.get();
            case FERRO_SLUG -> AMMO_FERRO_SLUG.get();
            case POTATO -> Items.POTATO;
            case NONE, ANY -> null;
        };
    }

    /**
     * True if the item may be loaded into a base's ammo slots (any turret's accepted ammo). Cobblestone
     * and planks are here because they are the 1.12 default {@code stringAmmoAllowList}, the disposable
     * turret's advertised ammo; 1.12 added planks as a wildcard-meta entry, so the whole tag counts.
     */
    public static boolean isAmmoItem(Item item) {
        return item == AMMO_BULLET.get()
                || item == AMMO_GRENADE.get()
                || item == AMMO_ROCKET.get()
                || item == AMMO_BLAZING_CLAY.get()
                || item == AMMO_FERRO_SLUG.get()
                || item == Items.POTATO
                || item == Items.COBBLESTONE
                || item.builtInRegistryHolder().is(ItemTags.PLANKS);
    }

    /**
     * True for the five crafted turret rounds, this port's stand in for 1.12's {@code AmmoMetaItem}.
     * The disposable turret checks this to leave another head's ammo alone: 1.12's disposable ammo
     * search skips anything that is an AmmoMetaItem and throws only the junk.
     */
    public static boolean isCraftedAmmoItem(Item item) {
        return item == AMMO_BULLET.get()
                || item == AMMO_GRENADE.get()
                || item == AMMO_ROCKET.get()
                || item == AMMO_BLAZING_CLAY.get()
                || item == AMMO_FERRO_SLUG.get();
    }

    /** The upgrade/addon slot contents for a stack, or null if it is not a modifier item or is empty. */
    @Nullable
    public static TurretUpgrades.Slot slotFor(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof UpgradeItem up) {
            return new TurretUpgrades.Slot(up.meta(), stack.getCount());
        }
        if (stack.getItem() instanceof AddonItem addon) {
            return new TurretUpgrades.Slot(addon.meta(), stack.getCount());
        }
        return null;
    }

    /** True if the item belongs in a base upgrade slot (11, 12). */
    public static boolean isUpgradeItem(Item item) {
        return item instanceof UpgradeItem;
    }

    /** True if the item belongs in a base addon slot (9, 10). */
    public static boolean isAddonItem(Item item) {
        return item instanceof AddonItem;
    }

    /**
     * True for the two addons that work in levels and so stack four to a slot: the damage amp and
     * fake drops. 1.12 keys this off the addon metas 1 and 7 in {@code AddonSlot.getItemStackLimit};
     * every other addon is one per slot.
     */
    public static boolean isLevelledAddon(Item item) {
        return item == ADDON_DAMAGE_AMP.get() || item == ADDON_FAKE_DROPS.get();
    }
}
