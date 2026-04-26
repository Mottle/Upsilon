package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.*;

/**
 * Builds composed {@link UIComponent} trees into concrete render/runtime artifacts.
 */
public class UIBuilder {
    private static final ThreadLocal<Deque<BuildSession>> ACTIVE_SESSION = ThreadLocal.withInitial(ArrayDeque::new);

    public static BuildSession currentSession() {
        Deque<BuildSession> sessions = ACTIVE_SESSION.get();
        return sessions.isEmpty() ? null : sessions.peek();
    }

    public static List<ComponentMount> collectDirtyDynamicMounts(BuildResult activeBuild) {
        IdentityHashMap<DynamicUIComponent, ComponentMount> dirty = new IdentityHashMap<>();

        for (ComponentMount mount : activeBuild.dynamicMounts()) {
            DynamicUIComponent dynamic = mount.getDynamicOwner();
            if (dynamic == null) {
                continue;
            }

            dynamic.tick();
            if (dynamic.dirty) {
                dynamic.dirty = false;
                dirty.put(dynamic, mount);
            }
        }

        return new ArrayList<>(dirty.values());
    }

    public static List<ComponentMount> filterTopLevelDirty(List<ComponentMount> dirtyMounts) {
        List<ComponentMount> result = new ArrayList<>();

        outer:
        for (ComponentMount mount : dirtyMounts) {
            ComponentMount current = mount.getParent();
            while (current != null) {
                if (dirtyMounts.contains(current)) {
                    continue outer;
                }
                current = current.getParent();
            }
            result.add(mount);
        }

        return result;
    }

    /**
     * Builds a UI component tree and collects renderables/listeners/slots into a context.
     *
     * @param layout      layout constraints and positioning context for the root
     * @param theme       active theme used by primitive components
     * @param uiComponent root component to build
     * @param context     build artifact collector
     * @return accumulated size of the built branch
     */
    public static SimpleVec2i build(Layout layout, Theme theme, UIComponent uiComponent, BuildContext context) {
        if (!ACTIVE_SESSION.get().isEmpty()) {
            return buildMounted(layout, theme, uiComponent, ACTIVE_SESSION.get().peek(), SimpleVec2i.zero());
        }
        return build(layout, theme, uiComponent, context, SimpleVec2i.zero());
    }

    public static SimpleVec2i measure(Layout layout, Theme theme, UIComponent component) {
        BuildContext isolatedContext = new BuildContext();
        BuildSession session = new BuildSession(BuildMode.MOUNTED_MEASURE, isolatedContext);
        ACTIVE_SESSION.get().push(session);
        try {
            return buildMounted(layout, theme, component, session, SimpleVec2i.zero());
        } finally {
            ACTIVE_SESSION.get().pop();
        }
    }

    public static BuildResult buildTree(Layout layout, Theme theme, UIComponent component) {
        BuildContext context = new BuildContext();
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, context);
        ACTIVE_SESSION.get().push(session);
        try {
            SimpleVec2i size = buildMounted(layout, theme, component, session, SimpleVec2i.zero());
            BuildResult indexed = indexBuild(size, context, new ArrayList<>(session.getRootMounts()));
            return new BuildResult(indexed.size(), indexed.context(), indexed.rootMounts(), indexed.preorderMounts(), indexed.dynamicMounts(), session.snapshotStagedStates());
        } finally {
            ACTIVE_SESSION.get().pop();
        }
    }

    public static BuildResult indexBuild(SimpleVec2i size, BuildContext context, List<ComponentMount> rootMounts) {
        List<ComponentMount> preorder = new ArrayList<>();
        List<ComponentMount> dynamic = new ArrayList<>();
        for (ComponentMount root : rootMounts) {
            List<ComponentMount> subtree = root.collectSubtreePreorder();
            preorder.addAll(subtree);
            for (ComponentMount mount : subtree) {
                if (mount.getDynamicOwner() != null) {
                    dynamic.add(mount);
                }
            }
        }
        return new BuildResult(size, context, rootMounts, preorder, dynamic, new IdentityHashMap<>());
    }

    public static void promoteStagedStates(BuildResult result) {
        BuildSession.promoteStagedStates(result.stagedStates());
    }

    public static boolean containsPartialCommitUnsafe(ComponentMount mount) {
        for (ComponentMount node : mount.collectSubtreePreorder()) {
            if (node.getOwner() instanceof PartialCommitUnsafe) {
                return true;
            }
        }
        return false;
    }

    public static ComponentMount chooseCommitTarget(ComponentMount dirtyMount) {
        ComponentMount current = dirtyMount;
        while (current != null) {
            if (!containsPartialCommitUnsafe(current)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    public static ComponentMount findReusableMount(ComponentMount currentTarget, ComponentMount candidateRoot) {
        ComponentMount target = currentTarget;
        while (target != null) {
            if (candidateRoot.containsOwnerIdentity(target.getOwner())) {
                return target;
            }
            target = target.getParent();
        }
        return null;
    }

    public static BuildResult rebuildFrom(ComponentMount target) {
        return buildTree(target.getSavedLayout().copy(), target.getSavedTheme(), target.getOwner());
    }

    public static PartialCommitPlan planPartialCommit(ComponentMount dirtyMount, BuildContext rootContext) {
        ComponentMount current = chooseCommitTarget(dirtyMount);
        while (current != null) {
            BuildResult candidate = rebuildFrom(current);
            if (!candidate.canPartialCommit()) {
                current = current.getParent();
                continue;
            }

            ComponentMount candidateRoot = candidate.rootMounts().isEmpty() ? null : candidate.rootMounts().getFirst();
            if (candidateRoot == null) {
                current = current.getParent();
                continue;
            }

            ComponentMount reusableTarget = findReusableMount(current, candidateRoot);
            if (reusableTarget == null) {
                current = current.getParent();
                continue;
            }

            ComponentMount candidateTarget = candidateRoot.findByOwnerIdentity(reusableTarget.getOwner());
            if (candidateTarget == null) {
                current = current.getParent();
                continue;
            }

            if (candidateTarget.getBuiltSize().x != reusableTarget.getBuiltSize().x || candidateTarget.getBuiltSize().y != reusableTarget.getBuiltSize().y) {
                current = current.getParent();
                continue;
            }

            ContextRanges oldRanges = reusableTarget.getRanges();
            ContextRanges replacementRanges = candidateTarget.getRanges();
            return new PartialCommitPlan(
                    reusableTarget,
                    candidate,
                    candidateRoot,
                    candidateTarget,
                    reusableTarget.collectSubtreePreorder(),
                    candidateTarget.collectSubtreePreorder(),
                    reusableTarget.getArtifactContext(),
                    candidateTarget.getArtifactContext(),
                    oldRanges,
                    replacementRanges,
                    BuildContext.rangeLength(replacementRanges.renderableStart(), replacementRanges.renderableEnd()) - BuildContext.rangeLength(oldRanges.renderableStart(), oldRanges.renderableEnd()),
                    BuildContext.rangeLength(replacementRanges.tooltipStart(), replacementRanges.tooltipEnd()) - BuildContext.rangeLength(oldRanges.tooltipStart(), oldRanges.tooltipEnd()),
                    BuildContext.rangeLength(replacementRanges.dynamicStart(), replacementRanges.dynamicEnd()) - BuildContext.rangeLength(oldRanges.dynamicStart(), oldRanges.dynamicEnd()),
                    BuildContext.rangeLength(replacementRanges.listenerStart(), replacementRanges.listenerEnd()) - BuildContext.rangeLength(oldRanges.listenerStart(), oldRanges.listenerEnd()),
                    BuildContext.rangeLength(replacementRanges.slotStart(), replacementRanges.slotEnd()) - BuildContext.rangeLength(oldRanges.slotStart(), oldRanges.slotEnd())
            );
        }
        return null;
    }

    public static List<DynamicUIComponent> collectOrphanDynamics(List<ComponentMount> oldSubtree, List<ComponentMount> newSubtree) {
        IdentityHashMap<DynamicUIComponent, Boolean> surviving = new IdentityHashMap<>();
        for (ComponentMount mount : newSubtree) {
            if (mount.getDynamicOwner() != null) {
                surviving.put(mount.getDynamicOwner(), Boolean.TRUE);
            }
        }

        List<DynamicUIComponent> orphans = new ArrayList<>();
        for (ComponentMount mount : oldSubtree) {
            DynamicUIComponent dynamic = mount.getDynamicOwner();
            if (dynamic != null && !surviving.containsKey(dynamic)) {
                orphans.add(dynamic);
            }
        }
        return orphans;
    }

    public static void destroyOrphans(List<ComponentMount> oldSubtree, List<ComponentMount> newSubtree) {
        for (DynamicUIComponent orphan : collectOrphanDynamics(oldSubtree, newSubtree)) {
            orphan.destroy();
        }
    }

    public static void shiftSubtreeRanges(List<ComponentMount> mounts, int dr, int dt, int dd, int dl, int ds) {
        for (ComponentMount mount : mounts) {
            mount.setRanges(mount.getRanges().shiftedBy(dr, dt, dd, dl, ds));
        }
    }

    public static void growAncestorRangeEnds(ComponentMount start, BuildContext targetContext, int dr, int dt, int dd, int dl, int ds) {
        ComponentMount current = start.getParent();
        while (current != null) {
            if (current.getArtifactContext() == targetContext) {
                ContextRanges ranges = current.getRanges();
                current.setRanges(new ContextRanges(
                        ranges.renderableStart(), ranges.renderableEnd() + dr,
                        ranges.tooltipStart(), ranges.tooltipEnd() + dt,
                        ranges.dynamicStart(), ranges.dynamicEnd() + dd,
                        ranges.listenerStart(), ranges.listenerEnd() + dl,
                        ranges.slotStart(), ranges.slotEnd() + ds
                ));
            }
            current = current.getParent();
        }
    }

    public static BuildResult applyPartialCommit(BuildResult activeBuild, PartialCommitPlan plan, BuildContext mainContext) {
        ComponentMount candidateTarget = plan.candidateTarget();
        ContextRanges oldRanges = plan.oldRanges();

        destroyOrphans(plan.oldSubtree(), plan.newSubtree());
        BuildContext.splice(plan.targetContext(), oldRanges, plan.replacementContext(), plan.replacementRanges());
        shiftSubtreeRanges(
                plan.newSubtree(),
                oldRanges.renderableStart() - plan.replacementRanges().renderableStart(),
                oldRanges.tooltipStart() - plan.replacementRanges().tooltipStart(),
                oldRanges.dynamicStart() - plan.replacementRanges().dynamicStart(),
                oldRanges.listenerStart() - plan.replacementRanges().listenerStart(),
                oldRanges.slotStart() - plan.replacementRanges().slotStart()
        );

        ComponentMount parent = plan.activeTarget().getParent();
        SimpleVec2i resultSize = activeBuild.size();
        if (parent == null) {
            activeBuild.rootMounts().clear();
            activeBuild.rootMounts().add(candidateTarget);
            resultSize = plan.candidate().size();
        } else {
            plan.activeTarget().replaceWith(candidateTarget);
        }

        promoteStagedStates(plan.candidate());

        List<ComponentMount> activeRoots = activeBuild.rootMounts();
        BuildResult indexedBeforeTailShift = indexBuild(resultSize, mainContext, activeRoots);
        for (ComponentMount mount : indexedBeforeTailShift.preorderMounts()) {
            if (mount.getArtifactContext() == plan.targetContext() && mount.getRanges().renderableStart() >= oldRanges.renderableEnd()) {
                mount.setRanges(mount.getRanges().shiftedBy(
                        plan.renderableDelta(),
                        plan.tooltipDelta(),
                        plan.dynamicDelta(),
                        plan.listenerDelta(),
                        plan.slotDelta()
                ));
            }
        }

        growAncestorRangeEnds(plan.activeTarget(), plan.targetContext(), plan.renderableDelta(), plan.tooltipDelta(), plan.dynamicDelta(), plan.listenerDelta(), plan.slotDelta());

        return indexBuild(resultSize, mainContext, activeRoots);
    }

    // param size is the accumulated size of this component branch
    private static SimpleVec2i build(Layout layout, Theme theme, UIComponent uiComponent, BuildContext context, SimpleVec2i size) {
        if (uiComponent instanceof DynamicUIComponent dynamicUIComponent) {
            dynamicUIComponent.buildDynamic(context, layout.copy(), theme);
            context.dynamicUIComponents().add(dynamicUIComponent);
        }

        if (uiComponent instanceof GuiEventListener) {
            context.eventListeners().add((GuiEventListener) uiComponent);
        }

        if (uiComponent instanceof PrimitiveUIComponent primitiveUIComponent) {
            size.add(primitiveUIComponent.build(layout, theme, context));
        }

        UIComponent next = uiComponent.build(layout, theme);

        if (next == null) {
            finishDynamicUIComponent(uiComponent, context);
            return size;
        }

        SimpleVec2i resultSize = build(layout, theme, next, context, size);
        finishDynamicUIComponent(uiComponent, context);
        return resultSize;
    }

    private static void finishDynamicUIComponent(UIComponent uiComponent, BuildContext context) {
        if (uiComponent instanceof DynamicUIComponent dynamicUIComponent) {
            dynamicUIComponent.finalizeDynamic(context);
        }
    }

    private static SimpleVec2i buildMounted(Layout layout, Theme theme, UIComponent uiComponent, BuildSession session, SimpleVec2i size) {
        if (uiComponent instanceof BuilderShell) {
            UIComponent next = uiComponent.build(layout, theme);
            if (next == null) {
                return size;
            }
            return buildMounted(layout, theme, next, session, size);
        }

        session.checkUnique(uiComponent);
        DynamicUIComponent dynamic = uiComponent instanceof DynamicUIComponent d ? d : null;
        ComponentMount mount = new ComponentMount(uiComponent, dynamic);
        mount.setSavedLayout(layout.copy());
        mount.setSavedTheme(theme);
        BuildContext currentContext = session.getContext();
        mount.setArtifactContext(currentContext);
        mount.setRanges(new ContextRanges(
                currentContext.renderables().size(), -1,
                currentContext.tooltips().size(), -1,
                currentContext.dynamicUIComponents().size(), -1,
                currentContext.eventListeners().size(), -1,
                currentContext.slots().size(), -1
        ));

        if (session.getMountStack().isEmpty()) {
            session.getRootMounts().add(mount);
        } else {
            session.getMountStack().peek().addChild(mount);
        }
        session.getMountStack().push(mount);

        if (dynamic != null) {
            dynamic.buildDynamic(currentContext, layout.copy(), theme);
            session.addDynamic(dynamic);
        }

        if (uiComponent instanceof GuiEventListener listener) {
            session.addListener(listener);
        }

        if (uiComponent instanceof PrimitiveUIComponent primitive) {
            size.add(primitive.build(layout, theme, currentContext));
        }

        UIComponent next = uiComponent.build(layout, theme);
        if (next != null) {
            buildMounted(layout, theme, next, session, size);
        }

        currentContext = session.getContext();
        ContextRanges old = mount.getRanges();
        mount.setRanges(new ContextRanges(
                old.renderableStart(), currentContext.renderables().size(),
                old.tooltipStart(), currentContext.tooltips().size(),
                old.dynamicStart(), currentContext.dynamicUIComponents().size(),
                old.listenerStart(), currentContext.eventListeners().size(),
                old.slotStart(), currentContext.slots().size()
        ));
        mount.setBuiltSize(new SimpleVec2i(size.x, size.y));

        if (dynamic != null) {
            dynamic.finalizeDynamic(currentContext);
        }
        session.getMountStack().pop();
        return size;
    }

    /**
     * Ticks dynamic components and reports whether the owning renderer should rebuild the full UI tree.
     *
     * @param dynamicUIComponents dynamic components currently registered in the renderer
     * @return {@code true} if any component requested rebuild via {@code DynamicUIComponent#rebuild()}
     */
    public static boolean tickDynamicUIComponents(List<DynamicUIComponent> dynamicUIComponents) {
        boolean needsRebuild = false;

        for (DynamicUIComponent dynamicUIComponent : dynamicUIComponents) {
            dynamicUIComponent.tick();
            needsRebuild |= dynamicUIComponent.dirty;
        }

        if (needsRebuild) {
            for (DynamicUIComponent component : new ArrayList<>(dynamicUIComponents)) {
                component.dirty = false;
            }
        }

        return needsRebuild;
    }
}
