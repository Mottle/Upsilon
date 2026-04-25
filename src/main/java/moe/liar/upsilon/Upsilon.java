package moe.liar.upsilon;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runtime entrypoint for the Upsilon NeoForge mod.
 * <p>
 * This class owns the canonical mod id and logger used across the project.
 */
@Mod(Upsilon.MOD_ID)
public class Upsilon {
    /** Runtime mod id and resource namespace. */
    public static final String MOD_ID = "upsilon";
    /** Shared logger configured with {@link #MOD_ID} as logger name. */
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    /**
     * Creates the mod instance.
     *
     * @param bus mod event bus provided by NeoForge
     */
    public Upsilon(IEventBus bus) {
    }
}
