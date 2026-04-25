package com.github.wintersteve25.tau.menu;

import com.github.wintersteve25.tau.menu.handlers.ISlotHandler;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

/**
 * Lightweight slot descriptor produced during UI build.
 *
 * @param pos slot position in GUI coordinates
 * @param handler slot handler responsible for creating synced slots
 * @param <T> concrete slot handler type
 */
public record MenuSlot<T extends ISlotHandler>(SimpleVec2i pos, T handler) {
}
