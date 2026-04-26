package com.github.wintersteve25.tau.menu.handlers;

import com.github.wintersteve25.tau.menu.TauContainerMenu;
import com.github.wintersteve25.tau.utils.Variable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Slot handler that maps the player's full inventory (3 rows + hotbar).
 */
public class PlayerInventoryHandler implements ISlotHandler {

    private final Variable<Boolean> enabled;

    /**
     * Creates a player inventory slot handler.
     */
    public PlayerInventoryHandler(Variable<Boolean> enabled) {
        this.enabled = enabled;
    }

    /**
     * Adds main inventory and hotbar slots into the target menu.
     */
    @Override
    public void setupSync(TauContainerMenu menu, Inventory playerInv, int x, int y) {
        x += 1;
        y += 1;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                menu.addSlot(new DisablableSlot(playerInv, j + i * 9 + 9, x + j * 18, y + i * 18, enabled));
            }
        }

        for (int i = 0; i < 9; i++) {
            // skip the space of the first 3 rows and some gap
            menu.addSlot(new DisablableSlot(playerInv, i, x + i * 18, y + 58, enabled));
        }
    }

    @Override
    public Object getStructureKey() {
        return getClass().getName() + ":player-inventory";
    }

    /**
     * Slot implementation that can be hidden/disabled via {@link Variable}.
     */
    private static class DisablableSlot extends Slot {

        private final Variable<Boolean> enabled;

        /**
         * Creates a player inventory slot with toggleable visibility.
         */
        public DisablableSlot(Container container, int slot, int x, int y, Variable<Boolean> enabled) {
            super(container, slot, x, y);
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
