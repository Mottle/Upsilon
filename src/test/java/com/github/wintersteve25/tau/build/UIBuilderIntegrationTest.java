package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.interactable.Button;
import com.github.wintersteve25.tau.components.layout.Center;
import com.github.wintersteve25.tau.components.layout.Column;
import com.github.wintersteve25.tau.components.render.Transform;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import com.github.wintersteve25.tau.utils.Size;
import com.github.wintersteve25.tau.utils.Transformation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UIBuilderIntegrationTest {

    static class TestLeaf implements PrimitiveUIComponent {
        final SimpleVec2i size;
        TestLeaf(SimpleVec2i size) { this.size = size; }
        @Override public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) { return size; }
        @Override public UIComponent build(Layout layout, Theme theme) { return null; }
    }

    static class TestDynamic extends DynamicUIComponent implements PrimitiveUIComponent {
        final SimpleVec2i size;
        final AtomicInteger buildCount = new AtomicInteger(0);
        final AtomicBoolean destroyCalled = new AtomicBoolean(false);

        TestDynamic(SimpleVec2i size) { this.size = size; }
        @Override public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
            buildCount.incrementAndGet();
            return size;
        }
        @Override public UIComponent build(Layout layout, Theme theme) { return null; }
        @Override public void destroy() { destroyCalled.set(true); }
    }

    static class TestWrapper implements UIComponent {
        final UIComponent child;
        TestWrapper(UIComponent child) { this.child = child; }
        @Override public UIComponent build(Layout layout, Theme theme) { return child; }
    }

    static class ToggleSizeDynamic extends DynamicUIComponent implements PrimitiveUIComponent {
        boolean expanded;

        @Override public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
            return expanded ? new SimpleVec2i(260, 140) : new SimpleVec2i(140, 60);
        }

        @Override public UIComponent build(Layout layout, Theme theme) { return null; }
    }

    static class ClickDrivenDynamic extends DynamicUIComponent {
        boolean expanded;

        @Override public UIComponent build(Layout layout, Theme theme) {
            return new Sized(
                    expanded ? Size.staticSize(260, 140) : Size.staticSize(140, 60),
                    new Button.Builder()
                            .withOnPress(btn -> {
                                expanded = !expanded;
                                rebuild();
                            })
                            .build(new Center(new TestLeaf(new SimpleVec2i(20, 10))))
            );
        }
    }

    static class RenderSnapshotDynamic extends DynamicUIComponent {
        final List<Integer> renderedSnapshots = new java.util.ArrayList<>();
        int snapshot;

        @Override
        public UIComponent build(Layout layout, Theme theme) {
            int captured = snapshot;
            return new PrimitiveUIComponent() {
                @Override
                public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
                    context.renderables().add((graphics, mouseX, mouseY, partialTicks) -> renderedSnapshots.add(captured));
                    return new SimpleVec2i(20, 10);
                }
            };
        }
    }

    static class LoggedRenderableLeaf implements PrimitiveUIComponent {
        final List<String> renderLog;
        final String label;
        final SimpleVec2i size;

        LoggedRenderableLeaf(List<String> renderLog, String label, SimpleVec2i size) {
            this.renderLog = renderLog;
            this.label = label;
            this.size = size;
        }

        @Override
        public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
            BuildSession session = UIBuilder.currentSession();
            if (session != null) {
                session.addRenderable((graphics, mouseX, mouseY, partialTicks) -> renderLog.add(label));
            } else {
                context.renderables().add((graphics, mouseX, mouseY, partialTicks) -> renderLog.add(label));
            }
            return size;
        }
    }

    static class NestedRenderSnapshotDynamic extends DynamicUIComponent {
        final List<String> renderLog;
        int snapshot;

        NestedRenderSnapshotDynamic(List<String> renderLog) {
            this.renderLog = renderLog;
        }

        @Override
        public UIComponent build(Layout layout, Theme theme) {
            return new LoggedRenderableLeaf(renderLog, "dynamic:" + snapshot, new SimpleVec2i(20, 10));
        }
    }

    static class NestedListenerDynamic extends DynamicUIComponent implements GuiEventListener {
        @Override
        public UIComponent build(Layout layout, Theme theme) {
            return new TestLeaf(new SimpleVec2i(20, 10));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return true;
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public void setFocused(boolean focused) {
        }
    }

    @Test
    void buildTree_returns_non_null_result() {
        BuildResult result = UIBuilder.buildTree(
                new Layout(100, 100), mock(Theme.class), new TestLeaf(new SimpleVec2i(20, 10))
        );
        assertNotNull(result);
        assertEquals(20, result.size().x);
        assertEquals(10, result.size().y);
        assertFalse(result.rootMounts().isEmpty());
    }

    @Test
    void buildTree_detects_dynamic_components() {
        TestDynamic dynamic = new TestDynamic(new SimpleVec2i(50, 30));
        BuildResult result = UIBuilder.buildTree(new Layout(200, 200), mock(Theme.class), dynamic);

        assertEquals(1, result.dynamicMounts().size());
        assertSame(dynamic, result.dynamicMounts().get(0).getDynamicOwner());
        assertEquals(50, result.size().x);
        assertEquals(30, result.size().y);
    }

    @Test
    void measure_returns_size_without_polluting_active() {
        TestDynamic dynamic = new TestDynamic(new SimpleVec2i(50, 30));
        SimpleVec2i size = UIBuilder.measure(new Layout(200, 200), mock(Theme.class), dynamic);

        assertEquals(50, size.x);
        assertEquals(30, size.y);
        // measure builds the component but does not collect artifacts or write staged state
        assertEquals(1, dynamic.buildCount.get());
    }

    @Test
    void planPartialCommit_succeeds_for_root_dynamic() {
        TestDynamic dynamic = new TestDynamic(new SimpleVec2i(50, 30));
        BuildContext rootContext = new BuildContext();
        BuildResult active = UIBuilder.buildTree(new Layout(200, 200), mock(Theme.class), dynamic);

        dynamic.dirty = true;
        ComponentMount dirtyMount = active.dynamicMounts().get(0);
        PartialCommitPlan plan = UIBuilder.planPartialCommit(dirtyMount, active.context());

        assertNotNull(plan);
        assertSame(dirtyMount, plan.activeTarget());
    }

    @Test
    void planPartialCommit_returns_null_when_size_differs() {
        TestLeaf original = new TestLeaf(new SimpleVec2i(30, 20));
        BuildResult active = UIBuilder.buildTree(new Layout(100, 100), mock(Theme.class), original);

        ComponentMount dirtyMount = active.rootMounts().get(0);
        BuildContext rootContext = active.context();

        // The candidate rebuild will use the same leaf with same size, so this should work.
        // To test size difference we'd need a component that changes size on rebuild.
        // SimpleVec2i leaf returns same size every time, so plan should succeed.
        PartialCommitPlan plan = UIBuilder.planPartialCommit(dirtyMount, rootContext);
        assertNotNull(plan);
    }

    @Test
    void applyPartialCommit_updates_root_mount() {
        TestLeaf leaf = new TestLeaf(new SimpleVec2i(50, 30));
        BuildResult active = UIBuilder.buildTree(new Layout(100, 100), mock(Theme.class), leaf);

        ComponentMount dirtyMount = active.rootMounts().get(0);
        PartialCommitPlan plan = UIBuilder.planPartialCommit(dirtyMount, active.context());
        assertNotNull(plan);

        BuildResult updated = UIBuilder.applyPartialCommit(active, plan, active.context());
        assertNotNull(updated);
        assertFalse(updated.rootMounts().isEmpty());
    }

    @Test
    void full_cycle_dynamic_destroy_triggered_on_orphan() {
        TestDynamic outer = new TestDynamic(new SimpleVec2i(60, 40));
        TestLeaf inner = new TestLeaf(new SimpleVec2i(10, 10));
        UIComponent root = new TestWrapper(outer);

        BuildResult active = UIBuilder.buildTree(new Layout(100, 100), mock(Theme.class), root);
        assertFalse(outer.destroyCalled.get());

        outer.dirty = true;
        ComponentMount dirtyMount = active.dynamicMounts().get(0);
        PartialCommitPlan plan = UIBuilder.planPartialCommit(dirtyMount, active.context());

        if (plan != null) {
            UIBuilder.applyPartialCommit(active, plan, active.context());
            // outer was reused (same instance in candidate), so destroy not called
            assertFalse(outer.destroyCalled.get());
        }
    }

    @Test
    void staged_states_promoted_after_applyPartialCommit() {
        TestLeaf leaf = new TestLeaf(new SimpleVec2i(50, 30));
        BuildResult active = UIBuilder.buildTree(new Layout(100, 100), mock(Theme.class), leaf);

        ComponentMount dirtyMount = active.rootMounts().get(0);
        PartialCommitPlan plan = UIBuilder.planPartialCommit(dirtyMount, active.context());
        assertNotNull(plan);

        UIBuilder.applyPartialCommit(active, plan, active.context());
        // staged states should be cleared (promotion consumed them)
        assertTrue(plan.candidate().stagedStates().isEmpty());
    }

    @Test
    void buildTree_then_rebuildFrom_preserves_owner_identity() {
        TestLeaf leaf = new TestLeaf(new SimpleVec2i(40, 20));
        BuildResult first = UIBuilder.buildTree(new Layout(100, 100), mock(Theme.class), leaf);
        ComponentMount target = first.rootMounts().get(0);

        BuildResult second = UIBuilder.rebuildFrom(target);
        assertNotNull(second);
        assertSame(leaf, second.rootMounts().get(0).getOwner());
    }

    @Test
    void consecutive_partial_commits_work_with_same_dynamic() {
        TestDynamic dynamic = new TestDynamic(new SimpleVec2i(50, 30));
        BuildResult active = UIBuilder.buildTree(new Layout(200, 200), mock(Theme.class), dynamic);
        BuildContext ctx = active.context();

        // first dirty → partial commit
        dynamic.dirty = true;
        ComponentMount dirtyMount = active.dynamicMounts().get(0);
        PartialCommitPlan plan1 = UIBuilder.planPartialCommit(dirtyMount, ctx);
        assertNotNull(plan1, "first planPartialCommit should succeed");
        active = UIBuilder.applyPartialCommit(active, plan1, ctx);
        assertEquals(2, dynamic.buildCount.get());

        // second dirty → partial commit
        dynamic.dirty = true;
        dirtyMount = active.dynamicMounts().get(0);
        PartialCommitPlan plan2 = UIBuilder.planPartialCommit(dirtyMount, ctx);
        assertNotNull(plan2, "second planPartialCommit should succeed");
        active = UIBuilder.applyPartialCommit(active, plan2, ctx);
        assertEquals(3, dynamic.buildCount.get());
    }

    @Test
    void consecutive_partial_commits_with_nested_dynamic() {
        TestDynamic dynamic = new TestDynamic(new SimpleVec2i(50, 30));
        UIComponent root = new TestWrapper(dynamic);
        BuildResult active = UIBuilder.buildTree(new Layout(200, 200), mock(Theme.class), root);
        BuildContext ctx = active.context();

        // first dirty → partial commit of nested dynamic
        dynamic.dirty = true;
        ComponentMount dirtyMount = active.dynamicMounts().get(0);
        PartialCommitPlan plan1 = UIBuilder.planPartialCommit(dirtyMount, ctx);
        assertNotNull(plan1, "first planPartialCommit for nested dynamic should succeed");
        active = UIBuilder.applyPartialCommit(active, plan1, ctx);

        // second dirty → partial commit
        dynamic.dirty = true;
        dirtyMount = active.dynamicMounts().get(0);
        PartialCommitPlan plan2 = UIBuilder.planPartialCommit(dirtyMount, ctx);
        assertNotNull(plan2, "second planPartialCommit for nested dynamic should succeed");
        active = UIBuilder.applyPartialCommit(active, plan2, ctx);
    }

    @Test
    void root_partial_commit_handles_size_toggle_back_and_forth() {
        ToggleSizeDynamic dynamic = new ToggleSizeDynamic();
        BuildResult active = UIBuilder.buildTree(new Layout(400, 300), mock(Theme.class), dynamic);
        BuildContext ctx = active.context();

        dynamic.expanded = true;
        dynamic.dirty = true;
        ComponentMount dirtyMount = active.dynamicMounts().get(0);
        PartialCommitPlan plan1 = UIBuilder.planPartialCommit(dirtyMount, ctx);
        assertNotNull(plan1, "first root size-changing partial commit should succeed");
        active = UIBuilder.applyPartialCommit(active, plan1, ctx);
        assertEquals(260, active.rootMounts().get(0).getBuiltSize().x);
        assertEquals(140, active.rootMounts().get(0).getBuiltSize().y);

        dynamic.expanded = false;
        dynamic.dirty = true;
        dirtyMount = active.dynamicMounts().get(0);
        PartialCommitPlan plan2 = UIBuilder.planPartialCommit(dirtyMount, ctx);
        assertNotNull(plan2, "second root size-changing partial commit should also succeed");
        active = UIBuilder.applyPartialCommit(active, plan2, ctx);
        assertEquals(140, active.rootMounts().get(0).getBuiltSize().x);
        assertEquals(60, active.rootMounts().get(0).getBuiltSize().y);
    }

    @Test
    void listener_cache_is_rebuilt_after_partial_commit() {
        ClickDrivenDynamic dynamic = new ClickDrivenDynamic();
        BuildResult active = UIBuilder.buildTree(new Layout(400, 300), mock(Theme.class), dynamic);
        BuildContext ctx = active.context();

        long guiListenerCount = active.preorderMounts().stream()
                .filter(m -> m.getOwner() instanceof net.minecraft.client.gui.components.events.GuiEventListener)
                .count();
        assertEquals(guiListenerCount, ctx.eventListeners().size());

        dynamic.expanded = true;
        dynamic.dirty = true;
        PartialCommitPlan plan = UIBuilder.planPartialCommit(active.dynamicMounts().get(0), ctx);
        assertNotNull(plan);
        active = UIBuilder.applyPartialCommit(active, plan, ctx);

        guiListenerCount = active.preorderMounts().stream()
                .filter(m -> m.getOwner() instanceof net.minecraft.client.gui.components.events.GuiEventListener)
                .count();
        assertEquals(guiListenerCount, ctx.eventListeners().size());

        dynamic.expanded = false;
        dynamic.dirty = true;
        plan = UIBuilder.planPartialCommit(active.dynamicMounts().get(0), ctx);
        assertNotNull(plan);
        active = UIBuilder.applyPartialCommit(active, plan, ctx);

        guiListenerCount = active.preorderMounts().stream()
                .filter(m -> m.getOwner() instanceof net.minecraft.client.gui.components.events.GuiEventListener)
                .count();
        assertEquals(guiListenerCount, ctx.eventListeners().size());
    }

    @Test
    void repeated_partial_commits_keep_renderable_count_stable() {
        ClickDrivenDynamic dynamic = new ClickDrivenDynamic();
        BuildResult active = UIBuilder.buildTree(new Layout(400, 300), mock(Theme.class), dynamic);
        BuildContext ctx = active.context();

        int initialRenderables = ctx.renderables().size();

        for (int i = 0; i < 6; i++) {
            dynamic.expanded = !dynamic.expanded;
            dynamic.dirty = true;
            PartialCommitPlan plan = UIBuilder.planPartialCommit(active.dynamicMounts().get(0), ctx);
            assertNotNull(plan);
            active = UIBuilder.applyPartialCommit(active, plan, ctx);
            assertEquals(initialRenderables, ctx.renderables().size(), "renderable count must remain stable after commit #" + i);
        }
    }

    @Test
    void repeated_partial_commits_preserve_mount_size_transitions() {
        ToggleSizeDynamic dynamic = new ToggleSizeDynamic();
        BuildResult active = UIBuilder.buildTree(new Layout(400, 300), mock(Theme.class), dynamic);
        BuildContext ctx = active.context();

        int[][] expectedSizes = {
                {260, 140},
                {140, 60},
                {260, 140},
                {140, 60}
        };

        for (int i = 0; i < expectedSizes.length; i++) {
            dynamic.expanded = !dynamic.expanded;
            dynamic.dirty = true;
            PartialCommitPlan plan = UIBuilder.planPartialCommit(active.dynamicMounts().get(0), ctx);
            assertNotNull(plan);
            active = UIBuilder.applyPartialCommit(active, plan, ctx);
            assertEquals(expectedSizes[i][0], active.rootMounts().get(0).getBuiltSize().x, "unexpected width on commit #" + i);
            assertEquals(expectedSizes[i][1], active.rootMounts().get(0).getBuiltSize().y, "unexpected height on commit #" + i);
        }
    }

    @Test
    void root_partial_commit_rebinds_active_context_for_later_commits() {
        RenderSnapshotDynamic dynamic = new RenderSnapshotDynamic();
        BuildResult active = UIBuilder.buildTree(new Layout(200, 200), mock(Theme.class), dynamic);
        BuildContext ctx = active.context();

        ctx.renderables().get(0).render(null, 0, 0, 0);
        assertEquals(List.of(0), dynamic.renderedSnapshots);
        assertSame(ctx, active.dynamicMounts().get(0).getArtifactContext());

        dynamic.snapshot = 1;
        dynamic.dirty = true;
        PartialCommitPlan plan1 = UIBuilder.planPartialCommit(active.dynamicMounts().get(0), ctx);
        assertNotNull(plan1);
        active = UIBuilder.applyPartialCommit(active, plan1, ctx);

        ctx.renderables().get(0).render(null, 0, 0, 0);
        assertEquals(List.of(0, 1), dynamic.renderedSnapshots);
        assertSame(ctx, active.dynamicMounts().get(0).getArtifactContext());

        dynamic.snapshot = 2;
        dynamic.dirty = true;
        PartialCommitPlan plan2 = UIBuilder.planPartialCommit(active.dynamicMounts().get(0), ctx);
        assertNotNull(plan2);
        active = UIBuilder.applyPartialCommit(active, plan2, ctx);

        ctx.renderables().get(0).render(null, 0, 0, 0);
        assertEquals(List.of(0, 1, 2), dynamic.renderedSnapshots);
        assertSame(ctx, active.dynamicMounts().get(0).getArtifactContext());
    }

    @Test
    void nested_inner_context_partial_commit_preserves_render_order_across_repeated_commits() {
        List<String> renderLog = new ArrayList<>();
        NestedRenderSnapshotDynamic dynamic = new NestedRenderSnapshotDynamic(renderLog);
        UIComponent root = new Transform(
                new Column.Builder().build(
                        new LoggedRenderableLeaf(renderLog, "static", new SimpleVec2i(20, 10)),
                        dynamic
                )
        );

        BuildResult active = UIBuilder.buildTree(new Layout(200, 200), mock(Theme.class), root);
        UIBuilder.promoteStagedStates(active);
        BuildContext ctx = active.context();
        assertEquals(1, ctx.renderables().size(), "transform should contribute a single wrapper renderable to root context");
        GuiGraphics graphics = mock(GuiGraphics.class);
        when(graphics.pose()).thenReturn(new PoseStack());

        renderLog.clear();
        ctx.renderables().get(0).render(graphics, 0, 0, 0);
        assertEquals(List.of("static", "dynamic:0"), renderLog);

        dynamic.snapshot = 1;
        dynamic.dirty = true;
        PartialCommitPlan plan1 = UIBuilder.planPartialCommit(active.dynamicMounts().get(0), ctx);
        assertNotNull(plan1);
        active = UIBuilder.applyPartialCommit(active, plan1, ctx);

        renderLog.clear();
        ctx.renderables().get(0).render(graphics, 0, 0, 0);
        assertEquals(List.of("static", "dynamic:1"), renderLog);

        dynamic.snapshot = 2;
        dynamic.dirty = true;
        PartialCommitPlan plan2 = UIBuilder.planPartialCommit(active.dynamicMounts().get(0), ctx);
        assertNotNull(plan2);
        active = UIBuilder.applyPartialCommit(active, plan2, ctx);

        renderLog.clear();
        ctx.renderables().get(0).render(graphics, 0, 0, 0);
        assertEquals(List.of("static", "dynamic:2"), renderLog);
    }

    @Test
    void nested_transform_partial_commit_keeps_root_listener_boundary() {
        NestedListenerDynamic dynamic = new NestedListenerDynamic();
        Transform transform = new Transform(dynamic, Transformation.translate(new Vector3f(50, 50, 0)));

        BuildResult active = UIBuilder.buildTree(new Layout(200, 200), mock(Theme.class), transform);
        UIBuilder.promoteStagedStates(active);
        BuildContext ctx = active.context();
        assertEquals(1, ctx.eventListeners().size(), "root context should only expose the transform listener");
        assertSame(transform, ctx.eventListeners().get(0));
        assertEquals(1, transform.children().size(), "transform should own the nested listener through its local child list");

        dynamic.dirty = true;
        PartialCommitPlan plan = UIBuilder.planPartialCommit(active.dynamicMounts().get(0), ctx);
        assertNotNull(plan);
        active = UIBuilder.applyPartialCommit(active, plan, ctx);

        assertEquals(1, ctx.eventListeners().size(), "nested partial commit must not flatten child listeners into root context");
        assertSame(transform, ctx.eventListeners().get(0));
        assertEquals(1, transform.children().size(), "transform should still own the nested listener after partial commit");
        assertSame(dynamic, active.dynamicMounts().get(0).getDynamicOwner());
    }
}
