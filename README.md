# RitualPractice

Paper **1.21.11** plugin. Local Hypixel SkyBlock Mythological Ritual (Mayor Diana) practice sandbox, wired for **SBO** (vanilla client + debug toggles “always on SkyBlock” / “always Diana”).

Current build: **2.1.22**. Java 21, Maven 3.9+. To build: `mvn -q clean package -DskipTests` → `target/RitualPractice.jar`. Drop it in `plugins/`.

Join `world`, `/ritual give`, `/ritual start`. Echo with the spade (RMB, 10 mana). Break grass to dig (1s cooldown).

Griffin is locked **Mythic / Empyrean**. Burrows are client-sided, hard-capped at **7** undug. No sphinx questionnaire.

---

## Commands

`/set*` amounts take `k` / `m` / `b`. Cap is `2,147,483,647` on every stat.

| Command | Notes |
|---|---|
| `/ritual start\|stop\|reset\|give\|griffin\|rates\|status\|warp` | Session. `give` = kit. Griffin is always MYTHIC. |
| `/setmagicfind` `/setmf` | Magic Find |
| `/settracking` `/settrack` | Tracking (elusive weight) |
| `/sethealth` `/sethp` | Max HP (fills current) |
| `/setdefense` `/setdef` | Defense |
| `/setmana` | Max mana |
| `/setdamage` `/setdmg` | Daedalus damage |
| `/items` | Claim kit + extras (free) |
| `/anvil` | 1 Looting book = +1 level, max 5 |
| `/trades` | Sell drops |
| `/purse` | Coins |
| `/p` `/pc` | Party invite / chat (party only) |
| `/togglechance` | Extra rare-drop chance line |
| `/compactor` | 160 claws → ench claw; 160 ench gold → ench gold block |
| `/togglebreak` | Op — vanilla block break |
| `/togglefakelag` | Op — `Fake lag ON (~18 TPS).` |
| `/warp` | `hub` `castle` `wizard` `crypt` `stonks` `da` `museum` `taylor` |

`/items`: Spade, AOTV, Daedalus, Melon, Mana Fruit, Fire Freeze Staff, Crown of Avarice, Shuriken, Four-Eyed Fish, Looting Book.

---

## Combat

`taken = raw * 100 / (defense + 100)`

Defaults: 5,000 HP, 1,000 def, 1,000 mana, 400 MF, 50 Tracking, 1,000,000 damage. Regen 5% HP /s (unless Manticore sting) and 4% mana /s. I-frames 0.25s. Melee reach 4 blocks. Only **Daedalus Blade** damages mytho mobs. Rare-mob hits splash everyone within 4 of the target. Slot 8 is the SkyBlock menu (locked).

Players cannot receive Wither. Fall / starve / drown / cram cancelled. Players can still void-die. Portals off. Food locked at 20.

Mobs that fall into the void hover, then teleport to the owner after **3s** (chain stays up, no loot). Unload / world-border / `/kill` still free the burrow with no loot. Player death despawns owned mobs and **breaks the chain**.

Death chat (SBO waypoint): `§c ☠ §7You were killed by §2Empyrean …` — others get `Name was killed by …`.

---

## Burrows

7 undug max (`start-burrows`). Chain 10. Treasure 33%. Per-player packets. Spawn box x `[-282..174]`, y `[61..104]`, z `[-207..204]`, grass + open sky, min sep 8.

Dig = break grass with the spade. Cannot dig while defenders live. Non-burrow grass sends `LARGE_SMOKE` (SBO close-detect). Extra burrows despawn as `A Griffin Burrow disappeared (7/7)`.

---

## Mobs

Elusive weight × `(1 + tracking/100)`. Gold: 50% to drop the same count as claws (Looting-ranged). Ench claws: Inq/Sphinx 2 @ 50%; Manti/King 4 @ 50%.

| Mob | W | HP | Unique | % | Claws | Coins |
|---|---|---|---|---|---|---|
| Minos Hunter | 1000 | 1.75m | Hilt of Revelations | 2 | 8 | 500 |
| Siamese Lynxes | 1000 | 1.25m | Crochet Tiger Plushie | 0.4 | 8 | 500 |
| Stranded Nymph | 800 | 2.5m | Washed-up Souvenir | 0.5 | 10 | 550 |
| Cretan Bull | 800 | 3m | Cretan Urn | 0.5 | 10 | 550 |
| Harpy | 600 | 3.75m | Antique Remedies | 0.6 | 12 | 600 |
| Gaia Construct | 600 | 3.5m | Dwarf Turtle Shelmet | 0.6 | 12 | 600 |
| Minotaur | 400 | 15.5m | Daedalus Stick | 0.08 | 14 | 1500 |
| Minos Champion | 400 | 25m | Minos Relic | 0.04 | 14 | 1500 |
| Minos Inquisitor | 75 | 80m | Chimera 1 | 1.25 | 32 | 2500 |
| Sphinx | 75 | 65m | Brain Food | 0.5 | 32 | 2500 |
| Manticore | 15 | 125m | Stinger 0.5 / Manti-core 0.2 | — | 48 | 5000 |
| King Minos | 15 | 100m | Crown 2 / Wool 0.2 (independent) | — | 48 | 5000 |

Commons circle after a short walk-out (can hit on the way). **Lynxes** chase (twins; killing one activates the other). **Harpy** ~10 blocks up. **Gaia** lightning every 2s, 1/6 hit, 15k raw on the ground, random player within 10 of the mob. **Bull** waits 1s then rams. **Champion** +3%/s to 600% then despawn (~200s). **Inquisitor** +6%/s to 600% (~100s), lightning every 10s, 10% shred per hit max 40%. **Manticore** 2s grace then sting; fail to kill in 45s and you die (heal locked). **King** 150 true + 2500 DPS, no adds, 75-hit shield.

Skins: `MythoSkins/` (or `skins-folder`). `.png` / 64-char `.txt` hash / `.url`. See `src/main/resources/MythoSkins/README.txt`.

---

## Loot

```text
unique chance = (base%/100) * lootingMult * (1 + MF/100)
lootingMult   = 1 + 0.15 * level     (max 1.75 at L5)
```

Claws/gold amount is uniform in `[base, floor(base * lootingMult)]`. Looting does not apply to lootshare.

Lootshare: hitters only, 20% gate (no MF), then the same unique table with **their** MF and no Looting. Chat: `LOOT SHARE You received loot for assisting <name>`.

Every unique: `§6§lRARE DROP! <name> §b(+§b<MF> ✯ Magic Find)`  
Optional `/togglechance` line after. Treasure: `§6§lRARE DROP! §eYou dug out a <name>§e!`  
All gameplay coins: `§6§lWow! §eYou dug out §6<n> coins§e!`

**Crown of Avarice** — 5× absorb until 1b stored, then 2× to purse. **Four-Eyed Fish** — +2,000 coins per burrow. **Shuriken** — +5% MF on that kill.

Sacks (10 ticks after death hologram): `[Sacks] +X items. (Last 5s.)` hover `+N Ancient Claw / Enchanted Gold Ingot / Enchanted Ancient Claw (Last 5s.)`.

Treasure weights (total 3221): feather 2020, 10k 500, fragment 300, 25k 200, 50k 100, 100k 50, 250k 25, 500k 15, 1m 10, braided 1.

| Sell | Coins |
|---|---|
| Ancient Claw | 500 |
| Enchanted Gold Ingot | 1,200 |
| Enchanted Ancient Claw | 80,000 |
| Enchanted Gold Block | 192,000 |
| Mythos Fragment | 25,000 |
| Hilt | 150,000 |
| Griffin Feather | 200,000 |
| Souvenir / Urn / Shelmet / Plushie / Remedies | 250,000 |
| Crown of Greed | 1,000,000 |
| Brain Food | 2,000,000 |
| Daedalus Stick | 2,500,000 |
| Fateful Stinger | 5,000,000 |
| Chimera / Manti-core / Minos Relic | 30,000,000 |
| Braided Griffin Feather | 40,000,000 |
| Shimmering Wool | 50,000,000 |

---

## Items

- **Spade** — Efficiency 5 (hidden). Echo 10 mana. Dig on break.
- **AOTV** — 25 mana, 12 blocks. Ignores plants. Aimed into a block lands on that face. Snaps to `(x.5, y.0, z.5)`, copies look, `PLUGIN` teleport. Shift = etherwarp.
- **Daedalus** — only mytho weapon. Looting at `/anvil`.
- **Melon** — 50% max HP, 5s, 200 mana.
- **Mana Fruit** — 50% max mana for 50% current HP.
- **Fire Freeze Staff** — 5s ring, freeze 10s. No chat.
- Names/lore match Hypixel for SBO.

---

## SBO fingerprints

Leave SBO unpatched.

| What | Pattern |
|---|---|
| Coins | `^§6§lWow! §eYou dug out §6(.*?) coins§e!$` |
| Treasure | `^§6§lRARE DROP! §eYou dug out a (.*?)§e!$` |
| Rare drop | `§6§lRARE DROP!` + colored name + MF |
| Sacks | `[Sacks] +X items. (Last 5s.)` + hover `+N Name (Last 5s.)` |
| Death | `^§c ☠ §7You .+$` |
| Close-detect | `LARGE_SMOKE` |
| Hologram | `([0-9]+(?:\.[0-9]+)?[MK]?)§f/` and `§2` name. 0-HP stand stays ~12 ticks. |
| Echo | `DRIPPING_LAVA` count=2 speed=-0.5 ~18 ticks |
| Arrow | `DUST` count=0 speed=1, RGB green / yellow / red |
| Particles | ENCHANT / EMPTY / MOB / TREASURE / FOOTSTEP |

See `SBO-PATCH.md`.

---

## Config

`plugins/RitualPractice/config.yml` — `world`, `start-burrows: 7`, `chain-length: 10`, `treasure-chance: 0.33`, `close-detect-range: 32`, `echo-plings: 12`, `sounds`, `default-griffin: MYTHIC`, `gaia-hit-chance: 0.333333`, `skins-folder`, `warps`. Per-player YAML stores stats, purse, chance-messages, auto-compactor.

Main class: `dev.practice.ritual.RitualPlugin`.
