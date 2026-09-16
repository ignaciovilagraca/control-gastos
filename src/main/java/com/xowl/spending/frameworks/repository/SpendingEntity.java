package com.xowl.spending.frameworks.repository;


import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "spending")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SpendingEntity {
    @Id
    private UUID id;
    @Column(name = "date")
    private Date fecha;
    @Column(name = "description")
    private String concepto;
    @Column(name = "amount")
    private Double monto;
    @Column(name = "transaction_id")
    private String comprobante;
    @Column(name = "currency")
    private String currency;
    @Column(name = "version")
    private Long version;
    @Column(name = "creation_date")
    private Timestamp creationDate;
}
