package com.github.wintersteve25.tau.tests;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.interactable.Button;
import com.github.wintersteve25.tau.components.layout.Center;
import com.github.wintersteve25.tau.components.layout.Column;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.components.utils.Text;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.Size;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests Button mount state through partial rebuild: a counter label
 * and a clickable increment button. Button mouseClicked reads
 * activeMountState geometry; after dirty rebuild the mount state
 * is re-promoted from staged.
 */
public class TestPartialButton extends DynamicUIComponent {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        int value = counter.get();
        return new Center(new Column.Builder()
                .withSpacing(4)
                .build(
                        new Sized(
                                Size.staticSize(200, 20),
                                new Center(new Text.Builder("Clicks: " + value))
                        ),
                        new Sized(
                                Size.staticSize(100, 20),
                                new Button.Builder()
                                        .withOnPress(btn -> {
                                            counter.incrementAndGet();
                                            rebuild();
                                        })
                                        .build(new Center(new Text.Builder("Click Me")))
                        )
                )
        );
    }
}
