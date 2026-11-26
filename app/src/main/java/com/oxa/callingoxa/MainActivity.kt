package com.oxa.callingoxa

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var photo: ImageView
    private lateinit var nameField: EditText
    private lateinit var numberField: EditText
    private lateinit var upField: EditText
    private lateinit var downField: EditText
    private lateinit var status: TextView
    private lateinit var permBtn: Button
    private lateinit var toggle: Button

    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) savePhoto(uri)
        }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(32))
        }

                root.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.logo)
                adjustViewBounds = true
                setBackgroundColor(Color.WHITE)
            },
            LinearLayout.LayoutParams(dp(220), LinearLayout.LayoutParams.WRAP_CONTENT)
        )

        photo = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#64748B"))
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            setOnClickListener { pickPhoto.launch(arrayOf("image/*")) }
        }
        val photoParams = LinearLayout.LayoutParams(dp(120), dp(120))
        photoParams.topMargin = dp(24)
        root.addView(photo, photoParams)

        root.addView(Button(this).apply {
            text = "Elegir foto del contacto"
            setOnClickListener { pickPhoto.launch(arrayOf("image/*")) }
        })

        label(root, "Nombre")
        nameField = input(root, InputType.TYPE_CLASS_TEXT, Prefs.name(this))

        label(root, "Número")
        numberField = input(root, InputType.TYPE_CLASS_PHONE, Prefs.number(this))

        label(root, "Volumen ARRIBA: segundos hasta que suena")
        upField = input(root, InputType.TYPE_CLASS_NUMBER, Prefs.upSeconds(this).toString())

        label(root, "Volumen ABAJO: segundos hasta que suena")
        downField = input(root, InputType.TYPE_CLASS_NUMBER, Prefs.downSeconds(this).toString())

        status = TextView(this).apply {
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(20), 0, dp(4))
        }
        root.addView(status)

        permBtn = Button(this).apply {
            text = "Dar permisos"
            setOnClickListener { requestNext() }
        }
        root.addView(permBtn)

        toggle = Button(this).apply {
            setOnClickListener { toggleService() }
        }
        root.addView(toggle)

        root.addView(Button(this).apply {
            text = "Probar llamada ahora"
            setOnClickListener {
                saveAll()
                startActivity(Intent(this@MainActivity, CallActivity::class.java))
            }
        })

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor("#0F172A"))
        scroll.addView(root)
        setContentView(scroll)

        val file = Prefs.photoFile(this)
        if (file.exists()) {
            photo.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
        } else {
            photo.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        if (!Prefs.setupDone(this)) {
            AlertDialog.Builder(this)
                .setTitle("Permisos necesarios")
                .setMessage(
                    "Esta aplicación requiere funcionar en segundo plano para detectar " +
                        "los botones de volumen con la pantalla apagada.\n\n" +
                        "Te voy a pedir: notificaciones, ignorar la optimización de batería " +
                        "y notificaciones a pantalla completa."
                )
                .setPositiveButton("Continuar") { _, _ ->
                    Prefs.setSetupDone(this)
                    requestNext()
                }
                .setCancelable(false)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onPause() {
        super.onPause()
        saveAll()
    }

    private fun label(parent: LinearLayout, t: String) {
        parent.addView(TextView(this).apply {
            text = t
            textSize = 13f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(16), 0, dp(4))
        })
    }

    private fun input(parent: LinearLayout, type: Int, value: String): EditText {
        val e = EditText(this).apply {
            inputType = type
            setText(value)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        parent.addView(
            e,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        return e
    }

    private fun saveAll() {
        val up = upField.text.toString().toIntOrNull() ?: 5
        val down = downField.text.toString().toIntOrNull() ?: 10
        Prefs.save(
            this,
            nameField.text.toString().ifBlank { "Desconocido" },
            numberField.text.toString(),
            up.coerceIn(1, 3600),
            down.coerceIn(1, 3600)
        )
    }

    private fun savePhoto(uri: Uri) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return
        Prefs.photoFile(this).outputStream().use {
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        photo.setImageBitmap(bmp)
    }

    // 0 = notificaciones, 1 = batería, 2 = pantalla completa, -1 = todo listo
    private fun nextMissing(): Int {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return 0

        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) return 1

        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) return 2
        }
        return -1
    }

    private fun requestNext() {
        when (nextMissing()) {
            0 -> requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1002)
            1 -> startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
            2 -> if (Build.VERSION.SDK_INT >= 34) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        }
    }

    private fun updateStatus() {
        val missing = nextMissing()
        if (missing == -1) {
            status.text = "Permisos listos"
            permBtn.visibility = View.GONE
        } else {
            status.text = "Faltan permisos. Pulsa el botón hasta que desaparezca."
            permBtn.visibility = View.VISIBLE
        }
        toggle.text = if (Prefs.active(this)) "Desactivar" else "Activar"
    }

    private fun toggleService() {
        saveAll()
        val intent = Intent(this, VolumeService::class.java)
        if (Prefs.active(this)) {
            stopService(intent)
            Prefs.setActive(this, false)
        } else {
            if (nextMissing() != -1) {
                Toast.makeText(this, "Faltan permisos", Toast.LENGTH_SHORT).show()
                requestNext()
                return
            }
            ContextCompat.startForegroundService(this, intent)
            Prefs.setActive(this, true)
        }
        updateStatus()
    }
}
