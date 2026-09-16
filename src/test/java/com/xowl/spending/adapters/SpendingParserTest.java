package com.xowl.spending.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xowl.spending.entities.Moneda;
import com.xowl.spending.entities.Spending;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SpendingParserTest {

    private final SpendingParser parser = new SpendingParser();

    @Test
    void parseSpending_parsesArsLine() {
        Spending s = parser.parseSpending("01/03/2026 COMPRA EJEMPLO EXTRA 12345 1500.00 0.00");
        assertEquals(LocalDate.of(2026, 3, 1), s.fecha());
        assertEquals(1500.0, s.monto(), 0.001);
        assertEquals(Moneda.ARS, s.moneda());
        assertEquals("12345", s.comprobante().orElseThrow());
    }

    @Test
    void parseAuth_parsesArsAuthorization() {
        Spending a = parser.parseAuth("01/03/2026 COMPRA X CONS. ARS $ 500.00 ");
        assertEquals(LocalDate.of(2026, 3, 1), a.fecha());
        assertEquals(500.0, a.monto(), 0.001);
        assertEquals(Moneda.ARS, a.moneda());
    }
}
