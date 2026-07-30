# Upsilon

Upsilon is a composable UI library for Minecraft NeoForge mods.

It provides a Flutter-like composition style for building screens, HUD overlays, and container-backed UIs with a consistent component model.

## Highlights

- Composable `UIComponent` tree model
- Built-in layout primitives (`Column`, `Row`, `Stack`, `Center`, `Align`, `Sized`, etc.)
- Interactive widgets (`Button`, `TextField`, `Slider`, `ListView`)
- Runtime render targets for screen, HUD, and menu/container UIs
- Theme abstraction (`Theme`) with default vanilla-style implementation (`MinecraftTheme`)

## Runtime Targets

Upsilon supports three main hosting modes:

1. Screen UI via `ScreenUIRenderer`
2. HUD UI via `HudUIRenderer`
3. Container/menu UI via `UIMenu` + `TauContainerScreen`

## Quick Start (Screen)

```java
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.interactable.Button;
import com.github.wintersteve25.tau.components.layout.Center;
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
                        Size.staticSize(120, 20),
                        new Button.Builder()
                                .withOnPress(btn -> Minecraft.getInstance().player
                                        .sendSystemMessage(Component.literal("Clicked")))
                                .build(new Center(new Text.Builder("Click Me")))
                )
        );
    }
}
```

Open it:

```java
Minecraft.getInstance().setScreen(new ScreenUIRenderer(new ExampleScreenUI()));
```

## Build And Run

Project baseline in this branch:

- Minecraft `1.21.1`
- NeoForge `21.1.20`
- Java `21`

Common commands:

- Build jar: `bash gradlew build`
- Run JVM tests: `bash gradlew test`
- Run client: `bash gradlew runClient`
- Run server: `bash gradlew runServer`
- Run datagen: `bash gradlew runData`

## Documentation

- Usage guide: [`docs/usage.md`](docs/usage.md)
- Architecture guide: [`docs/architecture.md`](docs/architecture.md)
- Agent guide: [`docs/agent-guide.md`](docs/agent-guide.md)

## Testing

JVM regression tests cover build-pipeline, layout, list-input, and menu-slot
invariants. Run them with `bash gradlew test`.

Visual and game-input behavior still needs in-game validation:

- test hub: `src/main/java/com/github/wintersteve25/tau/tests/TestAll.java`
- launch the development client and press `,` to open the test hub

## Project Metadata

- mod id: `upsilon`
- runtime entrypoint: `moe.liar.upsilon.Upsilon`
- package root: `com.github.wintersteve25.tau`

## License

[MIT](LICENSE)
