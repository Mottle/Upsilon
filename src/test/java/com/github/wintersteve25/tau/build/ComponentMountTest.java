package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ComponentMountTest {

    @Test
    void collectSubtreePreorder_includes_all_descendants() {
        UIComponent rootOwner = mock(UIComponent.class);
        UIComponent childOwner = mock(UIComponent.class);
        UIComponent grandchildOwner = mock(UIComponent.class);

        ComponentMount root = new ComponentMount(rootOwner, null);
        ComponentMount child = new ComponentMount(childOwner, null);
        ComponentMount grandchild = new ComponentMount(grandchildOwner, null);

        root.addChild(child);
        child.addChild(grandchild);

        List<ComponentMount> preorder = root.collectSubtreePreorder();
        assertEquals(3, preorder.size());
        assertSame(root, preorder.get(0));
        assertSame(child, preorder.get(1));
        assertSame(grandchild, preorder.get(2));
    }

    @Test
    void indexInParent_returns_correct_index() {
        ComponentMount parent = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount a = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount b = new ComponentMount(mock(UIComponent.class), null);

        parent.addChild(a);
        parent.addChild(b);

        assertEquals(0, a.indexInParent());
        assertEquals(1, b.indexInParent());
    }

    @Test
    void indexInParent_root_returns_minus_one() {
        ComponentMount root = new ComponentMount(mock(UIComponent.class), null);
        assertEquals(-1, root.indexInParent());
    }

    @Test
    void replaceWith_swaps_child_in_parent() {
        ComponentMount parent = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount original = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount replacement = new ComponentMount(mock(UIComponent.class), null);

        parent.addChild(original);
        original.replaceWith(replacement);

        assertEquals(1, parent.getChildren().size());
        assertSame(replacement, parent.getChildren().get(0));
        assertSame(parent, replacement.getParent());
    }

    @Test
    void findByOwnerIdentity_finds_by_reference() {
        UIComponent target = mock(UIComponent.class);
        ComponentMount root = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount child = new ComponentMount(target, null);
        root.addChild(child);

        assertSame(child, root.findByOwnerIdentity(target));
    }

    @Test
    void findByOwnerIdentity_returns_null_when_absent() {
        ComponentMount root = new ComponentMount(mock(UIComponent.class), null);
        assertNull(root.findByOwnerIdentity(mock(UIComponent.class)));
    }

    @Test
    void containsOwnerIdentity_finds_by_reference() {
        UIComponent target = mock(UIComponent.class);
        ComponentMount root = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount child = new ComponentMount(target, null);
        root.addChild(child);

        assertTrue(root.containsOwnerIdentity(target));
        assertFalse(root.containsOwnerIdentity(mock(UIComponent.class)));
    }

    @Test
    void dynamicOwner_is_set() {
        DynamicUIComponent dynamic = mock(DynamicUIComponent.class);
        ComponentMount mount = new ComponentMount(dynamic, dynamic);
        assertSame(dynamic, mount.getOwner());
        assertSame(dynamic, mount.getDynamicOwner());
    }

    @Test
    void ranges_and_builtSize_roundtrip() {
        ComponentMount mount = new ComponentMount(mock(UIComponent.class), null);
        ContextRanges ranges = new ContextRanges(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        SimpleVec2i size = new SimpleVec2i(100, 200);

        mount.setRanges(ranges);
        mount.setBuiltSize(size);

        assertSame(ranges, mount.getRanges());
        assertSame(size, mount.getBuiltSize());
    }

    @Test
    void artifactContext_roundtrip() {
        ComponentMount mount = new ComponentMount(mock(UIComponent.class), null);
        BuildContext ctx = new BuildContext();
        mount.setArtifactContext(ctx);
        assertSame(ctx, mount.getArtifactContext());
    }

    @Test
    void savedLayout_and_theme_roundtrip() {
        ComponentMount mount = new ComponentMount(mock(UIComponent.class), null);
        Layout layout = mock(Layout.class);
        Theme theme = mock(Theme.class);

        mount.setSavedLayout(layout);
        mount.setSavedTheme(theme);

        assertSame(layout, mount.getSavedLayout());
        assertSame(theme, mount.getSavedTheme());
    }
}
