package com.example.ui.reports

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.content.contentValuesOf
import com.example.data.model.Attendance
import com.example.data.model.Group
import com.example.data.model.Payment
import com.example.data.model.Profile
import com.example.data.model.Student
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PdfGeneratorService {

    private const val PAGE_WIDTH = 595 // A4 Width in PostScript points
    private const val PAGE_HEIGHT = 842 // A4 Height in PostScript points
    private const val MARGIN = 40f

    // Standard Arabic month formatter
    private val sdfDate = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
    private val sdfTime = SimpleDateFormat("hh:mm a", Locale("ar"))

    /**
     * Helper to draw RTL Arabic text with right alignment
     */
    private fun drawRtlText(canvas: Canvas, text: String, xRight: Float, y: Float, paint: Paint) {
        val width = paint.measureText(text)
        canvas.drawText(text, xRight - width, y, paint)
    }

    /**
     * Helper to draw a clean horizontal line
     */
    private fun drawHorizontalLine(canvas: Canvas, y: Float, color: Int = 0xFFE0E0E0.toInt(), strokeWidth: Float = 1f) {
        val paint = Paint().apply {
            this.color = color
            this.strokeWidth = strokeWidth
            style = Paint.Style.STROKE
        }
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)
    }

    /**
     * Draw professional report header banner
     */
    private fun drawHeaderBanner(canvas: Canvas, reportTitle: String, centerName: String, phone: String, currency: String): Float {
        // Draw colored header bar (Midnight/Navy Blue background)
        val rectPaint = Paint().apply {
            color = 0xFF1E293B.toInt() // Slate Dark
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(MARGIN, 25f, PAGE_WIDTH - MARGIN, 85f), 12f, 12f, rectPaint)

        // Title text (Centered)
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleWidth = titlePaint.measureText(reportTitle)
        canvas.drawText(reportTitle, (PAGE_WIDTH - titleWidth) / 2f, 58f, titlePaint)

        // Subtitle / Info block below banner
        val infoPaint = Paint().apply {
            color = 0xFF64748B.toInt() // Slate Light
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val rightAlign = PAGE_WIDTH - MARGIN
        val leftAlign = MARGIN

        // Draw Center Name & Phone
        val brandName = if (centerName.isNotEmpty()) centerName else "Nexora التعليمي"
        drawRtlText(canvas, brandName, rightAlign, 110f, Paint().apply {
            color = 0xFF1E293B.toInt()
            textSize = 14f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        })

        if (phone.isNotEmpty()) {
            drawRtlText(canvas, "هاتف: $phone", rightAlign, 126f, infoPaint)
        }

        // Left alignment for date/time
        val dateStr = "تاريخ التصدير: ${sdfDate.format(Date())}"
        canvas.drawText(dateStr, leftAlign, 110f, infoPaint)
        canvas.drawText("العملة المعتمدة: $currency", leftAlign, 126f, infoPaint)

        drawHorizontalLine(canvas, 138f, color = 0xFFCBD5E1.toInt(), strokeWidth = 1.5f)

        return 160f // Current Y cursor position
    }

    /**
     * Draw professional footer
     */
    private fun drawFooter(canvas: Canvas, pageNum: Int) {
        drawHorizontalLine(canvas, PAGE_HEIGHT - 45f, color = 0xFFE2E8F0.toInt())

        val footerPaint = Paint().apply {
            color = 0xFF94A3B8.toInt()
            textSize = 9f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        canvas.drawText("نظام إدارة السناتر التعليمية - Nexora", MARGIN, PAGE_HEIGHT - 30f, footerPaint)
        
        val pageStr = "صفحة $pageNum"
        val pageStrWidth = footerPaint.measureText(pageStr)
        canvas.drawText(pageStr, PAGE_WIDTH - MARGIN - pageStrWidth, PAGE_HEIGHT - 30f, footerPaint)
    }

    /**
     * Draw a clean info key-value card
     */
    private fun drawInfoCard(canvas: Canvas, startY: Float, title: String, items: List<Pair<String, String>>): Float {
        var currentY = startY

        // Card frame
        val cardHeight = 25f + (items.size * 22f)
        val cardPaint = Paint().apply {
            color = 0xFFF8FAFC.toInt() // Very light grey/slate background
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = 0xFFE2E8F0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val cardRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + cardHeight)
        canvas.drawRoundRect(cardRect, 10f, 10f, cardPaint)
        canvas.drawRoundRect(cardRect, 10f, 10f, borderPaint)

        // Card header
        val headerPaint = Paint().apply {
            color = 0xFF475569.toInt()
            textSize = 12f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        drawRtlText(canvas, title, PAGE_WIDTH - MARGIN - 15f, currentY + 22f, headerPaint)
        drawHorizontalLine(canvas, currentY + 30f, color = 0xFFE2E8F0.toInt())

        currentY += 45f

        // Card items
        val labelPaint = Paint().apply {
            color = 0xFF64748B.toInt()
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val valuePaint = Paint().apply {
            color = 0xFF1E293B.toInt()
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Draw in two columns if multiple items, or simple vertical table
        items.forEach { (label, value) ->
            // Label RTL
            drawRtlText(canvas, "$label:", PAGE_WIDTH - MARGIN - 20f, currentY, labelPaint)
            // Value LTR or RTL on the left side of the card
            canvas.drawText(value, MARGIN + 20f, currentY, valuePaint)
            currentY += 22f
        }

        return currentY + 10f
    }

    /**
     * Draw a structured table
     */
    private fun drawTable(
        canvas: Canvas,
        startY: Float,
        headers: List<String>,
        columnWidths: List<Float>, // Percentage widths, summing to 1.0
        rows: List<List<String>>
    ): Float {
        var currentY = startY

        // Table Header
        val headerBgPaint = Paint().apply {
            color = 0xFFF1F5F9.toInt()
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = 0xFFCBD5E1.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val tableWidth = PAGE_WIDTH - (2 * MARGIN)
        val headerHeight = 26f
        canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + headerHeight, headerBgPaint)
        canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + headerHeight, borderPaint)

        val textPaint = Paint().apply {
            color = 0xFF1E293B.toInt()
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Draw Header text
        var xOffset = PAGE_WIDTH - MARGIN
        headers.forEachIndexed { index, header ->
            val colWidth = columnWidths[index] * tableWidth
            val textWidth = textPaint.measureText(header)
            // Center in column
            canvas.drawText(header, xOffset - (colWidth / 2f) - (textWidth / 2f), currentY + 17f, textPaint)
            xOffset -= colWidth
        }

        currentY += headerHeight

        // Table Rows
        val rowPaint = Paint().apply {
            color = 0xFF1E293B.toInt()
            textSize = 9f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        rows.forEachIndexed { rowIndex, row ->
            val isEven = rowIndex % 2 == 0
            val rowBgPaint = Paint().apply {
                color = if (isEven) Color.WHITE else 0xFFF8FAFC.toInt()
                style = Paint.Style.FILL
            }

            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 24f, rowBgPaint)
            canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 24f, borderPaint)

            xOffset = PAGE_WIDTH - MARGIN
            row.forEachIndexed { colIndex, cellValue ->
                val colWidth = columnWidths[colIndex] * tableWidth
                val textWidth = rowPaint.measureText(cellValue)
                canvas.drawText(cellValue, xOffset - (colWidth / 2f) - (textWidth / 2f), currentY + 15f, rowPaint)
                xOffset -= colWidth
            }
            currentY += 24f
        }

        return currentY + 15f
    }

    /**
     * 1. Generate Individual Student Report
     */
    fun generateStudentReport(
        context: Context,
        profile: Profile?,
        student: Student,
        group: Group?,
        attendanceList: List<Attendance>,
        payments: List<Payment>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val sysProfile = profile ?: Profile()
        var currentY = drawHeaderBanner(
            canvas = canvas,
            reportTitle = "تقرير الطالب الفردي الشامل",
            centerName = sysProfile.centerName,
            phone = sysProfile.phone,
            currency = sysProfile.currency
        )

        // Draw Basic Info Card
        val basicInfo = listOf(
            Pair("اسم الطالب", student.name),
            Pair("المجموعة الدراسية", group?.name ?: "غير محدد"),
            Pair("السنة الدراسية", student.grade.ifEmpty { "غير محدد" }),
            Pair("المسار / الكورس الدراسي", student.customCourse.ifEmpty { "غير محدد" }),
            Pair("رقم هاتف الطالب", student.studentPhone.ifEmpty { "غير مسجل" }),
            Pair("اسم ولي الأمر ورقم الهاتف", "${student.parentName} (${student.parentPhone.ifEmpty { "بدون هاتف" }})"),
            Pair("تاريخ التسجيل بالسنتر", sdfDate.format(Date(student.registrationDate)))
        )
        currentY = drawInfoCard(canvas, currentY, "البيانات الأساسية للطالب", basicInfo)

        // Calculate stats
        val presentCount = attendanceList.count { it.status == "present" }
        val absentCount = attendanceList.count { it.status == "absent" }
        val lateCount = attendanceList.count { it.status == "late" }
        val excusedCount = attendanceList.count { it.status == "excused" }
        val totalAttendance = attendanceList.size
        val commitmentRate = if (totalAttendance > 0) {
            ((presentCount + lateCount).toFloat() / totalAttendance * 100).toInt()
        } else {
            100
        }

        val totalPaid = payments.sumOf { it.amount }
        // Simple billing model: monthlyFee since join date
        val monthsJoined = maxOf(1, getMonthsDifference(student.registrationDate, System.currentTimeMillis()))
        val expectedFee = student.netFee * monthsJoined
        val arrears = maxOf(0.0, expectedFee - totalPaid)

        // Draw Academic & Financial Stats Card
        val statsInfo = listOf(
            Pair("مرات الحضور", "$presentCount مرات"),
            Pair("مرات الغياب", "$absentCount مرات"),
            Pair("مرات التأخير", "$lateCount مرات"),
            Pair("نسبة الالتزام والانتظام", "$commitmentRate%"),
            Pair("رسوم الشهر المقررة", "${student.monthlyFee} ${sysProfile.currency}"),
            Pair("الخصم المطبق", "${student.discount}%"),
            Pair("صافي الرسوم المطلوب شهرياً", "${student.netFee} ${sysProfile.currency}"),
            Pair("إجمالي المبالغ المدفوعة", "$totalPaid ${sysProfile.currency}"),
            Pair("إجمالي المتأخرات المتبقية", "$arrears ${sysProfile.currency}")
        )
        currentY = drawInfoCard(canvas, currentY, "إحصائيات الحضور والمعاملات المالية", statsInfo)

        drawFooter(canvas, 1)
        document.finishPage(page)

        // Write to cache directory to prepare for sharing/printing
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()
        
        val pdfFile = File(reportsDir, "student_${student.name.replace(" ", "_")}_report.pdf")
        document.writeTo(FileOutputStream(pdfFile))
        document.close()

        return pdfFile
    }

    /**
     * 2. Generate Group Report
     */
    fun generateGroupReport(
        context: Context,
        profile: Profile?,
        group: Group,
        studentsInGroup: List<Student>,
        allAttendance: List<Attendance>,
        allPayments: List<Payment>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val sysProfile = profile ?: Profile()
        var currentY = drawHeaderBanner(
            canvas = canvas,
            reportTitle = "تقرير المجموعة الأكاديمي والمالي",
            centerName = sysProfile.centerName,
            phone = sysProfile.phone,
            currency = sysProfile.currency
        )

        // Calculate Group metrics
        val studentCount = studentsInGroup.size
        
        // Attendance percent in this group
        val studentIds = studentsInGroup.map { it.id }.toSet()
        val groupAttendance = allAttendance.filter { it.studentId in studentIds }
        val totalAtt = groupAttendance.size
        val presentAtt = groupAttendance.count { it.status == "present" || it.status == "late" }
        val attendancePercent = if (totalAtt > 0) (presentAtt.toFloat() / totalAtt * 100).toInt() else 100

        // Financials in this group
        val groupPayments = allPayments.filter { it.studentId in studentIds }
        val groupRevenues = groupPayments.sumOf { it.amount }

        var totalGroupArrears = 0.0
        studentsInGroup.forEach { student ->
            val paymentsForStudent = groupPayments.filter { it.studentId == student.id }
            val paid = paymentsForStudent.sumOf { it.amount }
            val months = maxOf(1, getMonthsDifference(student.registrationDate, System.currentTimeMillis()))
            val expected = student.netFee * months
            totalGroupArrears += maxOf(0.0, expected - paid)
        }

        // Draw Group Details Card
        val groupInfo = listOf(
            Pair("اسم المجموعة", group.name),
            Pair("المعلم المسؤول", group.teacherName.ifEmpty { "غير محدد" }),
            Pair("موقع القاعة", group.classroom.ifEmpty { "غير محدد" }),
            Pair("أوقات المحاضرات / المواعيد", group.schedule.ifEmpty { "غير محدد" }),
            Pair("عدد الطلاب المقيدين", "$studentCount طلاب نشطين"),
            Pair("نسبة حضور المجموعة الشاملة", "$attendancePercent%"),
            Pair("إجمالي الإيرادات المحصلة", "$groupRevenues ${sysProfile.currency}"),
            Pair("إجمالي المتأخرات المتبقية", "$totalGroupArrears ${sysProfile.currency}")
        )
        currentY = drawInfoCard(canvas, currentY, "تفاصيل وإحصائيات المجموعة", groupInfo)

        // Add a beautiful table of students
        if (studentsInGroup.isNotEmpty()) {
            val tableHeaders = listOf("اسم الطالب", "نسبة الحضور", "المدفوع", "المتأخرات")
            val colWidths = listOf(0.4f, 0.2f, 0.2f, 0.2f)
            
            val rows = studentsInGroup.take(15).map { student ->
                val atts = groupAttendance.filter { it.studentId == student.id }
                val totalS = atts.size
                val presS = atts.count { it.status == "present" || it.status == "late" }
                val sPercent = if (totalS > 0) "${(presS.toFloat() / totalS * 100).toInt()}%" else "100%"

                val sPayments = groupPayments.filter { it.studentId == student.id }
                val paid = sPayments.sumOf { it.amount }
                val months = maxOf(1, getMonthsDifference(student.registrationDate, System.currentTimeMillis()))
                val expected = student.netFee * months
                val due = maxOf(0.0, expected - paid)

                listOf(
                    student.name,
                    sPercent,
                    "$paid ${sysProfile.currency}",
                    "$due ${sysProfile.currency}"
                )
            }
            
            drawRtlText(canvas, "جدول تفصيلي للطلاب (أول 15 طالب):", PAGE_WIDTH - MARGIN, currentY, Paint().apply {
                color = 0xFF1E293B.toInt()
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            })
            currentY += 18f
            currentY = drawTable(canvas, currentY, tableHeaders, colWidths, rows)
        }

        drawFooter(canvas, 1)
        document.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val pdfFile = File(reportsDir, "group_${group.name.replace(" ", "_")}_report.pdf")
        document.writeTo(FileOutputStream(pdfFile))
        document.close()

        return pdfFile
    }

    /**
     * 3. Generate Central/Teacher Financial Report
     */
    fun generateFinancialReport(
        context: Context,
        profile: Profile?,
        payments: List<Payment>,
        allStudents: List<Student>,
        allGroups: List<Group>,
        expenses: List<com.example.data.model.Expense>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val sysProfile = profile ?: Profile()
        var currentY = drawHeaderBanner(
            canvas = canvas,
            reportTitle = "التقرير المالي العام والحسابات الختامية",
            centerName = sysProfile.centerName,
            phone = sysProfile.phone,
            currency = sysProfile.currency
        )

        // Monthly / Annual boundaries
        val calendar = Calendar.getInstance()
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)
        val currentYearStr = SimpleDateFormat("yyyy", Locale.US).format(calendar.time)

        // Revenues
        val monthlyRevenues = payments.filter { it.month == currentMonthStr }.sumOf { it.amount }
        val yearlyRevenues = payments.filter { it.month.startsWith(currentYearStr) }.sumOf { it.amount }

        // Expenses
        val monthlyExpenses = expenses.filter { it.month == currentMonthStr }.sumOf { it.amount }
        val yearlyExpenses = expenses.filter { it.month.startsWith(currentYearStr) }.sumOf { it.amount }

        // Net Profits
        val monthlyNetProfit = monthlyRevenues - monthlyExpenses
        val yearlyNetProfit = yearlyRevenues - yearlyExpenses

        // Lists
        val exemptStudents = allStudents.filter { it.isExempt }
        
        // Late students (arrears > 0)
        val lateStudentsWithArrears = allStudents.map { student ->
            val sPayments = payments.filter { it.studentId == student.id }
            val paid = sPayments.sumOf { it.amount }
            val months = maxOf(1, getMonthsDifference(student.registrationDate, System.currentTimeMillis()))
            val expected = student.netFee * months
            val due = maxOf(0.0, expected - paid)
            Pair(student, due)
        }.filter { it.second > 0 }

        val totalUnpaidArrears = lateStudentsWithArrears.sumOf { it.second }

        // General Financial Overview Card
        val financialsInfo = listOf(
            Pair("إيرادات الشهر الحالي ($currentMonthStr)", "$monthlyRevenues ${sysProfile.currency}"),
            Pair("مصروفات الشهر الحالي ($currentMonthStr)", "$monthlyExpenses ${sysProfile.currency}"),
            Pair("صافي أرباح الشهر الحالي", "$monthlyNetProfit ${sysProfile.currency}"),
            Pair("إيرادات العام الحالي ($currentYearStr)", "$yearlyRevenues ${sysProfile.currency}"),
            Pair("مصروفات العام الحالي ($currentYearStr)", "$yearlyExpenses ${sysProfile.currency}"),
            Pair("صافي أرباح العام الحالي", "$yearlyNetProfit ${sysProfile.currency}"),
            Pair("إجمالي المتأخرات المستحقة للسنتر", "$totalUnpaidArrears ${sysProfile.currency}"),
            Pair("عدد الطلاب المتأخرين في السداد", "${lateStudentsWithArrears.size} طلاب"),
            Pair("عدد الطلاب المعفيين تماماً (المنح)", "${exemptStudents.size} طلاب")
        )
        currentY = drawInfoCard(canvas, currentY, "الخلاصة المالية العامة", financialsInfo)

        // Draw Table for Late Students (Arrears)
        if (lateStudentsWithArrears.isNotEmpty()) {
            val tableHeaders = listOf("اسم الطالب", "المجموعة", "رقم الهاتف", "المتأخرات")
            val colWidths = listOf(0.4f, 0.2f, 0.2f, 0.2f)

            val rows = lateStudentsWithArrears.take(8).map { (student, arrears) ->
                val groupName = allGroups.find { it.id == student.groupId }?.name ?: "بدون مجموعة"
                listOf(
                    student.name,
                    groupName,
                    student.studentPhone.ifEmpty { student.parentPhone.ifEmpty { "بدون هاتف" } },
                    "$arrears ${sysProfile.currency}"
                )
            }

            drawRtlText(canvas, "قائمة عينة من الطلاب المتأخرين في الدفع (أعلى 8):", PAGE_WIDTH - MARGIN, currentY, Paint().apply {
                color = 0xFF1E293B.toInt()
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            })
            currentY += 18f
            currentY = drawTable(canvas, currentY, tableHeaders, colWidths, rows)
        }

        drawFooter(canvas, 1)
        document.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val pdfFile = File(reportsDir, "financial_report_${currentMonthStr}.pdf")
        document.writeTo(FileOutputStream(pdfFile))
        document.close()

        return pdfFile
    }

    /**
     * 4. Generate Student Financial Statement Report
     */
    fun generateStudentFinancialStatement(
        context: Context,
        profile: Profile?,
        student: Student,
        group: Group?,
        payments: List<Payment>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val sysProfile = profile ?: Profile()
        var currentY = drawHeaderBanner(
            canvas = canvas,
            reportTitle = "كشف الحساب المالي للطالب",
            centerName = sysProfile.centerName,
            phone = sysProfile.phone,
            currency = sysProfile.currency
        )

        // Draw Student Info Card
        val basicInfo = listOf(
            Pair("اسم الطالب", student.name),
            Pair("المجموعة الدراسية", group?.name ?: "غير محدد"),
            Pair("السنة الدراسية", student.grade.ifEmpty { "غير محدد" }),
            Pair("رقم هاتف الطالب / ولي الأمر", "${student.studentPhone.ifEmpty { "غير مسجل" }} / ${student.parentPhone.ifEmpty { "بدون هاتف" }}")
        )
        currentY = drawInfoCard(canvas, currentY, "بيانات الطالب الأساسية", basicInfo)

        // Financial Math
        val monthsJoined = maxOf(1, getMonthsDifference(student.registrationDate, System.currentTimeMillis()))
        val expectedFee = if (student.isExempt) 0.0 else student.netFee * monthsJoined
        val totalPaid = payments.sumOf { it.amount }
        val arrears = maxOf(0.0, expectedFee - totalPaid)

        // Draw Summary Card
        val financialSummary = listOf(
            Pair("الرسوم الشهرية المقررة", "${student.netFee} ${sysProfile.currency}"),
            Pair("عدد الأشهر المشترك بها", "$monthsJoined شهور"),
            Pair("إجمالي الرسوم المطلوبة", "$expectedFee ${sysProfile.currency}"),
            Pair("إجمالي المبالغ المدفوعة", "$totalPaid ${sysProfile.currency}"),
            Pair("المبالغ المتبقية للتحصيل", "$arrears ${sysProfile.currency}")
        )
        currentY = drawInfoCard(canvas, currentY, "ملخص الحالة المالية للاشتراكات", financialSummary)

        // Draw Payments Table
        if (payments.isNotEmpty()) {
            val tableHeaders = listOf("تاريخ الدفع", "اشتراك شهر", "المبلغ المدفوع", "طريقة الدفع")
            val colWidths = listOf(0.3f, 0.25f, 0.25f, 0.2f)

            val rows = payments.map { pay ->
                val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(pay.date))
                val methodStr = if (pay.method == "cash") "كاش" else "تحويل"
                listOf(
                    dateStr,
                    pay.month,
                    "${pay.amount} ${sysProfile.currency}",
                    methodStr
                )
            }

            drawRtlText(canvas, "سجل الدفعات والمعاملات المالية بالتفصيل:", PAGE_WIDTH - MARGIN, currentY, Paint().apply {
                color = 0xFF1E293B.toInt()
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            })
            currentY += 18f
            currentY = drawTable(canvas, currentY, tableHeaders, colWidths, rows)
        } else {
            drawRtlText(canvas, "لا توجد أي سندات دفع أو معاملات مسجلة للطالب.", PAGE_WIDTH - MARGIN, currentY, Paint().apply {
                color = 0xFF94A3B8.toInt()
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            })
            currentY += 20f
        }

        drawFooter(canvas, 1)
        document.finishPage(page)

        // Write to cache directory to prepare for sharing/printing
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val pdfFile = File(reportsDir, "statement_${student.name.replace(" ", "_")}.pdf")
        document.writeTo(FileOutputStream(pdfFile))
        document.close()

        return pdfFile
    }

    /**
     * Share PDF file via system chooser
     */
    fun sharePdf(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        try {
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة تقرير PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "فشلت مشاركة الملف: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Print PDF file via Android PrintManager
     */
    fun printPdf(context: Context, file: File, documentName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "الطباعة غير مدعومة على هذا الجهاز", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val printAdapter = PdfPrintAdapter(file)
            printManager.print(documentName, printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            Toast.makeText(context, "فشلت عملية الطباعة: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Save PDF file to public storage (Downloads folder)
     */
    fun savePdfToDownloads(context: Context, file: File, displayName: String): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { outputStream ->
                        if (outputStream != null) {
                            file.inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                            Toast.makeText(context, "تم حفظ الملف بنجاح في مجلد التنزيلات (Downloads)", Toast.LENGTH_LONG).show()
                            return true
                        }
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, displayName)
                file.inputStream().use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Toast.makeText(context, "تم حفظ الملف بنجاح: ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
                return true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "فشل حفظ الملف: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
        return false
    }

    /**
     * Helper to compute approximate number of months between two timestamps
     */
    private fun getMonthsDifference(startMillis: Long, endMillis: Long): Int {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
        
        val yearsDiff = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
        val monthsDiff = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
        
        return maxOf(1, (yearsDiff * 12) + monthsDiff)
    }
}

/**
 * Custom PrintDocumentAdapter to handle printing of a PDF file
 */
class PdfPrintAdapter(private val file: File) : android.print.PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
        }

        val info = android.print.PrintDocumentInfo.Builder(file.name)
            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1) // Single page reports for simplicity
            .build()
        callback?.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out android.print.PageRange>?,
        destination: android.os.ParcelFileDescriptor?,
        cancellationSignal: android.os.CancellationSignal?,
        callback: WriteResultCallback?
    ) {
        var input: java.io.FileInputStream? = null
        var output: java.io.FileOutputStream? = null

        try {
            input = java.io.FileInputStream(file)
            output = java.io.FileOutputStream(destination?.fileDescriptor)

            val buf = ByteArray(16384)
            var bytesRead: Int
            while (input.read(buf).also { bytesRead = it } >= 0) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                output.write(buf, 0, bytesRead)
            }
            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
        } finally {
            try {
                input?.close()
                output?.close()
            } catch (e: Exception) {}
        }
    }
}
