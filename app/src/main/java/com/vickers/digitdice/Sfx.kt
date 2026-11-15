package com.vickers.digitdice

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Short sounds through SoundPool, which is built for exactly this and does not
 * stall the way MediaPlayer can. Loading is asynchronous, so the very first tap
 * after a cold start may be silent.
 */
class Sfx(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val tapId = pool.load(context, R.raw.tap, 1)
    private val winId = pool.load(context, R.raw.win, 1)
    private val loseId = pool.load(context, R.raw.lose, 1)

    fun tap() = play(tapId, 0.45f)

    fun win() = play(winId, 0.9f)

    fun lose() = play(loseId, 0.8f)

    fun release() {
        pool.release()
    }

    private fun play(id: Int, volume: Float) {
        if (id == 0) return
        pool.play(id, volume, volume, 1, 0, 1f)
    }
}
