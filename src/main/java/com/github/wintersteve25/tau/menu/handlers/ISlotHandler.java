package com.github.wintersteve25.tau.menu.handlers;

import com.github.wintersteve25.tau.menu.TauContainerMenu;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.world.entity.player.Inventory;

/**
 * Contract for translating UI slot descriptors into synced container slots.
 */
public interface ISlotHandler {
    /**
     * Adds one or more concrete slots to the target menu at the provided GUI origin.
     */
    void setupSync(TauContainerMenu menu, Inventory playerInv, int x, int y);

    /**
     * Returns a stable key describing this handler's structural slot identity.
     */
    default Object getStructureKey() {
        return getClass().getName();
    }

    /**
     * Returns the number of vanilla slots materialized by this descriptor.
     * <p>
     * Most handlers create exactly one slot. Composite handlers, such as the
     * player inventory, must override this so the client can keep every
     * materialized slot aligned with its UI descriptor.
     */
    default int getSlotCount() {
        return 1;
    }

    /**
     * Returns the relative position of one materialized vanilla slot.
     * <p>
     * The screen adds the standard one-pixel slot-frame inset after applying
     * this offset.
     *
     * @param slotIndex index in {@code [0, getSlotCount())}
     * @return offset relative to the descriptor origin
     */
    default SimpleVec2i getSlotOffset(int slotIndex) {
        if (slotIndex != 0) {
            throw new IndexOutOfBoundsException("Slot index " + slotIndex + " outside single-slot handler");
        }
        return SimpleVec2i.zero();
    }
}
