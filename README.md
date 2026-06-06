# AzLagControl

**Professional lag control suite for Minecraft servers — by AztrixDigitalStudio**

AzLagControl is a modular performance toolkit for Paper/Spigot servers. Its philosophy is **Prevention First, Cleanup Second**: instead of only deleting entities after the damage is done, it actively throttles the systems that cause lag (redstone, hoppers, explosions, mob overpopulation) while still providing smart scheduled cleanup, live performance monitoring, and an automatic emergency mode that kicks in when your TPS collapses.

Every module is fully independent — enable only what you need. Everything hot-reloads with `/azlag reload`.

---

## Features

| Module | What it does |
|--------|--------------|
| **Smart Cleaner** | Scheduled + manual cleanup of dropped items, arrows, projectiles, and optionally mobs/vehicles. Countdown warnings, world filtering, and rich protection rules (named, tamed, leashed, passengers, bosses, recent player drops). |
| **Smart Mob Control** | Caps mobs per chunk by category (animals, monsters, villagers, water, ambient). Periodic overflow purge for chunks over the limit. Whitelist for bosses. |
| **Smart Item Control** | Per-chunk ground-item limits, nearby item merging, configurable despawn lifetime, and periodic item cleanup. |
| **Redstone Control** | Throttles excessive redstone state changes per chunk and detects redstone clocks. Actions: `LOG`, `THROTTLE`, or `ALERT` admins. |
| **Hopper Control** | Reduces hopper tick overhead via a configurable transfer tick-rate, optional per-chunk hopper caps, and auto-disable below a TPS threshold. |
| **Explosion Control** | Caps explosions per tick, TNT entities per chunk, and creepers per chunk. Optional reduced block damage for large TNT farms. |
| **Chunk Control** | Periodically analyzes chunk health, tracks the worst N chunks, and alerts admins when a chunk exceeds an entity threshold. |
| **Performance Monitor** | Live sampling of TPS, MSPT, and memory with a rolling history and configurable warning/critical alerts. |
| **Emergency Mode** | Auto-activates on severe TPS drops. Reduces view distance, halts mob spawning, aggressively purges entities, removes TNT, and optionally disables hoppers — then auto-deactivates once TPS recovers. |
| **Analytics** | Keeps a rolling history of performance samples and can log snapshots to file. |

---

## Requirements

- **Server:** Paper 1.20+ (Spigot-compatible API used throughout; api-version `1.20`)
- **Java:** 17 or newer
- **Optional integrations:** [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/), [spark](https://spark.lucko.me/)

The plugin runs fully without any optional dependency.

---

## Installation

1. Download `AzLagControl-x.y.z.jar` from [Modrinth](https://modrinth.com/), [SpigotMC](https://www.spigotmc.org/), or the [Releases page](https://github.com/Light0mxph/AzLagControl/releases).
2. Drop the jar into your server's `plugins/` folder.
3. Restart (or reload) the server.
4. Edit `plugins/AzLagControl/config.yml` to taste, then run `/azlag reload`.

---

## Commands

Base command: `/azlagcontrol`, aliases `/azlag` and `/alc`.

| Command | Description |
|---------|-------------|
| `/azlag stats` | Full server stats |
| `/azlag tps` | Show TPS (1m / 5m / 15m) |
| `/azlag mspt` | Show milliseconds per tick |
| `/azlag memory` | Show JVM memory usage |
| `/azlag entities` | Entity counts per world |
| `/azlag chunks [analyze]` | Show chunk stats |
| `/azlag cleanup` | Trigger manual cleanup now |
| `/azlag reload` | Reload configuration |
| `/azlag emergency <on\|off\|status>` | Manage emergency mode |
| `/azlag diagnose` | Full system diagnostic |
| `/azlag profiler [last N]` | Show performance history |
| `/azlag help` | List available commands |

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `azlagcontrol.admin` | Full administrative access (grants all below) | op |
| `azlagcontrol.use` | Basic command access (stats, tps) | op |
| `azlagcontrol.reload` | Reload plugin configuration | op |
| `azlagcontrol.stats` | View server performance statistics | op |
| `azlagcontrol.cleanup` | Trigger manual entity/item cleanup | op |
| `azlagcontrol.emergency` | Activate/deactivate emergency mode | op |
| `azlagcontrol.diagnose` | Run server diagnostics | op |
| `azlagcontrol.profiler` | Access the performance profiler | op |

---

## PlaceholderAPI

When PlaceholderAPI is installed, the `azlagcontrol` expansion is registered automatically.

| Placeholder | Output |
|-------------|--------|
| `%azlagcontrol_tps%` | Current TPS |
| `%azlagcontrol_tps_1m%` | TPS — 1 minute average |
| `%azlagcontrol_tps_5m%` | TPS — 5 minute average |
| `%azlagcontrol_tps_15m%` | TPS — 15 minute average |
| `%azlagcontrol_tps_colored%` | TPS, color-coded by health |
| `%azlagcontrol_mspt%` | Milliseconds per tick |
| `%azlagcontrol_memory_used%` | Used JVM memory |
| `%azlagcontrol_memory_max%` | Max JVM memory |
| `%azlagcontrol_memory_pct%` | Memory usage percentage |
| `%azlagcontrol_entities%` | Total entity count |
| `%azlagcontrol_items%` | Ground item count |
| `%azlagcontrol_chunks%` | Loaded chunk count |
| `%azlagcontrol_emergency%` | Emergency mode state |

---

## Developer API

AzLagControl fires Bukkit events other plugins can listen to:

- `AzCleanupEvent` — fired around cleanup runs.
- `AzEmergencyModeChangeEvent` — fired when emergency mode toggles.

```java
@EventHandler
public void onEmergency(AzEmergencyModeChangeEvent event) {
    // react to emergency mode changes
}
```

---

## Configuration

The full, heavily-commented `config.yml` is generated on first run. Highlights:

- **`core`** — debug toggle, message prefix, update checks.
- **`cleaner`** — interval, warn-times, what to remove, protection rules, per-world scope.
- **`mob-control`** — per-chunk category caps, overflow purge, whitelist.
- **`item-control`** — per-chunk limit, merge radius, despawn lifetime.
- **`redstone`** — update caps, clock detection + action.
- **`hopper`** — tick-rate, per-chunk cap, TPS auto-disable.
- **`explosion`** — per-tick cap, TNT/creeper caps, block-damage reduction.
- **`chunk-control`** — analysis interval, top-N tracking, alert threshold.
- **`monitor`** — sampling, history size, TPS alert thresholds.
- **`emergency`** — auto-activate/deactivate TPS, actions, messages.
- **`analytics`** — history window, file logging.
- **`integrations`** — PlaceholderAPI, spark toggles.

All sections support `worlds: []` (empty = all worlds) where applicable, and every module has its own `enabled` flag.

---

## Building from source

```bash
git clone https://github.com/Light0mxph/AzLagControl.git
cd AzLagControl
./gradlew build
```

The shaded, ready-to-use jar lands in `build/libs/AzLagControl-<version>.jar`.

---

## License

Licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE).

---

## Credits

Developed and maintained by **AztrixDigitalStudio**.
