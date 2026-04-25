package com.github.wintersteve25.tau;

import net.neoforged.bus.api.IEventBus;
import moe.liar.upsilon.Upsilon;

/**
 * @deprecated Replaced by {@link moe.liar.upsilon.Upsilon} as the mod entrypoint.
 */
@Deprecated(forRemoval = false)
public class Tau {
    public static final String MOD_ID = Upsilon.MOD_ID;
    public static final org.apache.logging.log4j.Logger LOGGER = Upsilon.LOGGER;

    public Tau(IEventBus bus) {
    }
}
