package com.github.wintersteve25.tau.components.render;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.menu.MenuSlot;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.theme.Theme;
import moe.liar.upsilon.Upsilon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Renderable;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import com.github.wintersteve25.tau.utils.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Component that applies render/input transformations to a child subtree.
 * <p>
 * Translation transforms are applied in layout space for reliable hit-testing.
 * Non-translation transforms are rendered as visual-only best effort.
 */
public final class Transform implements PrimitiveUIComponent, ContainerEventHandler {

    private final UIComponent child;
    private final Iterable<Transformation> transformations;
    private final List<GuiEventListener> childrenEventListeners;

    private boolean dragging;
    private GuiEventListener focused;

    /**
     * Creates a transform wrapper from vararg transformations.
     */
    public Transform(UIComponent child, Transformation... transformations) {
        this(child, Arrays.asList(transformations));
    }

    /**
     * Creates a transform wrapper from iterable transformations.
     */
    public Transform(UIComponent child, Iterable<Transformation> transformations) {
        this.child = child;
        this.transformations = transformations;
        this.childrenEventListeners = new ArrayList<>();
    }

    /**
     * Builds transformed child renderables/listeners and applies visual transforms.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        List<Transformation> visualOnlyTransforms = new ArrayList<>();
        Vector3f translation = new Vector3f();

        for (Transformation transformation : transformations) {
            if (transformation.isTranslationOnly()) {
                translation.add(transformation.getTranslation());
            } else {
                visualOnlyTransforms.add(transformation);
            }
        }

        if (!visualOnlyTransforms.isEmpty()) {
            Upsilon.LOGGER.warn("Transform only guarantees correct layout/input for translation transforms; applying non-translation transforms as visual-only best effort");
        }

        List<Renderable> children = new ArrayList<>();
        List<MenuSlot<?>> slots = new ArrayList<>();

        childrenEventListeners.clear();
        BuildContext innerContext = new BuildContext(children, context.tooltips(), context.dynamicUIComponents(), childrenEventListeners, slots);

        // Translation participates in layout for consistent input and slot mapping.
        Layout transformedLayout = layout.copy();
        transformedLayout.pushOffset(Axis.HORIZONTAL, Math.round(translation.x));
        transformedLayout.pushOffset(Axis.VERTICAL, Math.round(translation.y));

        SimpleVec2i size;
        try {
            size = UIBuilder.build(transformedLayout, theme, child, innerContext);
        } finally {
            transformedLayout.popOffset(Axis.VERTICAL);
            transformedLayout.popOffset(Axis.HORIZONTAL);
        }

        // Non-translation transforms are visual-only best effort.
        context.renderables().add((graphics, pMouseX, pMouseY, pPartialTicks) -> {
            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();

            for (Transformation transformation : visualOnlyTransforms) {
                transformation.transform(poseStack);
            }

            for (Renderable renderable : children) {
                renderable.render(graphics, pMouseX, pMouseY, pPartialTicks);
            }

            poseStack.popPose();
        });

        // Slots are adjusted only for visual-only transforms.
        for (MenuSlot<?> slot : slots) {
            for (Transformation transformation : visualOnlyTransforms) {
                transformation.transformPoint(slot.pos());
            }

            context.slots().add(slot);
        }

        return size;
    }

    /**
     * Returns child listeners collected during build.
     */
    @Override
    public List<? extends GuiEventListener> children() {
        return childrenEventListeners;
    }

    /**
     * Returns whether this container is in drag state.
     */
    @Override
    public boolean isDragging() {
        return dragging;
    }

    /**
     * Updates drag state for this container.
     */
    @Override
    public void setDragging(boolean pIsDragging) {
        dragging = pIsDragging;
    }

    /**
     * Returns currently focused child listener.
     */
    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return focused;
    }

    /**
     * Sets focused child listener.
     */
    @Override
    public void setFocused(@Nullable GuiEventListener pFocused) {
        focused = pFocused;
    }

    /**
     * Resolves child under mouse, accounting for visual-only transforms.
     */
    @Override
    public Optional<GuiEventListener> getChildAt(double pMouseX, double pMouseY) {
        Vector2d mousePos = new Vector2d(pMouseX, pMouseY);

        for (Transformation transformation : transformations) {
            if (!transformation.isTranslationOnly()) {
                transformation.transformPoint(mousePos);
            }
        }

        return ContainerEventHandler.super.getChildAt(mousePos.x, mousePos.y);
    }

    /**
     * Forwards click input to transformed child listeners.
     */
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        Vector2d mousePos = new Vector2d(pMouseX, pMouseY);

        for (Transformation transformation : transformations) {
            if (!transformation.isTranslationOnly()) {
                transformation.transformPoint(mousePos);
            }
        }

        return ContainerEventHandler.super.mouseClicked(mousePos.x, mousePos.y, pButton);
    }

    /**
     * Forwards release input to transformed child listeners.
     */
    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        Vector2d mousePos = new Vector2d(pMouseX, pMouseY);

        for (Transformation transformation : transformations) {
            if (!transformation.isTranslationOnly()) {
                transformation.transformPoint(mousePos);
            }
        }

        return ContainerEventHandler.super.mouseReleased(mousePos.x, mousePos.y, pButton);
    }

    /**
     * Forwards drag input to transformed child listeners.
     */
    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        Vector2d mousePos = new Vector2d(pMouseX, pMouseY);

        for (Transformation transformation : transformations) {
            if (!transformation.isTranslationOnly()) {
                transformation.transformPoint(mousePos);
            }
        }

        return ContainerEventHandler.super.mouseDragged(mousePos.x, mousePos.y, pButton, pDragX, pDragY);
    }

    /**
     * Forwards wheel input to transformed child listeners.
     */
    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        Vector2d mousePos = new Vector2d(pMouseX, pMouseY);

        for (Transformation transformation : transformations) {
            if (!transformation.isTranslationOnly()) {
                transformation.transformPoint(mousePos);
            }
        }

        return ContainerEventHandler.super.mouseScrolled(mousePos.x, mousePos.y, pScrollX, pScrollY);
    }
}
