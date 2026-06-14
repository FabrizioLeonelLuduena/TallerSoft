package com.tallersoft.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
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
import java.io.IOException;
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

    // ── Paleta ───────────────────────────────────────────────────────────────
    private static final DeviceRgb COLOR_DARK        = new DeviceRgb(26,  43,  74);
    private static final DeviceRgb COLOR_ACCENT      = new DeviceRgb(37,  99,  235);
    private static final DeviceRgb COLOR_LIGHT_BG    = new DeviceRgb(239, 246, 255);
    private static final DeviceRgb COLOR_GRAY        = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb COLOR_LGRAY       = new DeviceRgb(226, 232, 240);
    private static final DeviceRgb COLOR_HEADER_SUB  = new DeviceRgb(147, 197, 253);
    private static final DeviceRgb COLOR_FOOTER_TXT  = new DeviceRgb(148, 163, 184);
    private static final DeviceRgb COLOR_NOTE_BG     = new DeviceRgb(254, 249, 195);
    private static final DeviceRgb COLOR_NOTE_BORDER = new DeviceRgb(253, 224,  71);
    private static final DeviceRgb COLOR_NOTE_TXT    = new DeviceRgb(120,  53,  15);
    private static final DeviceRgb COLOR_STRIPE      = new DeviceRgb(248, 250, 252);

    private static final float HEADER_HEIGHT = 148f;
    private static final float ACCENT_BAR    = 5f;
    private static final float FOOTER_HEIGHT = 40f;
    private static final float PAGE_MARGIN   = 40f;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Punto de entrada ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generarPresupuesto(Long ordenId) {
        OrdenTrabajo orden = ordenTrabajoRepository.findById(ordenId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada con id: " + ordenId));

        log.info("Generando PDF de presupuesto para orden {}", ordenId);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter   writer  = new PdfWriter(baos);
            PdfDocument pdf     = new PdfDocument(writer);
            Document    document = new Document(pdf, PageSize.A4);

            // El margen superior deja espacio al header dibujado con canvas
            document.setMargins(HEADER_HEIGHT + ACCENT_BAR + 12, PAGE_MARGIN, FOOTER_HEIGHT + 16, PAGE_MARGIN);

            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            // Contenido
            agregarClienteEquipo(document, orden, regular, bold);
            agregarDescripcionTrabajo(document, orden, regular, bold);
            agregarTablaRepuestos(document, orden, regular, bold);
            agregarTotal(document, orden, regular, bold);
            agregarNota(document, regular, bold);

            // Header y footer se dibujan sobre cada página con PdfCanvas
            int totalPages = pdf.getNumberOfPages();
            for (int i = 1; i <= totalPages; i++) {
                PdfPage page = pdf.getPage(i);
                dibujarHeader(page, pdf, ordenId, regular, bold);
                dibujarFooter(page, pdf, regular);
            }

            document.close();
            log.info("PDF generado para orden {}, tamaño: {} bytes", ordenId, baos.size());
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error al generar el PDF del presupuesto", e);
        }
    }

    // ── Sección 1 — Header (PdfCanvas) ──────────────────────────────────────

    private void dibujarHeader(PdfPage page, PdfDocument pdf, Long ordenId,
                                PdfFont regular, PdfFont bold) throws IOException {
        PageSize ps   = pdf.getDefaultPageSize();
        float    w    = ps.getWidth();
        float    h    = ps.getHeight();
        PdfCanvas canvas = new PdfCanvas(page);

        // Banda navy
        canvas.saveState()
              .setFillColor(COLOR_DARK)
              .rectangle(0, h - HEADER_HEIGHT, w, HEADER_HEIGHT)
              .fill();

        // Barra accent debajo de la banda
        canvas.setFillColor(COLOR_ACCENT)
              .rectangle(0, h - HEADER_HEIGHT - ACCENT_BAR, w, ACCENT_BAR)
              .fill()
              .restoreState();

        float leftX  = PAGE_MARGIN;

        // ── Línea divisoria horizontal de borde a borde (canvas directo) ──
        float yLinea = h - 78f;
        canvas.saveState()
              .setStrokeColor(COLOR_LGRAY)
              .setLineWidth(0.5f)
              .moveTo(PAGE_MARGIN, yLinea)
              .lineTo(w - PAGE_MARGIN, yLinea)
              .stroke()
              .restoreState();

        // ── Columna izquierda: nombre del taller y tagline ──
        try (Canvas cv = new Canvas(canvas, new Rectangle(leftX, h - HEADER_HEIGHT, w * 0.55f, HEADER_HEIGHT))) {
            cv.add(new Paragraph(tallerNombre)
                    .setFont(bold)
                    .setFontSize(22)
                    .setFontColor(ColorConstants.WHITE)
                    .setMarginTop(20)
                    .setMarginBottom(4));
            cv.add(new Paragraph("Servicio técnico especializado")
                    .setFont(regular)
                    .setFontSize(9)
                    .setFontColor(COLOR_HEADER_SUB)
                    .setMarginBottom(30));

            // "PRESUPUESTO #XXXX" en la misma línea
            Text labelPres = new Text("PRESUPUESTO  ")
                    .setFont(bold)
                    .setFontSize(18)
                    .setFontColor(ColorConstants.WHITE);
            Text numPres = new Text("#" + String.format("%04d", ordenId))
                    .setFont(bold)
                    .setFontSize(18)
                    .setFontColor(COLOR_ACCENT);
            cv.add(new Paragraph().add(labelPres).add(numPres).setMarginBottom(0));
        }

        // ── Columna derecha: datos de contacto + fecha ──
        float colW = w * 0.38f;
        float colX = w - PAGE_MARGIN - colW;
        try (Canvas cv = new Canvas(canvas, new Rectangle(colX, h - HEADER_HEIGHT, colW, HEADER_HEIGHT))) {
            cv.add(new Paragraph(tallerDireccion)
                    .setFont(regular)
                    .setFontSize(8)
                    .setFontColor(COLOR_LGRAY)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(20)
                    .setMarginBottom(2));
            cv.add(new Paragraph("Tel: " + tallerTelefono)
                    .setFont(regular)
                    .setFontSize(8)
                    .setFontColor(COLOR_LGRAY)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(2));
            cv.add(new Paragraph(tallerEmail)
                    .setFont(regular)
                    .setFontSize(8)
                    .setFontColor(COLOR_LGRAY)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(30));

            cv.add(new Paragraph("Fecha: " + LocalDate.now().format(DATE_FMT))
                    .setFont(regular)
                    .setFontSize(9)
                    .setFontColor(COLOR_GRAY)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginBottom(2));
            cv.add(new Paragraph("Validez: 30 días")
                    .setFont(regular)
                    .setFontSize(9)
                    .setFontColor(COLOR_GRAY)
                    .setTextAlignment(TextAlignment.RIGHT));
        }
    }

    // ── Sección 2 — Cliente y Equipo (dos columnas) ──────────────────────────

    private void agregarClienteEquipo(Document doc, OrdenTrabajo orden,
                                       PdfFont regular, PdfFont bold) {
        Table outer = new Table(new float[]{47, 6, 47});
        outer.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(16);

        outer.addCell(buildInfoBlock("CLIENTE",
                new String[]{"Nombre", "Teléfono", "Email", "Dirección"},
                new String[]{
                        d(orden.getCliente().getNombre()),
                        d(orden.getCliente().getTelefono()),
                        d(orden.getCliente().getEmail()),
                        d(orden.getCliente().getDireccion())
                }, regular, bold));

        outer.addCell(new Cell().setBorder(Border.NO_BORDER));

        outer.addCell(buildInfoBlock("EQUIPO",
                new String[]{"Tipo", "Marca", "Modelo", "N° de serie"},
                new String[]{
                        d(orden.getEquipo().getTipo()),
                        d(orden.getEquipo().getMarca()),
                        d(orden.getEquipo().getModelo()),
                        d(orden.getEquipo().getNumeroSerie())
                }, regular, bold));

        doc.add(outer);
    }

    private Cell buildInfoBlock(String titulo, String[] labels, String[] valores,
                                 PdfFont regular, PdfFont bold) {
        Cell wrapper = new Cell().setBorder(Border.NO_BORDER).setPaddingRight(4);

        // Título de sección
        wrapper.add(new Paragraph(titulo)
                .setFont(bold)
                .setFontSize(7.5f)
                .setFontColor(COLOR_ACCENT)
                .setBorderBottom(new SolidBorder(COLOR_ACCENT, 1f))
                .setPaddingBottom(4)
                .setMarginBottom(8));

        // Grilla 2 columnas: label | valor
        Table grid = new Table(new float[]{1, 1});
        grid.setWidth(UnitValue.createPercentValue(100));

        for (int i = 0; i < labels.length; i++) {
            grid.addCell(labelCell(labels[i], regular));
            grid.addCell(valorCell(valores[i], bold));
        }

        wrapper.add(grid);
        return wrapper;
    }

    // ── Sección 3 — Descripción del trabajo ─────────────────────────────────

    private void agregarDescripcionTrabajo(Document doc, OrdenTrabajo orden,
                                            PdfFont regular, PdfFont bold) {
        doc.add(tituloSeccion("DESCRIPCIÓN DEL TRABAJO", bold));

        String falla = (orden.getFallaReportada() != null && !orden.getFallaReportada().isBlank())
                ? orden.getFallaReportada() : "Pendiente";
        String diag  = (orden.getDiagnostico() != null && !orden.getDiagnostico().isBlank())
                ? orden.getDiagnostico() : "Pendiente de diagnóstico";

        Table tabla = new Table(new float[]{47, 6, 47});
        tabla.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(16);

        tabla.addCell(cajaTrabajo("Falla reportada", falla, regular, bold));
        tabla.addCell(new Cell().setBorder(Border.NO_BORDER));
        tabla.addCell(cajaTrabajo("Diagnóstico", diag, regular, bold));

        doc.add(tabla);
    }

    private Cell cajaTrabajo(String label, String valor, PdfFont regular, PdfFont bold) {
        Cell cell = new Cell()
                .setBackgroundColor(COLOR_LIGHT_BG)
                .setBorder(new SolidBorder(COLOR_LGRAY, 0.5f))
                .setPadding(10);
        cell.add(new Paragraph(label)
                .setFont(bold)
                .setFontSize(7.5f)
                .setFontColor(COLOR_GRAY)
                .setMarginBottom(4));
        cell.add(new Paragraph(valor)
                .setFont(regular)
                .setFontSize(9)
                .setFontColor(ColorConstants.BLACK));
        return cell;
    }

    // ── Sección 4 — Tabla de repuestos ───────────────────────────────────────

    private void agregarTablaRepuestos(Document doc, OrdenTrabajo orden,
                                        PdfFont regular, PdfFont bold) {
        doc.add(tituloSeccion("REPUESTOS Y MANO DE OBRA", bold));

        Table tabla = new Table(UnitValue.createPercentArray(new float[]{45, 12, 20, 23}));
        tabla.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);

        // Encabezado
        String[] cabeceras = {"Ítem / Descripción", "Cant.", "Precio unit.", "Subtotal"};
        TextAlignment[] aligns = {TextAlignment.LEFT, TextAlignment.CENTER,
                                   TextAlignment.RIGHT, TextAlignment.RIGHT};
        for (int i = 0; i < cabeceras.length; i++) {
            tabla.addHeaderCell(new Cell()
                    .setBackgroundColor(COLOR_DARK)
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph(cabeceras[i])
                            .setFont(bold)
                            .setFontSize(8)
                            .setFontColor(ColorConstants.WHITE)
                            .setTextAlignment(aligns[i]))
                    .setPaddingTop(7)
                    .setPaddingBottom(7)
                    .setPaddingLeft(i == 0 ? 8 : 4)
                    .setPaddingRight(i == 3 ? 8 : 4));
        }

        // Filas
        if (orden.getRepuestos() != null && !orden.getRepuestos().isEmpty()) {
            boolean par = false;
            for (OrdenRepuesto or : orden.getRepuestos()) {
                DeviceRgb bg = par ? COLOR_STRIPE : new DeviceRgb(255, 255, 255);
                BigDecimal subtotal = or.getPrecioUnit().multiply(new BigDecimal(or.getCantidad()));

                tabla.addCell(filaCell(or.getRepuesto().getNombre(), bg, TextAlignment.LEFT,  true,  regular));
                tabla.addCell(filaCell(String.valueOf(or.getCantidad()), bg, TextAlignment.CENTER, false, regular));
                tabla.addCell(filaCell(fmt(or.getPrecioUnit()), bg, TextAlignment.RIGHT, false, regular));
                tabla.addCell(filaCell(fmt(subtotal), bg, TextAlignment.RIGHT, true,  regular));
                par = !par;
            }
        } else {
            tabla.addCell(new Cell(1, 4)
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(COLOR_LGRAY, 0.3f))
                    .add(new Paragraph("Sin repuestos registrados")
                            .setFont(regular)
                            .setFontSize(9)
                            .setFontColor(COLOR_GRAY)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setPadding(12));
        }

        doc.add(tabla);
    }

    private Cell filaCell(String texto, DeviceRgb bg, TextAlignment align,
                           boolean outerPad, PdfFont regular) {
        return new Cell()
                .setBackgroundColor(bg)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_LGRAY, 0.3f))
                .add(new Paragraph(texto)
                        .setFont(regular)
                        .setFontSize(8.5f)
                        .setFontColor(ColorConstants.BLACK)
                        .setTextAlignment(align))
                .setPaddingTop(6)
                .setPaddingBottom(6)
                .setPaddingLeft(outerPad ? 8 : 4)
                .setPaddingRight(outerPad ? 8 : 4);
    }

    // ── Sección 5 — Total ────────────────────────────────────────────────────

    private void agregarTotal(Document doc, OrdenTrabajo orden,
                               PdfFont regular, PdfFont bold) {
        Table tabla = new Table(UnitValue.createPercentArray(new float[]{60, 40}));
        tabla.setWidth(UnitValue.createPercentValue(100)).setMarginLeft(0).setMarginRight(0).setMarginBottom(12);

        tabla.addCell(new Cell().setBorder(Border.NO_BORDER));

        Cell totalCell = new Cell()
                .setBackgroundColor(COLOR_DARK)
                .setBorder(new SolidBorder(COLOR_ACCENT, 1.5f))
                .setPadding(12);

        totalCell.add(new Paragraph("TOTAL A PAGAR")
                .setFont(regular)
                .setFontSize(9)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(2));
        totalCell.add(new Paragraph(fmt(orden.getPresupuesto()))
                .setFont(bold)
                .setFontSize(15)
                .setFontColor(COLOR_HEADER_SUB)
                .setTextAlignment(TextAlignment.RIGHT));

        tabla.addCell(totalCell);
        doc.add(tabla);
    }

    // ── Sección 6 — Nota de validez ─────────────────────────────────────────

    private void agregarNota(Document doc, PdfFont regular, PdfFont bold) {
        Text icono = new Text("⚠  Nota: ")
                .setFont(bold)
                .setFontSize(8)
                .setFontColor(COLOR_NOTE_TXT);
        Text cuerpo = new Text("Este presupuesto tiene una validez de 30 días desde la fecha de emisión.")
                .setFont(regular)
                .setFontSize(8)
                .setFontColor(COLOR_NOTE_TXT);

        doc.add(new Paragraph().add(icono).add(cuerpo)
                .setBackgroundColor(COLOR_NOTE_BG)
                .setBorder(new SolidBorder(COLOR_NOTE_BORDER, 0.5f))
                .setPadding(10)
                .setMarginTop(4));
    }

    // ── Sección 7 — Footer (PdfCanvas) ───────────────────────────────────────

    private void dibujarFooter(PdfPage page, PdfDocument pdf, PdfFont regular) throws IOException {
        PageSize ps = pdf.getDefaultPageSize();
        float    w  = ps.getWidth();
        PdfCanvas canvas = new PdfCanvas(page);

        canvas.saveState()
              .setFillColor(COLOR_DARK)
              .rectangle(0, 0, w, FOOTER_HEIGHT)
              .fill()
              .restoreState();

        try (Canvas cv = new Canvas(canvas, new Rectangle(0, 0, w, FOOTER_HEIGHT))) {
            cv.add(new Paragraph(tallerNombre + "  ·  " + tallerDireccion
                    + "  ·  " + tallerTelefono + "  ·  " + tallerEmail)
                    .setFont(regular)
                    .setFontSize(7.5f)
                    .setFontColor(COLOR_FOOTER_TXT)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8)
                    .setMarginBottom(2));
            cv.add(new Paragraph("Gracias por confiar en nosotros")
                    .setFont(regular)
                    .setFontSize(7.5f)
                    .setFontColor(COLOR_FOOTER_TXT)
                    .setTextAlignment(TextAlignment.CENTER));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Paragraph tituloSeccion(String texto, PdfFont bold) {
        return new Paragraph(texto)
                .setFont(bold)
                .setFontSize(7.5f)
                .setFontColor(COLOR_ACCENT)
                .setBorderBottom(new SolidBorder(COLOR_ACCENT, 1f))
                .setPaddingBottom(4)
                .setMarginBottom(10)
                .setMarginTop(4);
    }

    private Cell labelCell(String texto, PdfFont regular) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(6)
                .add(new Paragraph(texto)
                        .setFont(regular)
                        .setFontSize(8)
                        .setFontColor(COLOR_GRAY));
    }

    private Cell valorCell(String texto, PdfFont bold) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(6)
                .add(new Paragraph(texto)
                        .setFont(bold)
                        .setFontSize(8.5f)
                        .setFontColor(ColorConstants.BLACK));
    }

    private String fmt(BigDecimal monto) {
        if (monto == null) return "$0,00";
        return "$" + String.format("%,.2f", monto)
                .replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private String d(String value) {
        return (value != null && !value.isBlank()) ? value : "—";
    }
}
