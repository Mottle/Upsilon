package com.github.wintersteve25.tau.menu.handlers;

import com.github.wintersteve25.tau.menu.TauContainerMenu;
import com.github.wintersteve25.tau.utils.Variable;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Slot handler that binds a single {@link IItemHandler} slot into a menu.
 */
public class ItemSlotHandler implements ISlotHandler {

    private final IItemHandler inventory;
    private final int index;
    private final Variable<Boolean> enabled;

    /**
     * Creates a handler for one logical item slot.
     */
    public ItemSlotHandler(IItemHandler inventory, int index, Variable<Boolean> enabled) {
        this.inventory = inventory;
        this.index = index;
        this.enabled = enabled;
    }

    /**
     * Adds the configured slot into the target menu.
     */
    @Override
    public void setupSync(TauContainerMenu menu, Inventory playerInv, int x, int y) {
        menu.addSlot(new DisablableSlot(inventory, index, x + 1, y + 1, enabled));
    }
    
    /** Slot implementation that can be hidden/disabled via {@link Variable}. */
    private static class DisablableSlot extends SlotItemHandler {
        private final Variable<Boolean> enabled;

        /**
         * Creates a slot that can be toggled visible/active.
         */
        public DisablableSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition, Variable<Boolean> enabled) {
            super(itemHandler, index, xPosition, yPosition);
            this.enabled = enabled;
        }

        /**
         * Returns current active state based on bound variable.
         */
        @Override
        public boolean isActive() {
            return enabled.getValue();
        }
    }
}
