package com.xowl.spending.adapters;

import com.xowl.spending.entities.Authorization;
import com.xowl.spending.entities.ConfirmedSpending;
import com.xowl.spending.entities.Moneda;
import com.xowl.spending.entities.Spending;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

public class SpendingParser {
    private final static NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH);

    public Spending parseAuth(String auth) {
        String[] fecha = auth.substring(0, 10).split("/");
        String concepto = auth.split("\\$")[0].substring(11).split(" CONS\\. ")[0];
        String monto;
        Moneda moneda;
        if (auth.contains("CONS.USD")) {
            String[] splitAuth = auth.split(" ");
            monto = splitAuth[splitAuth.length - 1];
            moneda = Moneda.USD;
        } else {
            monto = auth.split("\\$")[1].split(" ")[1];
            moneda = Moneda.ARS;
        }

        Number number;
        try {
            number = numberFormat.parse(monto.trim());
        } catch (ParseException e) {
            throw new RuntimeException(e.getCause());
        }

        LocalDate localDate = LocalDate.of(Integer.parseInt(fecha[2]), Integer.parseInt(fecha[1]), Integer.parseInt(fecha[0]));

        return Authorization.builder()
                .fecha(localDate)
                .concepto(concepto)
                .monto(number.doubleValue())
                .moneda(moneda)
                .build();
    }

    public Spending parseSpending(String spending) {
        String[] fields = spending.split(" ");
        String[] fecha = fields[0].split("/");
        String monto = fields[fields.length - 2];
        String comprobante = fields[fields.length - 3];

        StringBuilder concepto = new StringBuilder();
        for (int i = 1; i < fields.length - 3; i++) {
            if (i == fields.length - 4) {
                concepto.append(fields[i]);
            } else {
                concepto.append(fields[i]).append(" ");
            }
        }

        Moneda moneda;
        if (Objects.equals(monto, "0.00") || Objects.equals(monto, "0,00")) {
            monto = fields[fields.length - 1];
            moneda = Moneda.USD;
        } else {
            moneda = Moneda.ARS;
        }

        Number number;
        try {
            number = numberFormat.parse(monto);
        } catch (ParseException e) {
            throw new RuntimeException(e.getCause());
        }

        LocalDate localDate = LocalDate.of(Integer.parseInt(fecha[2]), Integer.parseInt(fecha[1]), Integer.parseInt(fecha[0]));

        return ConfirmedSpending.builder()
                .authorization(Authorization.builder()
                        .fecha(localDate)
                        .concepto(concepto.toString())
                        .monto(number.doubleValue())
                        .moneda(moneda)
                        .build())
                .comprobante(comprobante)
                .build();
    }
}
