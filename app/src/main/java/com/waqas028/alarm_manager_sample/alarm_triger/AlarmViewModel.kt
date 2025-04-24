package com.waqas028.alarm_manager_sample.alarm_triger

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmViewModel : ViewModel() {
    private var _mediaPlayer: MediaPlayer? = null
    private var fadingJob: Job? = null

    // For controlling alarm sound
    var mediaPlayer: MediaPlayer?
        get() = _mediaPlayer
        set(value) {
            _mediaPlayer?.release() // Release previous media player
            _mediaPlayer = value
        }

    // For fading out the alarm sound
    fun fadeOutAlarm(durationMillis: Long = 3000) {
        fadingJob?.cancel()
        fadingJob = viewModelScope.launch {
            val player = _mediaPlayer ?: return@launch
            val initialVolume = 1.0f
            val steps = 30
            val stepDuration = durationMillis / steps
            val volumeStep = initialVolume / steps

            repeat(steps) {
                player.setVolume(initialVolume - (volumeStep * it), initialVolume - (volumeStep * it))
                delay(stepDuration)
            }
            stopAlarm()
        }
    }

    fun stopAlarm() {
        fadingJob?.cancel()
        _mediaPlayer?.stop()
        _mediaPlayer?.release()
        _mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAlarm()
    }
}