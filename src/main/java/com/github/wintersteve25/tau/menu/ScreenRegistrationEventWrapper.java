package com.github.wintersteve25.tau.menu;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Small wrapper record around {@link RegisterMenuScreensEvent}.
 */
public record ScreenRegistrationEventWrapper(RegisterMenuScreensEvent event) {
}
