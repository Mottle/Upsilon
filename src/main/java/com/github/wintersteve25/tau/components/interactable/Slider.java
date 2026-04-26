package com.github.wintersteve25.tau.components.interactable;

import com.github.wintersteve25.tau.build.BuilderShell;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.utils.WidgetFactoryWrapper;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;

import java.util.function.Consumer;

/**
 * Slider component backed by NeoForge {@link ExtendedSlider}.
 */
public final class Slider implements UIComponent {

    private final Component prefix;
    private final Component suffix;
    private final float stepSize;
    private final int decimalPlaces;
    private final double minimum;
    private final double maximum;
    private final Runnable onPress;
    private final Consumer<Double> onValueChanged;
    private double value;

    /**
     * Creates a slider.
     *
     * @param prefix         text shown before the value
     * @param suffix         text shown after the value
     * @param stepSize       slider snap step
     * @param decimalPlaces  number of decimals shown
     * @param value          initial value
     * @param minimum        minimum allowed value
     * @param maximum        maximum allowed value
     * @param onPress        callback fired on click
     * @param onValueChanged callback fired when value changes
     */
    public Slider(Component prefix, Component suffix, float stepSize, int decimalPlaces, double value, double minimum,
                  double maximum, Runnable onPress, Consumer<Double> onValueChanged) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.stepSize = stepSize;
        this.decimalPlaces = decimalPlaces;
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
        this.onPress = onPress;
        this.onValueChanged = onValueChanged;
    }

    /**
     * Builds the slider widget wrapper.
     */
    @Override
    public UIComponent build(Layout layout, Theme theme) {
        return new WidgetFactoryWrapper(() -> new SliderWidget(this, prefix, suffix, stepSize, decimalPlaces, minimum, maximum, value, onPress, onValueChanged));
    }

    private static final class SliderWidget extends ExtendedSlider {
        private final Slider owner;
        private final Runnable onPress;
        private final Consumer<Double> onValueChange;

        /**
         * Creates the backing widget used by {@link Slider}.
         */
        public SliderWidget(Slider owner, Component prefix, Component suffix, float stepSize, int decimalAmounts, double minVal, double maxVal, double currentVal, Runnable onPress, Consumer<Double> onValueChange) {
            super(0, 0, 0, 0, prefix, suffix, minVal, maxVal, currentVal, stepSize, decimalAmounts, true);
            this.owner = owner;
            this.onPress = onPress;
            this.onValueChange = onValueChange;
            setValue(owner.value);
        }

        /**
         * Invokes optional click callback before normal slider handling.
         */
        @Override
        public void onClick(double mouseX, double mouseY) {
            if (onPress != null) onPress.run();
            super.onClick(mouseX, mouseY);
        }

        @Override
        protected void applyValue() {
            owner.value = getValue();
            if (onValueChange != null) onValueChange.accept(getValue());
        }
    }

    public static final class Builder implements UIComponent, BuilderShell {
        private Component prefix;
        private Component suffix;
        private float stepSize = 0.1f;
        private int decimalPlaces = 2;
        private double value;
        private double minimum = 0;
        private double maximum = 1;
        private Runnable onPress;
        private Consumer<Double> onValueChanged;

        /**
         * Creates a new slider builder.
         */
        public Builder() {
        }

        /**
         * Sets the text prefix displayed before value.
         */
        public Builder withPrefix(Component prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Sets the text suffix displayed after value.
         */
        public Builder withSuffix(Component suffix) {
            this.suffix = suffix;
            return this;
        }

        /**
         * Sets how many decimals are displayed in the value label.
         */
        public Builder withDecimalPlaces(int decimalPlaces) {
            this.decimalPlaces = decimalPlaces;
            return this;
        }

        /**
         * Sets slider snapping step size.
         */
        public Builder withStepSize(float stepSize) {
            this.stepSize = stepSize;
            return this;
        }

        /**
         * Sets initial slider value.
         */
        public Builder withValue(double value) {
            this.value = value;
            return this;
        }

        /**
         * Sets minimum slider value.
         */
        public Builder withMinimum(double minimum) {
            this.minimum = minimum;
            return this;
        }

        /**
         * Sets maximum slider value.
         */
        public Builder withMaximum(double maximum) {
            this.maximum = maximum;
            return this;
        }

        /**
         * Sets callback invoked when the slider is clicked.
         */
        public Builder withOnPress(Runnable onPress) {
            this.onPress = onPress;
            return this;
        }

        /**
         * Sets callback invoked when slider value changes.
         */
        public Builder withOnValueChanged(Consumer<Double> onValueChanged) {
            this.onValueChanged = onValueChanged;
            return this;
        }

        /**
         * Builds a slider from configured values.
         */
        public Slider build() {
            return new Slider(
                    prefix == null ? Component.empty() : prefix,
                    suffix == null ? Component.empty() : suffix,
                    stepSize,
                    decimalPlaces,
                    value,
                    minimum,
                    maximum,
                    onPress,
                    onValueChanged
            );
        }

        /**
         * Returns a built slider for this builder state.
         */
        @Override
        public UIComponent build(Layout layout, Theme theme) {
            return build();
        }
    }
}
