package com.github.wintersteve25.tau.renderer;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Stable root event dispatcher that reads the current listener tree lazily.
 */
public final class RootInputDispatcher implements ContainerEventHandler {
    private final Supplier<List<GuiEventListener>> childrenSupplier;
    private GuiEventListener focused;
    private boolean dragging;

    public RootInputDispatcher(Supplier<List<GuiEventListener>> childrenSupplier) {
        this.childrenSupplier = childrenSupplier;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return childrenSupplier.get();
    }

    @Override
    public boolean isDragging() {
        return dragging;
    }

    @Override
    public void setDragging(boolean pIsDragging) {
        dragging = pIsDragging;
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return focused;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener pFocused) {
        focused = pFocused;
    }

    public void clearFocusedIfMissing() {
        if (focused != null && !childrenSupplier.get().contains(focused)) {
            focused = null;
            dragging = false;
        }
    }
}
