package com.github.wintersteve25.tau.tests;

import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.interactable.Button;
import com.github.wintersteve25.tau.components.interactable.ListView;
import com.github.wintersteve25.tau.components.layout.Align;
import com.github.wintersteve25.tau.components.layout.Center;
import com.github.wintersteve25.tau.components.layout.Column;
import com.github.wintersteve25.tau.components.layout.Row;
import com.github.wintersteve25.tau.components.layout.Stack;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.components.utils.Text;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.layout.LayoutSetting;
import com.github.wintersteve25.tau.renderer.ScreenUIRenderer;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.FlexSizeBehaviour;
import com.github.wintersteve25.tau.utils.Size;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.List;

public class TestAll implements UIComponent {

    private static final int MAX_PER_COLUMN = 10;

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        List<UIComponent> entries = Arrays.asList(
                button(new TestAlign()),
                button(new com.github.wintersteve25.tau.tests.TestButton()),
                button(new TestCenter()),
                button(new TestClip()),
                button(new TestColumn()),
                button(new TestContainer()),
                button(new TestDynamic()),
                button(new TestListView()),
                button(new TestPadding()),
                button(new TestPositioned()),
                button(new TestRender()),
                button(new TestRenderable()),
                button(new TestRow()),
                button(new TestSized()),
                button(new TestSlider()),
                button(new TestStack()),
                button(new TestText()),
                button(new TestTextField()),
                button(new TestTexture()),
                button(new TestTooltip()),
                button(new TestTransform()),
                button(new TestWidgetWrapper()),
                button(new TestInventoryVisual()),
                button(new TestPartialText()),
                button(new TestPartialButton()),
                button(new TestPartialTransform()),
                button(new TestPartialListView())
        );

        List<UIComponent> columns = new java.util.ArrayList<>();
        for (int i = 0; i < entries.size(); i += MAX_PER_COLUMN) {
            int end = Math.min(i + MAX_PER_COLUMN, entries.size());
            columns.add(new Column.Builder()
                    .withSpacing(2)
                    .build(entries.subList(i, end)));
        }

        return new ListView.Builder()
                .withSpacing(8)
                .build(new Row.Builder()
                        .withSpacing(12)
                        .withAlignment(LayoutSetting.START)
                        .build(columns));
    }

    private static UIComponent button(UIComponent screen) {
        return new Sized(
                Size.staticSize(200, 20),
                new Button.Builder()
                        .withOnPress((b) -> Minecraft.getInstance().setScreen(new ScreenUIRenderer(new TestScreen(screen), true)))
                        .build(new Center(new Text.Builder(screen.getClass().getSimpleName())))
        );
    }

    private static final class TestScreen implements UIComponent {

        private final UIComponent screen;

        private TestScreen(UIComponent screen) {
            this.screen = screen;
        }

        @Override
        public UIComponent build(Layout layout, Theme theme) {
            return new Stack(
                    FlexSizeBehaviour.MAX,
                    screen,
                    new Align.Builder()
                            .withHorizontal(LayoutSetting.START)
                            .withVertical(LayoutSetting.START)
                            .build(new Sized(
                                    Size.staticSize(60, 20),
                                    new Button.Builder()
                                            .withOnPress((button) -> Minecraft.getInstance().setScreen(new ScreenUIRenderer(new TestAll(), true)))
                                            .build(new Center(new Text.Builder("Back"))))
                            )
            );
        }
    }
}
