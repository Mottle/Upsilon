package com.github.wintersteve25.tau.menu;

import moe.liar.upsilon.Upsilon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base container menu implementation used by {@link UIMenu} screens.
 * <p>
 * It stores menu identity, level/position context, and named data slot helpers.
 */
public class TauContainerMenu extends AbstractContainerMenu {

    public final Level level;
    public final BlockPos pos;

    private final Map<String, IndexedDataSlot> dataSlots;
    private final UIMenu menu;

    /**
     * Creates a container menu bound to a holder/menu pair.
     */
    public TauContainerMenu(TauMenuHolder holder, Inventory playerInventory, int pContainerId, BlockPos pos) {
        super(holder.get(), pContainerId);

        this.level = playerInventory.player.level();
        this.pos = pos;
        this.dataSlots = new HashMap<>();
        this.menu = holder.getMenu();
    }

    /**
     * Delegates quick-move handling to the active {@link UIMenu}.
     */
    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return menu.quickMoveStack(this, pPlayer, pIndex);
    }

    /**
     * Delegates validity checks to the active {@link UIMenu}.
     */
    @Override
    public boolean stillValid(Player pPlayer) {
        return menu.stillValid(this, pPlayer);
    }

    /**
     * Exposes slot addition for slot handlers.
     */
    public @NotNull Slot addSlot(@NotNull Slot pSlot) {
        return super.addSlot(pSlot);
    }

    /**
     * Registers a named data slot for integer synchronization.
     */
    public void addDataSlot(String name, DataSlot slot) {
        if (dataSlots.containsKey(name)) {
            Upsilon.LOGGER.error("Duplicated data slot key: " + name);
            return;
        }

        dataSlots.put(name, new IndexedDataSlot(dataSlots.size(), slot));
        addDataSlot(slot);
    }
    
    /**
     * Convenience overload to register a named slot from getter/setter lambdas.
     */
    public void addDataSlot(String name, Supplier<Integer> getter, Consumer<Integer> setter) {
        addDataSlot(name, new DataSlot() {
            /**
             * Reads the current synced integer value.
             */
            @Override
            public int get() {
                return getter.get();
            }

            /**
             * Writes a synced integer value.
             */
            @Override
            public void set(int value) {
                setter.accept(value);
            }
        });
    }

    /**
     * Returns a supplier for reading a named data slot value.
     */
    public Optional<Supplier<Integer>> getGetterForDataSlot(String name) {
        if (!dataSlots.containsKey(name)) {
            Upsilon.LOGGER.error("Unknown data slot key: " + name);
            return Optional.empty();
        }

        return Optional.of(dataSlots.get(name).dataSlot()::get);
    }

    /**
     * Exposes vanilla move logic to subclasses/custom menus.
     */
    @Override
    public boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        return super.moveItemStackTo(stack, startIndex, endIndex, reverseDirection);
    }

    /**
     * Returns the block entity at this menu position, if present.
     */
    public BlockEntity getBlockEntity() {
        return level.getBlockEntity(pos);
    }

    /**
     * Returns the block entity at this menu position cast to the requested type.
     */
    public <T extends BlockEntity> Optional<T> getBlockEntity(Class<T> t) {
        BlockEntity e = level.getBlockEntity(pos);
        if (t.isInstance(e)) {
            return Optional.of(t.cast(e));
        }
        
        return Optional.empty();
    }

    private record IndexedDataSlot(int index, DataSlot dataSlot) {}
}
