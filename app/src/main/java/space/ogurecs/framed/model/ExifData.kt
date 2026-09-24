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
    TIKTOK_OPTIMIZED("для соцсетей", "2.5K • чётко")
}

enum class FrameStyle(val displayName: String, val badge: String) {
    BLUR_CLASSIC("синематик блюр", "классика"),
    STUDIO_PASSEPARTOUT("студийный паспарту", "чистый цвет"),
    SIDEBAR_LEFT("боковой сайдбар", "2.0 хит"),
    MINIMAL_FINEART("галерейный минимал", "минимализм"),
    POLAROID_VINTAGE("винтажный полароид", "ретро"),
    EDITORIAL_SPLIT("журнальный сплит", "акцент")
}

enum class BackgroundType(val displayName: String) {
    BLUR("размытие"),
    SOLID_COLOR("сплошной цвет")
}

data class SolidColorOption(
    val label: String,
    val colorLong: Long,
    val isLight: Boolean
)

object SolidColorPresets {
    val OBSIDIAN = SolidColorOption("обсидиан", 0xFF0A0C10L, false)
    val PURE_BLACK = SolidColorOption("чёрный oled", 0xFF000000L, false)
    val STUDIO_WHITE = SolidColorOption("студийный белый", 0xFFF8F9FAL, true)
    val WARM_CREAM = SolidColorOption("тёплый крем", 0xFFFAF8F5L, true)
    val SLATE_GREY = SolidColorOption("графит", 0xFF181C24L, false)
    val DEEP_NAVY = SolidColorOption("индиго", 0xFF0E131FL, false)
    val MUTED_SAGE = SolidColorOption("шалфей", 0xFF1A221EL, false)

    val ALL = listOf(OBSIDIAN, PURE_BLACK, STUDIO_WHITE, WARM_CREAM, SLATE_GREY, DEEP_NAVY, MUTED_SAGE)
}

data class FrameConfig(
    val style: FrameStyle = FrameStyle.BLUR_CLASSIC,
    val bgType: BackgroundType = BackgroundType.BLUR,
    val solidColor: Long = 0xFF0A0C10L,
    val borderWidth: Float = 0f,
    val borderColor: Long = 0x33FFFFFFL,
    val sidebarWidthRatio: Float = 0.22f,
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

data class FramePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val style: FrameStyle,
    val config: FrameConfig
)

object FramePresets {
    val SIDEBAR_LEFT = FramePreset(
        id = "sidebar_left",
        title = "боковой сайдбар",
        subtitle = "вертикальная колонка параметров слева, сплошной фон",
        badge = "2.0 хит",
        style = FrameStyle.SIDEBAR_LEFT,
        config = FrameConfig(
            style = FrameStyle.SIDEBAR_LEFT,
            bgType = BackgroundType.SOLID_COLOR,
            solidColor = 0xFF0A0C10L,
            photoScale = 0.92f,
            cornerRadius = 32f,
            shadowAlpha = 0.30f,
            shadowRadius = 28f,
            sidebarWidthRatio = 0.22f,
            ratio = CanvasRatio.RATIO_3_4
        )
    )

    val STUDIO_PASSEPARTOUT = FramePreset(
        id = "studio_passepartout",
        title = "студийный паспарту",
        subtitle = "глубокий сплошной фон, изысканный мат и парящая тень",
        badge = "чистый цвет",
        style = FrameStyle.STUDIO_PASSEPARTOUT,
        config = FrameConfig(
            style = FrameStyle.STUDIO_PASSEPARTOUT,
            bgType = BackgroundType.SOLID_COLOR,
            solidColor = 0xFF0A0C10L,
            photoScale = 0.88f,
            cornerRadius = 24f,
            shadowAlpha = 0.42f,
            shadowRadius = 40f,
            ratio = CanvasRatio.RATIO_4_5
        )
    )

    val BLUR_CLASSIC = FramePreset(
        id = "blur_classic",
        title = "синематик блюр",
        subtitle = "фирменный размытый фон с tpdf-дизерингом и плавающей карточкой",
        badge = "классика",
        style = FrameStyle.BLUR_CLASSIC,
        config = FrameConfig(
            style = FrameStyle.BLUR_CLASSIC,
            bgType = BackgroundType.BLUR,
            photoScale = 0.95f,
            cornerRadius = 60f,
            blurRadius = 40f,
            blurDimming = 0.12f,
            ratio = CanvasRatio.RATIO_3_4
        )
    )

    val MINIMAL_FINEART = FramePreset(
        id = "minimal_fineart",
        title = "галерейный минимал",
        subtitle = "тонкая окантовка, плоская геометрия без теней и воздух",
        badge = "минимализм",
        style = FrameStyle.MINIMAL_FINEART,
        config = FrameConfig(
            style = FrameStyle.MINIMAL_FINEART,
            bgType = BackgroundType.SOLID_COLOR,
            solidColor = 0xFF0A0C10L,
            photoScale = 0.88f,
            cornerRadius = 8f,
            shadowAlpha = 0f,
            borderWidth = 2f,
            borderColor = 0x33FFFFFFL,
            textAlignment = TextAlignment.CENTER,
            ratio = CanvasRatio.RATIO_4_5
        )
    )

    val POLAROID_VINTAGE = FramePreset(
        id = "polaroid_vintage",
        title = "винтажный полароид",
        subtitle = "широкое нижнее поле, тёплая бумага и плёночная эстетика",
        badge = "ретро",
        style = FrameStyle.POLAROID_VINTAGE,
        config = FrameConfig(
            style = FrameStyle.POLAROID_VINTAGE,
            bgType = BackgroundType.SOLID_COLOR,
            solidColor = 0xFFFAF8F5L,
            photoScale = 0.92f,
            cornerRadius = 6f,
            shadowAlpha = 0.25f,
            shadowRadius = 24f,
            logoColorMode = LogoColorMode.BLACK,
            fontOption = FontOption.JETBRAINS,
            textAlignment = TextAlignment.CENTER,
            ratio = CanvasRatio.RATIO_3_4
        )
    )

    val EDITORIAL_SPLIT = FramePreset(
        id = "editorial_split",
        title = "журнальный сплит",
        subtitle = "синхронизированные метаданные по противоположным краям",
        badge = "акцент",
        style = FrameStyle.EDITORIAL_SPLIT,
        config = FrameConfig(
            style = FrameStyle.EDITORIAL_SPLIT,
            bgType = BackgroundType.BLUR,
            photoScale = 0.94f,
            cornerRadius = 48f,
            textAlignment = TextAlignment.SPLIT,
            ratio = CanvasRatio.RATIO_4_5
        )
    )

    val ALL = listOf(
        SIDEBAR_LEFT,
        STUDIO_PASSEPARTOUT,
        BLUR_CLASSIC,
        MINIMAL_FINEART,
        POLAROID_VINTAGE,
        EDITORIAL_SPLIT
    )

    fun getById(id: String): FramePreset {
        return ALL.firstOrNull { it.id == id } ?: BLUR_CLASSIC
    }
}

