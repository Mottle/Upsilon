package com.github.wintersteve25.tau.menu;

import com.github.wintersteve25.tau.menu.handlers.ISlotHandler;
import com.github.wintersteve25.tau.menu.handlers.PlayerInventoryHandler;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import com.github.wintersteve25.tau.utils.Variable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TauContainerScreenTest {

    @Test
    void playerInventoryDescriptor_materializesAndPositionsEveryVanillaSlot() {
        MenuSlot<PlayerInventoryHandler> descriptor = new MenuSlot<>(
                new SimpleVec2i(10, 20), new PlayerInventoryHandler(new Variable<>(true))
        );
        List<Slot> slots = slots(36);

        assertEquals(36, TauContainerScreen.materializedSlotCount(List.of(descriptor)));
        assertTrue(TauContainerScreen.applySlotVisualState(List.of(descriptor), slots));

        assertSlotPosition(slots.get(0), 11, 21);
        assertSlotPosition(slots.get(8), 155, 21);
        assertSlotPosition(slots.get(26), 155, 57);
        assertSlotPosition(slots.get(27), 11, 79);
        assertSlotPosition(slots.get(35), 155, 79);
    }

    @Test
    void slotVisualState_rejectsMismatchedMaterializedSlotCount() {
        MenuSlot<PlayerInventoryHandler> descriptor = new MenuSlot<>(
                SimpleVec2i.zero(), new PlayerInventoryHandler(new Variable<>(true))
        );

        assertFalse(TauContainerScreen.applySlotVisualState(List.of(descriptor), slots(35)));
    }

    @Test
    void slotStructureVersionChange_requestsAFullRefreshWithoutDirtyComponents() {
        assertTrue(TauContainerScreen.slotStructureChanged(1, 0));
        assertTrue(TauContainerScreen.slotStructureChanged(-1, 0));
        assertFalse(TauContainerScreen.slotStructureChanged(4, 4));
    }

    @Test
    void compositeHandlerCanExposeMultipleMaterializedSlots() {
        ISlotHandler composite = new ISlotHandler() {
            @Override
            public void setupSync(TauContainerMenu menu, Inventory playerInv, int x, int y) {
            }

            @Override
            public int getSlotCount() {
                return 2;
            }

            @Override
            public SimpleVec2i getSlotOffset(int slotIndex) {
                return slotIndex == 0 ? SimpleVec2i.zero() : new SimpleVec2i(18, 0);
            }
        };
        List<Slot> slots = slots(2);

        assertEquals(2, TauContainerScreen.materializedSlotCount(List.of(new MenuSlot<>(new SimpleVec2i(4, 6), composite))));
        assertTrue(TauContainerScreen.applySlotVisualState(List.of(new MenuSlot<>(new SimpleVec2i(4, 6), composite)), slots));
        assertSlotPosition(slots.get(0), 5, 7);
        assertSlotPosition(slots.get(1), 23, 7);
    }

    private static List<Slot> slots(int count) {
        Container container = mock(Container.class);
        List<Slot> slots = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            slots.add(new Slot(container, index, 0, 0));
        }
        return slots;
    }

    private static void assertSlotPosition(Slot slot, int x, int y) {
        assertEquals(x, slot.x);
        assertEquals(y, slot.y);
    }
}
