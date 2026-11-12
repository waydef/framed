package space.ogurecs.framed.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import space.ogurecs.framed.model.ExifData
import space.ogurecs.framed.parser.ExifParser

/** A decoded photo, already turned upright, with the EXIF data that belongs to exactly this file. */
class LoadedImage(val bitmap: Bitmap, val exif: ExifData)

object ImageLoader {

    /** Decodes [uri] and applies the EXIF orientation. Returns null when the file cannot be read or decoded. */
    fun load(context: Context, uri: Uri): LoadedImage? {
        return try {
            val exif = ExifParser.parse(context, uri)
            val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null
            val upright = ImageOrientation.apply(decoded, exif.orientation)
            if (upright !== decoded) decoded.recycle()
            val fixedExif = if (ImageOrientation.transformFor(exif.orientation).swapsSides) {
                exif.copy(width = exif.height, height = exif.width, orientation = 1)
            } else {
                exif.copy(orientation = 1)
            }
            LoadedImage(upright, fixedExif)
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }
}
