# Create: Constellation

Skill tree progression mod for **Minecraft 1.21.1 (NeoForge)** that gates **Create 6.0.10+** recipes until you unlock them.

![icon](promo/icon_pixel.png)

## How it works

- Craft gated items with Create machines (or the vanilla crafting table) to **earn EXP**
- EXP converts into **skill points** (`exp_per_point`, default 100 EXP = 1 point)
- Spend points in the **skill tree GUI** (press `I`) to unlock recipes
- Until an item is unlocked, machines refuse its recipe — items are returned, never destroyed

The tree is a solar system: the main Create branch is the Sun, other branches (`fluids`, `logistics`, `contraptions`, ...) are planets orbiting it. Click a planet to focus and follow it, drag to pan, wheel to zoom, `C` to return to the Sun.

Unlock rule: ring-1 nodes cost points only; deeper nodes require any one of their parent nodes to be unlocked first.

## Gated machines

| Machine | Hook |
|---|---|
| Crafting table / 2x2 grid | `Slot.mayPickup` mixin |
| Mechanical Mixer & Press (basin) | `BasinOperatingBlockEntity.matchBasinRecipe` |
| Mechanical Press (belt/world) | `getRecipe` |
| Mechanical Crafter | `RecipeGridHandler.tryToApplyRecipe` redirect |
| Mechanical Saw | `applyRecipe` |
| Crushing Wheels | `applyRecipe` |

## Configuration

All in `data/<namespace>/createtree/*.json` (datapack-overridable, `/reload` supported):

```json
{
  "exp_per_point": 100,
  "starting_points": 2,
  "entries": [
    { "item": "create:shaft", "category": "light" },
    { "item": "create:precision_mechanism", "category": "complex", "cost": 5, "exp": 40 },
    { "item": "create:mechanical_pump", "category": "medium", "branch": "fluids" }
  ]
}
```

- `category`: `light` (1 pt) / `medium` (2 pts) / `complex` (3 pts) — default cost & EXP
- `cost` / `exp`: optional per-item overrides
- `branch`: optional; anything except `create` becomes a new planet

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.219+
- Create 6.0.10+

## Building

```
gradlew build
```

The Create jars in `libs/` are compile-time/runtime-only for development and are **not** redistributed or packaged — remove them and download your own copies if you rebuild from source.

Progress (unlocks, EXP, points) is stored server-side per player and survives death and relog.
