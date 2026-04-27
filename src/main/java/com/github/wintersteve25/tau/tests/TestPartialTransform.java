package com.github.wintersteve25.tau.tests;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.interactable.Button;
import com.github.wintersteve25.tau.components.layout.Center;
import com.github.wintersteve25.tau.components.render.Transform;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.components.utils.Text;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.Size;
import com.github.wintersteve25.tau.utils.Transformation;
import org.joml.Vector3f;

/**
 * Tests partial rebuild inside a Transform using a real bounded button.
 * The dynamic component lives inside a translated container, and button
 * presses trigger a partial commit within the Transform's inner context.
 */
public class TestPartialTransform implements UIComponent {
    private final InnerDynamic inner = new InnerDynamic();

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        return new Transform(
                inner,
                Transformation.translate(new Vector3f(50, 50, 0))
        );
    }

    private static final class InnerDynamic extends DynamicUIComponent {
        private boolean state;

        @Override
        public UIComponent build(Layout layout, Theme theme) {
            return new Sized(
                    Size.staticSize(200, 20),
                    new Button.Builder()
                            .withOnPress(btn -> {
                                state = !state;
                                rebuild();
                            })
                            .build(new Center(new Text.Builder(state ? "Inside Transform - A" : "Inside Transform - B")))
            );
        }
    }
}
