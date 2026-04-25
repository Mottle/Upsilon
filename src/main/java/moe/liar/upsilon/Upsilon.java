package moe.liar.upsilon;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Upsilon.MOD_ID)
public class Upsilon {
    public static final String MOD_ID = "upsilon";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public Upsilon(IEventBus bus) {
    }
}
