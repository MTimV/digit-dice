package com.vickers.digitdice

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Two buzzes. A light tick when a finger lands, a double thump on the reveal. */
class Haptics(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun tap() {
        play(VibrationEffect.createOneShot(14L, 45))
    }

    fun reveal() {
        play(
            VibrationEffect.createWaveform(
                longArrayOf(0L, 45L, 70L, 140L),
                intArrayOf(0, 110, 0, 255),
                -1
            )
        )
    }

    private fun play(effect: VibrationEffect) {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        runCatching { device.vibrate(effect) }
    }
}
