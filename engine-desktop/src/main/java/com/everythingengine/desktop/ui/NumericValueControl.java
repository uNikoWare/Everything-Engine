package com.everythingengine.desktop.ui;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

final class NumericValueControl extends HBox {
    private final Kind kind;
    private final ComboBox<ValueMode> valueMode = new ComboBox<>();
    private final Label minimumLabel = new Label();
    private final TextField minimum = new TextField();
    private final Label maximumLabel = new Label();
    private final TextField maximum = new TextField();

    NumericValueControl(String defaultValue, Kind kind) {
        super(8);
        this.kind = kind;
        valueMode.getItems().setAll(ValueMode.values());
        valueMode.setValue(ValueMode.FIXED);
        valueMode.setConverter(new StringConverter<>() {
            @Override
            public String toString(ValueMode value) {
                return value == null ? "" : Localization.text(value.label);
            }

            @Override
            public ValueMode fromString(String value) {
                return null;
            }
        });
        minimum.setText(defaultValue);
        maximum.setText(defaultValue);
        minimum.setPromptText(Localization.text("Value / minimum"));
        maximum.setPromptText(Localization.text("Maximum"));
        minimum.setPrefColumnCount(9);
        maximum.setPrefColumnCount(9);
        valueMode.setPrefWidth(110);
        getChildren().addAll(valueMode, minimumLabel, minimum, maximumLabel, maximum);
        valueMode.setOnAction(event -> updateFields());
        updateFields();
    }

    Number sample() {
        Number lower = parse(minimum.getText());
        if (valueMode.getValue() == ValueMode.FIXED) {
            return lower;
        }
        Number upper = parse(maximum.getText());
        if (compare(lower, upper) > 0) {
            throw new IllegalArgumentException(Localization.text("Range minimum must not exceed maximum."));
        }
        return switch (kind) {
            case DECIMAL -> sampleDecimal(lower.doubleValue(), upper.doubleValue());
            case INTEGER -> Math.toIntExact(sampleLong(lower.longValue(), upper.longValue()));
            case LONG -> sampleLong(lower.longValue(), upper.longValue());
        };
    }

    private Number parse(String text) {
        try {
            return switch (kind) {
                case DECIMAL -> {
                    double value = Double.parseDouble(text.trim());
                    if (!Double.isFinite(value)) {
                        throw new NumberFormatException();
                    }
                    yield value;
                }
                case INTEGER -> Integer.parseInt(text.trim());
                case LONG -> Long.parseLong(text.trim());
            };
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(Localization.text("Enter a valid numeric value."), exception);
        }
    }

    private int compare(Number first, Number second) {
        return switch (kind) {
            case DECIMAL -> Double.compare(first.doubleValue(), second.doubleValue());
            case INTEGER -> Integer.compare(first.intValue(), second.intValue());
            case LONG -> Long.compare(first.longValue(), second.longValue());
        };
    }

    private double sampleDecimal(double lower, double upper) {
        return lower == upper ? lower : ThreadLocalRandom.current().nextDouble(lower, upper);
    }

    private long sampleLong(long lower, long upper) {
        if (lower == upper) {
            return lower;
        }
        BigInteger low = BigInteger.valueOf(lower);
        BigInteger range = BigInteger.valueOf(upper).subtract(low).add(BigInteger.ONE);
        int byteCount = (range.bitLength() + 7) / 8;
        int excessBits = byteCount * 8 - range.bitLength();
        BigInteger offset;
        do {
            byte[] bytes = new byte[byteCount];
            ThreadLocalRandom.current().nextBytes(bytes);
            bytes[0] &= (byte) (0xff >>> excessBits);
            offset = new BigInteger(1, bytes);
        } while (offset.compareTo(range) >= 0);
        return low.add(offset).longValueExact();
    }

    private void updateFields() {
        boolean range = valueMode.getValue() == ValueMode.RANGE;
        minimumLabel.setText(Localization.text(range ? "Minimum:" : "Value:"));
        maximum.setVisible(range);
        maximum.setManaged(range);
        maximumLabel.setVisible(range);
        maximumLabel.setManaged(range);
    }

    enum Kind {
        DECIMAL,
        INTEGER,
        LONG
    }

    private enum ValueMode {
        FIXED("Fixed"),
        RANGE("Range");

        private final String label;

        ValueMode(String label) {
            this.label = label;
        }
    }
}
