package space.ogurecs.framed.util

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * BitmapFactory ignores the EXIF orientation tag. Cameras such as the Sony a6600 store portrait
 * shots as landscape pixels with orientation 6 or 8, so the bitmap has to be turned by hand.
 */
object ImageOrientation {

    /** Rotation first (clockwise, degrees), then a horizontal mirror. */
    data class Transform(val rotationDegrees: Int, val mirror: Boolean) {
        val isIdentity: Boolean get() = rotationDegrees == 0 && !mirror
        val swapsSides: Boolean get() = rotationDegrees % 180 != 0
    }

    fun transformFor(orientation: Int): Transform = when (orientation) {
        2 -> Transform(0, true)     // mirrored horizontally
        3 -> Transform(180, false)
        4 -> Transform(180, true)   // mirrored vertically
        5 -> Transform(90, true)    // transposed (rotate right, then mirror)
        6 -> Transform(90, false)
        7 -> Transform(270, true)   // transversed (rotate left, then mirror)
        8 -> Transform(270, false)
        else -> Transform(0, false) // 1 and unknown values
    }

    /** Returns [bitmap] itself when nothing has to change, otherwise a new bitmap (the caller recycles the old one). */
    fun apply(bitmap: Bitmap, orientation: Int): Bitmap {
        val transform = transformFor(orientation)
        if (transform.isIdentity) return bitmap
        val matrix = Matrix()
        if (transform.rotationDegrees != 0) matrix.postRotate(transform.rotationDegrees.toFloat())
        if (transform.mirror) matrix.postScale(-1f, 1f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
