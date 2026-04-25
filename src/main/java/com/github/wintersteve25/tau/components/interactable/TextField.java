package com.github.wintersteve25.tau.components.interactable;

import com.github.wintersteve25.tau.components.utils.WidgetWrapper;
import com.github.wintersteve25.tau.theme.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Text input component backed by Minecraft {@code EditBox}.
 */
public final class TextField implements UIComponent {

    private final Component message;
    private final Component hintText;
    private final Consumer<String> onChange;
    private final Predicate<String> validator;
    private final BiFunction<String, Integer, FormattedCharSequence> formatter;

    /**
     * Creates a text field component.
     *
     * @param message narration/label message
     * @param onChange callback fired when text changes
     * @param validator optional input validator
     * @param hintText optional hint shown when empty
     * @param formatter optional display formatter
     */
    public TextField(Component message, Consumer<String> onChange, Predicate<String> validator, Component hintText,
                     BiFunction<String, Integer, FormattedCharSequence> formatter) {
        this.message = message;
        this.onChange = onChange;
        this.validator = validator;
        this.hintText = hintText;
        this.formatter = formatter;
    }

    /**
     * Builds the wrapped edit-box widget component.
     */
    @Override
    public UIComponent build(Layout layout, Theme theme) {
        return new WidgetWrapper(new TextFieldWidget(message, hintText, onChange, validator, formatter));
    }

    private static final class TextFieldWidget extends net.minecraft.client.gui.components.EditBox {
        /**
         * Creates the backing edit box used by {@link TextField}.
         */
        public TextFieldWidget(Component message, Component hintText, Consumer<String> onChange, Predicate<String> validator, BiFunction<String, Integer, FormattedCharSequence> formatter) {
            super(Minecraft.getInstance().font, 0, 0, 0, 0, message);
            if (validator != null) setFilter(validator);
            if (hintText != null) setSuggestion(hintText.getString());
            if (formatter != null) setFormatter(formatter);

            setResponder(text -> {
                if (onChange != null) {
                    onChange.accept(text);
                }

                if (!text.isEmpty()) {
                    setSuggestion("");
                } else {
                    if (hintText != null) setSuggestion(hintText.getString());
                }
            });
        }
    }

    public static final class Builder implements UIComponent {
        private Component message;
        private Component hintText;
        private Consumer<String> onChange;
        private Predicate<String> validator;
        private BiFunction<String, Integer, FormattedCharSequence> formatter;

        /** Creates a new text field builder. */
        public Builder() {
        }

        /** Sets narration/label message for the text field. */
        public Builder withMessage(Component message) {
            this.message = message;
            return this;
        }

        /** Sets hint text shown while input is empty. */
        public Builder withHintText(Component hintText) {
            this.hintText = hintText;
            return this;
        }

        /** Sets callback invoked whenever text changes. */
        public Builder withOnChange(Consumer<String> onChange) {
            this.onChange = onChange;
            return this;
        }

        /** Sets input validator used by the edit box filter. */
        public Builder withValidator(Predicate<String> validator) {
            this.validator = validator;
            return this;
        }

        /** Sets optional formatter for rendered text segments. */
        public Builder withFormatter(BiFunction<String, Integer, FormattedCharSequence> formatter) {
            this.formatter = formatter;
            return this;
        }

        /** Builds a text field from configured values. */
        public TextField build() {
            return new TextField(message == null ? Component.empty() : message, onChange, validator, hintText, formatter);
        }

        /**
         * Returns a built text field for this builder state.
         */
        @Override
        public UIComponent build(Layout layout, Theme theme) {
            return build();
        }
    }
}
