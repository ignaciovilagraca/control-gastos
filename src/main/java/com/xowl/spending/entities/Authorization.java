package com.xowl.spending.entities;

import com.xowl.spending.frameworks.repository.SpendingEntity;
import com.xowl.spending.utils.NumberFormatter;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;

@Builder
public class Authorization implements Spending {
    private final LocalDate fecha;
    private final String concepto;
    private final Double monto;
    private final Moneda moneda;

    @Override
    public Optional<String> comprobante() {
        return Optional.empty();
    }

    @Override
    public String print() {
        return "\uD83D\uDCC5 " + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                "\n\uD83D\uDCD6 " + concepto +
                "\n\uD83D\uDCB5 " + NumberFormatter.format(monto) + " " + moneda.toString();
    }

    @Override
    public LocalDate fecha() {
        return fecha;
    }

    @Override
    public Double monto() {
        return monto;
    }

    public SpendingEntity toEntity(String comprobante, Long version) {
        return new SpendingEntity(UUID.randomUUID(), Date.valueOf(fecha), concepto, monto, comprobante, moneda.name(), version, Timestamp.from(Instant.now()));
    }

    @Override
    public SpendingEntity toEntity(Long version) {
        return new SpendingEntity(UUID.randomUUID(), Date.valueOf(fecha), concepto, monto, null, moneda.name(), version, Timestamp.from(Instant.now()));
    }

    @Override
    public Moneda moneda() {
        return moneda;
    }
}
