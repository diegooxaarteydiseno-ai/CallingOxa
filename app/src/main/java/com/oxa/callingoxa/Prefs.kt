package com.oxa.callingoxa

import android.content.Context
import java.io.File

object Prefs {
    private fun sp(c: Context) =
        c.getSharedPreferences("callingoxa", Context.MODE_PRIVATE)

    fun name(c: Context): String = sp(c).getString("name", "Desconocido") ?: "Desconocido"
    fun number(c: Context): String = sp(c).getString("number", "") ?: ""
    fun upSeconds(c: Context): Int = sp(c).getInt("up", 5)
    fun downSeconds(c: Context): Int = sp(c).getInt("down", 10)
    fun active(c: Context): Boolean = sp(c).getBoolean("active", false)
    fun setupDone(c: Context): Boolean = sp(c).getBoolean("setupDone", false)

    fun save(c: Context, name: String, number: String, up: Int, down: Int) {
        sp(c).edit()
            .putString("name", name)
            .putString("number", number)
            .putInt("up", up)
            .putInt("down", down)
            .apply()
    }

    fun setActive(c: Context, v: Boolean) {
        sp(c).edit().putBoolean("active", v).apply()
    }

    fun setSetupDone(c: Context) {
        sp(c).edit().putBoolean("setupDone", true).apply()
    }

    fun photoFile(c: Context): File = File(c.filesDir, "contact.jpg")
}
