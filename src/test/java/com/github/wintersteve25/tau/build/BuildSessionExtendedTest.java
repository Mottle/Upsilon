package com.github.wintersteve25.tau.build;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.menu.MenuSlot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BuildSessionExtendedTest {

    @Test
    void getRootContext_returns_initial_context() {
        BuildContext root = new BuildContext();
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, root);
        session.pushContext(new BuildContext());
        assertSame(root, session.getRootContext());
    }

    @Test
    void clearStagedStatesForSubtree_removes_owners() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        UIComponent a = mock(UIComponent.class);
        UIComponent b = mock(UIComponent.class);

        MountState sa = session.getOrCreateStagedState(a, () -> mock(MountState.class));
        session.getOrCreateStagedState(b, () -> mock(MountState.class));

        ComponentMount mountA = new ComponentMount(a, null);
        session.clearStagedStatesForSubtree(List.of(mountA));

        assertNotSame(sa, session.getOrCreateStagedState(a, () -> mock(MountState.class)));
    }

    @Test
    void addTooltip_adds_to_current_context() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        Renderable r = mock(Renderable.class);
        session.addTooltip(r);
        assertEquals(1, session.getContext().tooltips().size());
    }

    @Test
    void addDynamic_adds_to_current_context() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        DynamicUIComponent d = mock(DynamicUIComponent.class);
        session.addDynamic(d);
        assertEquals(1, session.getContext().dynamicUIComponents().size());
    }

    @Test
    void addSlot_adds_to_current_context() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        MenuSlot<?> slot = mock(MenuSlot.class);
        session.addSlot(slot);
        assertEquals(1, session.getContext().slots().size());
    }

    @Test
    void addRenderable_not_collected_in_measure_mode() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_MEASURE, new BuildContext());
        session.addRenderable(mock(Renderable.class));
        assertEquals(0, session.getContext().renderables().size());
    }

    @Test
    void addListener_not_collected_in_measure_mode() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_MEASURE, new BuildContext());
        session.addListener(mock(GuiEventListener.class));
        assertEquals(0, session.getContext().eventListeners().size());
    }

    @Test
    void mountStack_tracks_pushed_mounts() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        assertTrue(session.getMountStack().isEmpty());
    }

    @Test
    void rootMounts_starts_empty() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        assertTrue(session.getRootMounts().isEmpty());
    }
}
