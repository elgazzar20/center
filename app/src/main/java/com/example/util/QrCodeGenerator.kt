package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream

object QrCodeGenerator {

    fun generateQrCode(text: String, size: Int = 512): Bitmap? {
        if (text.isEmpty()) return null
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                size,
                size
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun printQrCode(context: Context, qrBitmap: Bitmap, studentName: String) {
        try {
            val printHelper = PrintHelper(context)
            printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
            printHelper.printBitmap("QR_$studentName", qrBitmap)
        } catch (e: Exception) {
            Toast.makeText(context, "فشلت عملية الطباعة: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareQrCode(context: Context, qrBitmap: Bitmap, studentName: String) {
        try {
            val cachePath = File(context.cacheDir, "qr_codes")
            cachePath.mkdirs()
            val cleanName = studentName.replace("\\s+".toRegex(), "_")
            val file = File(cachePath, "QR_$cleanName.png")
            val stream = FileOutputStream(file)
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة رمز QR للطالب $studentName"))
        } catch (e: Exception) {
            Toast.makeText(context, "فشلت مشاركة الرمز: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
