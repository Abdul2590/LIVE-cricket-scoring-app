package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoundManager {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            toneGenerator = null
        }
    }

    fun playBatCrack(enabled: Boolean = true) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        } catch (_: Exception) {}
    }

    fun playBoundaryChime(enabled: Boolean = true) {
        if (!enabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 70)
                delay(90)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 90)
            } catch (_: Exception) {}
        }
    }

    fun playSixFanfare(enabled: Boolean = true) {
        if (!enabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_3, 80)
                delay(100)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_6, 80)
                delay(100)
                toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 140)
            } catch (_: Exception) {}
        }
    }

    fun playWicketAlert(enabled: Boolean = true) {
        if (!enabled) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 160)
            } catch (_: Exception) {}
        }
    }

    fun playUndoTone(enabled: Boolean = true) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 50)
        } catch (_: Exception) {}
    }

    fun playUmpireWhistle(enabled: Boolean = true) {
        if (!enabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
        } catch (_: Exception) {}
    }
}
