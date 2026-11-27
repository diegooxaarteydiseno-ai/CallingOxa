package com.oxa.callingoxa

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser

private const val MATCH = -1
private const val WRAP = -2

private const val P_PERSON =
    "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z"
private const val P_CALL =
    "M6.62,10.79c1.44,2.83 3.76,5.14 6.59,6.59l2.2,-2.2c0.27,-0.27 0.67,-0.36 1.02,-0.24 1.12,0.37 2.33,0.57 3.57,0.57 0.55,0 1,0.45 1,1V20c0,0.55 -0.45,1 -1,1 -9.39,0 -17,-7.61 -17,-17 0,-0.55 0.45,-1 1,-1h3.5c0.55,0 1,0.45 1,1 0,1.25 0.2,2.45 0.57,3.57 0.11,0.35 0.03,0.74 -0.25,1.02l-2.2,2.2z"
private const val P_VOLUME =
    "M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z"
private const val P_CAMERA =
    "M12,12m-3.2,0a3.2,3.2 0,1 1,6.4 0a3.2,3.2 0,1 1,-6.4 0M9,2L7.17,4H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V6c0,-1.1 -0.9,-2 -2,-2h-3.17L15,2H9zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5z"
private const val P_POWER =
    "M13,3h-2v10h2V3zM17.83,5.17l-1.42,1.42C17.99,7.86 19,9.81 19,12c0,3.87 -3.13,7 -7,7s-7,-3.13 -7,-7c0,-2.19 1.01,-4.14 2.58,-5.42L6.17,5.17C4.23,6.82 3,9.26 3,12c0,4.97 4.03,9 9,9s9,-4.03 9,-9c0,-2.74 -1.23,-5.18 -3.17,-6.83z"
private const val P_LIST =
    "M3,13h2v-2H3v2zM3,17h2v-2H3v2zM3,9h2V7H3v2zM7,13h14v-2H7v2zM7,17h14v-2H7v2zM7,7v2h14V7H7z"
private const val P_LOCK =
    "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10c0,-1.1 -0.9,-2 -2,-2zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z"

class PathIcon(pathData: String, tint: Int) : Drawable() {
    private val path: Path = PathParser.createPathFromPathData(pathData)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = tint
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val s = canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        canvas.scale(b.width() / 24f, b.height() / 24f)
        canvas.drawPath(path, paint)
        canvas.restoreToCount(s)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint.colorFilter = cf
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

class MainActivity : AppCompatActivity() {

    private lateinit var photo: ImageView
    private lateinit var nameField: EditText
    private lateinit var numberField: EditText
    private lateinit var upField: EditText
    private lateinit var downField: EditText
    private lateinit var status: TextView
    private lateinit var permBtn: View
    private lateinit var toggleBtn: View
    private lateinit var toggleLabel: TextView

    private val blue = Color.parseColor("#1D4ED8")

    private val pickPhoto =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) savePhoto(uri)
        }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun icon(path: String, color: Int, size: Int): ImageView =
        ImageView(this).apply {
            setImageDrawable(PathIcon(path, color))
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
        }

    private fun pill(
        caption: String,
        iconPath: String,
        filled: Boolean,
        onClick: () -> Unit
    ): LinearLayout {
        val fg = if (filled) Color.WHITE else blue
        val bg = GradientDrawable()
        if (filled) {
            bg.orientation = GradientDrawable.Orientation.LEFT_RIGHT
            bg.setColors(intArrayOf(Color.parseColor("#2563EB"), Color.parseColor("#0B3FB5")))
        } else {
            bg.setColor(Color.WHITE)
            bg.setStroke(dp(2), blue)
        }
        bg.cornerRadius = dp(32).toFloat()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = bg
            elevation = dp(6).toFloat()
            isClickable = true
            setOnClickListener { onClick() }
        }
        row.addView(icon(iconPath, fg, 28))
        val tv = TextView(this).apply {
            this.text = caption
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(fg)
            setPadding(dp(12), 0, 0, 0)
            tag = "label"
        }
        row.addView(tv)
        return row
    }

    private fun addPill(parent: LinearLayout, p: LinearLayout, topMargin: Int) {
        val lp = LinearLayout.LayoutParams(MATCH, dp(60))
        lp.topMargin = dp(topMargin)
        lp.leftMargin = dp(4)
        lp.rightMargin = dp(4)
        parent.addView(p, lp)
    }

    private fun fieldCard(
        parent: LinearLayout,
        iconPath: String,
        caption: String,
        type: Int,
        value: String
    ): EditText {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(22).toFloat()
            }
            elevation = dp(4).toFloat()
        }
        card.addView(icon(iconPath, blue, 30))

        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply {
            this.text = caption
            textSize = 13f
            setTextColor(Color.parseColor("#475569"))
        })
        val e = EditText(this).apply {
            inputType = type
            setText(value)
            textSize = 22f
            setTextColor(Color.parseColor("#0B1B4B"))
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
            setPadding(0, dp(4), 0, dp(6))
        }
        col.addView(e, LinearLayout.LayoutParams(MATCH, WRAP))

        val colParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        colParams.leftMargin = dp(16)
        card.addView(col, colParams)

        val cardParams = LinearLayout.LayoutParams(MATCH, WRAP)
        cardParams.topMargin = dp(14)
        cardParams.leftMargin = dp(4)
        cardParams.rightMargin = dp(4)
        parent.addView(card, cardParams)
        return e
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0F2A5F")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(12), dp(20), dp(32))
            clipChildren = false
            clipToPadding = false
        }

        root.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.logo_app)
                adjustViewBounds = true
            },
            LinearLayout.LayoutParams(dp(280), WRAP)
        )

        val ring = FrameLayout(this).apply {
            setPadding(dp(5), dp(5), dp(5), dp(5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
            elevation = dp(6).toFloat()
        }
        photo = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#94A3B8"))
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            setOnClickListener { pickPhoto.launch(arrayOf("image/*")) }
        }
        ring.addView(photo, FrameLayout.LayoutParams(dp(140), dp(140)))
        val ringParams = LinearLayout.LayoutParams(WRAP, WRAP)
        ringParams.topMargin = dp(8)
        root.addView(ring, ringParams)

        addPill(
            root,
            pill("ELEGIR FOTO DEL CONTACTO", P_CAMERA, true) {
                pickPhoto.launch(arrayOf("image/*"))
            },
            22
        )

        nameField = fieldCard(root, P_PERSON, "Nombre", InputType.TYPE_CLASS_TEXT, Prefs.name(this))
        numberField = fieldCard(root, P_CALL, "Número", InputType.TYPE_CLASS_PHONE, Prefs.number(this))
        upField = fieldCard(
            root, P_VOLUME, "Volumen ARRIBA: segundos hasta que suena",
            InputType.TYPE_CLASS_NUMBER, Prefs.upSeconds(this).toString()
        )
        downField = fieldCard(
            root, P_VOLUME, "Volumen ABAJO: segundos hasta que suena",
            InputType.TYPE_CLASS_NUMBER, Prefs.downSeconds(this).toString()
        )

        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(20), dp(8), dp(4))
        }
        statusRow.addView(icon(P_LIST, blue, 26))
        status = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.parseColor("#475569"))
            setPadding(dp(16), 0, 0, 0)
        }
        statusRow.addView(status)
        root.addView(statusRow, LinearLayout.LayoutParams(MATCH, WRAP))

        val perm = pill("DAR PERMISOS", P_LOCK, false) { requestNext() }
        permBtn = perm
        addPill(root, perm, 10)

        val tg = pill("ACTIVAR", P_POWER, true) { toggleService() }
        toggleBtn = tg
        toggleLabel = tg.findViewWithTag("label")
        addPill(root, tg, 12)

        addPill(
            root,
            pill("PROBAR LLAMADA AHORA", P_CALL, false) {
                saveAll()
                startActivity(Intent(this@MainActivity, CallActivity::class.java))
            },
            14
        )

        val frame = FrameLayout(this)
        frame.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.bg_main)
                scaleType = ImageView.ScaleType.CENTER_CROP
            },
            FrameLayout.LayoutParams(MATCH, MATCH)
        )
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipChildren = false
        }
        scroll.addView(root)
        frame.addView(scroll, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(frame)

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
                    "Esta aplicación necesita notificaciones a pantalla completa para " +
                        "mostrar la llamada, y el servicio de accesibilidad para detectar " +
                        "los botones de volumen con la pantalla apagada."
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

    // 0 = notificaciones, 2 = pantalla completa, -1 = todo listo
    private fun nextMissing(): Int {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return 0

        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) return 2
        }
        return -1
    }

    private fun requestNext() {
        when (nextMissing()) {
            0 -> requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1002)
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
        val acc = VolumeAccessibilityService.running
        status.text = when {
            missing != -1 -> "Faltan permisos"
            !acc -> "Falta activar CallingOxa en Accesibilidad"
            else -> "Todo listo"
        }
        permBtn.visibility = if (missing == -1) View.GONE else View.VISIBLE
        toggleLabel.text = if (Prefs.active(this)) "DESACTIVAR" else "ACTIVAR"
    }

    private fun toggleService() {
        saveAll()
        if (Prefs.active(this)) {
            Prefs.setActive(this, false)
            updateStatus()
            return
        }
        if (!VolumeAccessibilityService.running) {
            AlertDialog.Builder(this)
    .setTitle("Uso del servicio de accesibilidad")
    .setMessage(
        "CallingOxa usa el servicio de accesibilidad de Android únicamente para detectar cuándo pulsas los botones de volumen, incluso con la pantalla apagada. Así puede iniciar la llamada simulada que configuraste.\n\n" +
            "La app no lee el contenido de tu pantalla ni accede a tus mensajes, contraseñas u otras apps, y no recopila ni comparte datos personales con terceros.\n\n" +
            "Mientras CallingOxa esté activada, los botones de volumen no cambiarán el volumen del teléfono.\n\n" +
            "Para activarlo, habilita CallingOxa en los ajustes de Accesibilidad."
    )
    .setPositiveButton("Acepto") { _, _ ->
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
    .setNegativeButton("No acepto", null)
    .setCancelable(false)
    .show()
                .setMessage(
                    "En Accesibilidad, busca CallingOxa y actívalo. " +
                        "Luego vuelve aquí y pulsa Activar otra vez."
                )
                .setPositiveButton("Abrir Accesibilidad") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }
        Prefs.setActive(this, true)
        updateStatus()
    }
}
