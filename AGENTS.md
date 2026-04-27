# AGENTS.md — Upsilon (Minecraft NeoForge UI Library)

Compact reference for agents working in this repo.

## Project Basics

- **What**: Minecraft NeoForge mod — a UI widget library ("Upsilon") with Flutter-like composition.
- **MC version**: 1.21.1, **NeoForge**: 21.1.20, **Java**: 21.
- **Package root**: `com.github.wintersteve25.tau`
- **Mod entrypoint**: `moe.liar.upsilon.Upsilon`
- **Runtime mod id / resource namespace**: `upsilon`
- **Build tool**: Gradle (NeoGradle userdev plugin `7.0.152`).

## Build & Run

| Goal | Command |
|------|---------|
| Build JAR | `./gradlew build` |
| Run client | `./gradlew runClient` |
| Run server | `./gradlew runServer` |
| Run data generators | `./gradlew runData` |
| Publish local | `./gradlew publishToMavenLocal` |

- Data generators write to `src/generated/resources` (already included in `sourceSets.main.resources`).
- `gradle.properties` sets `org.gradle.daemon=false` and `-Xmx4G`.
- **Rule**: when running Gradle in this repo, prefer `powershell.exe ./gradlew ...` over `bash gradlew ...`.
- **WSL/Linux gotcha**: `gradlew` has Windows (CRLF) line endings. In this repo the most reliable path is usually `powershell.exe ./gradlew ...`.
- If PowerShell is unavailable, run with `bash gradlew ...` or convert line endings first.

## Architecture

### Core abstractions
- **`UIComponent`** (`components/base/UIComponent.java`) — root interface; `build(Layout, Theme)` returns next node in tree.
- **`PrimitiveUIComponent`** — leaf nodes that actually render.
- **`DynamicUIComponent`** — nodes that can mark themselves `dirty` and rebuild at tick time.
- **`UIBuilder.build(...)`** — recursively walks the component tree and collects `Renderable`s, `GuiEventListener`s, and `MenuSlot`s into a `BuildContext`.

### Partial-rebuild pipeline (v2.1+)
- **`BuildMode`** — 三态：`MOUNTLESS` / `MOUNTED_COMMITTABLE` / `MOUNTED_MEASURE`
- **`BuildSession`** — ThreadLocal 构建会话，管理当前 mode、context 栈和 session-scoped staged state
- **`BuildResult`** — 包含 `BuildContext`、挂载树（`rootMounts/preorderMounts/dynamicMounts`）、staged states
- **`ComponentMount`** — 挂载树节点，记录 `ContextRanges`、`artifactContext`、`builtSize`
- **`MountState` / `MountStateHost<S>`** — 组件上的 active mount state 接口；staged 写 session，commit 后才 promote 到 active
- **`PartialCommitPlan` / `PartialCommitUnsafe`** — partial commit 候选与 unsafe 标记
- **`UIBuilder.planPartialCommit(...)`** — 从 dirty mount 向上回流选提交点，生成内层 context 感知的提交计划
- **`UIBuilder.applyPartialCommit(...)`** — 在 direct artifact context 中 splice 五类 artifact，rebind nested inner-context mounts，并用 candidate subtree 替换 active subtree
- **上下文栈**：`Transform/ListView/Clip` 通过 `session.pushContext/popContext` 包裹内层 builds；partial commit 必须尊重这些 inner context 边界，不能把 child listeners 平铺回 root

### Renderers
- **`ScreenUIRenderer`** — wraps a `UIComponent` as a Minecraft `Screen`.
- **`HudUIRenderer`** — renders a `UIComponent` to the HUD; auto-rebuilds on window resize.

### Menu/Container support
- **`UIMenu`** — interface for container-backed UIs; provides `build(...)`, slot handlers, data slots, and screen factory hooks.
- **`TauContainerMenu`** / **`TauContainerScreen`** — server/client container implementations.
- **`TauMenuHelper.registerMenuScreen(...)`** — binds a `TauMenuHolder` to a screen during `RegisterMenuScreensEvent`.

### Theming
- **`Theme`** interface defines draw methods for buttons, containers, scrollbars, tooltips, and slots.
- Default implementation: **`MinecraftTheme.INSTANCE`**.

## Source Layout

```
src/main/java/com/github/wintersteve25/tau/
  Tau.java                     # Minimal mod entrypoint
  build/                       # UIBuilder, BuildContext, BuildResult, BuildSession, BuildMode
                                # ComponentMount, ContextRanges, MountState, MountStateHost
                                # PartialCommitPlan, PartialCommitUnsafe, BuilderShell
  components/
    base/                      # UIComponent, PrimitiveUIComponent, DynamicUIComponent
    layout/                    # Stack, Column, Row, Center, Align, Spacer
    interactable/              # Button, TextField, Slider, ListView
    render/                    # Render, RenderableComponent, Transform
    utils/                     # Container, Sized, Padding, Positioned, Text, Texture, Tooltip, Clip, WidgetWrapper, WidgetFactoryWrapper
    inventory/                 # ItemSlot, PlayerInventory
    animated/                  # AnimatedTexture
  layout/                      # Layout, LayoutSetting, Axis, Size, FlexSizeBehaviour
  renderer/                    # ScreenUIRenderer, HudUIRenderer, RootInputDispatcher
  menu/                        # UIMenu, TauContainerMenu, TauContainerScreen, TauMenuHelper, TauMenuHolder, MenuSlot
  theme/                       # Theme, MinecraftTheme
  tests/                       # In-game manual test screens (see below)
  utils/                       # Color, SimpleVec2i, Transformation, Variable, ClientSoundHelper, ...
src/main/java/moe/liar/upsilon/
  Upsilon.java                 # Mod entrypoint
  client/UpsilonClientTestHooks.java  # Dev hotkey (`,`) to open TestAll
src/main/resources/META-INF/
  neoforge.mods.toml           # Mod metadata (templated from gradle.properties)
  accesstransformer.cfg        # Exposes AbstractContainerMenu#dataSlots and Slot.x/y
```

## Testing

- There is a meaningful JVM test suite under `src/test/java/com/github/wintersteve25/tau/build/` covering `BuildContext`, `BuildSession`, `BuildResult`, `ComponentMount`, partial-commit planning, and integration-level runtime invariants.
- Run targeted JVM verification with `powershell.exe ./gradlew test --tests com.github.wintersteve25.tau.build.UIBuilderIntegrationTest` or `powershell.exe ./gradlew build` for the full build.
- Test UIs live in `src/main/java/.../tau/tests/`.
- `TestAll.java` is a hub screen with buttons that open each individual test screen.
- To run tests: launch the client, press `,` (comma key, registered by `UpsilonClientTestHooks`) to open `TestAll`.
- `src/main/java/moe/liar/upsilon/client/UpsilonClientTestHooks.java` registers the dev hotkey via `@EventBusSubscriber` — no manual code changes needed.
- For partial-runtime changes, start manual regression with:
  - `TestPartialText`
  - `TestPartialButton`
  - `TestPartialTransform`
  - `TestPartialListView`
  - `TestCorrectnessScenario`
- `TestCorrectnessScenario` is the current high-complexity acceptance page. It combines filtering, repeated row-local partial commits, translated panels, widgets, tooltips, clipping, and scrolling in one screen.
- `TestDynamic` is intentionally `PartialCommitUnsafe` and exercises full rebuild behavior, not partial-runtime correctness.

## Important Conventions

- **Composition over inheritance**: widgets are built by nesting `UIComponent` instances (e.g. `new Center(new Sized(..., new Text.Builder(...)))`).
- **Size behavior**: `Size.staticSize(...)`, `Size.flexible()`, and `FlexSizeBehaviour` control layout sizing.
- **`SimpleVec2i`** (formerly `Vector2i`) is the repo’s 2D integer vector type.
- **Sound**: use `ClientSoundHelper` to play sounds from UI code (moved out of `UIComponent` in v2.1.0).
- **Variable**: use `Variable<T>` for reactive values inside `DynamicUIComponent` without rebuilding the whole tree.
- **Partial-safe widgets**: `WidgetWrapper` is legacy/full-rebuild-only; partial-safe widget components should use `WidgetFactoryWrapper`.
- **Hit-testing in manual tests**: when validating container boundaries (`Transform`, `ListView`, clip/translation interactions), prefer real bounded widgets like `Button`. Bare `GuiEventListener` test components can mask boundary bugs by accepting clicks without geometry.

## Release & CI

- Workflow: `.github/workflows/release.yml`.
- Trigger: push to branch `1.20-release` (legacy branch name).
- Publishes to:
  - Saps Maven (`https://maven.saps.dev/releases`) — requires `SAPS_TOKEN`
  - `mavenLocal()` and local `repo/` directory
  - Modrinth & CurseForge via `mc-publish`
- Version and metadata are driven by `gradle.properties` (`mod_version`, `neo_version`, `minecraft_version`, etc.).

## Gotchas

- `gradlew` CRLF endings on Linux/WSL — prefer `powershell.exe ./gradlew ...`; only fall back to `bash gradlew ...` or `sed -i 's/\r$//' gradlew` when PowerShell is unavailable.
- Access transformers live in `src/main/resources/META-INF/accesstransformer.cfg`; build.gradle wires them via `minecraft.accessTransformers.files`.
- Current transformers: `AbstractContainerMenu#dataSlots` (protected → public), `Slot#x` and `Slot#y` (final → public-f).
- Parchment mappings are configured in `gradle.properties` (`neogradle.subsystems.parchment.*`).
- The `runs/` directory is gitignored; dev run configs are generated by NeoGradle.
