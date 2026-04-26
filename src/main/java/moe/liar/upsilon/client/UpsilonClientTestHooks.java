package moe.liar.upsilon.client;

import com.github.wintersteve25.tau.renderer.ScreenUIRenderer;
import com.github.wintersteve25.tau.tests.TestAll;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Minimal client-only hook for opening manual UI tests in dev runs.
 */
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class UpsilonClientTestHooks {
    private static final KeyMapping OPEN_TESTS = new KeyMapping("key.upsilon.open_tests", GLFW.GLFW_KEY_COMMA, "key.categories.upsilon");

    private UpsilonClientTestHooks() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TESTS);
    }

    @EventBusSubscriber(value = Dist.CLIENT)
    public static final class RuntimeEvents {
        private RuntimeEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            while (OPEN_TESTS.consumeClick()) {
                if (minecraft.player != null) {
                    minecraft.setScreen(new ScreenUIRenderer(new TestAll(), true));
                }
            }
        }
    }
}
