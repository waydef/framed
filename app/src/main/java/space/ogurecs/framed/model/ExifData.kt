package space.ogurecs.framed.model

import space.ogurecs.framed.R

enum class CameraBrand(val displayName: String, val iconRes: Int?) {
    SONY("SONY", R.drawable.ic_brand_sony),
    NIKON("Nikon", R.drawable.ic_brand_nikon),
    CANON("Canon", R.drawable.ic_brand_canon),
    FUJIFILM("FUJIFILM", R.drawable.ic_brand_fujifilm),
    LEICA("Leica", R.drawable.ic_brand_leica),
    HASSELBLAD("HASSELBLAD", R.drawable.ic_brand_hasselblad),
    LUMIX("LUMIX", R.drawable.ic_brand_lumix),
    APPLE("Apple", R.drawable.ic_brand_apple),
    OTHER("Camera", null);

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
    val showDate: Boolean = false
)
