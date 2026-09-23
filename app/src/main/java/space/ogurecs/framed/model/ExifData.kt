package space.ogurecs.framed.model

import space.ogurecs.framed.R

enum class CameraBrand(
    val displayName: String,
    val iconRes: Int?,
    val brandHexColor: String? = null
) {
    SONY("SONY", R.drawable.brand_sony, "#FF6600"),
    NIKON("Nikon", R.drawable.brand_nikon, "#FFE100"),
    CANON("Canon", R.drawable.brand_canon, "#CC0000"),
    FUJIFILM("FUJIFILM", R.drawable.brand_fujifilm, "#E60012"),
    LEICA("Leica", R.drawable.brand_leica, "#ED1C24"),
    HASSELBLAD("HASSELBLAD", R.drawable.brand_hasselblad, "#FFFFFF"),
    LUMIX("LUMIX", R.drawable.brand_lumix, "#E60012"),
    APPLE("Apple", R.drawable.brand_apple, "#FFFFFF"),
    OTHER("Камера", null, null);

    companion object {
        fun detect(make: String): CameraBrand {
            val lower = make.lowercase()
            return when {
                "sony" in lower -> SONY
                "nikon" in lower -> NIKON
                "canon" in lower -> CANON
                "fuji" in lower -> FUJIFILM
                "leica" in lower -> LEICA
                "hasselblad" in lower -> HASSELBLAD
                "lumix" in lower || "panasonic" in lower -> LUMIX
                "apple" in lower -> APPLE
                else -> OTHER
            }
        }
    }
}

enum class CanvasRatio(val label: String, val widthRatio: Float, val heightRatio: Float) {
    RATIO_4_5("4:5", 4f, 5f),
    RATIO_9_16("9:16", 9f, 16f),
    RATIO_1_1("1:1", 1f, 1f),
    RATIO_3_4("3:4", 3f, 4f),
    RATIO_16_9("16:9", 16f, 9f),
    ORIGINAL("Оригинал", 0f, 0f)
}

enum class LogoColorMode(val label: String) {
    WHITE("Белый"),
    BLACK("Чёрный"),
    BRAND("Бренд"),
    MATCH_TEXT("Текст")
}

enum class FontOption(val label: String, val assetPath: String?) {
    INTER("Inter", "fonts/inter.ttf"),
    SPACE_GROTESK("Space Grotesk", "fonts/spacegrotesk.ttf"),
    PLAYFAIR("Playfair", "fonts/playfair_regular.ttf"),
    JETBRAINS("JetBrains Mono", "fonts/jetbrains_mono_regular.ttf"),
    SYSTEM("Системный", null)
}

enum class CustomFontWeight(val label: String, val weightValue: Int) {
    REGULAR("Regular", 400),
    MEDIUM("Medium", 500),
    SEMIBOLD("SemiBold", 600),
    BOLD("Bold", 700)
}

data class ExifData(
    val brand: CameraBrand = CameraBrand.OTHER,
    val makeRaw: String = "",
    val model: String = "",
    val lens: String = "",
    val focalLength: String = "",
    val aperture: String = "",
    val shutterSpeed: String = "",
    val iso: String = "",
    val dateTime: String = "",
    val width: Int = 0,
    val height: Int = 0
) {
    val formattedParams: String
        get() = listOf(focalLength, aperture, shutterSpeed, iso)
            .filter { it.isNotBlank() }
            .joinToString("  ")
}

data class FrameConfig(
    val ratio: CanvasRatio = CanvasRatio.RATIO_3_4,
    val cornerRadius: Float = 28f,
    val shadowRadius: Float = 36f,
    val shadowAlpha: Float = 0.38f,
    val blurRadius: Float = 42f,
    val blurDimming: Float = 0.12f,
    val photoScale: Float = 0.86f,
    val showLogo: Boolean = true,
    val showModel: Boolean = true,
    val showParams: Boolean = true,
    val showLens: Boolean = false,
    val showDate: Boolean = false,
    val logoColorMode: LogoColorMode = LogoColorMode.WHITE,
    val fontOption: FontOption = FontOption.INTER,
    val fontWeight: CustomFontWeight = CustomFontWeight.MEDIUM
)
