package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.UIComponent;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import com.github.wintersteve25.tau.menu.MenuSlot;
import org.junit.jupiter.api.Test;

import java.util.IdentityHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BuildSessionTest {

    @Test
    void getOrCreateStagedState_creates_once() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        Object owner = new Object();
        MountState state1 = session.getOrCreateStagedState(owner, () -> mock(MountState.class));
        MountState state2 = session.getOrCreateStagedState(owner, () -> mock(MountState.class));

        assertSame(state1, state2);
    }

    @Test
    void clearStagedState_removes_specific_owner() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        Object owner = new Object();
        MountState state = session.getOrCreateStagedState(owner, () -> mock(MountState.class));
        session.clearStagedState(owner);

        MountState next = session.getOrCreateStagedState(owner, () -> mock(MountState.class));
        assertNotSame(state, next);
    }

    @Test
    void promoteStagedStates_writes_to_host() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        MountStateHostMock host = new MountStateHostMock();
        MountState state = mock(MountState.class);
        session.getOrCreateStagedState(host, () -> state);

        session.promoteStagedStates();

        assertSame(state, host.active);
    }

    @Test
    void pushContext_changes_current_context() {
        BuildContext root = new BuildContext();
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, root);
        BuildContext inner = new BuildContext();

        assertSame(root, session.getContext());
        session.pushContext(inner);
        assertSame(inner, session.getContext());
        session.popContext();
        assertSame(root, session.getContext());
    }

    @Test
    void popContext_throws_when_only_root_remains() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        assertThrows(IllegalStateException.class, session::popContext);
    }

    @Test
    void addRenderable_adds_to_current_context() {
        BuildContext root = new BuildContext();
        BuildContext inner = new BuildContext();
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, root);
        session.pushContext(inner);

        Renderable r = mock(Renderable.class);
        session.addRenderable(r);

        assertEquals(1, inner.renderables().size());
        assertEquals(0, root.renderables().size());
    }

    @Test
    void addListener_adds_to_current_context() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        GuiEventListener listener = mock(GuiEventListener.class);
        session.addListener(listener);

        assertEquals(1, session.getContext().eventListeners().size());
    }

    @Test
    void shouldCollectArtifacts_false_in_measure_mode() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_MEASURE, new BuildContext());
        assertFalse(session.shouldCollectArtifacts());
    }

    @Test
    void shouldCollectArtifacts_true_in_committable_mode() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        assertTrue(session.shouldCollectArtifacts());
    }

    @Test
    void measure_mode_does_not_collect_renderables() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_MEASURE, new BuildContext());
        Renderable r = mock(Renderable.class);
        session.addRenderable(r);
        assertEquals(0, session.getContext().renderables().size());
    }

    @Test
    void snapshotStagedStates_returns_independent_copy() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        Object owner = new Object();
        MountState state = mock(MountState.class);
        session.getOrCreateStagedState(owner, () -> state);

        IdentityHashMap<Object, MountState> snap = session.snapshotStagedStates();
        assertEquals(1, snap.size());
        assertSame(state, snap.get(owner));

        snap.clear();
        assertEquals(1, session.snapshotStagedStates().size());
    }

    @Test
    void discardStagedStates_clears_all() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        session.getOrCreateStagedState(new Object(), () -> mock(MountState.class));
        session.discardStagedStates();
        assertEquals(0, session.snapshotStagedStates().size());
    }

    @Test
    void checkUnique_throws_on_duplicate() {
        BuildSession session = new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext());
        UIComponent component = mock(UIComponent.class);
        session.checkUnique(component);
        assertThrows(IllegalStateException.class, () -> session.checkUnique(component));
    }

    @Test
    void buildSession_tracks_mode() {
        assertEquals(BuildMode.MOUNTED_COMMITTABLE,
                new BuildSession(BuildMode.MOUNTED_COMMITTABLE, new BuildContext()).getMode());
        assertEquals(BuildMode.MOUNTED_MEASURE,
                new BuildSession(BuildMode.MOUNTED_MEASURE, new BuildContext()).getMode());
        assertEquals(BuildMode.MOUNTLESS,
                new BuildSession(BuildMode.MOUNTLESS, new BuildContext()).getMode());
    }

    private static class MountStateHostMock implements MountStateHost<MountState> {
        MountState active;

        @Override
        public MountState getActiveMountState() { return active; }

        @Override
        public void setActiveMountState(MountState state) { this.active = state; }
    }
}
