package com.tallersoft.model;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobros")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cobro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "monto_recibido", precision = 10, scale = 2)
    private BigDecimal montoRecibido;

    @Column(precision = 10, scale = 2)
    private BigDecimal vuelto;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", nullable = false)
    private MedioPago medioPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false)
    @Builder.Default
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;

    @Column(name = "mp_payment_id")
    private String mpPaymentId;

    @Column(name = "mp_link_pago", length = 500)
    private String mpLinkPago;

    @Column(name = "mp_qr_base64", columnDefinition = "TEXT")
    private String mpQrBase64;

    @Column(name = "mp_qr_image_url", length = 500)
    private String mpQrImageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
