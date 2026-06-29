package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.util.Locale

object SignatureUtils {
    /**
     * Retrieves the SHA-1 fingerprint of the app's signing certificate dynamically.
     */
    fun getCertificateSHA1(context: Context): String {
        return try {
            val packageName = context.packageName
            val packageManager = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else {
                    null
                }
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                packageInfo.signatures
            }

            if (signatures != null && signatures.isNotEmpty()) {
                val signature = signatures[0]
                val md = MessageDigest.getInstance("SHA-1")
                val publicKey = md.digest(signature.toByteArray())
                val hexString = StringBuilder()
                for (i in publicKey.indices) {
                    val appendString = Integer.toHexString(0xFF and publicKey[i].toInt())
                    if (appendString.length == 1) hexString.append("0")
                    hexString.append(appendString.uppercase(Locale.ROOT))
                    if (i < publicKey.size - 1) hexString.append(":")
                }
                hexString.toString()
            } else {
                "لا توجد شهادات توقيع متوفرة"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "خطأ أثناء جلب بصمة التوقيع: ${e.message}"
        }
    }
}
