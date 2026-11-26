package com.oxa.callingoxa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator

class VolumeService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var session: MediaSession? = null
    private var silence: AudioTrack? = null
    private val ring = Runnable { showCall() }

    companion object {
        const val CH_SERVICE = "svc"
        const val CH_CALL = "call"
        const val CALL_ID = 2
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_SERVICE, "Servicio", NotificationManager.IMPORTANCE_LOW)
        )
        val callChannel =
            NotificationChannel(CH_CALL, "Llamada", NotificationManager.IMPORTANCE_HIGH)
        callChannel.setSound(null, null)
        nm.createNotificationChannel(callChannel)

        val n = Notification.Builder(this, CH_SERVICE)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentTitle("CallingOxa activo")
            .setContentText("Escuchando los botones de volumen")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, n)
        }

        startSession()
        startSilence()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startSession() {
        val s = MediaSession(this, "CallingOxa")
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)
                .setState(PlaybackState.STATE_PLAYING, 0L, 1f)
                .build()
        )
        s.setPlaybackToRemote(
            object : VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE, 100, 50) {
                override fun onAdjustVolume(direction: Int) {
                    handler.post { onKey(direction) }
                }
            }
        )
        s.isActive = true
        session = s
    }

    // Audio en silencio para que Android mantenga la sesión activa con la pantalla apagada
    private fun startSilence() {
        try {
            val rate = 8000
            val bytes = rate * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(ByteArray(bytes), 0, bytes)
            track.setLoopPoints(0, rate, -1)
            track.play()
            silence = track
        } catch (e: Exception) {
            // si falla, el servicio sigue funcionando igualmente
        }
    }

    private fun onKey(direction: Int) {
        val seconds = when {
            direction > 0 -> Prefs.upSeconds(this)
            direction < 0 -> Prefs.downSeconds(this)
            else -> return
        }
        handler.removeCallbacks(ring)
        handler.postDelayed(ring, seconds * 1000L)
        buzz()
    }

    private fun buzz() {
        val v = getSystemService(Vibrator::class.java)
        v?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun showCall() {
        val intent = Intent(this, CallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(this, CH_CALL)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(Prefs.name(this))
            .setContentText("Llamada entrante")
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        getSystemService(NotificationManager::class.java).notify(CALL_ID, n)

        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Android puede bloquearlo en segundo plano; la notificación lo cubre
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(ring)
        session?.isActive = false
        session?.release()
        silence?.stop()
        silence?.release()
        super.onDestroy()
    }
}
