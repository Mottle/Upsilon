package com.github.wintersteve25.tau.menu;

import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.menu.handlers.ISlotHandler;
import com.github.wintersteve25.tau.theme.MinecraftTheme;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Contract for container-backed UIs.
 * <p>
 * Implementations describe both client rendering and server menu behavior,
 * including slots, data synchronization, and lifecycle hooks.
 */
public interface UIMenu {
    /**
     * Builds UI components for this menu.
     *
     * @param layout menu layout constrained to {@link #getSize()}
     * @param theme active theme used by components
     * @param containerMenu backing container instance
     * @return root UI component tree for rendering/input
     */
    UIComponent build(Layout layout, Theme theme, TauContainerMenu containerMenu);

    /**
     * Returns fixed logical menu size.
     */
    SimpleVec2i getSize();

    /**
     * Returns menu title shown by container screens.
     */
    Component getTitle();

    /**
     * Computes top position for this menu on screen.
     */
    default int getTopPos(Layout layout, int width, int height) {
        return (height - layout.getHeight()) / 2;
    }

    /**
     * Computes left position for this menu on screen.
     */
    default int getLeftPos(Layout layout, int width, int height) {
        return (width - layout.getWidth()) / 2;
    }

    /**
     * Called every container tick on the client screen side.
     */
    default void tick(TauContainerMenu menu) {
    }

    /**
     * Registers additional synced integer data slots.
     */
    default void addDataSlots(TauContainerMenu menu) {
    }

    /**
     * Provides slot handlers used when creating the server-side menu.
     */
    default List<? extends ISlotHandler> getSlots(TauContainerMenu menu) {
        return List.of();
    }

    /**
     * Handles quick-move/shift-click behavior.
     */
    default ItemStack quickMoveStack(TauContainerMenu menu, Player player, int index) {
        return null;
    }

    /**
     * Validity check mirrored by {@link net.minecraft.world.inventory.AbstractContainerMenu#stillValid(Player)}.
     */
    default boolean stillValid(TauContainerMenu menu, Player player) {
        return true;
    }

    /**
     * Whether container background should be rendered.
     */
    default boolean shouldRenderBackground() {
        return true;
    }

    /**
     * Returns theme used by this menu UI.
     */
    default Theme getTheme() {
        return MinecraftTheme.INSTANCE;
    }

    /**
     * Creates the container screen instance.
     */
    default TauContainerScreen createScreen(TauContainerMenu menu, Inventory inv, Component title) {
        return new TauContainerScreen(menu, inv, UIMenu.this, UIMenu.this.shouldRenderBackground(), UIMenu.this.getTheme(), title);
    }

    /**
     * Creates the container menu instance.
     */
    default TauContainerMenu createMenu(TauMenuHolder menuHolder, Inventory playerInv, int containerId, BlockPos pos) {
        return new TauContainerMenu(menuHolder, playerInv, containerId, pos);
    }

    /**
     * Registers the NeoForge menu type for this menu.
     */
    default DeferredHolder<MenuType<?>, MenuType<TauContainerMenu>> registerMenuType(DeferredRegister<MenuType<?>> register, TauMenuHolder menu, String name, FeatureFlagSet featureFlagSet) {
        return register.register(name, () -> IMenuTypeExtension.create((cid, inv, data) -> newMenu(menu, inv, cid, data.readBlockPos())));
    }

    /**
     * Factory used by the registered menu type.
     * <p>
     * On the client, slot positions are derived from UI build output.
     * On the server, slot handlers come from {@link #getSlots(TauContainerMenu)}.
     */
    default TauContainerMenu newMenu(TauMenuHolder menuHolder, Inventory playerInv, int containerId, BlockPos pos) {
        SimpleVec2i size = getSize();
        Layout layout = new Layout(size.x, size.y);
        List<? extends MenuSlot<? extends ISlotHandler>> s;

        TauContainerMenu menu = createMenu(menuHolder, playerInv, containerId, pos);
        if (menu.level.isClientSide()) {
            s = TauMenuHelper.buildContainerOnClient(this, layout, getTheme(), menu);
        } else {
            s = getSlots(menu).stream().map(sl -> new MenuSlot<>(SimpleVec2i.zero(), sl)).toList();
        }

        for (MenuSlot<?> slot : s) {
            slot.handler().setupSync(menu, playerInv, slot.pos().x, slot.pos().y);
        }

        addDataSlots(menu);
        return menu;
    }
}
