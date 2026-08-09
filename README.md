<div align="center">

  <h1>Open Modular Turrets</h1>

  <p>Modular, upgradeable turrets that defend your base for Minecraft 1.20.1.</p>

</div>

A 1.20.1 port of Open Modular Turrets by the OpenModularTurrets Team (Poenjabiesous, UntouchedWagons,
Keridos).

## Features

- **Ten turret heads.** Disposable, Potato Cannon, Machine Gun, Incendiary, Grenade, Rocket, Laser,
  Railgun, Relativistic, and Teleporter. Each carries its own range, fire rate, damage, energy cost and
  accuracy, and the projectile turrets throw real flying rounds you can watch fly.
- **Five base tiers.** A turret base holds the energy, the ammo and the targeting rules; the head sits
  on top of it. Tier 1 stores 500 FE and carries one head, tier 5 stores 10,000,000 FE and carries four.
- **Five upgrades and six addons.** Accuracy, Efficiency, Fire Rate, Range and Scatter Shot upgrades;
  Concealer, Damage Boost, Recycler, Redstone Reactor, Solar Panel and Fake Drops addons. Upgrades
  stack four to a slot; addons take one slot each, except Damage Boost and Fake Drops, which also
  stack to four and get stronger as they do.
- **Targeting you control.** Per-base toggles for mobs, neutrals and players, a trust list so friends
  are not shot on sight, and a global trust list that applies to every base you own.
- **Support blocks.** The Expander for extra upgrade and addon room, the Energizer, the Loot Deleter
  base addon, a lever for redstone control, and the Turret Memory Card for copying a base's settings.
- **Recipe viewer integration.** EMI shows every recipe and the turret exclusion areas.

## Requirements

- Minecraft 1.20.1
- Minecraft Forge 47.4.10 or newer

No other dependencies. OMLib is bundled inside the jar.

## Installation

1. Install Minecraft Forge for 1.20.1.
2. Drop `openmodularturrets-4.0.0.jar` into your `mods` folder.
3. Launch the game. `config/omlib-common.toml` holds the client and startup settings. Turret
   balance and permissions are per world, in `saves/<world>/serverconfig/`, as
   `openmodularturrets-server.toml` and `omlib-server.toml`.

## Getting started

Craft a Turret Base, place it, and right click it to open the base GUI. Feed it energy from any Forge
energy cable, drop ammo in the ammo slots for the turrets that need it, then place a turret head on top
of the base. Open the base's Configure screen to set what it shoots at and who it trusts.

`/omlib trust gui` opens your global trust list, `/omlib share` lets another player operate the blocks
you own, and `/omlib changeowner` hands a block over for good.

## Changes from the original

- The three commands `/omshareowner`, `/omchangeowner` and `/omdebug` are now subcommands of one
  `/omlib` root.
- Three trust packets in the original let any client edit any player's global trust list without a
  permission check. Every trust action is now authorised against the sending connection's player.
- Camouflage, OpenModularPassiveDefense and FloodLights are not ported.
- The Potentia Converter addon is not included. It converted Thaumcraft essentia, had no recipe, and
  could not be obtained.
- The Serial Port addon is not included. It exposed a turret to computer mods that have no 1.20.1
  counterpart here.

## Building

```
./gradlew build
```

The built jar lands in `omt/build/libs/`. Requires JDK 17.

## Credits

- Original Open Modular Turrets, and all of its models, textures, sounds, and design, by the
  **OpenModularTurrets Team**: Poenjabiesous, UntouchedWagons and Keridos.
- 1.20.1 port by **haunterdev**.

## License

[GPL-3.0](LICENSE), following the original mod's published license. The bundled OMLib portion is MIT,
preserved in [LICENSE-OMLIB](LICENSE-OMLIB).
