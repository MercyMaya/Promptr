package com.mercymayagames.promptr

import android.content.Context
import androidx.core.content.edit

/**
 * Prefs – ultra-light SharedPreferences wrapper.
 * Stores:
 *   • scroll speed units (Int 1-40)
 *   • font size (Float 12–96)
 *   • dark mode (Boolean)
 *   • saved script URIs (Set<String>)
 */
object Prefs {
    private const val FILE       = "promptr_prefs"
    private const val KEY_SPEED  = "scroll_speed_units"
    private const val KEY_FONT   = "font_size_sp"
    private const val KEY_DARK   = "dark_mode"
    private const val KEY_SCRIPTS = "saved_scripts"  // set of uri strings

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /* ---- speed ---- */
    fun saveSpeed(ctx: Context, units: Int) =
        prefs(ctx).edit { putInt(KEY_SPEED, units) }
    fun loadSpeed(ctx: Context): Int = prefs(ctx).getInt(KEY_SPEED, 5)

    /* ---- font ---- */
    fun saveFont(ctx: Context, sizeSp: Float) =
        prefs(ctx).edit { putFloat(KEY_FONT, sizeSp) }
    fun loadFont(ctx: Context): Float = prefs(ctx).getFloat(KEY_FONT, 32f)

    /* ---- theme ---- */
    fun saveDark(ctx: Context, dark: Boolean) =
        prefs(ctx).edit { putBoolean(KEY_DARK, dark) }
    fun isDark(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_DARK, true)

    /* ---- script URIs ---- */
    fun addScript(ctx: Context, uri: String) =
        prefs(ctx).edit {
            val set = loadScripts(ctx).toMutableSet()
            set += uri
            putStringSet(KEY_SCRIPTS, set)
        }

    fun removeScript(ctx: Context, uri: String) =
        prefs(ctx).edit {
            val set = loadScripts(ctx).toMutableSet()
            set -= uri
            putStringSet(KEY_SCRIPTS, set)
        }

    fun loadScripts(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_SCRIPTS, emptySet()) ?: emptySet()
}
