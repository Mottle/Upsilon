package com.github.wintersteve25.tau.tests;

import com.github.wintersteve25.tau.build.PartialCommitUnsafe;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.interactable.Button;
import com.github.wintersteve25.tau.components.layout.Stack;
import com.github.wintersteve25.tau.components.render.Render;
import com.github.wintersteve25.tau.components.utils.Positioned;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.Size;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * A minimal full-rebuild test page.
 * The visual state is drawn by a single Render component so any mismatch is
 * easy to diagnose. The toggle button stays fixed at the bottom.
 */
public class TestDynamic extends DynamicUIComponent implements PartialCommitUnsafe {
    private boolean expanded;
    private int clickCount;

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        return new Stack(
                com.github.wintersteve25.tau.utils.FlexSizeBehaviour.MAX,
                new Positioned(
                        new com.github.wintersteve25.tau.utils.SimpleVec2i(180, 80),
                        new Sized(
                                expanded ? Size.staticSize(320, 220) : Size.staticSize(220, 140),
                                new Render(this::renderState)
                        )
                ),
                new Positioned(
                        new com.github.wintersteve25.tau.utils.SimpleVec2i(320, 340),
                        new Sized(
                                Size.staticSize(160, 20),
                                new Button.Builder()
                                        .withOnPress(button -> {
                                            expanded = !expanded;
                                            clickCount++;
                                            rebuild();
                                        })
                                        .build(new com.github.wintersteve25.tau.components.utils.Text.Builder("Toggle Dynamic"))
                        )
                )
        );
    }

    private void renderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, int x, int y, int width, int height) {
        int color = expanded ? 0xFFFF3B30 : 0xFF34C759;
        graphics.fill(x, y, x + width, y + height, color);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, expanded ? "EXPANDED" : "COMPACT", x + 8, y + 8, 0xFFFFFFFF, false);
        graphics.drawString(font, "Click count: " + clickCount, x + 8, y + 24, 0xFFFFFFFF, false);
        graphics.drawString(font, "Size: " + width + "x" + height, x + 8, y + 40, 0xFFFFFFFF, false);
    }
}
