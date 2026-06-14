# MiningBoost

A Paper plugin for Minecraft 26.1.* that makes mining faster while you hold a tool.
Enchanted tools get an extra nudge on top. Everything is configurable.

By [kezdev](https://github.com/kezdev).

## What it does

Two separate boosts, both set in `config.yml`:

- **break-speed-boost** — speeds up *all* mining with a tool (the base tool speed and any Efficiency).
- **efficiency-boost** — an extra boost applied to *only* the Efficiency enchantment's bonus.

`break-speed-boost` multiplies the whole break speed, so the Efficiency part gets
**both** boosts while the base tool speed only gets the first one. That means
Efficiency-enchanted tools always come out a bit further ahead.

### Example

A Diamond Pickaxe with Efficiency I mining stone, using the defaults (`break-speed-boost: 1.5`, `efficiency-boost: 0.05`):

```
base tool speed         8
Efficiency I bonus      2        (vanilla: level² + 1)
+ efficiency-boost      2 × 1.05 = 2.1
subtotal                8 + 2.1  = 10.1
+ break-speed-boost     10.1 × 2.5 = 25.25
```

Vanilla speed would be `8 + 2 = 10`, so it ends up about **2.5× faster**.

## Configuring

Values are multipliers: `0.05` = +5%, `1.5` = +150%, `0` = off.

```yaml
break-speed-boost: 1.5
efficiency-boost: 0.05
tools:
  pickaxe: true
  axe: true
  shovel: true
  hoe: true
  shears: true
```

Turn off a tool type by setting it to `false`. The live config lives at
`<server>/plugins/MiningBoost/config.yml`. Edit it, then run `/miningboost reload`
in-game — no restart needed.

## Commands

- `/miningboost` — show the current boosts and which tools are enabled.
- `/miningboost reload` — reload `config.yml` (needs op / `miningboost.reload`).

## Building

Requires JDK 25. In IntelliJ, run the Gradle **`deployToServer`** task — it builds the
jar and copies it into your server's `plugins/` folder, then restart the server.

Set a different deploy target with `-PserverPluginsDir=/path/to/plugins` if needed.

## License

Released under the [MIT License](LICENSE) © 2026 kezdev.
