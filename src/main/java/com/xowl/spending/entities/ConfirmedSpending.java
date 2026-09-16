package com.xowl.spending.entities;

import com.xowl.spending.frameworks.repository.SpendingEntity;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Builder;

@Builder
public class ConfirmedSpending implements Spending {
    private final Authorization authorization;
    private final String comprobante;

    @Override
    public Optional<String> comprobante() {
        return Optional.of(comprobante);
    }

    @Override
    public String print() {
        return authorization.print();
    }

    @Override
    public LocalDate fecha() {
        return authorization.fecha();
    }

    @Override
    public Double monto() {
        return authorization.monto();
    }

    @Override
    public SpendingEntity toEntity(Long version) {
        return authorization.toEntity(comprobante, version);
    }

    @Override
    public Moneda moneda() {
        return authorization.moneda();
    }
}
