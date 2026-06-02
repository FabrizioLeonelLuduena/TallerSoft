package com.tallersoft.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.tallersoft.exception.EntityNotFoundException;
import com.tallersoft.model.OrdenRepuesto;
import com.tallersoft.model.OrdenTrabajo;
import com.tallersoft.repository.OrdenTrabajoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresupuestoPdfService {

    private final OrdenTrabajoRepository ordenTrabajoRepository;

    @Value("${taller.nombre}")
    private String tallerNombre;

    @Value("${taller.direccion}")
    private String tallerDireccion;

    @Value("${taller.telefono}")
    private String tallerTelefono;

    @Value("${taller.email}")
    private String tallerEmail;

    private static final DeviceRgb COLOR_ACCENT    = new DeviceRgb(0, 180, 160);
    private static final DeviceRgb COLOR_DARK      = new DeviceRgb(30, 30, 40);
    private static final DeviceRgb COLOR_GRAY      = new DeviceRgb(100, 110, 120);
    private static final DeviceRgb COLOR_LIGHT_BG  = new DeviceRgb(245, 247, 250);
    private static final DeviceRgb COLOR_BORDER    = new DeviceRgb(210, 215, 220);

    @Transactional(readOnly = true)
    public byte[] generarPresupuesto(Long ordenId) {
        OrdenTrabajo orden = ordenTrabajoRepository.findById(ordenId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada con id: " + ordenId));

        log.info("Generando PDF de presupuesto para orden {}", ordenId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        agregarEncabezado(document, ordenId);
        agregarSeccionCliente(document, orden);
        agregarSeccionEquipo(document, orden);
        agregarSeccionTrabajo(document, orden);
        agregarTablaRepuestos(document, orden);
        agregarTotal(document, orden);

        document.close();
        log.info("PDF generado para orden {}, tamaño: {} bytes", ordenId, baos.size());
        return baos.toByteArray();
    }

    // ── Encabezado del taller ────────────────────────────────────────────────

    private void agregarEncabezado(Document doc, Long ordenId) {
        Table header = new Table(new float[]{1, 1});
        header.setWidth(UnitValue.createPercentValue(100));
        header.setMarginBottom(20);

        // Columna izquierda — nombre y datos del taller
        Cell izq = new Cell().setBorder(Border.NO_BORDER);
        izq.add(new Paragraph(tallerNombre)
                .setFontColor(COLOR_ACCENT)
                .setFontSize(18)
                .setBold()
                .setMarginBottom(4));
        izq.add(new Paragraph(tallerDireccion)
                .setFontColor(COLOR_GRAY)
                .setFontSize(9)
                .setMarginBottom(2));
        izq.add(new Paragraph("Tel: " + tallerTelefono + "  |  " + tallerEmail)
                .setFontColor(COLOR_GRAY)
                .setFontSize(9));

        // Columna derecha — número de presupuesto y fecha
        Cell der = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        der.add(new Paragraph("PRESUPUESTO")
                .setFontColor(COLOR_GRAY)
                .setFontSize(10)
                .setMarginBottom(2));
        der.add(new Paragraph("#" + String.format("%04d", ordenId))
                .setFontColor(COLOR_DARK)
                .setFontSize(20)
                .setBold()
                .setMarginBottom(4));
        der.add(new Paragraph("Fecha: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setFontColor(COLOR_GRAY)
                .setFontSize(9));

        header.addCell(izq);
        header.addCell(der);
        doc.add(header);

        // Línea divisora
        doc.add(new Paragraph("")
                .setBorderBottom(new SolidBorder(COLOR_ACCENT, 2))
                .setMarginBottom(20));
    }

    // ── Sección Cliente ──────────────────────────────────────────────────────

    private void agregarSeccionCliente(Document doc, OrdenTrabajo orden) {
        doc.add(etiquetaSeccion("CLIENTE"));

        Table tabla = new Table(new float[]{1, 1});
        tabla.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(16);

        tabla.addCell(campoInfo("Nombre", orden.getCliente().getNombre()));
        tabla.addCell(campoInfo("Teléfono", orDash(orden.getCliente().getTelefono())));
        tabla.addCell(campoInfo("Email", orDash(orden.getCliente().getEmail())));
        tabla.addCell(campoInfo("Dirección", orDash(orden.getCliente().getDireccion())));

        doc.add(tabla);
    }

    // ── Sección Equipo ───────────────────────────────────────────────────────

    private void agregarSeccionEquipo(Document doc, OrdenTrabajo orden) {
        doc.add(etiquetaSeccion("EQUIPO"));

        Table tabla = new Table(new float[]{1, 1, 1});
        tabla.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(16);

        tabla.addCell(campoInfo("Tipo", orden.getEquipo().getTipo()));
        tabla.addCell(campoInfo("Marca", orDash(orden.getEquipo().getMarca())));
        tabla.addCell(campoInfo("Modelo", orDash(orden.getEquipo().getModelo())));
        tabla.addCell(campoInfo("N° de serie", orDash(orden.getEquipo().getNumeroSerie())));
        tabla.addCell(new Cell(1, 2).setBorder(Border.NO_BORDER));

        doc.add(tabla);
    }

    // ── Sección Descripción del trabajo ─────────────────────────────────────

    private void agregarSeccionTrabajo(Document doc, OrdenTrabajo orden) {
        doc.add(etiquetaSeccion("DESCRIPCIÓN DEL TRABAJO"));

        Table tabla = new Table(UnitValue.createPercentArray(new float[]{1}));
        tabla.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(16);

        tabla.addCell(campoInfoFullWidth("Falla reportada", orden.getFallaReportada()));
        tabla.addCell(campoInfoFullWidth("Diagnóstico",
                orden.getDiagnostico() != null ? orden.getDiagnostico() : "Pendiente de diagnóstico"));

        doc.add(tabla);
    }

    // ── Tabla de repuestos ───────────────────────────────────────────────────

    private void agregarTablaRepuestos(Document doc, OrdenTrabajo orden) {
        doc.add(etiquetaSeccion("REPUESTOS Y MANO DE OBRA"));

        Table tabla = new Table(new float[]{4, 1, 2, 2});
        tabla.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);

        // Cabecera
        String[] cabeceras = {"Ítem", "Cant.", "Precio unit.", "Subtotal"};
        for (String cab : cabeceras) {
            tabla.addHeaderCell(new Cell()
                    .setBackgroundColor(COLOR_DARK)
                    .setBorder(new SolidBorder(COLOR_BORDER, 0.5f))
                    .add(new Paragraph(cab)
                            .setFontColor(ColorConstants.WHITE)
                            .setFontSize(9)
                            .setBold())
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(6));
        }

        // Filas de repuestos
        if (orden.getRepuestos() != null && !orden.getRepuestos().isEmpty()) {
            boolean filaPar = false;
            for (OrdenRepuesto or : orden.getRepuestos()) {
                DeviceRgb rowBg = filaPar ? COLOR_LIGHT_BG : new DeviceRgb(255, 255, 255);
                BigDecimal subtotal = or.getPrecioUnit().multiply(new BigDecimal(or.getCantidad()));

                tabla.addCell(filaRepuesto(or.getRepuesto().getNombre(), rowBg, TextAlignment.LEFT));
                tabla.addCell(filaRepuesto(String.valueOf(or.getCantidad()), rowBg, TextAlignment.CENTER));
                tabla.addCell(filaRepuesto(formatoPrecio(or.getPrecioUnit()), rowBg, TextAlignment.RIGHT));
                tabla.addCell(filaRepuesto(formatoPrecio(subtotal), rowBg, TextAlignment.RIGHT));
                filaPar = !filaPar;
            }
        } else {
            tabla.addCell(new Cell(1, 4)
                    .add(new Paragraph("Sin repuestos registrados")
                            .setFontColor(COLOR_GRAY)
                            .setFontSize(9)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBorder(new SolidBorder(COLOR_BORDER, 0.5f))
                    .setPadding(10));
        }

        doc.add(tabla);
    }

    // ── Total ────────────────────────────────────────────────────────────────

    private void agregarTotal(Document doc, OrdenTrabajo orden) {
        Table totalTable = new Table(new float[]{3, 1});
        totalTable.setWidth(UnitValue.createPercentValue(100));

        totalTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER));

        totalTable.addCell(new Cell()
                .setBackgroundColor(COLOR_ACCENT)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("TOTAL: " + formatoPrecio(orden.getPresupuesto()))
                        .setFontColor(ColorConstants.WHITE)
                        .setFontSize(13)
                        .setBold()
                        .setTextAlignment(TextAlignment.RIGHT))
                .setPadding(10));

        doc.add(totalTable);

        doc.add(new Paragraph("\nEste presupuesto tiene validez de 30 días desde la fecha de emisión.")
                .setFontColor(COLOR_GRAY)
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Paragraph etiquetaSeccion(String titulo) {
        return new Paragraph(titulo)
                .setFontColor(COLOR_DARK)
                .setFontSize(10)
                .setBold()
                .setBackgroundColor(COLOR_LIGHT_BG)
                .setPaddingLeft(8)
                .setPaddingTop(4)
                .setPaddingBottom(4)
                .setBorderLeft(new SolidBorder(COLOR_ACCENT, 3))
                .setMarginBottom(8);
    }

    private Cell campoInfo(String label, String valor) {
        Cell cell = new Cell().setBorder(Border.NO_BORDER).setPaddingBottom(8);
        cell.add(new Paragraph(label)
                .setFontColor(COLOR_GRAY)
                .setFontSize(8)
                .setMarginBottom(2));
        cell.add(new Paragraph(valor)
                .setFontColor(COLOR_DARK)
                .setFontSize(9));
        return cell;
    }

    private Cell campoInfoFullWidth(String label, String valor) {
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_BORDER, 0.5f))
                .setPaddingBottom(8)
                .setPaddingTop(4);
        cell.add(new Paragraph(label)
                .setFontColor(COLOR_GRAY)
                .setFontSize(8)
                .setMarginBottom(2));
        cell.add(new Paragraph(valor)
                .setFontColor(COLOR_DARK)
                .setFontSize(9));
        return cell;
    }

    private Cell filaRepuesto(String texto, DeviceRgb bg, TextAlignment align) {
        return new Cell()
                .setBackgroundColor(bg)
                .setBorder(new SolidBorder(COLOR_BORDER, 0.5f))
                .add(new Paragraph(texto)
                        .setFontSize(9)
                        .setFontColor(COLOR_DARK)
                        .setTextAlignment(align))
                .setPadding(5);
    }

    private String formatoPrecio(BigDecimal monto) {
        if (monto == null) return "$0,00";
        return "$" + String.format("%,.2f", monto).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private String orDash(String value) {
        return (value != null && !value.isBlank()) ? value : "—";
    }
}
