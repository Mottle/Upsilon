package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import com.github.wintersteve25.tau.menu.MenuSlot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UIBuilderExtendedTest {

    @Test
    void collectDirtyDynamicMounts_ticks_and_collects() {
        DynamicUIComponent dynamic = mock(DynamicUIComponent.class);
        dynamic.dirty = true;
        ComponentMount mount = new ComponentMount(dynamic, dynamic);

        BuildResult activeBuild = new BuildResult(
                SimpleVec2i.zero(), new BuildContext(), List.of(), List.of(), List.of(mount), null
        );

        List<ComponentMount> dirty = UIBuilder.collectDirtyDynamicMounts(activeBuild);
        assertEquals(1, dirty.size());
        assertSame(mount, dirty.get(0));
        verify(dynamic).tick();
        assertFalse(dynamic.dirty);
    }

    @Test
    void collectDirtyDynamicMounts_skips_clean() {
        DynamicUIComponent dynamic = mock(DynamicUIComponent.class);
        dynamic.dirty = false;
        ComponentMount mount = new ComponentMount(dynamic, dynamic);

        BuildResult activeBuild = new BuildResult(
                SimpleVec2i.zero(), new BuildContext(), List.of(), List.of(), List.of(mount), null
        );

        List<ComponentMount> dirty = UIBuilder.collectDirtyDynamicMounts(activeBuild);
        assertTrue(dirty.isEmpty());
    }

    @Test
    void tickDynamicUIComponents_detects_dirty() {
        DynamicUIComponent dynamic = mock(DynamicUIComponent.class);
        dynamic.dirty = true;
        assertTrue(UIBuilder.tickDynamicUIComponents(List.of(dynamic)));
        assertFalse(dynamic.dirty);
    }

    @Test
    void tickDynamicUIComponents_no_dirty() {
        DynamicUIComponent dynamic = mock(DynamicUIComponent.class);
        dynamic.dirty = false;
        assertFalse(UIBuilder.tickDynamicUIComponents(List.of(dynamic)));
    }

    @Test
    void indexBuild_flattens_roots() {
        ComponentMount root = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount child = new ComponentMount(mock(UIComponent.class), null);
        root.addChild(child);

        BuildResult result = UIBuilder.indexBuild(
                new SimpleVec2i(100, 200), new BuildContext(), List.of(root)
        );

        assertEquals(2, result.preorderMounts().size());
        assertSame(root, result.preorderMounts().get(0));
        assertSame(child, result.preorderMounts().get(1));
        assertEquals(100, result.size().x);
        assertEquals(200, result.size().y);
    }

    @Test
    void indexBuild_collects_dynamics() {
        DynamicUIComponent dynamic = mock(DynamicUIComponent.class);
        ComponentMount root = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount dynamicMount = new ComponentMount(dynamic, dynamic);
        root.addChild(dynamicMount);

        BuildResult result = UIBuilder.indexBuild(
                SimpleVec2i.zero(), new BuildContext(), List.of(root)
        );

        assertEquals(1, result.dynamicMounts().size());
        assertSame(dynamicMount, result.dynamicMounts().get(0));
    }

    @Test
    void promoteStagedStates_applies_via_build_result() {
        MountState mockState = mock(MountState.class);
        var host = new MountStateHostMock();

        java.util.IdentityHashMap<Object, MountState> staged = new java.util.IdentityHashMap<>();
        staged.put(host, mockState);

        BuildResult result = new BuildResult(
                SimpleVec2i.zero(), new BuildContext(), List.of(), List.of(), List.of(), staged
        );

        UIBuilder.promoteStagedStates(result);
        assertSame(mockState, host.active);
    }

    @Test
    void indexDynamics_creates_index() {
        DynamicUIComponent a = mock(DynamicUIComponent.class);
        DynamicUIComponent b = mock(DynamicUIComponent.class);

        ComponentMount ma = new ComponentMount(a, a);
        ComponentMount mb = new ComponentMount(b, b);

        var index = ComponentMount.indexDynamics(List.of(ma, mb));
        assertEquals(2, index.size());
        assertSame(ma, index.get(a));
        assertSame(mb, index.get(b));
    }
}

class MountStateHostMock implements MountStateHost<MountState> {
    MountState active;

    @Override
    public MountState getActiveMountState() { return active; }

    @Override
    public void setActiveMountState(MountState state) { this.active = state; }
}
