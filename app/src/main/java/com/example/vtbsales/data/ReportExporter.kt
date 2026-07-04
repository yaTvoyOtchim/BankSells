package com.example.vtbsales.data

import com.example.vtbsales.model.EmployeeDetail
import com.example.vtbsales.model.OfficeSummary
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportExporter {
    fun employeeXlsx(detail: EmployeeDetail): ByteArray {
        val rows = buildList {
            add(listOf("Сотрудник", detail.user.name))
            add(listOf("Офис", detail.user.office))
            add(listOf("Клиентов сегодня", detail.dailyReport.clients.toString()))
            add(listOf("Продуктов сегодня", detail.dailyReport.products.toString()))
            add(listOf("Баллов сегодня", detail.dailyReport.points.toString()))
            add(emptyList())
            add(listOf("Клиент", "Продукт", "Формат", "Количество", "Баллы"))
            detail.clientCards.forEach { card ->
                val clientLabel = if (card.session.sequence == 1) {
                    card.session.phoneLast4
                } else {
                    "${card.session.phoneLast4} #${card.session.sequence}"
                }
                card.sales.forEach { sale ->
                    add(listOf(clientLabel, sale.productType.title, sale.format, sale.count.toString(), sale.points.toString()))
                }
            }
        }
        return xlsx("employee-report", rows)
    }

    fun employeePdf(detail: EmployeeDetail): ByteArray {
        val lines = buildList {
            add("VTB sales report")
            add("Employee: ${detail.user.name}")
            add("Office: ${detail.user.office}")
            add("Today clients: ${detail.dailyReport.clients}")
            add("Today products: ${detail.dailyReport.products}")
            add("Today points: ${detail.dailyReport.points}")
            add("Month points: ${detail.monthReport.points}")
        }
        return simplePdf(lines)
    }

    fun officeXlsx(rows: List<OfficeSummary>): ByteArray =
        xlsx(
            "office-report",
            buildList {
                add(listOf("Офис", "Сотрудники", "Баллы сегодня", "Баллы месяц", "Внимание"))
                rows.forEach {
                    add(listOf(it.office.title, it.employees.toString(), it.dailyPoints.toString(), it.monthPoints.toString(), it.attention.toString()))
                }
            }
        )

    fun officePdf(rows: List<OfficeSummary>): ByteArray =
        simplePdf(
            buildList {
                add("VTB office report")
                rows.forEach {
                    add("${it.office.title}: employees ${it.employees}, today ${it.dailyPoints}, month ${it.monthPoints}")
                }
            }
        )

    private fun xlsx(sheetName: String, rows: List<List<String>>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putText("[Content_Types].xml", contentTypesXml)
            zip.putText("_rels/.rels", rootRelsXml)
            zip.putText("xl/workbook.xml", workbookXml(sheetName))
            zip.putText("xl/_rels/workbook.xml.rels", workbookRelsXml)
            zip.putText("xl/worksheets/sheet1.xml", worksheetXml(rows))
        }
        return output.toByteArray()
    }

    private fun worksheetXml(rows: List<List<String>>): String {
        val body = rows.mapIndexed { rowIndex, row ->
            val cells = row.mapIndexed { cellIndex, value ->
                val ref = "${('A'.code + cellIndex).toChar()}${rowIndex + 1}"
                """<c r="$ref" t="inlineStr"><is><t>${value.escapeXml()}</t></is></c>"""
            }.joinToString("")
            """<row r="${rowIndex + 1}">$cells</row>"""
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$body</sheetData></worksheet>"""
    }

    private fun workbookXml(sheetName: String): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="${sheetName.escapeXml()}" sheetId="1" r:id="rId1"/></sheets></workbook>"""

    private fun simplePdf(lines: List<String>): ByteArray {
        val escapedLines = lines.joinToString("\\n") { it.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)") }
        val content = "BT /F1 14 Tf 50 780 Td ($escapedLines) Tj ET"
        val objects = listOf(
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n",
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n",
            "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n",
            "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n",
            "5 0 obj << /Length ${content.toByteArray().size} >> stream\n$content\nendstream endobj\n"
        )
        val out = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf(0)
        objects.forEach { obj ->
            offsets += out.toString().toByteArray(StandardCharsets.UTF_8).size
            out.append(obj)
        }
        val xrefStart = out.toString().toByteArray(StandardCharsets.UTF_8).size
        out.append("xref\n0 ${objects.size + 1}\n")
        out.append("0000000000 65535 f \n")
        offsets.drop(1).forEach { out.append("%010d 00000 n \n".format(it)) }
        out.append("trailer << /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xrefStart\n%%EOF")
        return out.toString().toByteArray(StandardCharsets.UTF_8)
    }

    private fun ZipOutputStream.putText(path: String, text: String) {
        putNextEntry(ZipEntry(path))
        write(text.toByteArray(StandardCharsets.UTF_8))
        closeEntry()
    }

    private fun String.escapeXml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>"""

    private val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>"""
}
