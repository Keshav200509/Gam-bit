package com.example.presentation

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

enum class GameSound {
    TOKEN_PLACE,
    TOKEN_SELECT,
    SCOUT_ACTIVATE,
    SCOUT_CLEAR,
    SCOUT_CONTESTED,
    LOCK_TOGGLE,
    COMMIT,
    CLASH_WIN,
    CLASH_LOSE,
    CLASH_TIE,
    ROUND_COMPLETE,
    GAME_WIN,
    GAME_LOSE,
    ACHIEVEMENT_UNLOCK,
    GOLDEN_CELL,
    STREAK_BONUS,
    // Keep old ones for compatibility
    SCOUT,
    LOCK,
    GAME_OVER_WIN,
    GAME_OVER_LOSE,
    TIMER_TICK_SOFT,
    TIMER_TICK_URGENT,
    TIMER_EXPIRED,
    MENU_CLICK,
    MENU_HOVER,
    SCREEN_TRANSITION,
    LOGO_APPEAR
}

data class SoundSpec(
    val sound: GameSound,
    val fileName: String,
    val durationMs: Int,
    val waveGen: (Double) -> Double
)

@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("gambit_prefs", Context.MODE_PRIVATE)
    
    var isMuted: Boolean
        get() = prefs.getBoolean("mute_sounds", false)
        set(value) {
            prefs.edit().putBoolean("mute_sounds", value).apply()
        }

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<GameSound, Int>()
    private var isLoaded = false

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                generateAndLoadSounds()
            } catch (e: Exception) {
                Log.e("SoundManager", "Error generating or loading sounds", e)
            }
        }
    }

    private fun generateAndLoadSounds() {
        val soundsToGenerate = listOf(
            SoundSpec(GameSound.TOKEN_PLACE, "token_place.wav", 150) { t: Double ->
                sin(2.0 * PI * 120.0 * t)
            },
            SoundSpec(GameSound.TOKEN_SELECT, "token_select.wav", 50) { t: Double ->
                sin(2.0 * PI * 800.0 * t)
            },
            SoundSpec(GameSound.SCOUT_ACTIVATE, "scout_activate.wav", 300) { t: Double ->
                val f0 = 400.0
                val f1 = 800.0
                val duration = 0.3
                val phase = 2.0 * PI * (f0 * t + ((f1 - f0) / (2.0 * duration)) * t * t)
                sin(phase)
            },
            SoundSpec(GameSound.SCOUT_CLEAR, "scout_clear.wav", 200) { t: Double ->
                sin(2.0 * PI * 880.0 * t)
            },
            SoundSpec(GameSound.SCOUT_CONTESTED, "scout_contested.wav", 200) { t: Double ->
                val f = 200.0
                2.0 * (t * f - floor(t * f + 0.5))
            },
            SoundSpec(GameSound.LOCK_TOGGLE, "lock_toggle.wav", 80) { t: Double ->
                sign(sin(2.0 * PI * 500.0 * t))
            },
            SoundSpec(GameSound.COMMIT, "commit.wav", 400) { t: Double ->
                sin(2.0 * PI * 60.0 * t)
            },
            SoundSpec(GameSound.CLASH_WIN, "clash_win.wav", 400) { t: Double ->
                (sin(2.0 * PI * 523.25 * t) + sin(2.0 * PI * 659.25 * t) + sin(2.0 * PI * 783.99 * t)) / 3.0
            },
            SoundSpec(GameSound.CLASH_LOSE, "clash_lose.wav", 300) { t: Double ->
                val f0 = 300.0
                val f1 = 150.0
                val phase = 2.0 * PI * (f0 * t + ((f1 - f0) / (2.0 * 0.3)) * t * t)
                val phi = (phase / (2.0 * PI)) % 1.0
                2.0 * (phi - floor(phi + 0.5))
            },
            SoundSpec(GameSound.CLASH_TIE, "clash_tie.wav", 250) { t: Double ->
                val tri = { f: Double -> 2.0 * abs(2.0 * (t * f - floor(t * f + 0.5))) - 1.0 }
                (tri(440.0) + tri(442.0)) / 2.0
            },
            SoundSpec(GameSound.ROUND_COMPLETE, "round_complete.wav", 500) { t: Double ->
                val f0 = 400.0
                val f1 = 600.0
                val duration = 0.5
                val phase = 2.0 * PI * (f0 * t + ((f1 - f0) / (2.0 * duration)) * t * t)
                sin(phase)
            },
            SoundSpec(GameSound.GAME_WIN, "game_win.wav", 1200) { t: Double ->
                val step = (t / 0.3).toInt()
                val f = when (step) {
                    0 -> 261.63
                    1 -> 329.63
                    2 -> 392.00
                    else -> 523.25
                }
                sin(2.0 * PI * f * t)
            },
            SoundSpec(GameSound.GAME_LOSE, "game_lose.wav", 1000) { t: Double ->
                val f0 = 400.0
                val f1 = 100.0
                val duration = 1.0
                val phase = 2.0 * PI * (f0 * t + ((f1 - f0) / (2.0 * duration)) * t * t)
                val phi = (phase / (2.0 * PI)) % 1.0
                2.0 * (phi - floor(phi + 0.5))
            },
            SoundSpec(GameSound.ACHIEVEMENT_UNLOCK, "achievement_unlock.wav", 800) { t: Double ->
                val step = (t / 0.266).toInt()
                val f = when (step) {
                    0 -> 659.25
                    1 -> 783.99
                    else -> 1046.50
                }
                sin(2.0 * PI * f * t)
            },
            SoundSpec(GameSound.GOLDEN_CELL, "golden_cell.wav", 600) { t: Double ->
                val f0 = 880.0
                val f1 = 1760.0
                val duration = 0.6
                val phase = 2.0 * PI * (f0 * t + ((f1 - f0) / (2.0 * duration)) * t * t)
                sin(phase) * (0.8 + 0.2 * sin(2.0 * PI * 15.0 * t))
            },
            SoundSpec(GameSound.STREAK_BONUS, "streak_bonus.wav", 600) { t: Double ->
                val step = (t / 0.15).toInt()
                val f = when (step) {
                    0 -> 523.25
                    1 -> 659.25
                    2 -> 783.99
                    else -> 1046.50
                }
                sin(2.0 * PI * f * t)
            },
            // Legacy fallbacks mapped to standard waves
            SoundSpec(GameSound.SCOUT, "legacy_scout.wav", 300) { t: Double ->
                val f0 = 400.0
                val f1 = 800.0
                val duration = 0.3
                val phase = 2.0 * PI * (f0 * t + ((f1 - f0) / (2.0 * duration)) * t * t)
                sin(phase)
            },
            SoundSpec(GameSound.LOCK, "legacy_lock.wav", 80) { t: Double ->
                sign(sin(2.0 * PI * 500.0 * t))
            },
            SoundSpec(GameSound.GAME_OVER_WIN, "legacy_game_over_win.wav", 400) { t: Double ->
                val step = (t / 0.1).toInt()
                val f = when (step) {
                    0 -> 261.63
                    1 -> 329.63
                    2 -> 392.00
                    else -> 523.25
                }
                sin(2.0 * PI * f * t)
            },
            SoundSpec(GameSound.GAME_OVER_LOSE, "legacy_game_over_lose.wav", 600) { t: Double ->
                val f0 = 500.0
                val f1 = 150.0
                val duration = 0.6
                val phase = 2.0 * PI * (f0 * t + ((f1 - f0) / (2.0 * duration)) * t * t)
                val phi = (phase / (2.0 * PI)) % 1.0
                2.0 * (phi - floor(phi + 0.5))
            },
            SoundSpec(GameSound.TIMER_TICK_SOFT, "timer_tick_soft.wav", 100) { t: Double ->
                sin(2.0 * PI * 800.0 * t) * exp(-15.0 * t)
            },
            SoundSpec(GameSound.TIMER_TICK_URGENT, "timer_tick_urgent.wav", 120) { t: Double ->
                (sin(2.0 * PI * 1000.0 * t) + sin(2.0 * PI * 1200.0 * t)) * 0.5 * exp(-20.0 * t)
            },
            SoundSpec(GameSound.TIMER_EXPIRED, "timer_expired.wav", 400) { t: Double ->
                val f0 = 400.0
                val f1 = 100.0
                val duration = 0.4
                val phase = 2.0 * PI * (f0 * t + ((f1 - f0) / (2.0 * duration)) * t * t)
                sin(phase)
            },
            SoundSpec(GameSound.MENU_CLICK, "menu_click.wav", 50) { t: Double ->
                sin(2.0 * PI * 800.0 * t)
            },
            SoundSpec(GameSound.MENU_HOVER, "menu_hover.wav", 30) { t: Double ->
                sin(2.0 * PI * 1200.0 * t)
            },
            SoundSpec(GameSound.SCREEN_TRANSITION, "screen_transition.wav", 300) { t: Double ->
                val f0 = 400.0
                val f1 = 800.0
                val duration = 0.3
                val phase = 2.0 * PI * (f0 * t + ((f1 - f0) / (2.0 * duration)) * t * t)
                sin(phase)
            },
            SoundSpec(GameSound.LOGO_APPEAR, "logo_appear.wav", 800) { t: Double ->
                val f0 = 200.0
                val f1 = 600.0
                val duration = 0.8
                val phase1 = 2.0 * PI * (f0 * t + ((f1 - f0) / (2.0 * duration)) * t * t)
                val phase2 = 2.0 * PI * ((2.0 * f0) * t + (((2.0 * f1) - (2.0 * f0)) / (2.0 * duration)) * t * t)
                (sin(phase1) + 0.5 * sin(phase2)) / 1.5
            }
        )

        val sampleRate = 22050
        soundPool?.let { pool ->
            for (spec in soundsToGenerate) {
                val file = generateWavFile(spec.fileName, spec.durationMs, sampleRate, spec.waveGen)
                if (file.exists()) {
                    val id = pool.load(file.absolutePath, 1)
                    soundMap[spec.sound] = id
                }
            }
            isLoaded = true
        }
    }

    private fun generateWavFile(
        fileName: String,
        durationMs: Int,
        sampleRate: Int,
        waveGen: (Double) -> Double
    ): File {
        val totalSamples = (sampleRate * durationMs / 1000)
        val totalAudioLen = totalSamples * 2
        val totalDataLen = totalAudioLen + 36

        val tempFile = File(context.cacheDir, fileName)
        FileOutputStream(tempFile).use { fos ->
            writeWavHeader(fos, totalAudioLen.toLong(), totalDataLen.toLong(), sampleRate.toLong(), 1, (sampleRate * 2).toLong())
            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val rawVal = waveGen(t)
                // Fade out rawVal towards the end to avoid clicks
                val fadeOutThreshold = 0.85
                val progress = i.toDouble() / totalSamples
                val envelope = if (progress > fadeOutThreshold) {
                    1.0 - (progress - fadeOutThreshold) / (1.0 - fadeOutThreshold)
                } else {
                    1.0
                }
                val sampleValue = rawVal * envelope
                val s = (sampleValue * 32767.0).toInt().coerceIn(-32768, 32767)
                fos.write(s and 0xff)
                fos.write((s shr 8) and 0xff)
            }
        }
        return tempFile
    }

    private fun writeWavHeader(
        out: OutputStream,
        totalAudioLen: Long,
        totalDataLen: Long,
        longSampleRate: Long,
        channels: Int,
        byteRate: Long
    ) {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte() // WAVE
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // format chunk size
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte() // 'data' chunk
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        out.write(header, 0, 44)
    }

    fun playSound(sound: GameSound) {
        if (isMuted) return
        val soundId = soundMap[sound] ?: return
        val volume = if (sound == GameSound.MENU_HOVER) 0.3f else 0.6f
        soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)
    }

    fun playMenuClick() {
        playSound(GameSound.MENU_CLICK)
    }

    fun playMenuHover() {
        playSound(GameSound.MENU_HOVER)
    }

    fun playScreenTransition() {
        playSound(GameSound.SCREEN_TRANSITION)
    }

    fun playLogoAppear() {
        playSound(GameSound.LOGO_APPEAR)
    }

    fun playTokenPlace() {
        playSound(GameSound.TOKEN_PLACE)
    }

    fun playTokenSelect() {
        playSound(GameSound.TOKEN_SELECT)
    }

    fun playScoutActivate() {
        playSound(GameSound.SCOUT_ACTIVATE)
    }

    fun playScoutClear() {
        playSound(GameSound.SCOUT_CLEAR)
    }

    fun playScoutContested() {
        playSound(GameSound.SCOUT_CONTESTED)
    }

    fun playLockToggle() {
        playSound(GameSound.LOCK_TOGGLE)
    }

    fun playCommit() {
        playSound(GameSound.COMMIT)
    }

    fun playClashWin() {
        playSound(GameSound.CLASH_WIN)
    }

    fun playClashLose() {
        playSound(GameSound.CLASH_LOSE)
    }

    fun playClashTie() {
        playSound(GameSound.CLASH_TIE)
    }

    fun playRoundComplete() {
        playSound(GameSound.ROUND_COMPLETE)
    }

    fun playGameWin() {
        playSound(GameSound.GAME_WIN)
    }

    fun playGameLose() {
        playSound(GameSound.GAME_LOSE)
    }

    fun playAchievementUnlock() {
        playSound(GameSound.ACHIEVEMENT_UNLOCK)
    }

    fun playGoldenCell() {
        playSound(GameSound.GOLDEN_CELL)
    }

    fun playStreakBonus() {
        playSound(GameSound.STREAK_BONUS)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
        isLoaded = false
    }
}
