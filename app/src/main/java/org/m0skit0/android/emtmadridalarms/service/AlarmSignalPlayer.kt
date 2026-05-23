package org.m0skit0.android.emtmadridalarms.service

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest

class AlarmSignalPlayer(private val context: Context) {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    fun start(request: BusAlarmRequest) {
        Log.i(
            TAG,
            "Starting ringing line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}"
        )
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Uri.EMPTY
        Log.d(TAG, "Using ringtone uri=$uri")
        ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
            play()
        }
        if (ringtone == null) Log.w(TAG, "No ringtone available for uri=$uri")

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 900L, 600L), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0L, 900L, 600L), 0)
        }
        Log.d(TAG, "Ringing started: ringtone=${ringtone != null}, vibrator=${vibrator != null}")
    }

    fun stop() {
        Log.d(TAG, "Stopping ringtone/vibration")
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    private companion object {
        const val TAG = "BusAlarm"
    }
}
