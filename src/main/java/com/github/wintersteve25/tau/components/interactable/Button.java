package com.github.wintersteve25.tau.components.interactable;

import com.github.wintersteve25.tau.build.*;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.ClientSoundHelper;
import com.github.wintersteve25.tau.utils.InteractableState;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.function.Consumer;

/**
 * Primitive clickable button component with a single child content node.
 */
public final class Button implements PrimitiveUIComponent, GuiEventListener, MountStateHost<Button.ButtonMountState> {

    private final Consumer<Integer> onPress;
    private final UIComponent child;

    private ButtonMountState activeMountState;

    private boolean focus;

    /**
     * Creates a button.
     *
     * @param onPress callback receiving the clicked mouse button id
     * @param child   child component rendered inside the button frame
     */
    public Button(Consumer<Integer> onPress, UIComponent child) {
        this.onPress = onPress;
        this.child = child;
    }

    /**
     * Builds button visuals and child subtree for the current layout.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext buildContext) {
        BuildSession session = UIBuilder.currentSession();
        if (session != null && session.getMode() == com.github.wintersteve25.tau.build.BuildMode.MOUNTED_COMMITTABLE) {
            ButtonMountState state = session.getOrCreateStagedState(this, ButtonMountState::new);
            state.width = layout.getWidth();
            state.height = layout.getHeight();
            state.x = layout.getPosition(Axis.HORIZONTAL, state.width);
            state.y = layout.getPosition(Axis.VERTICAL, state.height);
            session.addRenderable((graphics, pMouseX, pMouseY, pPartialTicks) -> theme.drawButton(graphics, state.x, state.y, state.width, state.height, pPartialTicks, pMouseX, pMouseY, this.getInteractableState(pMouseX, pMouseY)));
        } else {
            int width = layout.getWidth();
            int height = layout.getHeight();
            int x = layout.getPosition(Axis.HORIZONTAL, width);
            int y = layout.getPosition(Axis.VERTICAL, height);
            buildContext.renderables().add((graphics, pMouseX, pMouseY, pPartialTicks) -> theme.drawButton(graphics, x, y, width, height, pPartialTicks, pMouseX, pMouseY, this.getInteractableState(pMouseX, pMouseY)));
        }

        UIBuilder.build(layout, theme, child, buildContext);

        return layout.getSize();
    }

    /**
     * Handles left-click presses inside the button bounds.
     */
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 0 && onPress != null && isHovered((int) pMouseX, (int) pMouseY)) {
            onPress.accept(pButton);
            ClientSoundHelper.playButtonClick();
            return true;
        }

        return false;
    }

    /**
     * Returns whether this button is currently focused.
     */
    @Override
    public boolean isFocused() {
        return focus;
    }

    /**
     * Updates focus flag for keyboard/gamepad navigation.
     */
    @Override
    public void setFocused(boolean pFocused) {
        focus = pFocused;
    }

    private boolean isHovered(int pMouseX, int pMouseY) {
        ButtonMountState state = activeMountState;
        return state != null && SimpleVec2i.within(pMouseX, pMouseY, state.x, state.y, state.width, state.height);
    }

    private InteractableState getInteractableState(int pMouseX, int pMouseY) {
        if (onPress == null) {
            return InteractableState.DISABLED;
        } else if (isHovered(pMouseX, pMouseY)) {
            return InteractableState.HOVERED;
        }

        return InteractableState.IDLE;
    }

    @Override
    public ButtonMountState getActiveMountState() {
        return activeMountState;
    }

    @Override
    public void setActiveMountState(ButtonMountState state) {
        this.activeMountState = state;
    }

    public static final class Builder {
        private Consumer<Integer> onPress;

        /**
         * Creates a new button builder.
         */
        public Builder() {
        }

        /**
         * Sets click callback.
         */
        public Builder withOnPress(Consumer<Integer> onPress) {
            this.onPress = onPress;
            return this;
        }

        /**
         * Builds a button with the given child content.
         */
        public Button build(UIComponent child) {
            return new Button(onPress, child);
        }
    }

    public static final class ButtonMountState implements MountState {
        int width;
        int height;
        int x;
        int y;
    }
}
