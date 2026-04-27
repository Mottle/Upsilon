package com.github.wintersteve25.tau.tests;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.interactable.Button;
import com.github.wintersteve25.tau.components.interactable.ListView;
import com.github.wintersteve25.tau.components.interactable.Slider;
import com.github.wintersteve25.tau.components.interactable.TextField;
import com.github.wintersteve25.tau.components.layout.Center;
import com.github.wintersteve25.tau.components.layout.Column;
import com.github.wintersteve25.tau.components.layout.Row;
import com.github.wintersteve25.tau.components.render.Transform;
import com.github.wintersteve25.tau.components.utils.Clip;
import com.github.wintersteve25.tau.components.utils.Container;
import com.github.wintersteve25.tau.components.utils.Padding;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.components.utils.Text;
import com.github.wintersteve25.tau.components.utils.Tooltip;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.layout.LayoutSetting;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.FlexSizeBehaviour;
import com.github.wintersteve25.tau.utils.Pad;
import com.github.wintersteve25.tau.utils.Size;
import com.github.wintersteve25.tau.utils.Transformation;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Comprehensive manual correctness scenario for the current UI runtime.
 *
 * This screen intentionally combines root-level filtering, repeated row-local
 * partial commits, size-changing translated panels, list clipping/scrolling,
 * tooltips, and widget-backed controls in one place so regressions are easy to
 * spot during manual verification.
 */
public class TestCorrectnessScenario extends DynamicUIComponent {
    private final StatsPanel statsPanel = new StatsPanel();
    private final SummaryPanel summaryPanel = new SummaryPanel();
    private final SelectionPanel selectionPanel = new SelectionPanel();
    private final TextField filterField;
    private final Slider thresholdSlider;
    private final List<ScenarioRow> rows;

    private String filter = "";
    private int minimumClicks;
    private boolean evenOnly;
    private int actionVersion;
    private ScenarioRow selectedRow;

    public TestCorrectnessScenario() {
        filterField = new TextField.Builder()
                .withHintText(Component.literal("filter rows by name or tag"))
                .withOnChange(value -> {
                    filter = value.toLowerCase(Locale.ROOT);
                    noteAction();
                    rebuild();
                })
                .build();

        thresholdSlider = new Slider.Builder()
                .withPrefix(Component.literal("Min clicks: "))
                .withMinimum(0)
                .withMaximum(6)
                .withStepSize(1f)
                .withDecimalPlaces(0)
                .withValue(0)
                .withOnValueChanged(value -> {
                    minimumClicks = (int) Math.round(value);
                    noteAction();
                    rebuild();
                })
                .build();

        rows = List.of(
                new ScenarioRow(1, "Quartz Relay", "sync"),
                new ScenarioRow(2, "Borealis Beacon", "render"),
                new ScenarioRow(3, "Delta Archive", "state"),
                new ScenarioRow(4, "Signal Garden", "input"),
                new ScenarioRow(5, "Ember Latch", "layout"),
                new ScenarioRow(6, "Tide Index", "scroll"),
                new ScenarioRow(7, "Aster Vault", "tooltip"),
                new ScenarioRow(8, "Kite Terminal", "widget"),
                new ScenarioRow(9, "Nova Anchor", "clip"),
                new ScenarioRow(10, "Cinder Trace", "transform")
        );
    }

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        List<UIComponent> visibleRows = new ArrayList<>();
        for (ScenarioRow row : rows) {
            if (row.matches(filter, minimumClicks, evenOnly)) {
                visibleRows.add(row);
            }
        }

        if (visibleRows.isEmpty()) {
            visibleRows.add(panelTextLine(380, "No rows match the current filter settings."));
        }

        return new Center(new Sized(
                Size.percentage(0.96f, 0.94f),
                new Row.Builder()
                        .withSpacing(12)
                        .withAlignment(LayoutSetting.START)
                        .build(
                                buildControlPanel(),
                                buildWorkspacePanel(visibleRows)
                        )
        ));
    }

    private UIComponent buildControlPanel() {
        return new Sized(Size.percentage(0.32f, 1f),
                new Container.Builder()
                        .withSizeBehaviour(FlexSizeBehaviour.MAX)
                        .withChild(new Padding(new Pad.Builder().all(8).build(),
                                new ListView.Builder()
                                        .withSpacing(6)
                                        .build(
                                                new Sized(Size.staticSize(220, 14), new Text.Builder("Correctness Scenario")),
                                                wrappedLine(220, 30, "Use this page to stress root rebuilds, nested partial commits, translated panels, widgets, and scrolling together."),
                                                instructionLine("1. Type in the filter box and move the slider."),
                                                instructionLine("2. Bump and expand rows in the list repeatedly."),
                                                instructionLine("3. Only the targeted row should change."),
                                                instructionLine("4. No row may overlap, vanish, or smear text."),
                                                instructionLine("5. Toggle the summary card and hover tooltip chips."),
                                                instructionLine("6. Text field and slider state must persist."),
                                                new Sized(Size.staticSize(220, 12), new Text.Builder("Filter rows (root partial commit)")),
                                                new Sized(Size.staticSize(220, 20), filterField),
                                                new Sized(Size.staticSize(220, 12), new Text.Builder("Minimum click threshold (root partial commit)")),
                                                new Sized(Size.staticSize(220, 20), thresholdSlider),
                                                actionButton(220, evenOnly ? "Show All Row IDs" : "Show Even Row IDs Only", button -> {
                                                    evenOnly = !evenOnly;
                                                    noteAction();
                                                    rebuild();
                                                }),
                                                actionButton(220, "Reset Row Counts And Details", button -> {
                                                    for (ScenarioRow row : rows) {
                                                        row.resetForRootRefresh();
                                                    }
                                                    noteAction();
                                                    rebuild();
                                                }),
                                                statsPanel
                                        )
                        ))
                        .build()
        );
    }

    private UIComponent buildWorkspacePanel(List<UIComponent> visibleRows) {
        return new Sized(
                Size.percentage(0.68f, 1f),
                new Container.Builder()
                        .withSizeBehaviour(FlexSizeBehaviour.MAX)
                        .withChild(new Padding(new Pad.Builder().all(8).build(),
                                new Column.Builder()
                                        .withSpacing(8)
                                        .withAlignment(LayoutSetting.START)
                                        .build(
                                                new Transform(
                                                        new Container.Builder()
                                                                .withSizeBehaviour(FlexSizeBehaviour.MIN)
                                                                .withChild(new Padding(new Pad.Builder().all(6).build(), summaryPanel))
                                                                .build(),
                                                        Transformation.translate(new Vector3f(10, 0, 0))
                                                ),
                                                new Transform(
                                                        new Container.Builder()
                                                                .withSizeBehaviour(FlexSizeBehaviour.MIN)
                                                                .withChild(new Padding(new Pad.Builder().all(6).build(), selectionPanel))
                                                                .build(),
                                                        Transformation.translate(new Vector3f(18, 0, 0))
                                                ),
                                                new Sized(
                                                        Size.percentage(1f, 0.75f),
                                                        new ListView.Builder()
                                                                .withSpacing(4)
                                                                .build(visibleRows)
                                                )
                                        )
                        ))
                        .build()
        );
    }

    private UIComponent actionButton(int width, String label, java.util.function.Consumer<Integer> onPress) {
        return new Sized(
                Size.staticSize(width, 20),
                new Button.Builder()
                        .withOnPress(onPress)
                        .build(new Center(new Text.Builder(label)))
        );
    }

    private UIComponent instructionLine(String text) {
        return wrappedLine(220, 18, text);
    }

    private UIComponent wrappedLine(int width, int height, String text) {
        return new Sized(
                Size.staticSize(width, height),
                new Text.Builder(text).withOverflowBehaviour(Text.OverflowBehaviour.WRAP)
        );
    }

    private UIComponent panelTextLine(int width, String text) {
        return new Sized(Size.staticSize(width, 14), new Text.Builder(text));
    }

    private void noteAction() {
        actionVersion++;
    }

    private int totalClicks() {
        int total = 0;
        for (ScenarioRow row : rows) {
            total += row.clicks;
        }
        return total;
    }

    private int expandedRows() {
        int total = 0;
        for (ScenarioRow row : rows) {
            if (row.expanded) {
                total++;
            }
        }
        return total;
    }

    private int visibleRowCount() {
        int total = 0;
        for (ScenarioRow row : rows) {
            if (row.matches(filter, minimumClicks, evenOnly)) {
                total++;
            }
        }
        return total;
    }

    private void onRowSelected(ScenarioRow row) {
        selectedRow = row;
        noteAction();
        summaryPanel.refresh();
        selectionPanel.refresh();
        statsPanel.refresh();
    }

    private void onRowBumped(ScenarioRow row) {
        noteAction();
        summaryPanel.refresh();
        selectionPanel.refresh();
        statsPanel.refresh();
    }

    private void onRowExpanded(ScenarioRow row) {
        noteAction();
        summaryPanel.refresh();
        selectionPanel.refresh();
        statsPanel.refresh();
    }

    private final class StatsPanel extends DynamicUIComponent {
        @Override
        public UIComponent build(Layout layout, Theme theme) {
            String selected = selectedRow == null ? "none" : selectedRow.name;
            return new Container.Builder()
                    .withSizeBehaviour(FlexSizeBehaviour.MIN)
                    .withChild(new Padding(new Pad.Builder().all(6).build(),
                            new Column.Builder()
                                    .withSpacing(4)
                                    .withAlignment(LayoutSetting.START)
                                    .build(
                                            panelTextLine(200, "Action version: " + actionVersion),
                                            panelTextLine(200, "Visible rows: " + visibleRowCount() + " / " + rows.size()),
                                            panelTextLine(200, "Total clicks: " + totalClicks()),
                                            panelTextLine(200, "Expanded rows: " + expandedRows()),
                                            panelTextLine(200, "Selected row: " + selected)
                                    )
                    ))
                    .build();
        }

        void refresh() {
            rebuild();
        }
    }

    private final class SummaryPanel extends DynamicUIComponent {
        private boolean expanded = true;
        private int toggleCount;

        @Override
        public UIComponent build(Layout layout, Theme theme) {
            List<UIComponent> lines = new ArrayList<>();
            lines.add(new Row.Builder()
                    .withSpacing(6)
                    .withAlignment(LayoutSetting.CENTER)
                    .build(
                            actionButton(160, expanded ? "Collapse Summary Card" : "Expand Summary Card", button -> {
                                expanded = !expanded;
                                toggleCount++;
                                noteAction();
                                statsPanel.refresh();
                                selectionPanel.refresh();
                                rebuild();
                            }),
                            new Sized(
                                    Size.staticSize(150, 20),
                                    new Tooltip.Builder()
                                            .withComponent(Component.literal("Hover verifies tooltip layering after repeated partial commits."))
                                            .build(new Center(new Text.Builder("Hover Summary Chip")))
                            )
                    ));
            lines.add(panelTextLine(330, "Summary toggles: " + toggleCount + " | action version: " + actionVersion));
            lines.add(panelTextLine(330, "Visible rows: " + visibleRowCount() + " | expanded rows: " + expandedRows()));

            if (expanded) {
                lines.add(new Transform(
                        new Container.Builder()
                                .withSizeBehaviour(FlexSizeBehaviour.MIN)
                                .withChild(new Padding(new Pad.Builder().all(4).build(),
                                        new Column.Builder()
                                                .withSpacing(2)
                                                .withAlignment(LayoutSetting.START)
                                                .build(
                                                        panelTextLine(300, "Type in the field, then use row buttons and slider."),
                                                        panelTextLine(300, "This translated card must resize cleanly every time."),
                                                        new Sized(
                                                                Size.staticSize(300, 16),
                                                                new Clip.Builder().build(new Text.Builder(
                                                                        "Long trail -> filter='" + filter + "' threshold=" + minimumClicks
                                                                                + " evenOnly=" + evenOnly + " totalClicks=" + totalClicks()
                                                                                + " selected=" + (selectedRow == null ? "none" : selectedRow.name)
                                                                ))
                                                        )
                                                )
                                ))
                                .build(),
                        Transformation.translate(new Vector3f(12, 0, 0))
                ));
            }

            return new Column.Builder()
                    .withSpacing(4)
                    .withAlignment(LayoutSetting.START)
                    .build(lines);
        }

        void refresh() {
            rebuild();
        }
    }

    private final class SelectionPanel extends DynamicUIComponent {
        @Override
        public UIComponent build(Layout layout, Theme theme) {
            String selected = selectedRow == null ? "none" : selectedRow.name + " [" + selectedRow.tag + "]";
            String detail = selectedRow == null
                    ? "Select any row from the list to populate this translated panel."
                    : "Selected row clicks=" + selectedRow.clicks + " expanded=" + selectedRow.expanded + " actionVersion=" + actionVersion;

            return new Column.Builder()
                    .withSpacing(4)
                    .withAlignment(LayoutSetting.START)
                    .build(
                            panelTextLine(300, "Selection panel"),
                            panelTextLine(300, "Current selection: " + selected),
                            panelTextLine(300, detail),
                            new Sized(
                                    Size.staticSize(300, 16),
                                    new Tooltip.Builder()
                                            .withComponent(Component.literal("This chip should keep its tooltip aligned while sibling rows rebuild."))
                                            .build(new Clip.Builder().build(new Text.Builder(
                                                    "Status trail -> visible=" + visibleRowCount()
                                                            + " totalClicks=" + totalClicks()
                                                            + " filter='" + filter + "'"
                                            )))
                            )
                    );
        }

        void refresh() {
            rebuild();
        }
    }

    private final class ScenarioRow extends DynamicUIComponent {
        private final int id;
        private final String name;
        private final String tag;

        private int clicks;
        private boolean expanded;

        private ScenarioRow(int id, String name, String tag) {
            this.id = id;
            this.name = name;
            this.tag = tag;
        }

        private boolean matches(String filter, int minimumClicks, boolean evenOnly) {
            if (clicks < minimumClicks) {
                return false;
            }
            if (evenOnly && (id % 2) != 0) {
                return false;
            }
            if (filter.isBlank()) {
                return true;
            }
            String haystack = (name + " " + tag).toLowerCase(Locale.ROOT);
            return haystack.contains(filter);
        }

        private void resetForRootRefresh() {
            clicks = 0;
            expanded = false;
        }

        @Override
        public UIComponent build(Layout layout, Theme theme) {
            List<UIComponent> children = new ArrayList<>();
            children.add(new Row.Builder()
                    .withSpacing(4)
                    .withAlignment(LayoutSetting.CENTER)
                    .build(
                            new Tooltip.Builder()
                                    .withComponent(Component.literal("Row " + id + " | tag=" + tag + " | clicks=" + clicks + " | expanded=" + expanded))
                                    .build(actionButton(130, name, button -> onRowSelected(this))),
                            actionButton(88, "Bump " + clicks, button -> {
                                clicks++;
                                onRowBumped(this);
                                rebuild();
                            }),
                            actionButton(88, expanded ? "Collapse" : "Expand", button -> {
                                expanded = !expanded;
                                onRowExpanded(this);
                                rebuild();
                            }),
                            new Sized(Size.staticSize(64, 14), new Center(new Text.Builder(tag)))
                    ));

            if (expanded) {
                children.add(new Transform(
                        new Container.Builder()
                                .withSizeBehaviour(FlexSizeBehaviour.MIN)
                                .withChild(new Padding(new Pad.Builder().all(4).build(),
                                        new Column.Builder()
                                                .withSpacing(2)
                                                .withAlignment(LayoutSetting.START)
                                                .build(
                                                        panelTextLine(320, "Expanded row " + id + " keeps list spacing stable during repeated commits."),
                                                        new Sized(
                                                                Size.staticSize(320, 16),
                                                                new Clip.Builder().build(new Text.Builder(
                                                                        "Detail trail -> name=" + name + " tag=" + tag + " clicks=" + clicks
                                                                                + " selected=" + (selectedRow == this)
                                                                                + " filter='" + filter + "' threshold=" + minimumClicks
                                                                ))
                                                        )
                                                )
                                ))
                                .build(),
                        Transformation.translate(new Vector3f(14, 0, 0))
                ));
            }

            return new Container.Builder()
                    .withSizeBehaviour(FlexSizeBehaviour.MIN)
                    .withChild(new Padding(new Pad.Builder().all(6).build(),
                            new Column.Builder()
                                    .withSpacing(4)
                                    .withAlignment(LayoutSetting.START)
                                    .build(children)
                    ))
                    .build();
        }
    }
}
