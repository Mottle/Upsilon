package com.github.wintersteve25.tau.menu.handlers;

import com.github.wintersteve25.tau.menu.TauContainerMenu;
import net.minecraft.world.entity.player.Inventory;

/**
 * Contract for translating UI slot descriptors into synced container slots.
 */
public interface ISlotHandler {
    /**
     * Adds one or more concrete slots to the target menu at the provided GUI origin.
     */
    void setupSync(TauContainerMenu menu, Inventory playerInv, int x, int y);
}
