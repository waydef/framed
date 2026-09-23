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
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            parse(stream)
        } ?: ExifData()
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

        val brand = CameraBrand.detect(make)

        return ExifData(
            brand = brand,
            makeRaw = make,
            model = model,
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
