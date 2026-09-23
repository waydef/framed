package space.ogurecs.framed.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test
import space.ogurecs.framed.model.CameraBrand
import space.ogurecs.framed.model.CanvasRatio
import space.ogurecs.framed.model.ExifData
import space.ogurecs.framed.model.ExportQuality
import space.ogurecs.framed.model.FrameConfig

class FramedModelTest {

    @Test
    fun testCameraBrandDetection() {
        assertEquals(CameraBrand.SONY_ALPHA, CameraBrand.detect("Sony", "ILCE-6600"))
        assertEquals(CameraBrand.SONY_ALPHA, CameraBrand.detect("Sony ILCE-6600"))
        assertEquals(CameraBrand.SONY, CameraBrand.detect("Sony DSC-RX100M7"))
        assertEquals(CameraBrand.CANON, CameraBrand.detect("Canon EOS R5"))
        assertEquals(CameraBrand.NIKON, CameraBrand.detect("NIKON CORPORATION"))
        assertEquals(CameraBrand.FUJIFILM, CameraBrand.detect("FUJIFILM X-T4"))
        assertEquals(CameraBrand.LEICA, CameraBrand.detect("Leica Camera AG"))
        assertEquals(CameraBrand.APPLE, CameraBrand.detect("Apple iPhone 15 Pro"))
    }

    @Test
    fun testFormattedParams() {
        val exif = ExifData(
            focalLength = "40mm",
            aperture = "F5.6",
            shutterSpeed = "1/4s",
            iso = "ISO200"
        )
        assertEquals("40mm  F5.6  1/4s  ISO200", exif.formattedParams)
    }

    @Test
    fun testFrameConfigDefaults() {
        val config = FrameConfig()
        assertEquals(34f, config.fontSizeLine1)
        assertEquals(22f, config.fontSizeLine2)
        assertEquals(0.89f, config.photoScale)
        assertEquals(1.0f, config.textMasterScale)
        assertEquals(1.0f, config.logoScale)
        assertEquals(0f, config.logoOffsetY)
        assertEquals(8f, config.shadowSpread)
        assertEquals(CanvasRatio.RATIO_3_4, config.ratio)
    }

    @Test
    fun testCameraModelNormalizer() {
        val normalizedSonyAlpha = space.ogurecs.framed.parser.CameraModelNormalizer.normalize(
            CameraBrand.SONY_ALPHA, "Sony", "ILCE-6600"
        )
        assertEquals("6600", normalizedSonyAlpha)

        val normalizedSonyClassic = space.ogurecs.framed.parser.CameraModelNormalizer.normalize(
            CameraBrand.SONY, "Sony", "ILCE-6600"
        )
        assertEquals("α6600", normalizedSonyClassic)

        val normalizedNikon = space.ogurecs.framed.parser.CameraModelNormalizer.normalize(
            CameraBrand.NIKON, "NIKON", "Z 6_2"
        )
        assertEquals("Z 6 II", normalizedNikon)

        val normalizedApple = space.ogurecs.framed.parser.CameraModelNormalizer.normalize(
            CameraBrand.APPLE, "Apple", "iPhone15,2"
        )
        assertEquals("iPhone 14 Pro", normalizedApple)
    }

    @Test
    fun testExportQualityBadges() {
        assertEquals("без потерь", ExportQuality.ORIGINAL_100.badge)
        assertEquals("1080p • чётко", ExportQuality.TIKTOK_OPTIMIZED.badge)
    }
}

