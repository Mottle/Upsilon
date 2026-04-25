# Upsilon UI Agent Guide

> This document is for coding agents.
>
> Use it as a deterministic workflow when generating Upsilon UI code in this repo.

This guide is intentionally operational: decide target, pick template, apply binding pattern, then run a completion checklist.

Related docs:

- practical API usage: `docs/usage.md`
- internal design details: `docs/architecture.md`

---

## Step 1: Determine Request Shape

Before writing code, identify three dimensions.

### 1.1 Host target

- **Screen UI**: one-off or navigable windows -> `ScreenUIRenderer`
- **HUD UI**: always-on overlay -> `HudUIRenderer`
- **Menu UI**: needs server-side data/slots -> `UIMenu` + `TauContainerMenu` + `TauContainerScreen`
- **Reusable subtree only**: return `UIComponent`, do not open/render directly

### 1.2 State model

- **Static UI**: no mutable state
- **Client-local mutable UI**: class fields + callbacks
- **Dynamic rebuilt UI**: subclass `DynamicUIComponent`, call `rebuild()` when state changes
- **Server-synced menu state**: `UIMenu#addDataSlots(...)`, slot handlers, `TauContainerMenu`

### 1.3 Interaction complexity

- basic click/input (`Button`, `TextField`, `Slider`)
- scroll and clipping (`ListView`, `Clip`)
- transforms (`Transform` with `Transformation`)

---

## Step 2: Choose Host Template

Use one template exactly, then extend.

### 2.1 Client Screen Template

```java
public final class ExampleScreenUI implements UIComponent {
    @Override
    public UIComponent build(Layout layout, Theme theme) {
        return new Center(new Text.Builder("Hello Upsilon"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ScreenUIRenderer(new ExampleScreenUI()));
    }
}
```

When to choose:

- no server inventory/menu synchronization needed
- opened from keybind/command/client event

### 2.2 HUD Template

```java
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
                .build(new Text.Builder("Ticks: " + ticks));
    }
}
```

Render integration:

```java
private static final HudUIRenderer HUD = new HudUIRenderer(new ExampleHudRoot());

// each frame
HUD.tick();
HUD.render(window, guiGraphics, partialTicks);
```

### 2.3 Menu Template (Server-Synced)

```java
public final class ExampleMenu implements UIMenu {
    private int clicks;

    @Override
    public UIComponent build(Layout layout, Theme theme, TauContainerMenu menu) {
        int currentClicks = menu.getGetterForDataSlot("clicks").map(Supplier::get).orElse(0);

        return new Column.Builder().withSpacing(4).build(
                new Text.Builder(Component.literal("Menu Example")),
                new Button.Builder()
                        .withOnPress(btn -> clicks++)
                        .build(new Text.Builder(Component.literal("Increment"))),
                new Text.Builder(Component.literal("Clicks: " + currentClicks))
        );
    }

    @Override
    public SimpleVec2i getSize() {
        return new SimpleVec2i(176, 166);
    }

    @Override
    public Component getTitle() {
        return Component.literal("Example Menu");
    }

    @Override
    public void addDataSlots(TauContainerMenu menu) {
        menu.addDataSlot("clicks", () -> clicks, v -> clicks = v);
    }
}
```

Holder + registration skeleton:

```java
public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, Upsilon.MOD_ID);

public static final TauMenuHolder EXAMPLE_MENU =
        new TauMenuHolder(MENUS, ExampleMenu::new, "example_menu", FeatureFlags.DEFAULT_FLAGS);

@SubscribeEvent
public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
    TauMenuHelper.registerMenuScreen(event, EXAMPLE_MENU);
}
```

Open from server side:

```java
EXAMPLE_MENU.openMenu((ServerPlayer) player, pos);
```

---

## Step 3: Pick Component Composition Strategy

Choose containers from layout intent, not habit.

### 3.1 Layout mapping

- vertical list -> `Column`
- horizontal row -> `Row`
- overlap/layers -> `Stack`
- center child -> `Center`
- explicit anchor -> `Align`
- strict dimensions -> `Sized`
- spacing/inset -> `Padding` / `Spacer`
- absolute anchor -> `Positioned`

### 3.2 Size policy

For `Row`/`Column`/`Stack`/`Container`:

- `FlexSizeBehaviour.MIN` -> size from content
- `FlexSizeBehaviour.MAX` -> consume available area

### 3.3 Common interactive widgets

- `Button`: click callback
- `TextField`: text input + validation + formatter
- `Slider`: numeric range input
- `ListView`: scrollable list with clipping and input remap

---

## Step 4: Data And Rebuild Patterns

Use exactly one pattern per state path.

### Pattern A: Plain local state (client)

Use simple fields and callbacks.

```java
private String name = "";

new TextField.Builder()
    .withOnChange(v -> name = v);
```

### Pattern B: Dynamic rebuild state

Use `DynamicUIComponent` and call `rebuild()` after mutation.

```java
public final class DynamicPanel extends DynamicUIComponent {
    private boolean toggled;

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        return toggled ? new Text.Builder("ON") : new Text.Builder("OFF");
    }

    public void toggle() {
        toggled = !toggled;
        rebuild();
    }
}
```

### Pattern C: Menu synced integers

Use named data slots.

```java
@Override
public void addDataSlots(TauContainerMenu menu) {
    menu.addDataSlot("energy", () -> energy, v -> energy = v);
}
```

Read in `build(...)`:

```java
int energyNow = menu.getGetterForDataSlot("energy").map(Supplier::get).orElse(0);
```

### Pattern D: Menu slot synchronization

Use `ISlotHandler` implementations and `MenuSlot` emission through UI components (`ItemSlot`, `PlayerInventory`).

---

## Step 5: Transform And Input Rules

`Transform` is safe only under known constraints:

- translation transforms are layout/input reliable
- non-translation transforms are visual-only best effort

Agent rule:

- if precise hit testing is required, prefer layout wrappers (`Align`, `Positioned`, `Padding`) or translation transform only
- avoid promising exact interaction behavior under scale/rotation transforms

---

## Step 6: Menu-Specific Decision Tree

Use this tree when user asks for inventory/container UI.

```mermaid
flowchart TD
    A[Need server data or slots?] -->|No| B[Use ScreenUIRenderer or HudUIRenderer]
    A -->|Yes| C[Implement UIMenu]
    C --> D[Define size/title/build]
    D --> E[Add data slots if needed]
    D --> F[Provide slot handlers if needed]
    E --> G[Create TauMenuHolder]
    F --> G
    G --> H[Register screen with TauMenuHelper.registerMenuScreen]
    H --> I[Open using holder.openMenu(serverPlayer, pos)]
```

---

## Step 7: Documentation Navigation Map

When agent needs detail, open targeted docs instead of guessing.

- usage API patterns: `docs/usage.md`
- internals and lifecycle: `docs/architecture.md`
- manual behavior samples: `src/main/java/com/github/wintersteve25/tau/tests/TestAll.java`

Useful concrete examples:

- dynamic rebuild: `src/main/java/com/github/wintersteve25/tau/tests/TestDynamic.java`
- scrolling list: `src/main/java/com/github/wintersteve25/tau/tests/TestListView.java`
- transform behavior: `src/main/java/com/github/wintersteve25/tau/tests/TestTransform.java`
- menu-like inventory visual: `src/main/java/com/github/wintersteve25/tau/tests/TestInventoryVisual.java`

---

## Step 8: Pre-Completion Checklist (Agent)

Before returning code, verify all items:

- [ ] Host type matches request (screen/hud/menu/component)
- [ ] Menu request uses `UIMenu` path, not plain `ScreenUIRenderer`
- [ ] `UIComponent#build(...)` chain is valid (`null` termination or primitive path)
- [ ] Layout wrappers are push/pop safe (no leaked state assumptions)
- [ ] Dynamic mutations call `rebuild()` when UI must refresh
- [ ] Non-translation transform limitations are acknowledged
- [ ] Example code uses currently available APIs in this repo
- [ ] Added docs links if a new guide file was created

---

## Step 9: Common Failure Modes And Fixes

### Symptom: Widget visible but not clickable

Likely causes:

- incorrect layout position/size
- clipping region excludes mouse area
- non-translation transform mismatch

Fix:

1. remove transform first
2. verify with `Center` + `Sized`
3. reintroduce transform as translation only

### Symptom: Dynamic state changes but UI does not update

Likely cause: state changed but `rebuild()` not triggered.

Fix:

- mutate state -> call `rebuild()` in dynamic component

### Symptom: Menu opens but slots are wrong

Likely causes:

- slot handler coordinates incorrect
- screen registration missing
- client slot discovery path bypassed

Fix:

1. ensure `TauMenuHelper.registerMenuScreen(...)` is called
2. verify `ISlotHandler#setupSync(...)` x/y offsets
3. verify `UIMenu#build(...)` and menu size alignment
