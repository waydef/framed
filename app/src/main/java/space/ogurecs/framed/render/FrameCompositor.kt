package space.ogurecs.framed.render

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import space.ogurecs.framed.model.CanvasRatio
import space.ogurecs.framed.model.ExifData
import space.ogurecs.framed.model.FrameConfig
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object FrameCompositor {

    fun render(
        context: Context,
        source: Bitmap,
        exif: ExifData,
        config: FrameConfig
    ): Bitmap {
        val srcW = source.width.toFloat()
        val srcH = source.height.toFloat()

        val targetRatio = when (config.ratio) {
            CanvasRatio.RATIO_4_5 -> 4f / 5f
            CanvasRatio.RATIO_9_16 -> 9f / 16f
            CanvasRatio.RATIO_1_1 -> 1f
            CanvasRatio.RATIO_3_4 -> 3f / 4f
            CanvasRatio.RATIO_16_9 -> 16f / 9f
            CanvasRatio.ORIGINAL -> srcW / srcH
        }

        // Calculate canvas dimensions keeping source at full 100% native resolution
        val scale = config.photoScale.coerceIn(0.6f, 0.95f)
        val footerSpaceRatio = 0.14f

        val photoSlotW: Float
        val photoSlotH: Float
        val canvasW: Int
        val canvasH: Int

        val availableHeightRatio = 1f - footerSpaceRatio
        val maxSlotRatio = targetRatio / availableHeightRatio

        if ((srcW / srcH) > maxSlotRatio) {
            photoSlotW = srcW / scale
            val totalW = photoSlotW
            val totalH = totalW / targetRatio
            canvasW = totalW.roundToInt()
            canvasH = totalH.roundToInt()
            photoSlotH = totalH * availableHeightRatio
        } else {
            photoSlotH = srcH / scale
            val totalH = photoSlotH / availableHeightRatio
            val totalW = totalH * targetRatio
            canvasW = totalW.roundToInt()
            canvasH = totalH.roundToInt()
            photoSlotW = totalW
        }

        val output = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Draw blurred background
        drawBlurredBackground(canvas, source, canvasW, canvasH, config)

        // 2. Position the main photo
        val photoW = srcW
        val photoH = srcH
        val photoLeft = (canvasW - photoW) / 2f
        val photoTop = (canvasH * availableHeightRatio - photoH) / 2f + (canvasH * 0.03f)
        val photoRect = RectF(photoLeft, photoTop, photoLeft + photoW, photoTop + photoH)

        val referenceDim = min(canvasW, canvasH).toFloat()
        val cornerPx = (config.cornerRadius / 1000f) * referenceDim
        val shadowPx = (config.shadowRadius / 1000f) * referenceDim

        // 3. Draw soft ambient shadow
        drawSoftShadow(canvas, photoRect, cornerPx, shadowPx, config.shadowAlpha)

        // 4. Draw rounded photo
        val clipPath = Path().apply {
            addRoundRect(photoRect, cornerPx, cornerPx, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(source, null, photoRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        canvas.restore()

        // 5. Draw Footer / Metadata block
        val footerCenterY = photoRect.bottom + (canvasH - photoRect.bottom) * 0.46f
        drawMetadataFooter(context, canvas, canvasW, footerCenterY, referenceDim, exif, config)

        return output
    }

    private fun drawBlurredBackground(
        canvas: Canvas,
        source: Bitmap,
        cw: Int,
        ch: Int,
        config: FrameConfig
    ) {
        // Fast blur via downsampled thumbnail
        val thumbW = 320
        val thumbH = max(240, (320f * ch / cw).roundToInt())
        val thumb = Bitmap.createScaledBitmap(source, thumbW, thumbH, true)
        val blurredThumb = fastBlur(thumb, config.blurRadius.roundToInt().coerceIn(4, 50))

        val dstRect = Rect(0, 0, cw, ch)
        val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
        }
        canvas.drawBitmap(blurredThumb, null, dstRect, bgPaint)
        blurredThumb.recycle()

        // Dimming overlay
        val dimAlpha = (config.blurDimming.coerceIn(0f, 0.8f) * 255).roundToInt()
        if (dimAlpha > 0) {
            val dimPaint = Paint().apply {
                color = Color.argb(dimAlpha, 0, 0, 0)
            }
            canvas.drawRect(0f, 0f, cw.toFloat(), ch.toFloat(), dimPaint)
        }
    }

    private fun drawSoftShadow(
        canvas: Canvas,
        rect: RectF,
        radius: Float,
        blurSize: Float,
        alphaFactor: Float
    ) {
        if (blurSize <= 0f || alphaFactor <= 0f) return

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val steps = 6
        val maxOffset = blurSize * 0.4f
        val stepAlpha = (alphaFactor * 255f / steps).roundToInt()

        for (i in steps downTo 1) {
            val spread = blurSize * (i.toFloat() / steps)
            val offsetY = maxOffset * (i.toFloat() / steps)
            shadowPaint.color = Color.argb((stepAlpha * 0.7f).roundToInt(), 0, 0, 0)

            val shadowRect = RectF(
                rect.left - spread * 0.5f,
                rect.top - spread * 0.2f + offsetY,
                rect.right + spread * 0.5f,
                rect.bottom + spread * 0.8f + offsetY
            )
            canvas.drawRoundRect(shadowRect, radius + spread * 0.2f, radius + spread * 0.2f, shadowPaint)
        }
    }

    private fun drawMetadataFooter(
        context: Context,
        canvas: Canvas,
        cw: Int,
        centerY: Float,
        refDim: Float,
        exif: ExifData,
        config: FrameConfig
    ) {
        val brandRes = exif.brand.iconRes
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }

        val titleSize = refDim * 0.032f
        val paramsSize = refDim * 0.022f

        val lineSpacing = titleSize * 0.65f

        // Top line: Logo / Brand + Model
        val modelText = if (config.showModel) exif.model else ""
        val brandName = if (brandRes == null && config.showLogo) exif.brand.displayName else ""

        val hasLogo = config.showLogo && brandRes != null
        val topY = centerY - lineSpacing * 0.5f

        if (hasLogo) {
            val drawable = ContextCompat.getDrawable(context, brandRes!!)
            if (drawable != null) {
                val logoH = (titleSize * 1.15f).roundToInt()
                val aspect = drawable.intrinsicWidth.toFloat() / max(1, drawable.intrinsicHeight)
                val logoW = (logoH * aspect).roundToInt()

                if (modelText.isNotBlank()) {
                    textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    textPaint.textSize = titleSize
                    val modelW = textPaint.measureText(modelText)
                    val gap = refDim * 0.02f
                    val totalW = logoW + gap + modelW

                    val startX = (cw - totalW) / 2f
                    val logoTop = (topY - logoH * 0.75f).roundToInt()

                    drawable.setBounds(startX.roundToInt(), logoTop, (startX + logoW).roundToInt(), logoTop + logoH)
                    drawable.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                    drawable.draw(canvas)

                    textPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(modelText, startX + logoW + gap, topY, textPaint)
                } else {
                    val startX = (cw - logoW) / 2f
                    val logoTop = (topY - logoH * 0.75f).roundToInt()
                    drawable.setBounds(startX.roundToInt(), logoTop, (startX + logoW).roundToInt(), logoTop + logoH)
                    drawable.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                    drawable.draw(canvas)
                }
            }
        } else if (brandName.isNotBlank() || modelText.isNotBlank()) {
            textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textPaint.textSize = titleSize
            textPaint.textAlign = Paint.Align.CENTER
            val combined = listOf(brandName, modelText).filter { it.isNotBlank() }.joinToString("  ")
            canvas.drawText(combined, cw / 2f, topY, textPaint)
        }

        // Bottom line: Parameters
        if (config.showParams) {
            val bottomY = centerY + lineSpacing * 0.95f
            textPaint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            textPaint.textSize = paramsSize
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.color = Color.argb(235, 255, 255, 255)

            val paramsList = mutableListOf<String>()
            if (exif.formattedParams.isNotBlank()) {
                paramsList.add(exif.formattedParams)
            }
            if (config.showLens && exif.lens.isNotBlank()) {
                paramsList.add(exif.lens)
            }
            if (config.showDate && exif.dateTime.isNotBlank()) {
                paramsList.add(exif.dateTime)
            }

            val finalParams = paramsList.joinToString("   ")
            if (finalParams.isNotBlank()) {
                canvas.drawText(finalParams, cw / 2f, bottomY, textPaint)
            }
        }
    }

    fun saveToGallery(context: Context, bitmap: Bitmap, title: String): Uri? {
        val filename = "${title}_${System.currentTimeMillis()}.jpg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Framed")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                return uri
            }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Framed")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            return Uri.fromFile(file)
        }
        return null
    }

    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yi = 0
        yw = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            for (idx in -radius..radius) {
                p = pix[yi + min(wm, max(idx, 0))]
                sir = stack[idx + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - Math.abs(idx)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (idx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius

            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            for (idx in -radius..radius) {
                yi = max(0, yp) + x
                sir = stack[idx + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(idx)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (idx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (idx < hm) {
                    yp += w
                }
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = (0xff000000.toInt() and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = min(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
                y++
            }
            x++
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
