package com.edgeplus

object BankingProtection {

    // Package names for:
    // brimo, livin, mybca, wondr bni, bca sekuritas, stockbit, bibit, ibkr, flip, gopay, dana, ovo, jago
    private val BANKING_PACKAGES = setOf(
        // BRImo
        "id.co.bri.brimo",
        // Livin' Mandiri
        "id.bmri.livin",
        "id.co.bankmandiri.mandirionline",
        // myBCA & BCA mobile
        "com.bca.mybca",
        "com.bca",
        // wondr by BNI & BNI Mobile
        "id.bni.wondr",
        "src.com.bni",
        // BCA Sekuritas (BEST Mobile)
        "com.bcasekuritas.mobile",
        "com.bcasekuritas.best",
        // Stockbit
        "com.stockbit.android",
        // Bibit
        "com.bibit.bibitid",
        // IBKR (Interactive Brokers)
        "com.interactivebrokers.mobile",
        "com.interactivebrokers.android",
        // Flip
        "id.flip",
        // GoPay
        "com.gojek.app",
        "com.gopay.wallet",
        // DANA
        "id.dana",
        // OVO
        "ovo.id",
        // Bank Jago
        "com.jago.digitalbanking"
    )

    fun isBankingPackage(pkg: String): Boolean {
        if (BANKING_PACKAGES.contains(pkg)) return true
        val lower = pkg.lowercase()
        return lower.contains("brimo") ||
                lower.contains("livin") ||
                lower.contains("mybca") ||
                lower.contains("wondr") ||
                lower.contains("bcasekuritas") ||
                lower.contains("stockbit") ||
                lower.contains("bibit") ||
                lower.contains("flip") ||
                lower.contains("jago")
    }
}
