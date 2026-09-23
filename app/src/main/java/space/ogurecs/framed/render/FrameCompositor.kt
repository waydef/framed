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

        // 3. Draw soft ambient shadow (symmetrical by default)
        drawSoftShadow(canvas, photoRect, cornerPx, shadowPx, config.shadowAlpha, config.shadowOffsetY)

        // 4. Draw rounded photo
        val clipPath = Path().apply {
            addRoundRect(photoRect, cornerPx, cornerPx, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(source, null, photoRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        canvas.restore()

        // 5. Draw Footer / Metadata block with custom vertical offset
        val baseRatio = 0.46f + (config.footerVerticalOffset / 100f).coerceIn(-0.3f, 0.3f)
        val footerCenterY = photoRect.bottom + (canvasH - photoRect.bottom) * baseRatio
        drawMetadataFooter(context, canvas, canvasW, photoRect, footerCenterY, referenceDim, exif, config)

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
        alphaFactor: Float,
        offsetYPercent: Float
    ) {
        if (blurSize <= 0f || alphaFactor <= 0f) return

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val steps = 6
        val stepAlpha = (alphaFactor * 255f / steps).roundToInt()

        for (i in steps downTo 1) {
            val progress = i.toFloat() / steps
            val spread = blurSize * progress
            // 0 offset by default gives perfectly symmetrical ambient glow
            val offsetY = (offsetYPercent / 100f) * blurSize * 0.5f * progress
            shadowPaint.color = Color.argb((stepAlpha * 0.7f).roundToInt(), 0, 0, 0)

            val shadowRect = RectF(
                rect.left - spread,
                rect.top - spread + offsetY,
                rect.right + spread,
                rect.bottom + spread + offsetY
            )
            canvas.drawRoundRect(shadowRect, radius + spread * 0.25f, radius + spread * 0.25f, shadowPaint)
        }
    }

    private val typefaceCache = mutableMapOf<String, Typeface>()

    private fun getTypeface(context: Context, option: space.ogurecs.framed.model.FontOption, weight: space.ogurecs.framed.model.CustomFontWeight): Typeface {
        val key = "${option.name}_${weight.name}"
        return typefaceCache.getOrPut(key) {
            try {
                if (option.assetPath != null) {
                    val base = Typeface.createFromAsset(context.assets, option.assetPath)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        Typeface.create(base, weight.weightValue, false)
                    } else {
                        val style = if (weight.weightValue >= 600) Typeface.BOLD else Typeface.NORMAL
                        Typeface.create(base, style)
                    }
                } else {
                    val style = if (weight.weightValue >= 600) Typeface.BOLD else Typeface.NORMAL
                    Typeface.create(Typeface.SANS_SERIF, style)
                }
            } catch (e: Exception) {
                Typeface.DEFAULT
            }
        }
    }

    private fun drawMetadataFooter(
        context: Context,
        canvas: Canvas,
        cw: Int,
        photoRect: RectF,
        centerY: Float,
        refDim: Float,
        exif: ExifData,
        config: FrameConfig
    ) {
        val brandRes = exif.brand.iconRes
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            letterSpacing = config.letterSpacing
        }

        val titleSize = refDim * 0.032f
        val paramsSize = refDim * 0.022f

        val lineSpacing = titleSize * (config.lineSpacing / 32f) * 0.65f
        val logoGap = refDim * (config.logoGap / 1000f)

        val modelText = if (config.showModel) exif.model else ""
        val brandName = if (brandRes == null && config.showLogo) exif.brand.displayName else ""
        val hasLogo = config.showLogo && brandRes != null

        // Bottom line params preparation
        val paramsList = mutableListOf<String>()
        if (config.showParams) {
            if (exif.formattedParams.isNotBlank()) paramsList.add(exif.formattedParams)
            if (config.showLens && exif.lens.isNotBlank()) paramsList.add(exif.lens)
            if (config.showDate && exif.dateTime.isNotBlank()) paramsList.add(exif.dateTime)
        }
        val paramsText = paramsList.joinToString("   ")

        when (config.textAlignment) {
            space.ogurecs.framed.model.TextAlignment.SPLIT -> {
                // Split: Brand/Model on Left, Params on Right (both on centerY line!)
                val startX = photoRect.left
                val endX = photoRect.right

                var currentX = startX
                if (hasLogo) {
                    val drawable = ContextCompat.getDrawable(context, brandRes)
                    if (drawable != null) {
                        applyLogoColorFilter(drawable, config.logoColorMode, exif.brand)
                        val logoH = (titleSize * 1.15f).roundToInt()
                        val aspect = drawable.intrinsicWidth.toFloat() / max(1, drawable.intrinsicHeight)
                        val logoW = (logoH * aspect).roundToInt()
                        val logoTop = (centerY - logoH * 0.75f).roundToInt()
                        drawable.setBounds(currentX.roundToInt(), logoTop, (currentX + logoW).roundToInt(), logoTop + logoH)
                        drawable.draw(canvas)
                        currentX += logoW + logoGap
                    }
                } else if (brandName.isNotBlank()) {
                    textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                    textPaint.textSize = titleSize
                    textPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(brandName, currentX, centerY, textPaint)
                    currentX += textPaint.measureText(brandName) + logoGap
                }

                if (modelText.isNotBlank()) {
                    textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                    textPaint.textSize = titleSize
                    textPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(modelText, currentX, centerY, textPaint)
                }

                if (paramsText.isNotBlank()) {
                    val paramsWeight = if (config.fontWeight == space.ogurecs.framed.model.CustomFontWeight.BOLD) {
                        space.ogurecs.framed.model.CustomFontWeight.MEDIUM
                    } else {
                        space.ogurecs.framed.model.CustomFontWeight.REGULAR
                    }
                    textPaint.typeface = getTypeface(context, config.fontOption, paramsWeight)
                    textPaint.textSize = paramsSize
                    textPaint.textAlign = Paint.Align.RIGHT
                    textPaint.color = Color.argb(235, 255, 255, 255)
                    canvas.drawText(paramsText, endX, centerY, textPaint)
                }
            }
            space.ogurecs.framed.model.TextAlignment.LEFT -> {
                // Left aligned: flush with photo's left border
                val startX = photoRect.left
                val topY = if (paramsText.isNotBlank()) centerY - lineSpacing * 0.5f else centerY
                val bottomY = centerY + lineSpacing * 0.95f

                var currentX = startX
                if (hasLogo) {
                    val drawable = ContextCompat.getDrawable(context, brandRes)
                    if (drawable != null) {
                        applyLogoColorFilter(drawable, config.logoColorMode, exif.brand)
                        val logoH = (titleSize * 1.15f).roundToInt()
                        val aspect = drawable.intrinsicWidth.toFloat() / max(1, drawable.intrinsicHeight)
                        val logoW = (logoH * aspect).roundToInt()
                        val logoTop = (topY - logoH * 0.75f).roundToInt()
                        drawable.setBounds(currentX.roundToInt(), logoTop, (currentX + logoW).roundToInt(), logoTop + logoH)
                        drawable.draw(canvas)
                        currentX += logoW + logoGap
                    }
                } else if (brandName.isNotBlank()) {
                    textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                    textPaint.textSize = titleSize
                    textPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(brandName, currentX, topY, textPaint)
                    currentX += textPaint.measureText(brandName) + logoGap
                }

                if (modelText.isNotBlank()) {
                    textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                    textPaint.textSize = titleSize
                    textPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText(modelText, currentX, topY, textPaint)
                }

                if (paramsText.isNotBlank()) {
                    val paramsWeight = if (config.fontWeight == space.ogurecs.framed.model.CustomFontWeight.BOLD) {
                        space.ogurecs.framed.model.CustomFontWeight.MEDIUM
                    } else {
                        space.ogurecs.framed.model.CustomFontWeight.REGULAR
                    }
                    textPaint.typeface = getTypeface(context, config.fontOption, paramsWeight)
                    textPaint.textSize = paramsSize
                    textPaint.textAlign = Paint.Align.LEFT
                    textPaint.color = Color.argb(235, 255, 255, 255)
                    canvas.drawText(paramsText, startX, bottomY, textPaint)
                }
            }
            space.ogurecs.framed.model.TextAlignment.CENTER -> {
                // Centered
                val topY = if (paramsText.isNotBlank()) centerY - lineSpacing * 0.5f else centerY
                val bottomY = centerY + lineSpacing * 0.95f

                if (hasLogo) {
                    val drawable = ContextCompat.getDrawable(context, brandRes)
                    if (drawable != null) {
                        applyLogoColorFilter(drawable, config.logoColorMode, exif.brand)
                        val logoH = (titleSize * 1.15f).roundToInt()
                        val aspect = drawable.intrinsicWidth.toFloat() / max(1, drawable.intrinsicHeight)
                        val logoW = (logoH * aspect).roundToInt()

                        if (modelText.isNotBlank()) {
                            textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                            textPaint.textSize = titleSize
                            val modelW = textPaint.measureText(modelText)
                            val totalW = logoW + logoGap + modelW

                            val startX = (cw - totalW) / 2f
                            val logoTop = (topY - logoH * 0.75f).roundToInt()

                            drawable.setBounds(startX.roundToInt(), logoTop, (startX + logoW).roundToInt(), logoTop + logoH)
                            drawable.draw(canvas)

                            textPaint.textAlign = Paint.Align.LEFT
                            canvas.drawText(modelText, startX + logoW + logoGap, topY, textPaint)
                        } else {
                            val startX = (cw - logoW) / 2f
                            val logoTop = (topY - logoH * 0.75f).roundToInt()
                            drawable.setBounds(startX.roundToInt(), logoTop, (startX + logoW).roundToInt(), logoTop + logoH)
                            drawable.draw(canvas)
                        }
                    }
                } else if (brandName.isNotBlank() || modelText.isNotBlank()) {
                    textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                    textPaint.textSize = titleSize
                    textPaint.textAlign = Paint.Align.CENTER
                    val combined = listOf(brandName, modelText).filter { it.isNotBlank() }.joinToString("  ")
                    canvas.drawText(combined, cw / 2f, topY, textPaint)
                }

                if (paramsText.isNotBlank()) {
                    val paramsWeight = if (config.fontWeight == space.ogurecs.framed.model.CustomFontWeight.BOLD) {
                        space.ogurecs.framed.model.CustomFontWeight.MEDIUM
                    } else {
                        space.ogurecs.framed.model.CustomFontWeight.REGULAR
                    }
                    textPaint.typeface = getTypeface(context, config.fontOption, paramsWeight)
                    textPaint.textSize = paramsSize
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.color = Color.argb(235, 255, 255, 255)
                    canvas.drawText(paramsText, cw / 2f, bottomY, textPaint)
                }
            }
        }
    }

    private fun applyLogoColorFilter(drawable: android.graphics.drawable.Drawable, mode: space.ogurecs.framed.model.LogoColorMode, brand: space.ogurecs.framed.model.CameraBrand) {
        val logoColor: Int? = when (mode) {
            space.ogurecs.framed.model.LogoColorMode.WHITE -> Color.WHITE
            space.ogurecs.framed.model.LogoColorMode.BLACK -> Color.BLACK
            space.ogurecs.framed.model.LogoColorMode.MATCH_TEXT -> Color.WHITE
            space.ogurecs.framed.model.LogoColorMode.BRAND -> when (brand) {
                space.ogurecs.framed.model.CameraBrand.CANON -> Color.parseColor("#CC0000")
                space.ogurecs.framed.model.CameraBrand.SONY -> Color.parseColor("#FF6600")
                space.ogurecs.framed.model.CameraBrand.NIKON -> Color.parseColor("#FFE100")
                space.ogurecs.framed.model.CameraBrand.LUMIX -> Color.parseColor("#E60012")
                space.ogurecs.framed.model.CameraBrand.LEICA -> null
                space.ogurecs.framed.model.CameraBrand.FUJIFILM -> null
                else -> Color.WHITE
            }
        }

        if (logoColor != null) {
            drawable.colorFilter = PorterDuffColorFilter(logoColor, PorterDuff.Mode.SRC_IN)
        } else {
            drawable.clearColorFilter()
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
