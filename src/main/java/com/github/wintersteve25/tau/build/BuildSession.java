package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.menu.MenuSlot;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.*;
import java.util.function.Supplier;

/**
 * Thread-local build session carrying mode, context, and staged state.
 */
public final class BuildSession {
    private final BuildMode mode;
    private final BuildContext rootContext;
    private final Deque<BuildContext> contextStack = new ArrayDeque<>();
    private final IdentityHashMap<Object, MountState> stagedStates = new IdentityHashMap<>();
    private final IdentityHashMap<UIComponent, Boolean> uniqueness = new IdentityHashMap<>();
    private final List<ComponentMount> rootMounts = new ArrayList<>();
    private final Deque<ComponentMount> mountStack = new ArrayDeque<>();

    public BuildSession(BuildMode mode, BuildContext context) {
        this.mode = mode;
        this.rootContext = context;
        this.contextStack.push(context);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void promoteStagedStates(IdentityHashMap<Object, MountState> states) {
        for (Object owner : new ArrayList<>(states.keySet())) {
            if (owner instanceof MountStateHost host) {
                host.setActiveMountState(states.get(owner));
            }
        }
        states.clear();
    }

    public BuildMode getMode() {
        return mode;
    }

    public BuildContext getContext() {
        return contextStack.peek();
    }

    public BuildContext getRootContext() {
        return rootContext;
    }

    public void pushContext(BuildContext context) {
        contextStack.push(context);
    }

    public void popContext() {
        if (contextStack.size() <= 1) {
            throw new IllegalStateException("Cannot pop root build context");
        }
        contextStack.pop();
    }

    @SuppressWarnings("unchecked")
    public <S extends MountState> S getOrCreateStagedState(Object owner, Supplier<S> factory) {
        return (S) stagedStates.computeIfAbsent(owner, ignored -> factory.get());
    }

    public void clearStagedState(Object owner) {
        stagedStates.remove(owner);
    }

    public void clearStagedStatesForSubtree(List<ComponentMount> mounts) {
        for (ComponentMount mount : mounts) {
            stagedStates.remove(mount.getOwner());
        }
    }

    public IdentityHashMap<Object, MountState> snapshotStagedStates() {
        return new IdentityHashMap<>(stagedStates);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void promoteStagedStates() {
        promoteStagedStates(stagedStates);
    }

    public void discardStagedStates() {
        stagedStates.clear();
    }

    public boolean shouldCollectArtifacts() {
        return mode != BuildMode.MOUNTED_MEASURE;
    }

    public void addRenderable(Renderable renderable) {
        if (shouldCollectArtifacts()) {
            getContext().renderables().add(renderable);
        }
    }

    public void addTooltip(Renderable tooltip) {
        if (shouldCollectArtifacts()) {
            getContext().tooltips().add(tooltip);
        }
    }

    public void addListener(GuiEventListener listener) {
        if (shouldCollectArtifacts()) {
            getContext().eventListeners().add(listener);
        }
    }

    public void addDynamic(com.github.wintersteve25.tau.components.base.DynamicUIComponent dynamic) {
        if (shouldCollectArtifacts()) {
            getContext().dynamicUIComponents().add(dynamic);
        }
    }

    public void addSlot(MenuSlot<?> slot) {
        if (shouldCollectArtifacts()) {
            getContext().slots().add(slot);
        }
    }

    public void checkUnique(UIComponent component) {
        if (uniqueness.put(component, Boolean.TRUE) != null) {
            throw new IllegalStateException("Duplicate mounted component instance: " + component.getClass().getName());
        }
    }

    public List<ComponentMount> getRootMounts() {
        return rootMounts;
    }

    public Deque<ComponentMount> getMountStack() {
        return mountStack;
    }
}
