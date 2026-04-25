# Upsilon Architecture Guide

This document explains Upsilon internals: build pipeline, renderer lifecycle, layout state, dynamic rebuild strategy, menu integration, and transform semantics.

If you are only consuming the library, start with `docs/usage.md`.

## 1. High-Level Architecture

Upsilon architecture is built around four layers:

1. **Component definition layer** (`UIComponent`, `PrimitiveUIComponent`, `DynamicUIComponent`)
2. **Build pipeline layer** (`UIBuilder`, `BuildContext`)
3. **Host renderer layer** (`ScreenUIRenderer`, `HudUIRenderer`, `TauContainerScreen`)
4. **Theme + runtime integration layer** (`Theme`, menu/slot synchronization)

At runtime, hosts invoke `UIBuilder` to transform a component tree into concrete runtime artifacts (renderables, listeners, slots, dynamic nodes).

## 2. Component Contracts

## 2.1 `UIComponent`

Primary abstraction:

- method: `UIComponent build(Layout, Theme)`
- returns next component in branch, or `null` to terminate

This enables chain-style composition while preserving low allocation for simple wrapper components.

## 2.2 `PrimitiveUIComponent`

Primitive nodes provide direct integration with build context:

- method: `SimpleVec2i build(Layout, Theme, BuildContext)`
- writes runtime artifacts directly
- usually terminates `UIComponent` chain (default `build(Layout, Theme)` returns `null`)

Examples: `Button`, `Text`, `Container`, `Texture`, `ListView`.

## 2.3 `DynamicUIComponent`

Stateful component with rebuild signaling.

Key field and hooks:

- `public boolean dirty`
- `tick()`
- `destroy()`
- `rebuild()` sets `dirty = true`

When a dynamic component marks dirty, host renderers perform a full tree rebuild on next tick.

## 3. Build Pipeline

## 3.1 `BuildContext` as artifact bus

`BuildContext` aggregates build outputs:

- `renderables`
- `tooltips`
- `dynamicUIComponents`
- `eventListeners`
- `slots`

`BuildContext#addAll(...)` is used by container-like components to compose child contexts.

## 3.2 `UIBuilder` traversal model

`UIBuilder.build(layout, theme, root, context)` recursively traverses component branches.

Per node, it performs:

1. dynamic pre-hook (`buildDynamic(...)`) and registration
2. event listener registration if component implements `GuiEventListener`
3. primitive build if node is `PrimitiveUIComponent`
4. resolve next node through `UIComponent#build(...)`
5. dynamic finalize hook (`finalizeDynamic(...)`)

Size is accumulated through recursive build calls.

## 3.3 Dynamic tick decision

`UIBuilder.tickDynamicUIComponents(...)`:

- calls `tick()` on each dynamic component
- checks `dirty`
- if any dirty: clears all dirty flags and returns `true`

Hosts treat `true` as signal for full UI tree rebuild.

## 4. Renderer Layer And Lifecycle

## 4.1 Screen renderer (`ScreenUIRenderer`)

Lifecycle:

- `init()` -> `rebuildUi()`
- `tick()` -> if dynamic dirty then `rebuildUi()`
- `onClose()` -> `destroy()` all tracked dynamic components
- `render()` -> background -> components -> tooltips

`rebuildUi()` clears old artifacts and rebuilds entire tree against current screen size.

## 4.2 HUD renderer (`HudUIRenderer`)

Lifecycle is similar but not attached to `Screen`:

- internal `init()` rebuilds with current scaled window size
- `tick()` handles dynamic dirty rebuild
- `render(window, graphics, partialTicks)` draws components and rebuilds when scaled dimensions change

## 4.3 Container screen renderer (`TauContainerScreen`)

`TauContainerScreen` hosts `UIMenu` component trees:

- computes `leftPos`/`topPos` from `UIMenu` hooks
- pushes layout offsets for container origin
- rebuilds tree into renderables/tooltips/dynamic nodes
- `containerTick()` handles dynamic dirty rebuild + calls `uiMenu.tick(menu)`

Dynamic nodes are destroyed on screen close and before rebuild.

## 5. Layout Engine Internals

`Layout` is mutable scoped state carried through build.

It maintains three axis-scoped stacks (`StackedAxialSettings`):

- offsets
- size modifications
- alignment settings (`LayoutSetting`)

Default root state:

- offset `(0,0)`
- size modifications `(0,0)`
- alignment `START` on both axes

Core operations:

- push/pop offset per axis
- push/pop size modifier per axis
- push/pop layout setting per axis
- compute placement via `getPosition(axis, length)`

`Layout#copy()` clones current stack state so nested builders can measure without mutating parent state.

## 6. Container/Layout Primitive Strategies

## 6.1 `Row` / `Column`

Both support `FlexSizeBehaviour`:

- `MIN`: pre-measure children, compute tight bounds
- `MAX`: use full available parent bounds

They then create child layout rooted at resolved container position and walk children with cumulative offsets.

## 6.2 `Stack`

- `MAX`: build all children in shared layout, return parent size
- `MIN`: build children in temp context and return max child width/height

## 6.3 Alignment wrappers

- `Center`: pushes `CENTER` on both axes
- `Align`: pushes user-specified horizontal/vertical settings

Both use push/pop scope discipline to avoid leaking settings to siblings.

## 6.4 Size/position wrappers

- `Sized`: resolves concrete `Size`, creates bounded child layout
- `Padding`: adjusts offsets and size modifiers around child build
- `Positioned`: creates absolute-position child layout

## 6.5 Clipping

`Clip` builds child into an isolated renderable list, then replays it under a scissor rectangle in GL space.

## 7. Interaction Components And Input Routing

## 7.1 Widget wrappers

`WidgetWrapper` adapts vanilla `AbstractWidget`:

- sets x/y/width/height from layout
- registers widget as both renderable and event listener

`TextField` and `Slider` use this strategy.

## 7.2 Custom interactive primitives

- `Button`: manual hover/click state; theme-driven visuals
- `ListView`: custom clipping + input forwarding with scroll offset compensation
- `Transform`: event remap through inverse transform path (with caveats below)

## 8. Dynamic Rebuild Model (Design Choice)

Current model is renderer-level full rebuild on dirty signal.

Why this approach:

- avoids fragile in-place patching/index replacement
- keeps state transitions deterministic
- simplifies correctness for listeners/renderables/slots alignment

Tradeoff:

- higher rebuild cost for large trees

Current code favors correctness and maintainability over partial-diff complexity.

## 9. Transform Semantics

`Transform` separates transformations into:

- translation-only transforms
- non-translation transforms

Behavior:

- translation-only transforms are applied in layout space (reliable for input + slot mapping)
- non-translation transforms are applied as visual-only best effort

When non-translation transforms are present, implementation logs a warning because hit-testing guarantees are intentionally limited.

## 10. Menu Architecture (Server/Client)

## 10.1 `UIMenu` contract

`UIMenu` merges visual and container behavior:

- build UI tree
- define size/title
- optional per-tick hook
- slot/data synchronization policies

## 10.2 `TauMenuHolder`

Responsibilities:

- registers `MenuType<TauContainerMenu>`
- owns bound `UIMenu`
- opens menu for player at a `BlockPos`

## 10.3 `TauContainerMenu`

- delegates quick-move and validity checks to `UIMenu`
- stores level and block position
- supports named data slot registration (`addDataSlot(name, ...)`)

## 10.4 Slot materialization pipeline

- UI build emits `MenuSlot<T extends ISlotHandler>` descriptors
- `ISlotHandler#setupSync(...)` converts descriptor into concrete vanilla/NeoForge slot instances
- built-in handlers:
  - `ItemSlotHandler`
  - `PlayerInventoryHandler`

## 10.5 Client-side slot discovery for menus

`TauMenuHelper.buildContainerOnClient(...)` builds menu UI client-side to discover slot positions so `TauContainerScreen` can align visuals and interactions.

## 11. Theme Layer

`Theme` is rendering abstraction for primitive visuals.

Methods define draw responsibilities for:

- button
- container
- scrollbar
- tooltip
- slot
- default text color

`MinecraftTheme` is default implementation using resource textures under `assets/upsilon`.

## 12. Utility Layer And State Helpers

Important utility abstractions:

- `SimpleVec2i`: mutable integer vector used in layout and slot positions
- `Size`: functional strategy for concrete size resolution
- `Variable<T>`: observable mutable value (listeners + changed flag)
- `Transformation`: forward/inverse matrix wrapper with translation metadata

## 13. Testing Architecture (Current)

There is no JUnit suite in this repo; verification is manual in-game.

`src/main/java/com/github/wintersteve25/tau/tests/TestAll.java` provides a hub screen to launch individual behavior samples (layout, interactables, transform, dynamic behavior, inventory visuals, etc.).

This test topology reflects UI-heavy behavior where visual/input correctness is primary.

## 14. Extension Points For Contributors

Most common extension points:

1. new primitive components implementing `PrimitiveUIComponent`
2. new theme implementation implementing `Theme`
3. custom menu systems implementing `UIMenu` + custom `ISlotHandler`
4. dynamic/stateful components by subclassing `DynamicUIComponent`

Key guideline: preserve push/pop layout symmetry and context merge correctness.

## 15. Known Constraints And Practical Tradeoffs

- dynamic updates rebuild full tree instead of partial diffs
- non-translation transform input mapping is best-effort only
- manual test-first workflow means regressions must be validated in-game

These constraints are intentional in current branch to keep behavior predictable and implementation maintainable.
