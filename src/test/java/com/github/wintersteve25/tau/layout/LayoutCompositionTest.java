package com.github.wintersteve25.tau.layout;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.utils.Padding;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.Pad;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class LayoutCompositionTest {

    @Test
    void nestedPadding_accumulatesAvailableSpaceAndOffset() {
        RecordingLeaf leaf = new RecordingLeaf();
        Padding outer = new Padding(new Pad.Builder().all(10).build(),
                new Padding(new Pad.Builder().all(5).build(), leaf));

        UIBuilder.build(new Layout(100, 80), mock(Theme.class), outer, new BuildContext());

        assertEquals(70, leaf.availableSize.x);
        assertEquals(50, leaf.availableSize.y);
        assertEquals(15, leaf.position.x);
        assertEquals(15, leaf.position.y);
    }

    @Test
    void copiedLayout_preservesAllActiveSizeModifiers() {
        Layout layout = new Layout(100, 80);
        layout.pushSizeMod(Axis.HORIZONTAL, -20);
        layout.pushSizeMod(Axis.HORIZONTAL, -10);
        layout.pushSizeMod(Axis.VERTICAL, -12);
        layout.pushSizeMod(Axis.VERTICAL, -8);

        Layout copy = layout.copy();

        assertEquals(70, copy.getWidth());
        assertEquals(60, copy.getHeight());
    }

    private static final class RecordingLeaf implements PrimitiveUIComponent {
        private SimpleVec2i availableSize;
        private SimpleVec2i position;

        @Override
        public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
            availableSize = layout.getSize();
            position = layout.getPosition(SimpleVec2i.zero());
            return SimpleVec2i.zero();
        }
    }
}
