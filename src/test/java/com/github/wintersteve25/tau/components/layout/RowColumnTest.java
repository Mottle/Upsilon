package com.github.wintersteve25.tau.components.layout;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.layout.LayoutSetting;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.FlexSizeBehaviour;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class RowColumnTest {

    @Test
    void row_materializesOneShotIterableBeforeMeasurement() {
        CountingLeaf first = new CountingLeaf(10, 4);
        CountingLeaf second = new CountingLeaf(20, 8);
        Row row = new Row(2, FlexSizeBehaviour.MIN, oneShot(first, second), LayoutSetting.CENTER);

        SimpleVec2i result = UIBuilder.build(new Layout(100, 100), mock(Theme.class), row, new BuildContext());

        assertEquals(32, result.x);
        assertEquals(8, result.y);
        assertEquals(2, first.builds.get(), "child must be measured and then built");
        assertEquals(2, second.builds.get(), "child must be measured and then built");
    }

    @Test
    void column_materializesOneShotIterableBeforeMeasurement() {
        CountingLeaf first = new CountingLeaf(4, 10);
        CountingLeaf second = new CountingLeaf(8, 20);
        Column column = new Column(2, FlexSizeBehaviour.MIN, oneShot(first, second), LayoutSetting.CENTER);

        SimpleVec2i result = UIBuilder.build(new Layout(100, 100), mock(Theme.class), column, new BuildContext());

        assertEquals(8, result.x);
        assertEquals(32, result.y);
        assertEquals(2, first.builds.get(), "child must be measured and then built");
        assertEquals(2, second.builds.get(), "child must be measured and then built");
    }

    private static Iterable<UIComponent> oneShot(UIComponent... children) {
        AtomicBoolean consumed = new AtomicBoolean();
        List<UIComponent> values = List.of(children);
        return () -> consumed.compareAndSet(false, true) ? values.iterator() : List.<UIComponent>of().iterator();
    }

    private static final class CountingLeaf implements PrimitiveUIComponent {
        private final SimpleVec2i size;
        private final AtomicInteger builds = new AtomicInteger();

        private CountingLeaf(int width, int height) {
            this.size = new SimpleVec2i(width, height);
        }

        @Override
        public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
            builds.incrementAndGet();
            return new SimpleVec2i(size.x, size.y);
        }
    }
}
