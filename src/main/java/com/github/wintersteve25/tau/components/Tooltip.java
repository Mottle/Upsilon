package com.github.wintersteve25.tau.components;

import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;

import java.util.ArrayList;
import java.util.List;

public final class Tooltip implements PrimitiveUIComponent {

    private final List<ClientTooltipComponent> clientComponents;
    private final List<Component> components;
    private final UIComponent child;

    public Tooltip(List<ClientTooltipComponent> clientComponents, List<Component> components, UIComponent child) {
        this.clientComponents = clientComponents;
        this.components = components;
        this.child = child;
    }

    @Override
    public SimpleVec2i build(Layout layout, Theme theme, List<Renderable> renderables, List<Renderable> tooltips, List<DynamicUIComponent> dynamicUIComponents, List<GuiEventListener> eventListeners) {
        Minecraft minecraft = Minecraft.getInstance();
        Font fontRenderer = minecraft.font;
        Window window = minecraft.getWindow();
        int screenWidth = window.getScreenWidth();
        int screenHeight = window.getScreenHeight();

        SimpleVec2i size = UIBuilder.build(layout, theme, child, renderables, tooltips, dynamicUIComponents, eventListeners);
        SimpleVec2i position = layout.getPosition(size);

        tooltips.add((pPoseStack, pMouseX, pMouseY, pPartialTicks) -> {
            if (SimpleVec2i.within(pMouseX, pMouseY, position.x - 1, position.y - 1, size.x + 1, size.y + 1)) {
                List<ClientTooltipComponent> components = ClientHooks.gatherTooltipComponents(ItemStack.EMPTY, this.components, pMouseX, screenWidth, screenHeight, fontRenderer);
                List<ClientTooltipComponent> all = new ArrayList<>(components.size() + clientComponents.size());
                all.addAll(clientComponents);
                all.addAll(components);
                theme.drawTooltip(pPoseStack, pMouseX, pMouseY, fontRenderer, all);
            }
        });

        return size;
    }

    public static final class Builder {
        private final List<ClientTooltipComponent> clientComponents;
        private final List<Component> components;

        public Builder() {
            clientComponents = new ArrayList<>();
            components = new ArrayList<>();
        }

        public Builder with(List<ClientTooltipComponent> components) {
            this.clientComponents.addAll(components);
            return this;
        }

        public Builder with(ClientTooltipComponent component) {
            this.clientComponents.add(component);
            return this;
        }

        public Builder withComponent(List<Component> components) {
            this.components.addAll(components);
            return this;
        }

        public Builder withComponent(Component component) {
            this.components.add(component);
            return this;
        }

        public Tooltip build(UIComponent child) {
            return new Tooltip(clientComponents, components, child);
        }
    }
}
