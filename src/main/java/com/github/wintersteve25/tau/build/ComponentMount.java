package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Mounted UI component node used for subtree bookkeeping.
 */
public final class ComponentMount {
    private final UIComponent owner;
    private final DynamicUIComponent dynamicOwner;
    private final List<ComponentMount> children = new ArrayList<>();
    private ComponentMount parent;
    private ContextRanges ranges;
    private Layout savedLayout;
    private Theme savedTheme;
    private SimpleVec2i builtSize;

    public ComponentMount(UIComponent owner, DynamicUIComponent dynamicOwner) {
        this.owner = owner;
        this.dynamicOwner = dynamicOwner;
    }

    public static IdentityHashMap<DynamicUIComponent, ComponentMount> indexDynamics(List<ComponentMount> mounts) {
        IdentityHashMap<DynamicUIComponent, ComponentMount> result = new IdentityHashMap<>();
        for (ComponentMount mount : mounts) {
            if (mount.dynamicOwner != null) {
                result.put(mount.dynamicOwner, mount);
            }
        }
        return result;
    }

    public UIComponent getOwner() {
        return owner;
    }

    public DynamicUIComponent getDynamicOwner() {
        return dynamicOwner;
    }

    public ComponentMount getParent() {
        return parent;
    }

    public void setParent(ComponentMount parent) {
        this.parent = parent;
    }

    public List<ComponentMount> getChildren() {
        return children;
    }

    public void addChild(ComponentMount child) {
        children.add(child);
        child.setParent(this);
    }

    public ContextRanges getRanges() {
        return ranges;
    }

    public void setRanges(ContextRanges ranges) {
        this.ranges = ranges;
    }

    public Layout getSavedLayout() {
        return savedLayout;
    }

    public void setSavedLayout(Layout savedLayout) {
        this.savedLayout = savedLayout;
    }

    public Theme getSavedTheme() {
        return savedTheme;
    }

    public void setSavedTheme(Theme savedTheme) {
        this.savedTheme = savedTheme;
    }

    public SimpleVec2i getBuiltSize() {
        return builtSize;
    }

    public void setBuiltSize(SimpleVec2i builtSize) {
        this.builtSize = builtSize;
    }

    public List<ComponentMount> collectSubtreePreorder() {
        List<ComponentMount> result = new ArrayList<>();
        collectSubtreePreorder(result);
        return result;
    }

    private void collectSubtreePreorder(List<ComponentMount> result) {
        result.add(this);
        for (ComponentMount child : children) {
            child.collectSubtreePreorder(result);
        }
    }

    public int indexInParent() {
        if (parent == null) {
            return -1;
        }
        return parent.children.indexOf(this);
    }

    public void replaceWith(ComponentMount replacement) {
        replacement.parent = parent;
        if (parent == null) {
            return;
        }

        int index = indexInParent();
        if (index < 0) {
            throw new IllegalStateException("ComponentMount is detached from parent");
        }

        parent.children.set(index, replacement);
    }

    public boolean containsOwnerIdentity(Object owner) {
        for (ComponentMount mount : collectSubtreePreorder()) {
            if (mount.owner == owner) {
                return true;
            }
        }
        return false;
    }
}
