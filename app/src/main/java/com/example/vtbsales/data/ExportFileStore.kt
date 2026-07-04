package com.example.vtbsales.data

import android.content.Context
import android.os.Environment
import com.example.vtbsales.model.EmployeeDetail
import com.example.vtbsales.model.OfficeSummary
import java.io.File
import java.time.LocalDate

object ExportFileStore {
    fun saveEmployeeXlsx(context: Context, detail: EmployeeDetail): File =
        save(context, employeeFileName(detail, "xlsx"), ReportExporter.employeeXlsx(detail))

    fun saveEmployeePdf(context: Context, detail: EmployeeDetail): File =
        save(context, employeeFileName(detail, "pdf"), ReportExporter.employeePdf(detail))

    fun saveOfficeXlsx(context: Context, rows: List<OfficeSummary>): File =
        save(context, "vtb_offices_${LocalDate.now()}.xlsx", ReportExporter.officeXlsx(rows))

    fun saveOfficePdf(context: Context, rows: List<OfficeSummary>): File =
        save(context, "vtb_offices_${LocalDate.now()}.pdf", ReportExporter.officePdf(rows))

    private fun save(context: Context, fileName: String, bytes: ByteArray): File {
        val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, fileName)
        file.writeBytes(bytes)
        return file
    }

    private fun employeeFileName(detail: EmployeeDetail, extension: String): String =
        "vtb_employee_${detail.user.uid}_${LocalDate.now()}.$extension"
}
