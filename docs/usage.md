# Upsilon Usage Guide

This document is a practical user guide for building UI with Upsilon.

If you want to understand internals and design rationale, read `docs/architecture.md`.

## 1. What Upsilon Is

Upsilon is a composable UI library for NeoForge mods.

You create a UI tree with `UIComponent` implementations, then host it in one of three runtime targets:

- screen UI via `ScreenUIRenderer`
- HUD UI via `HudUIRenderer`
- container/menu UI via `UIMenu` + `TauContainerScreen`

Core runtime identifiers in this branch:

- mod id: `upsilon`
- runtime entrypoint: `moe.liar.upsilon.Upsilon`

## 2. Setup And Environment

Project baseline:

- Minecraft `1.21.1`
- NeoForge `21.1.20`
- Java `21`

Common commands:

- build: `bash gradlew build`
- test: `bash gradlew test`
- run client: `bash gradlew runClient`
- run server: `bash gradlew runServer`

Note: in Linux/WSL environments where `gradlew` has CRLF, use `bash gradlew ...`.

## 3. Minimal Screen Example

Use this as a template for first integration.

```java
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.interactable.Button;
import com.github.wintersteve25.tau.components.layout.Center;
import com.github.wintersteve25.tau.components.utils.Container;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.components.utils.Text;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.renderer.ScreenUIRenderer;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ExampleScreenUI implements UIComponent {
    @Override
    public UIComponent build(Layout layout, Theme theme) {
        return new Center(
                new Sized(
                        Size.staticSize(180, 40),
                        new Button.Builder()
                                .withOnPress(btn -> Minecraft.getInstance().player
                                        .sendSystemMessage(Component.literal("Clicked")))
                                .build(
                                        new Container.Builder()
                                                .withChild(new Center(new Text.Builder("Hello Upsilon").build()))
                                                .build()
                                )
                )
        );
    }
}
```

Open it:

```java
Minecraft.getInstance().setScreen(new ScreenUIRenderer(new ExampleScreenUI()));
```

## 4. Component Model You Use Daily

### 4.1 `UIComponent`

`UIComponent#build(Layout, Theme)` returns the next node in the build chain.

- return another component to keep chaining
- return `null` to end this branch

In practice, most custom UI classes return one top-level composed component (for example `new Center(...)`, `new Column.Builder()...`).

### 4.2 Primitive components

Most built-in widgets are `PrimitiveUIComponent`s internally, which means they write renderables/listeners directly during build.

You usually do not need to implement primitives yourself unless building advanced custom widgets.

## 5. Layout System (Practical)

### 5.1 Main layout widgets

- `Column`: vertical stacking
- `Row`: horizontal stacking
- `Stack`: overlay children in the same area
- `Center`: center on both axes
- `Align`: explicit horizontal/vertical alignment
- `Sized`: force child bounds
- `Padding`: inset/outset spacing
- `Spacer`: fixed blank space
- `Positioned`: absolute position wrapper

### 5.2 Typical composition pattern

```java
new Column.Builder()
    .withSpacing(6)
    .build(
        new Text.Builder("Settings"),
        new Row.Builder().withSpacing(4).build(
            new Sized(Size.staticSize(100, 20),
                new Button.Builder().build(new Center(new Text.Builder("Apply")))),
            new Sized(Size.staticSize(100, 20),
                new Button.Builder().build(new Center(new Text.Builder("Cancel"))))
        )
    );
```

### 5.3 Size behavior choices

Some containers (`Row`, `Column`, `Stack`, `Container`) use `FlexSizeBehaviour`:

- `MIN`: derive size from content
- `MAX`: use available parent bounds

Use `MIN` when content should define panel size; use `MAX` when filling the assigned area.

### 5.4 Alignment choices

Use `LayoutSetting` constants:

- `START`
- `CENTER`
- `END`

Or `LayoutSetting.percentage(float)` when proportional placement is needed.

## 6. Core UI Widgets

### 6.1 Text

`Text.Builder` supports overflow modes:

- `OVERFLOW`
- `WRAP`
- `CLIP`
- `ELLIPSIS`

Example:

```java
new Text.Builder("Very long text")
    .withOverflowBehaviour(Text.OverflowBehaviour.ELLIPSIS);
```

### 6.2 Button

`Button.Builder().withOnPress(...)` receives mouse button id.

Button style is theme-driven (`Theme#drawButton`).

### 6.3 TextField

`TextField.Builder` options:

- `withMessage(Component)`
- `withHintText(Component)`
- `withOnChange(Consumer<String>)`
- `withValidator(Predicate<String>)`
- `withFormatter(BiFunction<String, Integer, FormattedCharSequence>)`

### 6.4 Slider

`Slider.Builder` options:

- value range: `withMinimum`, `withMaximum`, `withValue`
- precision/step: `withDecimalPlaces`, `withStepSize`
- callbacks: `withOnPress`, `withOnValueChanged`

### 6.5 ListView

`ListView` is a scrollable vertical list.

Useful settings:

- `withSpacing(int)`
- `withAlignment(LayoutSetting)`

Behavior notes:

- it clips child rendering to viewport
- it translates input by scroll offset
- it caches measured content height per viewport size

## 7. Render Helpers And Visual Utilities

### 7.1 Container and background panels

`Container.Builder`:

- `withChild(...)`
- `noBackground()` if only layout wrapper is needed
- `withSizeBehaviour(...)`

### 7.2 Clip

`Clip` enforces scissor clipping for subtree rendering.

Use when child render should not escape a viewport.

### 7.3 Texture / AnimatedTexture

- `Texture`: static atlas UV blit
- `AnimatedTexture`: UV rectangle driven by `Variable<SimpleVec2i>`

Typical texture flow:

```java
new Texture.Builder(textureLocation)
    .withTextureSize(new SimpleVec2i(256, 256))
    .withUv(new SimpleVec2i(0, 0))
    .withUvSize(new SimpleVec2i(18, 18))
    .withSize(new SimpleVec2i(18, 18));
```

## 8. Theme Customization

Implement `Theme` to control visuals of primitives:

- button/container/scrollbar/tooltip/slot draw methods
- default text color

Default implementation is `MinecraftTheme.INSTANCE`.

Recommended approach:

1. start from `MinecraftTheme` behavior
2. override only what you need first
3. keep slot/button hitbox assumptions consistent with your art

## 9. Hosting In A Screen

Use `ScreenUIRenderer`:

```java
Minecraft.getInstance().setScreen(new ScreenUIRenderer(rootComponent));
```

Options:

- `new ScreenUIRenderer(component)`
- `new ScreenUIRenderer(component, renderBackground)`
- `new ScreenUIRenderer(component, renderBackground, theme)`

The renderer handles dynamic component ticks. It applies a mounted partial
commit where safe and falls back to a full rebuild when a subtree cannot be
updated safely.

## 10. Hosting In HUD

Use `HudUIRenderer` when UI is not a `Screen`:

```java
HudUIRenderer hud = new HudUIRenderer(rootComponent);

hud.tick();
hud.render(window, guiGraphics, partialTicks);
```

`HudUIRenderer` refreshes when:

- a dynamic component marks dirty
- GUI scaled width/height changes

Dirty dynamic subtrees use a partial commit where possible; a viewport resize
always rebuilds the HUD tree.

## 11. Dynamic UI Patterns

Use `DynamicUIComponent` for stateful behavior that requires rebuild.

Pattern:

1. store mutable state in fields / `Variable<T>`
2. trigger `rebuild()` on interaction/state change
3. renderer refreshes the affected mounted subtree on the next tick when safe,
   otherwise rebuilds the full component tree

Reference example: `src/main/java/com/github/wintersteve25/tau/tests/TestDynamic.java`.

## 12. Container Menu Usage

For inventory-backed UI, implement `UIMenu`.

You provide:

- `build(layout, theme, menu)`
- `getSize()`
- `getTitle()`
- optional slot/data behavior hooks

### 12.1 Server/client flow

1. create `TauMenuHolder` with your `UIMenu`
2. register menu screen via `TauMenuHelper.registerMenuScreen(...)`
3. open via `TauMenuHolder#openMenu(ServerPlayer, BlockPos)`

Inside `UIMenu#newMenu(...)`:

- server side: slots come from `getSlots(menu)` handlers
- client side: slot positions are discovered by building UI and collecting `MenuSlot`s

### 12.2 Slot handlers

Implement `ISlotHandler#setupSync(...)` to add real menu slots.

Built-ins:

- `ItemSlotHandler` for single item handler slot
- `PlayerInventoryHandler` for 3-row inventory + hotbar

If one UI descriptor materializes multiple vanilla slots, implement
`ISlotHandler#getSlotCount()` and `getSlotOffset(int)` as well. The offset is
relative to the descriptor origin; the screen applies its one-pixel slot-frame
inset.

## 13. Input And Event Notes

- Many interactive primitives implement `GuiEventListener` or `ContainerEventHandler`.
- Input correctness depends on layout offsets, clipping, and transform strategy.
- For transformed subtrees, translation is safest for exact input mapping.

## 14. Transform Usage Notes

Use `Transform` with `Transformation`s.

Important behavior:

- translation transforms participate in layout and input mapping
- non-translation transforms are visual-only best effort

So for precise clickable moved widgets, prefer `Transformation.translate(...)`.

## 15. Manual Test Entry Points

This project primarily validates UI behavior manually in-game.

Start from:

- `src/main/java/com/github/wintersteve25/tau/tests/TestAll.java`

Useful concrete examples:

- list scrolling: `TestListView`
- dynamic rebuild behavior: `TestDynamic`
- transform behavior: `TestTransform`
- text input: `TestTextField`
- slider: `TestSlider`
- menu/inventory visuals: `TestInventoryVisual`

## 16. Common Pitfalls

- Forgetting to bound widgets with `Sized`, causing unexpected dimensions.
- Using non-translation transform expecting perfect hit-testing.
- Not wiring menu screen registration for `UIMenu`.
- Doing expensive allocation each tick in dynamic components.
- Returning oversized `Sized` values relative to parent available size.

## 17. Quick Reference

- screen host: `new ScreenUIRenderer(root)`
- HUD host: `new HudUIRenderer(root)`
- menu contract: `UIMenu`
- default theme: `MinecraftTheme.INSTANCE`
- vector type: `SimpleVec2i`
- reactive value helper: `Variable<T>`

## 18. Complete Example: Screen

This is a full, copyable screen setup with:

- title text
- text field bound to state
- slider bound to state
- submit button

```java
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.interactable.Button;
import com.github.wintersteve25.tau.components.interactable.Slider;
import com.github.wintersteve25.tau.components.interactable.TextField;
import com.github.wintersteve25.tau.components.layout.Center;
import com.github.wintersteve25.tau.components.layout.Column;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.components.utils.Text;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.renderer.ScreenUIRenderer;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ExampleFormScreen implements UIComponent {
    private String name = "";
    private double volume = 0.5;

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        return new Center(
                new Column.Builder()
                        .withSpacing(6)
                        .build(
                                new Text.Builder("Example Form"),
                                new Sized(
                                        Size.staticSize(220, 20),
                                        new TextField.Builder()
                                                .withHintText(Component.literal("Player name"))
                                                .withOnChange(v -> name = v)
                                ),
                                new Sized(
                                        Size.staticSize(220, 20),
                                        new Slider.Builder()
                                                .withMinimum(0)
                                                .withMaximum(1)
                                                .withStepSize(0.1f)
                                                .withDecimalPlaces(1)
                                                .withValue(volume)
                                                .withOnValueChanged(v -> volume = v)
                                ),
                                new Sized(
                                        Size.staticSize(120, 20),
                                        new Button.Builder()
                                                .withOnPress(btn -> {
                                                    String message = "name=" + name + ", volume=" + volume;
                                                    Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
                                                })
                                                .build(new Center(new Text.Builder("Submit")))
                                )
                        )
        );
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ScreenUIRenderer(new ExampleFormScreen()));
    }
}
```

## 19. Complete Example: HUD

This example creates a tiny HUD panel that shows tick counter and can be rendered each frame.

```java
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.layout.Align;
import com.github.wintersteve25.tau.components.layout.Column;
import com.github.wintersteve25.tau.components.utils.Container;
import com.github.wintersteve25.tau.components.utils.Padding;
import com.github.wintersteve25.tau.components.utils.Text;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.layout.LayoutSetting;
import com.github.wintersteve25.tau.renderer.HudUIRenderer;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.Pad;

public final class ExampleHudRoot extends DynamicUIComponent {
    private int ticks;

    @Override
    public void tick() {
        ticks++;
        if (ticks % 20 == 0) {
            rebuild();
        }
    }

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        return new Align.Builder()
                .withHorizontal(LayoutSetting.START)
                .withVertical(LayoutSetting.START)
                .build(
                        new Padding(
                                new Pad.Builder().all(6).build(),
                                new Container.Builder()
                                        .withChild(
                                                new Column.Builder()
                                                        .withSpacing(2)
                                                        .build(
                                                                new Text.Builder("HUD Example"),
                                                                new Text.Builder("Ticks: " + ticks)
                                                        )
                                        )
                        )
                );
    }
}
```

Integrate in your HUD render hook:

```java
private static final HudUIRenderer HUD = new HudUIRenderer(new ExampleHudRoot());

// each frame
HUD.tick();
HUD.render(window, guiGraphics, partialTicks);
```

## 20. Complete Example: `UIMenu`

This example shows a basic menu with:

- a themed background panel
- player inventory visuals/slots
- a synced integer data slot (`clicks`)
- a button that updates that value server-side

```java
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.interactable.Button;
import com.github.wintersteve25.tau.components.inventory.PlayerInventory;
import com.github.wintersteve25.tau.components.layout.Column;
import com.github.wintersteve25.tau.components.layout.Row;
import com.github.wintersteve25.tau.components.utils.Container;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.components.utils.Text;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.menu.TauContainerMenu;
import com.github.wintersteve25.tau.menu.UIMenu;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.Size;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import com.github.wintersteve25.tau.utils.Variable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.Supplier;

public final class ExampleMenu implements UIMenu {
    private final Variable<Boolean> invEnabled = new Variable<>(true);
    private int clicks;

    @Override
    public UIComponent build(Layout layout, Theme theme, TauContainerMenu menu) {
        Optional<Supplier<Integer>> clickGetter = menu.getGetterForDataSlot("clicks");
        int currentClicks = clickGetter.map(Supplier::get).orElse(0);

        return new Container.Builder()
                .withChild(
                        new Column.Builder()
                                .withSpacing(6)
                                .build(
                                        new Text.Builder(Component.literal("Example Menu")),
                                        new Row.Builder().withSpacing(4).build(
                                                new Sized(
                                                        Size.staticSize(120, 20),
                                                        new Button.Builder()
                                                                .withOnPress(btn -> clicks++)
                                                                .build(new Text.Builder("Increment"))
                                                ),
                                                new Text.Builder(Component.literal("Clicks: " + currentClicks))
                                        ),
                                        new PlayerInventory(invEnabled)
                                )
                );
    }

    @Override
    public SimpleVec2i getSize() {
        return new SimpleVec2i(190, 120);
    }

    @Override
    public Component getTitle() {
        return Component.literal("Example Menu");
    }

    @Override
    public void addDataSlots(TauContainerMenu menu) {
        menu.addDataSlot("clicks", () -> clicks, v -> clicks = v);
    }

    @Override
    public ItemStack quickMoveStack(TauContainerMenu menu, Player player, int index) {
        return ItemStack.EMPTY;
    }
}
```

Then wire it:

1. create a `TauMenuHolder` for this `UIMenu`
2. register its screen using `TauMenuHelper.registerMenuScreen(...)`
3. open with `holder.openMenu(serverPlayer, pos)`

## 21. Example Index In This Repo

You can also reuse ready-made sample classes under `src/main/java/com/github/wintersteve25/tau/tests/`.

Entry hub:

- `TestAll`

Useful references:

- `TestButton`
- `TestTextField`
- `TestSlider`
- `TestListView`
- `TestTransform`
- `TestDynamic`
- `TestInventoryVisual`
