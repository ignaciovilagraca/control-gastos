package com.xowl.spending.utils;

import java.text.NumberFormat;
import java.util.Locale;

public class NumberFormatter {
    private final static NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));

    public static String format(Double monto) {
        numberFormat.setMinimumFractionDigits(2);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(monto);
    }
}
