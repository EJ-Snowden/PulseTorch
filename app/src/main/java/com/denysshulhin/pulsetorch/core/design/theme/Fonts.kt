package com.denysshulhin.pulsetorch.core.design.theme

import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import androidx.compose.ui.text.font.FontFamily
import com.denysshulhin.pulsetorch.R

object PTFonts {
    private val provider = Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    private val spaceGrotesk = GoogleFont("Space Grotesk")
    private val manrope = GoogleFont("Manrope")
    private val inter = GoogleFont("Inter")

    val Display = FontFamily(
        Font(spaceGrotesk, provider),
    )

    val Body = FontFamily(
        Font(manrope, provider),
    )

    val System = FontFamily(
        Font(inter, provider),
    )
}