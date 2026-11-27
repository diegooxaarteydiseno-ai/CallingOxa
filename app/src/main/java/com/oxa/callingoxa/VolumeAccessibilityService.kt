package com.oxa.callingoxa

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class VolumeAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val ring = Runnable { showCall() }

    companion object {
        @Volatile
        var running = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = true
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(
            VolumeService.CH_CALL, "Llamada", NotificationManager.IMPORTANCE_HIGH
        )
        ch.setSound(null, null)
        nm.createNotificationChannel(ch)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!Prefs.active(this)) return false
        val code = event.keyCode
        if (code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            val seconds = if (code == KeyEvent.KEYCODE_VOLUME_UP) {
                Prefs.upSeconds(this)
            } else {
                Prefs.downSeconds(this)
            }
            handler.removeCallbacks(ring)
            handler.postDelayed(ring, seconds * 1000L)
            buzz()
        }
        return true
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
        val n = Notification.Builder(this, VolumeService.CH_CALL)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(Prefs.name(this))
            .setContentText("Llamada entrante")
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .build()
        getSystemService(NotificationManager::class.java).notify(VolumeService.CALL_ID, n)
        try {
            startActivity(intent)
        } catch (e: Exception) {
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        running = false
        handler.removeCallbacks(ring)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(ring)
        super.onDestroy()
    }
}
