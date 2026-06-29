package com.example.util

import java.util.Locale

object ValidationUtils {
    // List of known disposable/temporary email domains
    private val disposableDomains = setOf(
        "mailinator.com",
        "10minutemail.com",
        "tempmail.com",
        "yopmail.com",
        "guerrillamail.com",
        "sharklasers.com",
        "guerrillamail.info",
        "guerrillamail.biz",
        "guerrillamail.de",
        "guerrillamail.net",
        "guerrillamail.org",
        "guerrillamailblock.com",
        "pokemail.net",
        "trashmail.com",
        "disposable.com",
        "temp-mail.org",
        "getairmail.com",
        "generator.email",
        "throwawaymail.com",
        "maildrop.cc",
        "tempmailaddress.com",
        "fakeinbox.com",
        "mintemail.com",
        "getnada.com",
        "dispostable.com"
    )

    private val emailPattern = Regex(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )

    /**
     * Checks if the email is a valid format and is NOT from a disposable email service.
     */
    fun isValidAndTrustedEmail(email: String): Boolean {
        val trimmed = email.trim().lowercase(Locale.ROOT)
        if (!emailPattern.matches(trimmed)) {
            return false
        }
        
        val parts = trimmed.split("@")
        if (parts.size != 2) return false
        val domain = parts[1]
        
        // Prevent exact matches or subdomains of disposable domains
        if (disposableDomains.contains(domain)) {
            return false
        }
        
        return disposableDomains.none { disposable -> domain.endsWith(".$disposable") }
    }
}
