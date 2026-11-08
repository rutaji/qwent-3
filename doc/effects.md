# Effects

Everything a card does is an `Effect`. A card returns a list of effects from `onPlay`, `onDeath`,
`onDamagedSurvive` or `onOpponentSpell` in `CardType`. The `GameEngine` puts them in its **effect queue** and
resolves them one at a time.

Effects are small and do one thing each. Bigger card abilities are built by **combining** them. There are three ways
to combine:

| Way | Use it for | Example |
|---|---|---|
| **Chaining** with `then` / `thenWith` (decorators in `combinator/`) | "do A, then B" / "do A, then something built from what A did" | discard a card **to** deal damage |
| **Strategy pairs**: a selector or picker plus a `TileAction` (`board/`) | "which tiles" + "what happens to them" | destroy **the lowest power monsters** |
| **Configuration objects** (`effectTargets/`) | which side, pile, direction or cards an effect works on | draw **a spell** from **your opponent's graveyard** |

```
effect/
  Effect, ResultEffect        the two core interfaces
  combinator/                 ToEffect, SwitchEffect, IfEffect - link effects together
  board/                      effects on tiles and units
    factory/                  Select, Act, Pick - ready-made selectors, actions and pick effects
  pile/                       effects on cards in hand, deck and graveyard
  effectTargets/              Destination, Direction, Pack, SideTarget, Targets
    filters/                  CardFilter and its implementations
```

---

## How an effect resolves

Every queued effect has an **owner**, side 0 or 1. That's the player who played the card, or the player on whose
side a monster was damaged or died. Words like "your side" and "your deck" in an effect always mean the owner's.

When an effect reaches the front of the queue, the engine:

1. calls `hasValidTargets(state, owner)`. If it's **false**, the effect is **cancelled**: it's removed from the
   queue, logged as cancelled, and nothing happens.
2. if `needsTarget()` is **false**, calls `resolve(engine, owner, -1)` right away.
3. otherwise **waits** until the owner clicks a target that `isValidTarget` accepts. Depending on `targetKind()`,
   that's a board tile or a card in hand. Then it calls `resolve` with that target. A targeted effect can't be
   skipped.

`resolve` can queue more effects:
- `engine.queueNext(owner, effect)` puts one at the **front**, so it resolves next.
- `engine.queue(owner, effects)` adds them at the **end**. On-death and on-damaged triggers go there.

The combinators rely on this: they put their follow-up effects at the front, so the follow-ups resolve right after
the first part.

---

## Core interfaces

### `Effect`
The interface every effect implements.

| Method | Meaning | Default |
|---|---|---|
| `Component description()` | Text shown while the player picks the target and in the game history, e.g. "Mage: deal 2 damage to a monster" | must implement |
| `boolean needsTarget()` | Does the owner have to pick something? | `true` |
| `TargetKind targetKind()` | `TILE` (board tile index) or `HAND` (index in the owner's hand) | `TILE` |
| `boolean isValidTarget(state, owner, target)` | Can this tile or hand card be picked? | must implement |
| `boolean hasValidTargets(state, owner)` | Can the effect happen at all? If not, it's cancelled. | any `isValidTarget` is true (or `true` when no target is needed) |
| `String logDetail(state, owner)` | Extra history text for an untargeted effect, e.g. which monsters it destroys | `null` |
| `void resolve(engine, owner, target)` | Does it. `target` is -1 when no target is needed. | must implement |
| `Effect then(Effect... next)` | Chains: see `ToEffect` | |

Effects that need no target return `false` from `needsTarget()` and `false` from `isValidTarget`, and usually
override `hasValidTargets` to say when they'd do nothing.

### `ResultEffect<T>`
An `Effect` that **produces a value** when it resolves: the discarded card, the banished card, the picked unit, a
count. You implement `T resolveWithResult(engine, owner, target)` instead of `resolve`, and return `null` for
"nothing happened". `resolve` is implemented for you.

| Method | Meaning |
|---|---|
| `T resolveWithResult(engine, owner, target)` | Does it and returns what it did (or `null`) |
| `Effect thenWith(Function<T, List<Effect>> next)` | Chains with the result: see `SwitchEffect` |

Implemented by: `DiscardEffect` (`CardType`), `BanishEffect` (`CardType`), `CountEffect` (`Integer`),
`PickUnitEffect` (`Unit`).

---

## Combinators (decorators) - `combinator/`

Each combinator is itself an `Effect` that **wraps** other effects. So they nest, and the engine doesn't need to know
about them. You normally don't call their constructors yourself; use `then` / `thenWith`.

### `ToEffect` - "do A **to** do B"
`a.then(b, c, ...)` or `new ToEffect(a, b, c, ...)`

- The player sees `a`'s text and picks `a`'s target. `ToEffect` passes `description`, `needsTarget`, `targetKind`,
  `isValidTarget` and `logDetail` through to `a`.
- **Valid only if `a` and the first follow-up both have valid targets.** If either one can't happen, the whole thing
  is cancelled and `a` doesn't happen either. That's how "discard a card **to** …" means "only if you can do the …".
- When it resolves, it first queues the follow-ups at the front, then resolves `a`. If `a` re-queues itself (e.g. a
  multi-card discard), its next step still comes **before** the follow-ups.

```java
new DiscardEffect(this).then(Pick.addPower(this, -4, Targets.ENEMY, hit), ...)          // Shulker
BanishEffect.fromGraveyard(this, Tag.PLANT).then(Pick.summon(this, FARMER, 1, false))   // Farmer
new DiscardEffect(this, 4).then(Pick.summon(this, ENDER_DRAGON, 1, false))              // Summon Ender Dragon
```

### `SwitchEffect<T>` - "do A, then something **depending on what A did**"
`a.thenWith(result -> List.of(...))` or `new SwitchEffect<>(a, next)`. `a` must be a `ResultEffect<T>`.

- It shows `a`'s text and target, like `ToEffect`.
- **Valid if `a` is.** The follow-ups aren't known until `a` has resolved, so they can't be checked in advance.
- When it resolves: `T r = a.resolveWithResult(...)`. If `r` isn't `null`, it builds the follow-ups with
  `next.apply(r)` and queues them at the front, in order. If `r` is `null`, nothing follows.
- `a` must resolve in one step. Don't use it with an effect that re-queues itself, such as `DiscardEffect` with
  count > 1.

```java
// Librarian: discard a monster to draw a spell, or a spell to draw a monster
new DiscardEffect(this).thenWith(card -> List.of(
        new DrawEffect(this, 1, new FilterKind(card.isMonster() ? Kind.SPELL : Kind.MONSTER))))

// Potato: +3 power for every Potato in your graveyard
new CountEffect(this, PileCount.graveyard(POTATO), "+3 power for every Potato in your graveyard")
        .thenWith(n -> List.of(new AreaEffect(this, Select.self(self), Act.addPower(3 * n), "+" + 3 * n + " power")))

// Wooden Hoe: destroy your monster to play a copy of it
new PickUnitEffect(this, Targets.OWN, Act.destroy(), "destroy one of your monsters to play a copy of it")
        .thenWith(unit -> List.of(Pick.summon(this, unit.type(), 1, false)))
```

### `IfEffect` - "**if** a condition holds, do B"
`new IfEffect(source, (state, owner) -> condition, "text", b, c, ...)`

- No target. **Valid only if the condition holds** when it's this effect's turn; otherwise it's cancelled and the
  follow-ups don't happen.
- When it resolves, it queues the follow-ups at the front, in order.
- Shortcut: `IfEffect.onEnemySide(source, unit, ...)` = "if the unit is on the opponent's side of the owner". Use it
  for treacherous monsters.
- Conditions on piles come from `PileCount.atLeast(n)`.

```java
IfEffect.onEnemySide(this, self, new DrawEffect(this, 1, null, SideTarget.Owner, Pack.Deck, SideTarget.Opponent))  // Emerald
new IfEffect(this, PileCount.graveyard(MUSIC_DISC).atLeast(2), "at least 2 Music Discs in your graveyard",
        new DrawEffect(this, 1))                                                                                   // Music Disc
```

### Combining combinators
Combinators are effects, so they nest: `a.then(b.then(c))`, `IfEffect(..., a.thenWith(...))` and so on. For
plain "do these in order" with no condition, return several effects in the card's list:
`List.of(effectA, effectB)`.

---

## Board effects - `board/`

Two **strategy** interfaces split "where" from "what". They aren't effects themselves. An effect is built from them.

### `TileSelector` - *which tiles*
`List<Integer> select(state, owner)` returns tiles in board order. They may be empty tiles, e.g. a whole row.
`String text()` describes where they are, e.g. "in its row", "left and right of it".
Ready-made ones are in `factory/Select`.

### `TileAction` - *what happens to one tile*
- `boolean accepts(state, owner, tile)`: can it act on this tile now, e.g. is there a monster, can the card be
  played there?
- `void apply(engine, owner, tile)`: does it.
- `String text(String where)`: the whole effect's text, given the selector's `where`.

Ready-made ones are in `factory/Act`.

### `AreaEffect` - selector + action, **many tiles, no picking**
`new AreaEffect(source, selector, action)` or `new AreaEffect(source, selector, action, "custom text")`

- No target. All tiles are chosen first (selector, then filtered by `action.accepts`), then the action is applied
  to each in board order. Destroying one can't change which others were chosen, e.g. ties for the lowest power.
- **Cancelled if the action accepts none of the selected tiles.**
- Text is `action.text(selector.text())` unless you pass your own (use that when the built text reads badly).
- `logDetail` lists the affected monsters with their power, e.g. "Bear (7), Bear (7)".

```java
new AreaEffect(this, Select.lowestPower(), Act.destroy())                   // Gravel
new AreaEffect(this, Select.neighbours(self), Act.addPower(-1))             // Cactus
new AreaEffect(this, Select.neighboursOfLastTile(self), Act.destroy())      // Creeper (on death)
new AreaEffect(this, Select.backRow(), Act.summon(BEETROOT))                // Bad Farming
new AreaEffect(this, Select.row(self), Act.summon(SILVERFISH))              // Stone Bricks (on death)
new AreaEffect(this, Select.all(Targets.OWN), Act.addPower(2))              // Splash of Strength
new AreaEffect(this, Select.column(self, false), Act.destroy())             // Ender Dragon
```

### `PickUnitEffect` - the player **picks a monster**, an action is done to it
`new PickUnitEffect(source, targets, action, "text")` or `(..., "text", alreadyHit)`

- Targets a tile with a monster that `targets` allows (`Targets.ANY / OWN / ENEMY`) and that `action` accepts.
  `action` may be `null` = just pick it.
- `alreadyHit` (optional `Set<Unit>`): monsters in the set can't be picked, and the picked one is added. Share one
  set between several effects so they hit **different** monsters. The text then gets " (a different one)".
- It's a `ResultEffect<Unit>`: the result is the picked unit. Use `thenWith(unit -> ...)` to reuse its card, power
  and so on.
- Common ones are in `factory/Pick` (`addPower`, `setPower`).

```java
Pick.addPower(this, -4)                                    // Arrow: 4 damage to a monster
Set<Unit> hit = new HashSet<>();
List.of(Pick.addPower(this, -4, Targets.ANY, hit), Pick.addPower(this, -4, Targets.ANY, hit))   // Two Arrows
new PickUnitEffect(this, Targets.ANY, null, "put 3 copies of a monster into your deck")
        .thenWith(unit -> List.of(new AddToPackEffect(this, unit.type(), 3, SideTarget.Owner, Pack.Deck, false)))  // Diamond Hoe
```

### `PickTileEffect` - the player **picks a tile** (may be empty), an action is done to it, `count` times
`new PickTileEffect(source, action, "text", count, random)`

- Any tile `action` accepts can be picked, e.g. where a monster can be summoned. With `random = true` the tile is
  picked at random and the player isn't asked.
- It repeats `count` times, one tile at a time, and the text shows "(2 of 3)". It stops early when no tile is
  accepted any more, and it's cancelled if none is at the start.
- Usually made through `Pick.summon(...)`.

```java
Pick.summon(this, SILVERFISH, 3, false)       // Swarm: you place 3 Silverfish
Pick.summon(this, BASIC_ZOMBIE, 1, true)      // Leader Zombie: one on a random empty tile
```

### `MoveEffect` - the player picks a monster and moves it
`new MoveEffect(source, direction, distance, canTargetEnemy)`

- `Direction` is seen from the owner: `Forward` = towards the opponent, `Back` = towards the owner, `Left` /
  `Right` along the row (column 0 is on the left for both players).
- It moves one tile at a time, `distance` times. Monsters in the way are pushed along, and a monster that crosses to
  the other side belongs to that side. It stops early if the chain would be pushed off the board. A monster that
  can't move at all isn't a valid target.
- `canTargetEnemy = false` allows only the owner's own monsters.

```java
new MoveEffect(this, Direction.Forward, 1, true)                              // Piston
new DiscardEffect(this).then(new MoveEffect(this, Direction.Back, 1, true))   // Slime Piston
```

### Factories - `board/factory/`
Classes that only hold static methods returning the pieces above.

| Class | Returns | Methods |
|---|---|---|
| `Select` | `TileSelector` | `lowestPower()`, `highestPower()` (whole board, all tied), `all(Targets)`, `row(unit)` (its row, still works after it died), `backRow()` (owner's back row), `column(unit, includeSelf)`, `self(unit)` (its tile; nothing if it's gone), `neighbours(unit)` (left/right; nothing if it's gone), `neighboursOfLastTile(unit)` (left/right of where it was; works after death) |
| `Act` | `TileAction` | `destroy()` (on-death effects trigger, on-damaged ones don't), `addPower(n)` (buff, or damage if negative), `setPower(n)` (fixed value, not damage), `summon(card)` (plays a monster where `canPlay` allows) |
| `Pick` | `PickUnitEffect` / `PickTileEffect` | `addPower(source, n[, targets[, alreadyHit]])`, `setPower(source, n, targets)`, `summon(source, card, count, random)` |

Any `Act` works with any `Select` in an `AreaEffect`, and with `PickUnitEffect` / `PickTileEffect`. Adding a new
action or selector makes it available everywhere.

---

## Pile effects - `pile/`

Effects on cards in the **hand, deck and graveyard**. Most are configured with `SideTarget` (whose pile), `Pack`
(deck or graveyard) and an optional `CardFilter` (which cards; `null` = any).

"Top" means the same everywhere: the **deck's top is its first card** (drawn next), and the **graveyard's top is
the card that entered it last**.

### `DrawEffect` - draw cards
`new DrawEffect(source, count)`, `(source, count, filter)`, `(source, count, filter, who, pack, packSide)`

- `who` draws `count` cards from the top of `packSide`'s `pack`. With a filter, it draws the top-most matching ones,
  fewer if the pile runs out. With a full hand, the card goes to the drawer's graveyard.
- No target. It never cancels; if there's nothing to draw, it just draws nothing.

```java
new DiscardEffect(this, 2).then(new DrawEffect(this, 2))                                    // Fertilizing
new DiscardEffect(this, Tag.ZOMBIE).then(new DrawEffect(this, 1, new FilterKind(Kind.SPELL)))  // Zombie Librarian
```

### `DiscardEffect` - the player picks cards from their hand - `ResultEffect<CardType>`
`new DiscardEffect(source)`, `(source, count)`, `(source, tag)`,
`(source, count, filter, "what", destination)`

- Targets **hand cards** (`TargetKind.HAND`). The player picks `count` matching cards one at a time, and the text
  shows "(i of n)".
- Each card goes to `destination`: `Graveyard` (a normal discard), `DeckRandom`, `DeckTop`, `DeckBottom` or
  `Banished`.
- Cancelled if the hand has fewer than `count` matching cards when it starts.
- The result is the card of this pick. Use `thenWith` only with `count = 1`.
- It's the usual **cost** in `cost.then(payoff)`.

### `BanishEffect` - remove a random card for good - `ResultEffect<CardType>`
`new BanishEffect(source, filter, "what", side, pack)`, shortcut `BanishEffect.fromGraveyard(source, tag)`

- No target. It removes a random matching card from `side`'s `pack`. It's cancelled if there is none.
- The result is the banished card.

```java
BanishEffect.fromGraveyard(this, Tag.PLANT).then(Pick.addPower(this, -4))                                              // Thorny Rose
BanishEffect.fromGraveyard(this, Tag.PLANT).then(new AreaEffect(this, Select.self(self), Act.addPower(10), "+10 power"))  // Wheat
```

### `AddToPackEffect` - create copies of a card in a pile
`new AddToPackEffect(source, card, copies, side, pack, onTop)`

- No target. It puts `copies` new copies of `card` into `side`'s `pack`: on top, or at random positions. Never
  cancelled.

```java
new AddToPackEffect(this, SILVERFISH, 4, SideTarget.Opponent, Pack.Deck, false)   // Infestation
```

### `MoveBetweenPacksEffect` - move every matching card from one pile to another
`new MoveBetweenPacksEffect(source, filter, "what", fromSide, fromPack, toSide, toPack, onTop)`

- No target. It moves **every** matching card, on top or to random positions. It's cancelled if the source has none.
  Moving within the same pile works too: all the cards are taken out first.

```java
new MoveBetweenPacksEffect(this, new FilterCard(BROWN_MUSHROOM), "every Brown Mushroom",
        SideTarget.Owner, Pack.Deck, SideTarget.Owner, Pack.Graveyard, true)       // Brown Mushroom
```

### `SwapPileEffect` - swap the contents of two piles
`new SwapPileEffect(source, sideA, packA, sideB, packB)`

- No target. The two piles swap contents with every card keeping its list position, so there's no shuffling. Nothing
  happens if both are the same pile.

```java
new SwapPileEffect(this, SideTarget.Owner, Pack.Deck, SideTarget.Owner, Pack.Graveyard)   // Switcharoo
```

### `PlayFromPileEffect` - play cards from a pile
`new PlayFromPileEffect(source, filter, "what", count, side, pack, random)`; `count` can be `PlayFromPileEffect.ALL`

- No target at first. Each time, it takes the **top-most** matching card that can be played now:
  - a **monster** is shown to the owner, who picks its tile (or, with `random = true`, it goes to a random tile
    where it can be played). It leaves the pile when it's placed.
  - a **spell** leaves the pile and is cast for the owner right away, **as if played from hand**: it goes into the
    owner's graveyard, then its effects resolve.
- It repeats until `count` cards are played or none can be. It's cancelled if none can be at the start.

```java
new PlayFromPileEffect(this, new FilterCard(BASIC_ZOMBIE), "every Basic Zombie", PlayFromPileEffect.ALL,
        SideTarget.Owner, Pack.Deck, false)                                                   // Horde
new PlayFromPileEffect(this, new FilterKind(Kind.SPELL), "the last spell", 1,
        SideTarget.Opponent, Pack.Graveyard, false)                                           // Mirror
```

> Playing spells from your **own graveyard** with `count > 1` would pick the same spell again, because it lands back
> on top. No card does that today.

### `SummonSpellEffect` - create a spell and cast it
`new SummonSpellEffect(source, spell)`

- No target. It casts `spell` for the owner as if played from hand: into the graveyard, then its effects.
- It throws if given a monster. Use `Pick.summon` for monsters.

### `CountEffect` + `PileCount` - count cards and act on the number
- `PileCount(filter, pack, side)`, shortcut `PileCount.graveyard(card)`: a **strategy** object, not an effect.
  - `int count(state, owner)` returns how many matching cards are in that pile.
  - `atLeast(n)` returns a condition for `IfEffect`.
- `new CountEffect(source, pileCount, "text")`: a `ResultEffect<Integer>` that only counts, without changing the
  game. It's cancelled when the count is 0. Use it with `thenWith(n -> ...)`. The history shows the number.

See the Potato and Music Disc examples above.

---

## Configuration - `effectTargets/`

| Type | Values | Used by |
|---|---|---|
| `Targets` | `ANY`, `OWN`, `ENEMY`, with `allows(unit, owner)` and `text()` ("a monster", "one of your monsters", "an enemy monster") | `PickUnitEffect`, `Pick`, `Select.all` |
| `SideTarget` | `Owner`, `Opponent` (seen from the effect's owner) | pile effects, `DrawEffect` |
| `Pack` | `Deck`, `Graveyard` | pile effects |
| `Direction` | `Forward`, `Back`, `Left`, `Right` (seen from the owner) | `MoveEffect` |
| `Destination` | `Graveyard`, `DeckRandom`, `DeckTop`, `DeckBottom`, `Banished` | `DiscardEffect` |

### `CardFilter` - `effectTargets/filters/`
Decides which cards count. It has `check(CardType)` for cards in piles and hand, and `check(Unit)` for monsters on the
board. Pass `null` where a filter is optional to mean "any card".

| Filter | Matches |
|---|---|
| `FilterCard(card)` | exactly that card |
| `FilterKind(Kind.MONSTER / Kind.SPELL)` | monsters / spells |
| `FilterTags(Set.of(tag, ...))` | cards with any of the tags |
| `FilterPower(power -> power >= 5)` | by power (a card's base power, a unit's current power) |

---

## Recipes

| Card text | Build it as |
|---|---|
| "Discard a card to X" | `new DiscardEffect(this).then(X)` |
| "Discard a card; depending on it, X or Y" | `new DiscardEffect(this).thenWith(card -> List.of(card... ? X : Y))` |
| "Banish a random T from your graveyard to X" | `BanishEffect.fromGraveyard(this, T).then(X)` |
| "If <condition>, X" | `new IfEffect(this, (state, owner) -> ..., "text", X)` |
| "If you have N cards C in your graveyard, X" | `new IfEffect(this, PileCount.graveyard(C).atLeast(N), "text", X)` |
| "+P for every C in your graveyard" | `new CountEffect(...).thenWith(n -> List.of(new AreaEffect(this, Select.self(self), Act.addPower(P * n), ...)))` |
| "Deal D damage to a monster" / "give +P" | `Pick.addPower(this, -D)` / `Pick.addPower(this, P)` |
| "… to N different monsters" | N × `Pick.addPower(this, -D, targets, sharedSet)` |
| "Destroy / damage / buff every monster in <area>" | `new AreaEffect(this, Select.<area>(...), Act.<action>(...))` |
| "Fill <area> with M" | `new AreaEffect(this, Select.<area>(...), Act.summon(M))` |
| "Summon N copies of M" | `Pick.summon(this, M, N, random)` |
| "Pick a monster, then use its card" | `new PickUnitEffect(this, targets, action-or-null, "text").thenWith(unit -> List.of(... unit.type() ...))` |
| "Play every C from your deck / graveyard" | `new PlayFromPileEffect(this, new FilterCard(C), "every C", PlayFromPileEffect.ALL, Owner, pack, false)` |

To add something new:
- a new **kind of tile action** → add a method to `Act`; it works with every selector and picker.
- a new **area** → add a method to `Select`.
- a new **link between effects** → a new combinator in `combinator/`.
- a new **effect on piles** → a class in `pile/`, configured with `SideTarget` / `Pack` / `CardFilter` like the
  others.
