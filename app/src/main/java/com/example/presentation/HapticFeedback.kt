package com.example.presentation

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GambitHapticFeedback @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrator = try {
        val attributionContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.createAttributionContext("vibrator")
        } else {
            context
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = attributionContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            attributionContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (e: Exception) {
        Log.e("HapticFeedback", "Error accessing vibrator service", e)
        null
    }

    private fun vibrate(duration: Long, amplitude: Int) {
        try {
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(duration, amplitude))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(duration)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HapticFeedback", "Error during vibrate", e)
        }
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        try {
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(timings, -1)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HapticFeedback", "Error during vibratePattern", e)
        }
    }

    fun tokenPlaced() {
        vibrate(30, 80)
    }

    fun clashWon() {
        vibratePattern(longArrayOf(0, 50, 30, 80), intArrayOf(0, 180, 0, 255))
    }

    fun clashLost() {
        vibratePattern(longArrayOf(0, 80, 50, 40), intArrayOf(0, 100, 0, 80))
    }

    fun clashTied() {
        vibratePattern(longArrayOf(0, 30, 50, 30), intArrayOf(0, 120, 0, 120))
    }

    fun scoutUsed() {
        vibrate(60, 120)
    }

    fun lockToggled() {
        vibratePattern(longArrayOf(0, 40, 40, 60), intArrayOf(0, 150, 0, 200))
    }

    fun roundComplete() {
        vibratePattern(longArrayOf(0, 40, 40, 50, 40, 60), intArrayOf(0, 100, 0, 160, 0, 240))
    }

    fun gameOver() {
        vibrate(200, 200)
    }

    fun menuClick() {
        vibrate(15, 60) // light tick
    }

    fun menuSelect() {
        vibrate(40, 120) // medium confirm
    }

    fun tokenSelected() {
        vibrate(20, 60)
    }

    fun scoutCleared() {
        vibrate(30, 80)
    }

    fun scoutContested() {
        vibratePattern(longArrayOf(0, 40, 40, 40), intArrayOf(0, 150, 0, 150))
    }

    fun commitPressed() {
        vibrate(100, 150)
    }

    fun achievementUnlocked() {
        vibratePattern(longArrayOf(0, 30, 30, 30, 30, 50), intArrayOf(0, 100, 0, 150, 0, 200))
    }

    fun streakBonusApplied() {
        vibratePattern(longArrayOf(0, 20, 20, 20, 20, 20, 20, 20), intArrayOf(0, 80, 0, 100, 0, 120, 0, 140))
    }

    fun goldenCellActivated() {
        vibratePattern(longArrayOf(0, 40, 30, 40, 30, 40), intArrayOf(0, 120, 0, 150, 0, 180))
    }
}
