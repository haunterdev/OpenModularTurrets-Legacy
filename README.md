<div align="center">

  <h1>Open Modular Turrets Legacy</h1>

  <p>Modular turrets and passive defenses for Minecraft 1.20.1.</p>

</div>

A 1.20.1 port of Open Modular Turrets by the OpenModularTurrets Team (Poenjabiesous, UntouchedWagons,
Keridos), together with Open Modular Passive Defense by Keridos.

Client and server side mod.

## Features

- Ten turret heads: Disposable, Potato Cannon, Machine Gun, Incendiary, Grenade, Rocket, Laser,
  Railgun, Relativistic, and Teleporter, each with its own range, fire rate, damage and energy cost.
- Five base tiers: a turret base holds the energy, the ammo and the targeting rules, and the head
  sits on top. Tier 1 stores 500 FE and carries one head, tier 5 stores 10,000,000 FE and carries four.
- Five upgrades and six addons: Accuracy, Efficiency, Fire Rate, Range and Scatter Shot upgrades;
  Concealer, Damage Boost, Recycler, Redstone Reactor, Solar Panel and Fake Drops addons.
- Targeting you control: per-base toggles for mobs, neutrals and players, a trust list for friends,
  and a global trust list for every base you own.
- Support blocks: the Expander, the Energizer, the Loot Deleter, a lever for redstone control, and
  the Turret Memory Card for copying a base's settings.
- JEI, EMI and Jade support.
- Passive defense companion: Open Modular Passive Defense adds five tiers each of hardened
  blocks, walls and damaging fences, plus an owner-only camouflage trap.

## Requirements

- Minecraft 1.20.1
- Forge 47.4.10 or newer

## Installation

Put `openmodularturrets-4.0.0.jar` into your `mods` folder. Add `ompd-4.0.0.jar` for the passive
defense blocks.

## Getting started

Craft a Turret Base, place it, and right click it to open the base GUI. Feed it Forge energy, put ammo
in the ammo slots, then place a turret head on top of the base. The base's Configure screen sets what
it shoots at and who it trusts.

`/omlib trust gui` opens your global trust list, `/omlib share` lets another player use your blocks,
and `/omlib changeowner` hands a block over.

## Changes from the original

- `/omshareowner`, `/omchangeowner` and `/omdebug` are now subcommands of `/omlib`.
- Fixed: any player could edit another player's global trust list.
- FloodLights, the Potentia Converter addon and the Serial Port addon are not included.

## Building

```
./gradlew build
```

The jars land in `omt/build/libs/` and `ompd/build/libs/`. Requires JDK 17.

## Credits

- Original Open Modular Turrets, and all of its models, textures, sounds, and design, by the
  OpenModularTurrets Team: Poenjabiesous, UntouchedWagons and Keridos.
- Original Open Modular Passive Defense by Keridos.
- 1.20.1 port by haunterdev.

## License

[GPL-3.0](LICENSE), the same as the original. The bundled OMLib is MIT, see
[omlib/LICENSE](omlib/LICENSE).
