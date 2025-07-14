package com.mercymayagames.promptr

import android.content.Context
import androidx.core.content.edit

/**
 * Prefs – a microscopic wrapper around SharedPreferences.
 * We squirrel away speed, fontSize, and darkMode so the user
 * gets their last-used settings every launch.
 */
object Prefs {
    private const val FILE = "promptr_prefs"
    private const val KEY_SPEED = "scroll_speed"
    private const val KEY_FONT = "font_size_sp"
    private const val KEY_DARK = "dark_mode"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /* ---- Speed ---- */
    fun saveSpeed(ctx: Context, pxPerTick: Int) =
        prefs(ctx).edit { putInt(KEY_SPEED, pxPerTick) }

    fun loadSpeed(ctx: Context): Int = prefs(ctx).getInt(KEY_SPEED, 2)

    /* ---- Font size ---- */
    fun saveFont(ctx: Context, sizeSp: Float) =
        prefs(ctx).edit { putFloat(KEY_FONT, sizeSp) }

    fun loadFont(ctx: Context): Float = prefs(ctx).getFloat(KEY_FONT, 32f)

    /* ---- Dark / Light ---- */
    fun saveDark(ctx: Context, dark: Boolean) =
        prefs(ctx).edit { putBoolean(KEY_DARK, dark) }

    fun isDark(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_DARK, true)
}
