package org.m0skit0.android.emtmadridalarms.service

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import timber.log.Timber
import org.m0skit0.android.emtmadridalarms.domain.BusAlarmRequest
import org.m0skit0.android.emtmadridalarms.state.GlobalStateHolder

private const val TAG = "BusAlarm"

data class AlarmSignalState(
    val ringtone: Ringtone? = null,
    val vibrator: Vibrator? = null,
)

fun interface StartSignal : (BusAlarmRequest) -> Unit

internal fun startSignal(context: Context, globalState: GlobalStateHolder): StartSignal =
    StartSignal { request ->
        Timber.i("Starting ringing line=${request.line} stop=${request.stopId} targetMinutes=${request.targetMinutes}")
        val ringtone = resolveRingtone(context)
        val vibrator = startVibration(context)
        globalState.update { appState ->
            appState.copy(alarmSignal = AlarmSignalState(ringtone = ringtone, vibrator = vibrator))
        }
        Timber.d("Ringing started: ringtone=${ringtone != null}")
    }

private fun resolveRingtone(context: Context): Ringtone? {
    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ?: Uri.EMPTY
    Timber.d("Using ringtone uri=$uri")
    val ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
        play()
    }
    if (ringtone == null) Timber.w("No ringtone available for uri=$uri")
    return ringtone
}

private fun startVibration(context: Context): Vibrator {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 900L, 600L), 0))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(longArrayOf(0L, 900L, 600L), 0)
    }
    return vibrator
}

internal fun stopSignal(globalState: GlobalStateHolder): StopSignal =
    StopSignal {
        Timber.d("Stopping ringtone/vibration")
        val signal = globalState.state.alarmSignal
        signal.ringtone?.stop()
        signal.vibrator?.cancel()
        globalState.update { appState ->
            appState.copy(alarmSignal = AlarmSignalState())
        }
    }
