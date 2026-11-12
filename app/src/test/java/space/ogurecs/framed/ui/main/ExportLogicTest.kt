package space.ogurecs.framed.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import space.ogurecs.framed.model.CanvasRatio
import space.ogurecs.framed.model.ExifData
import space.ogurecs.framed.model.ExportQuality
import space.ogurecs.framed.model.FrameConfig
import space.ogurecs.framed.parser.ExifParser
import space.ogurecs.framed.render.FrameCompositor
import space.ogurecs.framed.util.ImageOrientation
import kotlin.math.max

class ExportLogicTest {

    @Test
    fun orientationTagsMapToRotationAndMirror() {
        assertTrue(ImageOrientation.transformFor(1).isIdentity)
        assertTrue(ImageOrientation.transformFor(0).isIdentity)
        assertTrue(ImageOrientation.transformFor(99).isIdentity)

        // portrait shots of a Sony a6600 carry 6 (turn right) or 8 (turn left)
        val right = ImageOrientation.transformFor(6)
        assertEquals(90, right.rotationDegrees)
        assertFalse(right.mirror)
        assertTrue(right.swapsSides)
        val left = ImageOrientation.transformFor(8)
        assertEquals(270, left.rotationDegrees)
        assertTrue(left.swapsSides)

        assertEquals(180, ImageOrientation.transformFor(3).rotationDegrees)
        assertFalse(ImageOrientation.transformFor(3).swapsSides)
        assertTrue(ImageOrientation.transformFor(2).mirror)
        // transpose = rotate right + mirror, transverse = rotate left + mirror
        assertEquals(90, ImageOrientation.transformFor(5).rotationDegrees)
        assertTrue(ImageOrientation.transformFor(5).mirror)
        assertEquals(270, ImageOrientation.transformFor(7).rotationDegrees)
        assertTrue(ImageOrientation.transformFor(7).mirror)
    }

    @Test
    fun exifDateKeepsTheClockWithColons() {
        assertEquals("2024.05.17 14:32", ExifParser.formatExifDate("2024:05:17 14:32:10"))
        assertEquals("", ExifParser.formatExifDate(null))
        assertEquals("not a date at al", ExifParser.formatExifDate("not a date at all really"))
    }

    // 24 MP landscape frame in a 9:16 canvas with the smallest photo scale: the case from the audit
    private val tallConfig = FrameConfig(ratio = CanvasRatio.RATIO_9_16, photoScale = 0.6f)

    @Test
    fun hugeCanvasIsPredictedBeforeAnythingIsAllocated() {
        val layout = FrameCompositor.computeLayout(6000, 4000, ExifData(), tallConfig)
        val pixels = layout.canvasW.toLong() * layout.canvasH
        assertTrue("canvas should be far above the budget, was $pixels", pixels > 100_000_000L)
    }

    @Test
    fun socialExportStaysInside2560() {
        val factor = FrameCompositor.exportSourceScale(6000, 4000, ExifData(), tallConfig, ExportQuality.TIKTOK_OPTIMIZED)
        assertTrue(factor < 1f)
        val w = (6000 * factor).toInt()
        val h = (4000 * factor).toInt()
        val layout = FrameCompositor.computeLayout(w, h, ExifData(), tallConfig)
        assertTrue("canvas side ${max(layout.canvasW, layout.canvasH)}", max(layout.canvasW, layout.canvasH) <= FrameCompositor.SOCIAL_MAX_SIDE + 2)
    }

    @Test
    fun originalExportStaysInsidePixelBudget() {
        val factor = FrameCompositor.exportSourceScale(6000, 4000, ExifData(), tallConfig, ExportQuality.ORIGINAL_100)
        assertTrue(factor < 1f)
        val layout = FrameCompositor.computeLayout((6000 * factor).toInt(), (4000 * factor).toInt(), ExifData(), tallConfig)
        val pixels = layout.canvasW.toLong() * layout.canvasH
        assertTrue("pixels $pixels", pixels <= FrameCompositor.ORIGINAL_MAX_PIXELS * 101 / 100)
    }

    @Test
    fun normalFrameIsNotShrunkForOriginalExport() {
        val factor = FrameCompositor.exportSourceScale(6000, 4000, ExifData(), FrameConfig(), ExportQuality.ORIGINAL_100)
        assertEquals(1f, factor, 0f)
    }
}
