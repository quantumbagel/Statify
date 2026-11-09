# Statify

[![CI](https://github.com/quantumbagel/Statify/actions/workflows/ci.yml/badge.svg)](https://github.com/quantumbagel/Statify/actions/workflows/ci.yml)

Server-wide leaderboards for Minecraft's built-in statistics, with a small query language for combining them.

```
/leaderboard killed:zombie + killed:skeleton
/leaderboard 5 mined:diamond_ore / (custom:play_time / 72000)
/custom set kdr killed:total / custom:deaths
/leaderboard quantumbagel:kdr
```

Statify reads the stat files Minecraft already writes, so it works on existing worlds with no tracking setup. It runs on Spigot and Paper 1.21 with Java 21.

## Commands

| Command | What it does |
| --- | --- |
| `/leaderboard [count] <query>` (alias `/lb`) | Ranks every player by a query. Shows 10 rows by default and up to 50. If you are below the cutoff, your own row is shown too. |
| `/stat [player] <category> [count]` | One player's top stats in a category, such as `/stat mined` for your most-mined blocks. |
| `/stat [player] <owner> [count]` | One player's result for each of `owner`'s saved custom queries. |
| `/custom set <name> <query>` | Saves a query as `yourname:name` so it can be used in other queries. |
| `/custom list [player]` | Lists saved queries. |
| `/custom get [player] <name>` | Shows one saved query. |
| `/custom delete <name>` | Deletes one of your queries and warns about any queries that used it. |

Every command has tab completion for categories, stat names, players and saved queries.

Permissions: `statify.leaderboard`, `statify.stat` and `statify.custom`, all granted to everyone by default.

## Query language

A query is arithmetic over stats.

- **Stats** are written `category:stat`, using Minecraft's names without the `minecraft:` prefix, such as `killed:zombie`, `mined:stone` or `custom:play_time`. The categories are `custom`, `mined`, `crafted`, `used`, `broken`, `picked_up`, `dropped`, `killed` and `killed_by`. `category:total` sums a whole category. A stat a player has never recorded counts as 0.
- **Saved queries** are written `owner:name`, such as `quantumbagel:kdr`, and can use other saved queries.
- **Operators**: `+ - * / ^` and parentheses. `^` is right-associative and binds tighter than unary minus, so `-2^2` is `-4`.
- **Functions**: `sqrt`, `abs`, `ln`, `log` (base 10), `floor`, `ceil`, `round`, and `sin`, `cos`, `tan` in degrees.

Leaderboards rank highest first, except a query on a single `killed_by` stat, which ranks lowest first. Players with none of a query's stats are left off. So are players whose result is undefined, such as dividing by a stat that is 0.

## How it works

```
query text ──Lexer──▶ tokens ──Parser──▶ syntax tree ──Query.compile──▶ expanded tree ──Evaluator──▶ value per player
                                                         │                               ▲
                                              CustomQueryStore                      StatTable
                                              (saved queries)              (stats/<uuid>.json, as long)
```

- **Parsing** (`query/`): a hand-written lexer and recursive-descent parser build an immutable syntax tree from sealed-interface records. The evaluator is a single pattern-matching `switch`. Errors carry the position where parsing failed, and the commands underline from that position on.
- **Custom queries** (`Query.compile`): saved queries are expanded inline before evaluation, so evaluating a query reads only plain stats. Expansion tracks the chain of queries being expanded, so a loop is reported as `a:x -> a:y -> a:x`. `/custom set` runs the same check against the new definition before saving.
- **Stats** (`stats/StatTable`): every stat file is loaded into memory with `long` values, and the table is reused for 30 seconds. Before reloading, Statify asks the server to save online players so their latest stats are on disk.
- **Ranking** (`rank/`): standard competition ranking (1, 2, =2, 4), generic over the key type so the same code ranks players and stat names.
- **Storage** (`stats/CustomQueryStore`): saved queries stay in memory and are written to `plugins/Statify/custom-queries.json` with an atomic replace on every change. If the file is corrupt, the plugin disables itself instead of overwriting it.

The parser, query expansion, stat loading, storage and ranking have no Bukkit dependency and are covered by JUnit tests.

## Building

```sh
./mvnw verify
```

The plugin jar is written to `target/Statify-<version>.jar`. CI builds and tests every push.

## History

Statify started in November 2023 as my first plugin. Version 2.0 (November 2026) reworks it:

- replaced the borrowed string evaluator with a lexer, parser and syntax tree
- detects custom-query loops properly instead of catching `StackOverflowError`
- uses `long` stat values, fixing overflow and values clamped at 2,147,483,647
- fixes leaderboard bugs: "top 10" showed 9 rows, and 21st showed as 21th
- new custom queries work without a restart, and the save file works on Linux
- adds tab completion, argument checks, and tests

Queries saved by 1.0 in `statify.json` are imported automatically on first start.

## Known limitations

- Saved queries belong to a username, so they don't follow a player who renames.
- Stat files are read on the main server thread. This is fine at small-server scale, but a server with thousands of players would want an asynchronous load.
