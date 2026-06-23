package com.example.locationtodo

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class HabitSoundPlayer(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val addSound = soundPool.load(context, R.raw.add_pop, 1)
    private val completeSound = soundPool.load(context, R.raw.complete_chime, 1)
    private val undoSound = soundPool.load(context, R.raw.undo_tap, 1)

    fun playAdd(enabled: Boolean) {
        if (enabled) soundPool.play(addSound, 0.7f, 0.7f, 1, 0, 1f)
    }

    fun playComplete(enabled: Boolean) {
        if (enabled) soundPool.play(completeSound, 0.75f, 0.75f, 1, 0, 1f)
    }

    fun playUndo(enabled: Boolean) {
        if (enabled) soundPool.play(undoSound, 0.55f, 0.55f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
