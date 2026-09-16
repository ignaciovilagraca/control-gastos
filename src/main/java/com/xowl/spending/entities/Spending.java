package com.xowl.spending.entities;

import com.xowl.spending.frameworks.repository.SpendingEntity;
import java.time.LocalDate;
import java.util.Optional;

public interface Spending {
    Optional<String> comprobante();

    String print();

    LocalDate fecha();

    Double monto();

    SpendingEntity toEntity(Long version);

    Moneda moneda();
}
