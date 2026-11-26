package com.oxa.callingoxa

import android.app.NotificationManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CallActivity : AppCompatActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        getSystemService(NotificationManager::class.java).cancel(VolumeService.CALL_ID)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)

        // Parte de arriba: texto, foto, nombre y número
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        top.addView(TextView(this).apply {
            text = "Llamada entrante"
            textSize = 20f
            setTextColor(Color.parseColor("#E8EAED"))
        })

        val photo = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#80868B"))
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }
        val file = Prefs.photoFile(this)
        if (file.exists()) {
            photo.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
        } else {
            photo.setImageResource(android.R.drawable.ic_menu_myplaces)
        }
        val photoParams = LinearLayout.LayoutParams(dp(112), dp(112))
        photoParams.topMargin = dp(32)
        top.addView(photo, photoParams)

        val nameView = TextView(this).apply {
            text = Prefs.name(this@CallActivity)
            textSize = 34f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(24), 0, 0)
        }
        top.addView(nameView)

        val number = Prefs.number(this)
        if (number.isNotBlank()) {
            top.addView(TextView(this).apply {
                text = number
                textSize = 18f
                setTextColor(Color.parseColor("#BDC1C6"))
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            })
        }

        val topParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        )
        topParams.topMargin = dp(96)
        root.addView(top, topParams)

        // Parte de abajo: botones rechazar y aceptar
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        row.addView(
            action("#EA5B4B", "Rechazar", 135f),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        row.addView(
            action("#3DBD5A", "Aceptar", 0f),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        val rowParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM
        )
        rowParams.bottomMargin = dp(72)
        root.addView(row, rowParams)

        setContentView(root)

        startRinging()
        handler.postDelayed({ endCall() }, 45000L)
    }

    private fun action(color: String, label: String, rot: Float): LinearLayout {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_call)
            setColorFilter(Color.WHITE)
            rotation = rot
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(color))
            }
            setOnClickListener { endCall() }
        }
        col.addView(icon, LinearLayout.LayoutParams(dp(72), dp(72)))
        col.addView(TextView(this).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })
        return col
    }

    private fun startRinging() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= 28) isLooping = true
                play()
            }
        } catch (e: Exception) {
        }
        vibrator = getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 800), 0))
    }

    private fun stopRinging() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
        }
        vibrator?.cancel()
    }

    private fun endCall() {
        stopRinging()
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopRinging()
        super.onDestroy()
    }
}
