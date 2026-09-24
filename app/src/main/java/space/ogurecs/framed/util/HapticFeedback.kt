package space.ogurecs.framed.util

import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants

object HapticFeedback {
    fun tick(context: Context) {
        vibrate(context, 12L, VibrationEffect.EFFECT_TICK)
    }

    fun click(context: Context) {
        vibrate(context, 25L, VibrationEffect.EFFECT_CLICK)
    }

    fun success(context: Context) {
        vibrate(context, 45L, VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    private fun vibrate(context: Context, fallbackDurationMs: Long, effectId: Int) {
        try {
            // 1. View-based haptics through Window Manager
            (context as? Activity)?.window?.decorView?.let { view ->
                val feedbackConstant = when (effectId) {
                    VibrationEffect.EFFECT_TICK -> HapticFeedbackConstants.CLOCK_TICK
                    VibrationEffect.EFFECT_HEAVY_CLICK -> HapticFeedbackConstants.CONFIRM
                    else -> HapticFeedbackConstants.VIRTUAL_KEY
                }
                view.performHapticFeedback(
                    feedbackConstant,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING or HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                )
            }

            // 2. Direct Vibrator hardware service with audio attributes
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                val audioAttrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val effect = VibrationEffect.createPredefined(effectId)
                        vibrator.vibrate(effect, audioAttrs)
                        return
                    } catch (_: Exception) {}
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val amplitude = when (effectId) {
                        VibrationEffect.EFFECT_TICK -> 90
                        VibrationEffect.EFFECT_HEAVY_CLICK -> 255
                        else -> 190
                    }
                    val effect = VibrationEffect.createOneShot(fallbackDurationMs, amplitude)
                    vibrator.vibrate(effect, audioAttrs)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(fallbackDurationMs)
                }
            }
        } catch (_: Exception) {}
    }
}
