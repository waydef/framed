package space.ogurecs.framed.util

import android.content.Context
import space.ogurecs.framed.model.CanvasRatio
import space.ogurecs.framed.model.CustomFontWeight
import space.ogurecs.framed.model.FontOption
import space.ogurecs.framed.model.FrameConfig
import space.ogurecs.framed.model.LogoColorMode
import space.ogurecs.framed.model.TextAlignment

object ConfigStorage {
    private const val PREFS_NAME = "framed_prefs"

    fun saveConfig(context: Context, config: FrameConfig) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .putString("ratio", config.ratio.name)
            .putFloat("cornerRadius", config.cornerRadius)
            .putFloat("shadowRadius", config.shadowRadius)
            .putFloat("shadowAlpha", config.shadowAlpha)
            .putFloat("shadowOffsetY", config.shadowOffsetY)
            .putFloat("blurRadius", config.blurRadius)
            .putFloat("blurDimming", config.blurDimming)
            .putFloat("photoScale", config.photoScale)
            .putBoolean("showLogo", config.showLogo)
            .putBoolean("showModel", config.showModel)
            .putBoolean("showParams", config.showParams)
            .putBoolean("showLens", config.showLens)
            .putBoolean("showDate", config.showDate)
            .putBoolean("separateExtraLine", config.separateExtraLine)
            .putString("logoColorMode", config.logoColorMode.name)
            .putString("fontOption", config.fontOption.name)
            .putString("fontWeight", config.fontWeight.name)
            .putString("textAlignment", config.textAlignment.name)
            .putFloat("logoGap", config.logoGap)
            .putFloat("lineSpacing", config.lineSpacing)
            .putFloat("footerVerticalOffset", config.footerVerticalOffset)
            .putFloat("letterSpacing", config.letterSpacing)
            .putFloat("fontSizeLine1", config.fontSizeLine1)
            .putFloat("fontSizeLine2", config.fontSizeLine2)
            .putFloat("logoOffsetY", config.logoOffsetY)
            .putFloat("shadowSpread", config.shadowSpread)
            .putFloat("textHorizontalOffset", config.textHorizontalOffset)
            .putFloat("textMasterScale", config.textMasterScale)
            .putFloat("logoScale", config.logoScale)
            .apply()
    }

    fun loadConfig(context: Context): FrameConfig {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val default = FrameConfig()
        return FrameConfig(
            ratio = sp.getString("ratio", null)?.let { runCatching { CanvasRatio.valueOf(it) }.getOrNull() } ?: default.ratio,
            cornerRadius = sp.getFloat("cornerRadius", default.cornerRadius),
            shadowRadius = sp.getFloat("shadowRadius", default.shadowRadius),
            shadowAlpha = sp.getFloat("shadowAlpha", default.shadowAlpha),
            shadowOffsetY = sp.getFloat("shadowOffsetY", default.shadowOffsetY),
            blurRadius = sp.getFloat("blurRadius", default.blurRadius),
            blurDimming = sp.getFloat("blurDimming", default.blurDimming),
            photoScale = sp.getFloat("photoScale", default.photoScale),
            showLogo = sp.getBoolean("showLogo", default.showLogo),
            showModel = sp.getBoolean("showModel", default.showModel),
            showParams = sp.getBoolean("showParams", default.showParams),
            showLens = sp.getBoolean("showLens", default.showLens),
            showDate = sp.getBoolean("showDate", default.showDate),
            separateExtraLine = sp.getBoolean("separateExtraLine", default.separateExtraLine),
            logoColorMode = sp.getString("logoColorMode", null)?.let { runCatching { LogoColorMode.valueOf(it) }.getOrNull() } ?: default.logoColorMode,
            fontOption = sp.getString("fontOption", null)?.let { runCatching { FontOption.valueOf(it) }.getOrNull() } ?: default.fontOption,
            fontWeight = sp.getString("fontWeight", null)?.let { runCatching { CustomFontWeight.valueOf(it) }.getOrNull() } ?: default.fontWeight,
            textAlignment = sp.getString("textAlignment", null)?.let { runCatching { TextAlignment.valueOf(it) }.getOrNull() } ?: default.textAlignment,
            logoGap = sp.getFloat("logoGap", default.logoGap),
            lineSpacing = sp.getFloat("lineSpacing", default.lineSpacing),
            footerVerticalOffset = sp.getFloat("footerVerticalOffset", default.footerVerticalOffset),
            letterSpacing = sp.getFloat("letterSpacing", default.letterSpacing),
            fontSizeLine1 = sp.getFloat("fontSizeLine1", default.fontSizeLine1),
            fontSizeLine2 = sp.getFloat("fontSizeLine2", default.fontSizeLine2),
            logoOffsetY = sp.getFloat("logoOffsetY", default.logoOffsetY),
            shadowSpread = sp.getFloat("shadowSpread", default.shadowSpread),
            textHorizontalOffset = sp.getFloat("textHorizontalOffset", default.textHorizontalOffset),
            textMasterScale = sp.getFloat("textMasterScale", default.textMasterScale),
            logoScale = sp.getFloat("logoScale", default.logoScale)
        )
    }
}
