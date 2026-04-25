package com.github.wintersteve25.tau.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Wrapper around a {@link UIMenu} that owns its registered {@link MenuType}.
 * <p>
 * This class connects menu registration with runtime opening logic.
 */
public class TauMenuHolder {

    private final UIMenu menu;
    private DeferredHolder<MenuType<?>, MenuType<TauContainerMenu>> inner;

    /**
     * Creates a holder, resolves the supplied menu once, and registers its menu type.
     */
    public TauMenuHolder(DeferredRegister<MenuType<?>> register, Supplier<UIMenu> menu, String name, FeatureFlagSet featureFlagSet) {
        this.menu = menu.get();
        setup(this.menu, register, name, featureFlagSet);
    }

    private void setup(UIMenu menu, DeferredRegister<MenuType<?>> register, String name, FeatureFlagSet set) {
        inner = menu.registerMenuType(register, this, name, set);
    }

    /**
     * Returns the registered menu type.
     */
    public MenuType<TauContainerMenu> get() {
        return inner.get();
    }

    /**
     * Returns the bound UI menu implementation.
     */
    public UIMenu getMenu() {
        return menu;
    }

    /**
     * Opens this menu for the given server player at a block position.
     */
    public void openMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new MenuProvider() {
            /**
             * Returns display title supplied by the bound UI menu.
             */
            @Override
            public @NotNull Component getDisplayName() {
                return getMenu().getTitle();
            }

            /**
             * Creates a fresh container menu instance for this open request.
             */
            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
                return getMenu().newMenu(TauMenuHolder.this, pPlayerInventory, pContainerId, pos);
            }
        }, buf -> {
            buf.writeBlockPos(pos);
        });
    }
}
