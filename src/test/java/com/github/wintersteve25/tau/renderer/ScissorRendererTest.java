package com.github.wintersteve25.tau.renderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ScissorRendererTest {

    @Test
    void rendersWithGuiCoordinateBounds() {
        GuiGraphics graphics = mock(GuiGraphics.class);
        Renderable child = mock(Renderable.class);

        ScissorRenderer.render(graphics, 10, 20, 30, 40, List.of(child), 4, 5, 0.5F);

        InOrder order = inOrder(graphics, child);
        order.verify(graphics).enableScissor(10, 20, 40, 60);
        order.verify(child).render(graphics, 4, 5, 0.5F);
        order.verify(graphics).disableScissor();
    }

    @Test
    void nestedClips_preserveStackOrder() {
        GuiGraphics graphics = mock(GuiGraphics.class);
        Renderable innerClip = (g, mouseX, mouseY, partialTicks) ->
                ScissorRenderer.render(g, 15, 25, 5, 6, List.of(), mouseX, mouseY, partialTicks);

        ScissorRenderer.render(graphics, 10, 20, 30, 40, List.of(innerClip), 4, 5, 0.5F);

        InOrder order = inOrder(graphics);
        order.verify(graphics).enableScissor(10, 20, 40, 60);
        order.verify(graphics).enableScissor(15, 25, 20, 31);
        order.verify(graphics, times(2)).disableScissor();
    }

    @Test
    void failingChild_restoresScissorStack() {
        GuiGraphics graphics = mock(GuiGraphics.class);
        Renderable failingChild = (g, mouseX, mouseY, partialTicks) -> {
            throw new IllegalStateException("expected test failure");
        };

        assertThrows(IllegalStateException.class,
                () -> ScissorRenderer.render(graphics, 10, 20, 30, 40, List.of(failingChild), 4, 5, 0.5F));

        verify(graphics).disableScissor();
    }
}
