package space.ogurecs.framed.parser

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import space.ogurecs.framed.model.CameraBrand
import space.ogurecs.framed.model.ExifData
import java.io.InputStream
import kotlin.math.roundToInt

object ExifParser {

    fun parse(context: Context, uri: Uri): ExifData {
        return try {
            val stream = try {
                context.contentResolver.openInputStream(uri)
            } catch (e: Exception) {
                if (uri.path != null) java.io.FileInputStream(java.io.File(uri.path!!)) else null
            }
            stream?.use { parse(it) } ?: ExifData()
        } catch (e: Exception) {
            ExifData()
        }
    }

    fun parse(stream: InputStream): ExifData {
        val exif = ExifInterface(stream)
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
        val lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim().orEmpty()

        val focal35 = exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)
        val focalDouble = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
        val focalLengthStr = when {
            focal35 > 0 -> "${focal35}mm"
            focalDouble > 0.0 -> "${focalDouble.roundToInt()}mm"
            else -> ""
        }

        val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
        val apertureStr = if (fNumber > 0.0) {
            if (fNumber % 1.0 == 0.0) "F${fNumber.toInt()}" else "F$fNumber"
        } else ""

        val exposureTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
        val shutterStr = formatShutterSpeed(exposureTime)

        val iso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0)
        val isoStr = if (iso > 0) "ISO$iso" else ""

        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?.replace(":", ".")
            ?.take(16)
            .orEmpty()

        val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

        val brand = CameraBrand.detect(make, model)
        val cleanModel = CameraModelNormalizer.normalize(brand, make, model)

        return ExifData(
            brand = brand,
            makeRaw = make,
            model = cleanModel,
            lens = lens,
            focalLength = focalLengthStr,
            aperture = apertureStr,
            shutterSpeed = shutterStr,
            iso = isoStr,
            dateTime = dateTime,
            width = width,
            height = height
        )
    }


    private fun formatShutterSpeed(timeSec: Double): String {
        if (timeSec <= 0.0) return ""
        return if (timeSec < 1.0) {
            val fraction = (1.0 / timeSec).roundToInt()
            "1/${fraction}s"
        } else {
            val rounded = (timeSec * 10).roundToInt() / 10.0
            if (rounded % 1.0 == 0.0) "${rounded.toInt()}s" else "${rounded}s"
        }
    }
}

object CameraModelNormalizer {
    fun normalize(brand: CameraBrand, make: String, rawModel: String): String {
        val trimmed = rawModel.trim()
        if (trimmed.isBlank()) return ""

        return when (brand) {
            CameraBrand.SONY, CameraBrand.SONY_ALPHA -> {
                val clean = trimmed.replace(Regex("(?i)^sony\\s+"), "")
                val isAlphaBrand = brand == CameraBrand.SONY_ALPHA
                when {
                    clean.equals("ILCE-1", true) -> if (isAlphaBrand) "1" else "α1"
                    clean.equals("ILCE-9M3", true) -> if (isAlphaBrand) "9 III" else "α9 III"
                    clean.equals("ILCE-9M2", true) -> if (isAlphaBrand) "9 II" else "α9 II"
                    clean.equals("ILCE-9", true) -> if (isAlphaBrand) "9" else "α9"
                    clean.equals("ILCE-7RM5", true) -> if (isAlphaBrand) "7R V" else "α7R V"
                    clean.equals("ILCE-7RM4", true) -> if (isAlphaBrand) "7R IV" else "α7R IV"
                    clean.equals("ILCE-7RM3", true) -> if (isAlphaBrand) "7R III" else "α7R III"
                    clean.equals("ILCE-7RM2", true) -> if (isAlphaBrand) "7R II" else "α7R II"
                    clean.equals("ILCE-7R", true) -> if (isAlphaBrand) "7R" else "α7R"
                    clean.equals("ILCE-7SM3", true) -> if (isAlphaBrand) "7S III" else "α7S III"
                    clean.equals("ILCE-7SM2", true) -> if (isAlphaBrand) "7S II" else "α7S II"
                    clean.equals("ILCE-7S", true) -> if (isAlphaBrand) "7S" else "α7S"
                    clean.equals("ILCE-7M4", true) -> if (isAlphaBrand) "7 IV" else "α7 IV"
                    clean.equals("ILCE-7M3", true) -> if (isAlphaBrand) "7 III" else "α7 III"
                    clean.equals("ILCE-7M2", true) -> if (isAlphaBrand) "7 II" else "α7 II"
                    clean.equals("ILCE-7", true) -> if (isAlphaBrand) "7" else "α7"
                    clean.equals("ILCE-7CR", true) -> if (isAlphaBrand) "7CR" else "α7CR"
                    clean.equals("ILCE-7CM2", true) -> if (isAlphaBrand) "7C II" else "α7C II"
                    clean.equals("ILCE-7C", true) -> if (isAlphaBrand) "7C" else "α7C"
                    clean.equals("ILCE-6700", true) -> if (isAlphaBrand) "6700" else "α6700"
                    clean.equals("ILCE-6600", true) -> if (isAlphaBrand) "6600" else "α6600"
                    clean.equals("ILCE-6500", true) -> if (isAlphaBrand) "6500" else "α6500"
                    clean.equals("ILCE-6400", true) -> if (isAlphaBrand) "6400" else "α6400"
                    clean.equals("ILCE-6300", true) -> if (isAlphaBrand) "6300" else "α6300"
                    clean.equals("ILCE-6100", true) -> if (isAlphaBrand) "6100" else "α6100"
                    clean.equals("ILCE-6000", true) -> if (isAlphaBrand) "6000" else "α6000"
                    clean.equals("ZV-E1", true) -> "ZV-E1"
                    clean.equals("ZV-E10", true) -> "ZV-E10"
                    clean.equals("ZV-E10M2", true) -> "ZV-E10 II"
                    clean.equals("ZV-1", true) -> "ZV-1"
                    clean.startsWith("DSC-RX100M", true) -> {
                        val gen = clean.removePrefix("DSC-RX100M")
                        val roman = when (gen) {
                            "7" -> "VII"; "6" -> "VI"; "5" -> "V"; "4" -> "IV"; "3" -> "III"; "2" -> "II"; else -> gen
                        }
                        "RX100 $roman"
                    }
                    clean.startsWith("ILCE-", true) -> if (isAlphaBrand) clean.removePrefix("ILCE-") else "α" + clean.removePrefix("ILCE-")
                    else -> clean
                }
            }
            CameraBrand.CANON -> {
                trimmed.replace(Regex("(?i)^canon\\s+"), "")
            }
            CameraBrand.NIKON -> {
                val clean = trimmed.replace(Regex("(?i)^nikon\\s+"), "")
                when {
                    clean.equals("Z 7_2", true) -> "Z 7 II"
                    clean.equals("Z 6_2", true) -> "Z 6 II"
                    clean.equals("Z 6_3", true) -> "Z 6 III"
                    else -> clean
                }
            }
            CameraBrand.FUJIFILM -> {
                trimmed.replace(Regex("(?i)^fujifilm\\s+"), "")
            }
            CameraBrand.APPLE -> {
                when (trimmed) {
                    "iPhone16,2" -> "iPhone 16 Pro Max"
                    "iPhone16,1" -> "iPhone 16 Pro"
                    "iPhone15,3" -> "iPhone 14 Pro Max"
                    "iPhone15,2" -> "iPhone 14 Pro"
                    "iPhone15,5" -> "iPhone 15 Plus"
                    "iPhone15,4" -> "iPhone 15"
                    "iPhone14,3" -> "iPhone 13 Pro Max"
                    "iPhone14,2" -> "iPhone 13 Pro"
                    "iPhone14,5" -> "iPhone 13"
                    "iPhone14,4" -> "iPhone 13 mini"
                    else -> trimmed
                }
            }
            else -> trimmed
        }
    }
}

