package space.ogurecs.framed.render

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
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
import space.ogurecs.framed.model.ExportQuality
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
        val baseAspect = max(srcW, srcH) / max(1f, min(srcW, srcH))
        val refDim = min(srcW, srcH) * minOf(1.0f, baseAspect / 1.5f)

        val titleSize = refDim * (config.fontSizeLine1 / 1000f) * config.textMasterScale
        val paramsSize = refDim * (config.fontSizeLine2 / 1000f) * config.textMasterScale
        val lineSpacing = titleSize * (config.lineSpacing / 32f) * 0.65f

        val brandRes = exif.brand.iconRes
        val modelText = if (config.showModel) exif.model else ""
        val brandName = if (brandRes == null && config.showLogo) exif.brand.displayName else ""
        val hasLogo = config.showLogo && brandRes != null
        val hasLine1 = hasLogo || brandName.isNotBlank() || modelText.isNotBlank()

        val exposureText = if (config.showParams) exif.formattedParams else ""
        val extraList = mutableListOf<String>()
        if (config.showLens && exif.lens.isNotBlank()) extraList.add(exif.lens)
        if (config.showDate && exif.dateTime.isNotBlank()) extraList.add(exif.dateTime)
        val extraText = extraList.joinToString("   ")

        val hasExposure = exposureText.isNotBlank()
        val hasExtra = extraText.isNotBlank()

        val hasLine2: Boolean
        val line2Text: String
        val hasLine3: Boolean
        val line3Text: String

        if (config.separateExtraLine && hasExposure && hasExtra) {
            hasLine2 = true
            line2Text = exposureText
            hasLine3 = true
            line3Text = extraText
        } else {
            val combined = listOf(exposureText, extraText).filter { it.isNotBlank() }.joinToString("   |   ")
            hasLine2 = combined.isNotBlank()
            line2Text = combined
            hasLine3 = false
            line3Text = ""
        }

        val footerHeight = when {
            hasLine1 && hasLine2 && hasLine3 -> titleSize + lineSpacing * 1.8f + paramsSize * 1.8f
            hasLine1 && hasLine2 -> titleSize + lineSpacing + paramsSize
            hasLine1 -> titleSize
            hasLine2 -> paramsSize
            else -> 0f
        }

        val baseGap = if (footerHeight > 0f) refDim * 0.045f else 0f
        val gapBelowPhoto = if (footerHeight > 0f) max(0f, baseGap + refDim * (config.footerVerticalOffset / 200f)) else 0f

        val totalContentW = srcW
        val totalContentH = srcH + gapBelowPhoto + footerHeight

        val targetRatio = when (config.ratio) {
            CanvasRatio.RATIO_4_5 -> 4f / 5f
            CanvasRatio.RATIO_9_16 -> 9f / 16f
            CanvasRatio.RATIO_1_1 -> 1f
            CanvasRatio.RATIO_3_4 -> 3f / 4f
            CanvasRatio.RATIO_16_9 -> 16f / 9f
            CanvasRatio.ORIGINAL -> totalContentW / totalContentH
        }

        val scale = config.photoScale.coerceIn(0.6f, 0.95f)
        val slotW = totalContentW / scale
        val slotH = totalContentH / scale

        val canvasW: Int
        val canvasH: Int

        if ((slotW / slotH) > targetRatio) {
            canvasW = slotW.roundToInt()
            canvasH = (slotW / targetRatio).roundToInt()
        } else {
            canvasH = slotH.roundToInt()
            canvasW = (slotH * targetRatio).roundToInt()
        }

        val output = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Draw blurred background
        drawBlurredBackground(canvas, source, canvasW, canvasH, config)

        // 2. Position the main photo and footer with pure vertical and horizontal symmetry
        val photoLeft = (canvasW - srcW) / 2f
        val contentTop = (canvasH - totalContentH) / 2f
        val photoTop = contentTop
        val photoRect = RectF(photoLeft, photoTop, photoLeft + srcW, photoTop + srcH)

        val cornerPx = (config.cornerRadius / 1000f) * refDim
        val blurPx = (config.shadowRadius / 1000f) * refDim
        val spreadPx = (config.shadowSpread / 1000f) * refDim

        // 3. Draw soft ambient shadow with real Gaussian blur
        drawSoftShadow(canvas, photoRect, cornerPx, blurPx, spreadPx, config.shadowAlpha, config.shadowOffsetY)

        // 4. Draw rounded photo
        val clipPath = Path().apply {
            addRoundRect(photoRect, cornerPx, cornerPx, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(source, null, photoRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        canvas.restore()

        // 5. Draw Footer / Metadata block with consistent gap and optical baseline alignment
        if (footerHeight > 0f) {
            val footerCenterY = photoRect.bottom + gapBelowPhoto + (footerHeight / 2f)
            drawMetadataFooter(
                context = context,
                canvas = canvas,
                cw = canvasW,
                photoRect = photoRect,
                centerY = footerCenterY,
                refDim = refDim,
                titleSize = titleSize,
                paramsSize = paramsSize,
                lineSpacing = lineSpacing,
                hasLine1 = hasLine1,
                hasLine2 = hasLine2,
                hasLine3 = hasLine3,
                hasLogo = hasLogo,
                brandRes = brandRes,
                brandName = brandName,
                modelText = modelText,
                line2Text = line2Text,
                line3Text = line3Text,
                config = config,
                exif = exif
            )
        }

        return output
    }

    private fun drawBlurredBackground(
        canvas: Canvas,
        source: Bitmap,
        cw: Int,
        ch: Int,
        config: FrameConfig
    ) {
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
        cornerPx: Float,
        blurPx: Float,
        spreadPx: Float,
        alphaFactor: Float,
        offsetYPercent: Float
    ) {
        if (alphaFactor <= 0.01f || (blurPx <= 0.5f && spreadPx <= 0f)) return

        val alphaInt = (alphaFactor.coerceIn(0f, 1f) * 255).roundToInt()
        val offsetY = (offsetYPercent / 100f) * (blurPx + spreadPx) * 0.5f

        val shadowRect = RectF(
            rect.left - spreadPx,
            rect.top - spreadPx + offsetY,
            rect.right + spreadPx,
            rect.bottom + spreadPx + offsetY
        )

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(alphaInt, 0, 0, 0)
            style = Paint.Style.FILL
            if (blurPx > 0.5f) {
                maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
        }

        val shadowCorner = cornerPx + spreadPx * 0.5f
        canvas.drawRoundRect(shadowRect, shadowCorner, shadowCorner, shadowPaint)
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
        titleSize: Float,
        paramsSize: Float,
        lineSpacing: Float,
        hasLine1: Boolean,
        hasLine2: Boolean,
        hasLine3: Boolean,
        hasLogo: Boolean,
        brandRes: Int?,
        brandName: String,
        modelText: String,
        line2Text: String,
        line3Text: String,
        config: FrameConfig,
        exif: ExifData
    ) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            letterSpacing = config.letterSpacing
        }

        val logoGap = refDim * (config.logoGap / 1000f)
        val logoYOffset = refDim * (config.logoOffsetY / 1000f)
        val hOffset = refDim * (config.textHorizontalOffset / 1000f)

        // Baseline positioning for 2-line layout
        val topY = if (hasLine1 && hasLine2) centerY - lineSpacing * 0.55f else centerY
        val bottomY = if (hasLine1 && hasLine2) centerY + lineSpacing * 0.75f else centerY

        // Measure optical cap-height of uppercase title font for baseline alignment
        textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
        textPaint.textSize = titleSize
        val capBounds = Rect()
        textPaint.getTextBounds("H", 0, 1, capBounds)
        val capHeight = max(1f, capBounds.height().toFloat())
        val capCenterY = topY - (capHeight / 2f)

        val paramsWeight = if (config.fontWeight == space.ogurecs.framed.model.CustomFontWeight.BOLD) {
            space.ogurecs.framed.model.CustomFontWeight.MEDIUM
        } else {
            space.ogurecs.framed.model.CustomFontWeight.REGULAR
        }

        when (config.textAlignment) {
            space.ogurecs.framed.model.TextAlignment.SPLIT -> {
                // In SPLIT mode, left side has brand/logo + model, right side has params
                // Vertical alignment must match baseline splitBaselineY
                val splitBaselineY = if (hasLine3) centerY - lineSpacing * 0.40f else centerY
                val splitCapCenterY = splitBaselineY - (capHeight / 2f)

                var currentX = photoRect.left + hOffset
                if (hasLine1) {
                    if (hasLogo && brandRes != null) {
                        val drawable = ContextCompat.getDrawable(context, brandRes)
                        if (drawable != null) {
                            applyLogoColorFilter(drawable, config.logoColorMode, exif.brand)
                            val logoH = (capHeight * 1.05f * config.logoScale).roundToInt()
                            val aspect = drawable.intrinsicWidth.toFloat() / max(1, drawable.intrinsicHeight)
                            val logoW = (logoH * aspect).roundToInt()
                            val logoTop = (splitCapCenterY - logoH / 2f + logoYOffset).roundToInt()
                            drawable.setBounds(currentX.roundToInt(), logoTop, (currentX + logoW).roundToInt(), logoTop + logoH)
                            drawable.draw(canvas)
                            currentX += logoW + logoGap
                        }
                    } else if (brandName.isNotBlank()) {
                        textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                        textPaint.textSize = titleSize
                        textPaint.textAlign = Paint.Align.LEFT
                        canvas.drawText(brandName, currentX, splitBaselineY, textPaint)
                        currentX += textPaint.measureText(brandName) + logoGap
                    }

                    if (modelText.isNotBlank()) {
                        textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                        textPaint.textSize = titleSize
                        textPaint.textAlign = Paint.Align.LEFT
                        canvas.drawText(modelText, currentX, splitBaselineY, textPaint)
                    }
                }

                if (hasLine2) {
                    textPaint.typeface = getTypeface(context, config.fontOption, paramsWeight)
                    textPaint.textSize = paramsSize
                    textPaint.textAlign = Paint.Align.RIGHT
                    textPaint.color = Color.argb(235, 255, 255, 255)
                    canvas.drawText(line2Text.trim(), photoRect.right + hOffset, splitBaselineY, textPaint)
                }

                if (hasLine3) {
                    val line3Y = splitBaselineY + lineSpacing * 0.85f
                    textPaint.typeface = getTypeface(context, config.fontOption, paramsWeight)
                    textPaint.textSize = paramsSize * 0.95f
                    textPaint.textAlign = Paint.Align.RIGHT
                    textPaint.color = Color.argb(200, 255, 255, 255)
                    canvas.drawText(line3Text.trim(), photoRect.right + hOffset, line3Y, textPaint)
                }
            }
            space.ogurecs.framed.model.TextAlignment.LEFT -> {
                val line1Y = when {
                    hasLine1 && hasLine2 && hasLine3 -> centerY - lineSpacing * 0.85f
                    hasLine1 && hasLine2 -> topY
                    else -> centerY
                }
                val line1CapCenterY = line1Y - (capHeight / 2f)

                var currentX = photoRect.left + hOffset
                if (hasLine1) {
                    if (hasLogo && brandRes != null) {
                        val drawable = ContextCompat.getDrawable(context, brandRes)
                        if (drawable != null) {
                            applyLogoColorFilter(drawable, config.logoColorMode, exif.brand)
                            val logoH = (capHeight * 1.05f * config.logoScale).roundToInt()
                            val aspect = drawable.intrinsicWidth.toFloat() / max(1, drawable.intrinsicHeight)
                            val logoW = (logoH * aspect).roundToInt()
                            val logoTop = (line1CapCenterY - logoH / 2f + logoYOffset).roundToInt()
                            drawable.setBounds(currentX.roundToInt(), logoTop, (currentX + logoW).roundToInt(), logoTop + logoH)
                            drawable.draw(canvas)
                            currentX += logoW + logoGap
                        }
                    } else if (brandName.isNotBlank()) {
                        textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                        textPaint.textSize = titleSize
                        textPaint.textAlign = Paint.Align.LEFT
                        canvas.drawText(brandName, currentX, line1Y, textPaint)
                        currentX += textPaint.measureText(brandName) + logoGap
                    }

                    if (modelText.isNotBlank()) {
                        textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                        textPaint.textSize = titleSize
                        textPaint.textAlign = Paint.Align.LEFT
                        canvas.drawText(modelText, currentX, line1Y, textPaint)
                    }
                }

                if (hasLine2) {
                    val line2Y = when {
                        hasLine1 && hasLine2 && hasLine3 -> centerY + lineSpacing * 0.15f
                        hasLine1 -> bottomY
                        else -> centerY
                    }
                    textPaint.typeface = getTypeface(context, config.fontOption, paramsWeight)
                    textPaint.textSize = paramsSize
                    textPaint.textAlign = Paint.Align.LEFT
                    textPaint.color = Color.argb(235, 255, 255, 255)
                    canvas.drawText(line2Text, photoRect.left + hOffset, line2Y, textPaint)
                }

                if (hasLine3) {
                    val line3Y = centerY + lineSpacing * 1.05f
                    textPaint.typeface = getTypeface(context, config.fontOption, paramsWeight)
                    textPaint.textSize = paramsSize * 0.92f
                    textPaint.textAlign = Paint.Align.LEFT
                    textPaint.color = Color.argb(200, 255, 255, 255)
                    canvas.drawText(line3Text, photoRect.left + hOffset, line3Y, textPaint)
                }
            }
            space.ogurecs.framed.model.TextAlignment.CENTER -> {
                val line1Y = when {
                    hasLine1 && hasLine2 && hasLine3 -> centerY - lineSpacing * 0.85f
                    hasLine1 && hasLine2 -> topY
                    else -> centerY
                }
                val line1CapCenterY = line1Y - (capHeight / 2f)

                if (hasLine1) {
                    if (hasLogo && brandRes != null) {
                        val drawable = ContextCompat.getDrawable(context, brandRes)
                        if (drawable != null) {
                            applyLogoColorFilter(drawable, config.logoColorMode, exif.brand)
                            val logoH = (capHeight * 1.05f * config.logoScale).roundToInt()
                            val aspect = drawable.intrinsicWidth.toFloat() / max(1, drawable.intrinsicHeight)
                            val logoW = (logoH * aspect).roundToInt()
                            val logoTop = (line1CapCenterY - logoH / 2f + logoYOffset).roundToInt()

                            if (modelText.isNotBlank()) {
                                textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                                textPaint.textSize = titleSize
                                val modelW = textPaint.measureText(modelText)
                                val totalW = logoW + logoGap + modelW

                                val startX = (cw - totalW) / 2f + hOffset
                                drawable.setBounds(startX.roundToInt(), logoTop, (startX + logoW).roundToInt(), logoTop + logoH)
                                drawable.draw(canvas)

                                textPaint.textAlign = Paint.Align.LEFT
                                canvas.drawText(modelText, startX + logoW + logoGap, line1Y, textPaint)
                            } else {
                                val startX = (cw - logoW) / 2f + hOffset
                                drawable.setBounds(startX.roundToInt(), logoTop, (startX + logoW).roundToInt(), logoTop + logoH)
                                drawable.draw(canvas)
                            }
                        }
                    } else if (brandName.isNotBlank() || modelText.isNotBlank()) {
                        textPaint.typeface = getTypeface(context, config.fontOption, config.fontWeight)
                        textPaint.textSize = titleSize
                        textPaint.textAlign = Paint.Align.CENTER
                        val combined = listOf(brandName, modelText).filter { it.isNotBlank() }.joinToString("  ")
                        canvas.drawText(combined, cw / 2f + hOffset, line1Y, textPaint)
                    }
                }

                if (hasLine2) {
                    val line2Y = when {
                        hasLine1 && hasLine2 && hasLine3 -> centerY + lineSpacing * 0.15f
                        hasLine1 -> bottomY
                        else -> centerY
                    }
                    textPaint.typeface = getTypeface(context, config.fontOption, paramsWeight)
                    textPaint.textSize = paramsSize
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.color = Color.argb(235, 255, 255, 255)
                    canvas.drawText(line2Text, cw / 2f + hOffset, line2Y, textPaint)
                }

                if (hasLine3) {
                    val line3Y = centerY + lineSpacing * 1.05f
                    textPaint.typeface = getTypeface(context, config.fontOption, paramsWeight)
                    textPaint.textSize = paramsSize * 0.92f
                    textPaint.textAlign = Paint.Align.CENTER
                    textPaint.color = Color.argb(200, 255, 255, 255)
                    canvas.drawText(line3Text, cw / 2f + hOffset, line3Y, textPaint)
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

    fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        title: String,
        quality: ExportQuality = ExportQuality.ORIGINAL_100
    ): Uri? {
        val exportBitmap: Bitmap
        val compressQuality: Int
        val prefix: String

        if (quality == ExportQuality.TIKTOK_OPTIMIZED) {
            prefix = "framed_tiktok"
            compressQuality = 97

            // TikTok photo mode optimal screen dimensions (1080p width, max 1920p height)
            val origW = bitmap.width
            val origH = bitmap.height
            val scale = min(1.0f, min(1080f / origW, 1920f / origH))
            val targetW = max(1, (origW * scale).roundToInt())
            val targetH = max(1, (origH * scale).roundToInt())

            val scaled = if (targetW != origW || targetH != origH) {
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else {
                bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            }

            // Restore micro-contrast and typographic sharpness so TikTok ingestion doesn't blur
            val sharpened = sharpenForWeb(scaled)
            if (scaled != bitmap) scaled.recycle()
            exportBitmap = sharpened
        } else {
            prefix = "framed_original"
            compressQuality = 100
            exportBitmap = bitmap
        }

        val filename = "${prefix}_${title}_${System.currentTimeMillis()}.jpg"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Framed")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val insertUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (insertUri != null) {
                    resolver.openOutputStream(insertUri)?.use { stream ->
                        exportBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, stream)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(insertUri, contentValues, null, null)
                    insertUri
                } else null
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Framed")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { out ->
                    exportBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, out)
                }
                Uri.fromFile(file)
            }
        } finally {
            if (exportBitmap != bitmap) {
                exportBitmap.recycle()
            }
        }
    }

    private fun sharpenForWeb(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val outPixels = IntArray(w * h)

        val cW = 1.30f
        val nW = -0.075f

        for (y in 0 until h) {
            val yOffset = y * w
            val yPrev = max(0, y - 1) * w
            val yNext = min(h - 1, y + 1) * w

            for (x in 0 until w) {
                val xPrev = max(0, x - 1)
                val xNext = min(w - 1, x + 1)

                val pC = pixels[yOffset + x]
                val pT = pixels[yPrev + x]
                val pB = pixels[yNext + x]
                val pL = pixels[yOffset + xPrev]
                val pR = pixels[yOffset + xNext]

                val a = (pC ushr 24) and 0xFF

                val rC = (pC ushr 16) and 0xFF
                val rT = (pT ushr 16) and 0xFF
                val rB = (pB ushr 16) and 0xFF
                val rL = (pL ushr 16) and 0xFF
                val rR = (pR ushr 16) and 0xFF
                val nR = (rC * cW + (rT + rB + rL + rR) * nW).roundToInt().coerceIn(0, 255)

                val gC = (pC ushr 8) and 0xFF
                val gT = (pT ushr 8) and 0xFF
                val gB = (pB ushr 8) and 0xFF
                val gL = (pL ushr 8) and 0xFF
                val gR = (pR ushr 8) and 0xFF
                val nG = (gC * cW + (gT + gB + gL + gR) * nW).roundToInt().coerceIn(0, 255)

                val bC = pC and 0xFF
                val bT = pT and 0xFF
                val bB = pB and 0xFF
                val bL = pL and 0xFF
                val bR = pR and 0xFF
                val nB = (bC * cW + (bT + bB + bL + bR) * nW).roundToInt().coerceIn(0, 255)

                outPixels[yOffset + x] = (a shl 24) or (nR shl 16) or (nG shl 8) or nB
            }
        }
        output.setPixels(outPixels, 0, w, 0, 0, w, h)
        return output
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
