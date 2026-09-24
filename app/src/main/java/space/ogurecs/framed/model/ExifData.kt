package space.ogurecs.framed.model

import space.ogurecs.framed.R

enum class CameraBrand(
    val displayName: String,
    val iconRes: Int?,
    val brandHexColor: String? = null
) {
    SONY("SONY", R.drawable.brand_sony, "#FF6600"),
    SONY_ALPHA("SONY α", R.drawable.ic_brand_sony_alpha, "#FF6600"),
    NIKON("Nikon", R.drawable.brand_nikon, "#FFE100"),
    CANON("Canon", R.drawable.brand_canon, "#CC0000"),
    FUJIFILM("FUJIFILM", R.drawable.brand_fujifilm, "#E60012"),
    LEICA("Leica", R.drawable.brand_leica, "#ED1C24"),
    HASSELBLAD("HASSELBLAD", R.drawable.brand_hasselblad, "#FFFFFF"),
    LUMIX("LUMIX", R.drawable.brand_lumix, "#E60012"),
    APPLE("Apple", R.drawable.brand_apple, "#FFFFFF"),
    OTHER("камера", null, null);

    companion object {
        fun detect(make: String, model: String = ""): CameraBrand {
            val combined = "$make $model".lowercase()
            return when {
                "sony" in combined -> {
                    if ("ilce" in combined || "alpha" in combined || "α" in combined || "zv-e" in combined) {
                        SONY_ALPHA
                    } else {
                        SONY
                    }
                }
                "nikon" in combined -> NIKON
                "canon" in combined -> CANON
                "fuji" in combined -> FUJIFILM
                "leica" in combined -> LEICA
                "hasselblad" in combined -> HASSELBLAD
                "lumix" in combined || "panasonic" in combined -> LUMIX
                "apple" in combined -> APPLE
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
    ORIGINAL("оригинал", 0f, 0f)
}

enum class LogoColorMode(val label: String) {
    WHITE("белый"),
    BLACK("чёрный"),
    BRAND("бренд"),
    MATCH_TEXT("текст")
}

enum class FontOption(val label: String, val assetPath: String?) {
    INTER("inter", "fonts/inter.ttf"),
    SPACE_GROTESK("space grotesk", "fonts/spacegrotesk.ttf"),
    PLAYFAIR("playfair", "fonts/playfair_regular.ttf"),
    JETBRAINS("jetbrains mono", "fonts/jetbrains_mono_regular.ttf"),
    SYSTEM("системный", null)
}

enum class CustomFontWeight(val label: String, val weightValue: Int) {
    REGULAR("regular", 400),
    MEDIUM("medium", 500),
    SEMIBOLD("semibold", 600),
    BOLD("bold", 700)
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

enum class TextAlignment(val label: String) {
    CENTER("по центру"),
    LEFT("слева"),
    SPLIT("по краям")
}

enum class ExportQuality(val label: String, val badge: String) {
    ORIGINAL_100("оригинал 100%", "без потерь"),
    TIKTOK_OPTIMIZED("для соцсетей", "1080p • чётко")
}

data class FrameConfig(
    val ratio: CanvasRatio = CanvasRatio.RATIO_3_4,
    val cornerRadius: Float = 60f,
    val shadowRadius: Float = 36f,
    val shadowAlpha: Float = 0.38f,
    val shadowOffsetY: Float = 0f,
    val blurRadius: Float = 40f,
    val blurDimming: Float = 0.12f,
    val photoScale: Float = 0.95f,
    val showLogo: Boolean = true,
    val showModel: Boolean = true,
    val showParams: Boolean = true,
    val showLens: Boolean = false,
    val showDate: Boolean = false,
    val separateExtraLine: Boolean = true,
    val logoColorMode: LogoColorMode = LogoColorMode.WHITE,
    val fontOption: FontOption = FontOption.INTER,
    val fontWeight: CustomFontWeight = CustomFontWeight.MEDIUM,
    val textAlignment: TextAlignment = TextAlignment.CENTER,
    val logoGap: Float = 20f,
    val lineSpacing: Float = 32f,
    val footerVerticalOffset: Float = 0f,
    val letterSpacing: Float = 0.02f,
    val fontSizeLine1: Float = 34f,
    val fontSizeLine2: Float = 22f,
    val logoOffsetY: Float = 0f,
    val shadowSpread: Float = 8f,
    val textHorizontalOffset: Float = 0f,
    val textMasterScale: Float = 1.5f,
    val logoScale: Float = 1.0f
)

