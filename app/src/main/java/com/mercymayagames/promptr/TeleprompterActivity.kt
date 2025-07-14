package com.mercymayagames.promptr

import android.graphics.Color
import android.net.Uri
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.mercymayagames.promptr.databinding.ActivityTeleprompterBinding
import kotlinx.coroutines.*

/**
 * TeleprompterActivity
 *
 * Features:
 *   • Scroll speed slider (1–20 px / 16 ms)
 *   • Font size slider  (12–96 sp)
 *   • Play / Pause FAB
 *   • Dark / Light toggle
 *   • Auto-hides overlay after 3 s of no touch
 *
 * All user tweaks persist in Prefs for the next launch.
 */
class TeleprompterActivity : AppCompatActivity() {

    companion object { const val EXTRA_FILE_URI = "file_uri" }

    private lateinit var ui: ActivityTeleprompterBinding
    private val lblSpeed: TextView by lazy { ui.lblSpeed }
    private val lblFont : TextView by lazy { ui.lblFont  }

    /* ── runtime state ────────────────────────── */
    private val scrollHandler = Handler(Looper.getMainLooper())
    private var scrollSpeedPx = 2           // overwritten from Prefs
    private var isPlaying = false
    private val scrollRunnable = object : Runnable {
        override fun run() {
            if (isPlaying) {
                ui.scrlScript.smoothScrollBy(0, scrollSpeedPx)
                scrollHandler.postDelayed(this, 16) // ≈60 fps
            }
        }
    }

    /* ── lifecycle ───────────────────────────── */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityTeleprompterBinding.inflate(layoutInflater)
        setContentView(ui.root)

        /* Load script (async) */
        intent.getStringExtra(EXTRA_FILE_URI)
            ?.let { loadScript(Uri.parse(it)) }

        /* Restore prefs */
        scrollSpeedPx = Prefs.loadSpeed(this).coerceIn(1, 20)
        ui.speedSlider.value = scrollSpeedPx.toFloat()
        lblSpeed.text = "Speed: $scrollSpeedPx"

        val fontSp = Prefs.loadFont(this).coerceIn(12f, 96f)
        ui.txtScript.textSize = fontSp
        ui.fontSlider.value = fontSp
        lblFont.text = "Font: ${fontSp.toInt()}sp"

        applyTheme(Prefs.isDark(this))

        /* Callbacks */
        ui.speedSlider.addOnChangeListener { _, v, fromUser ->
            if (fromUser) {
                scrollSpeedPx = v.toInt()
                lblSpeed.text = "Speed: $scrollSpeedPx"
                Prefs.saveSpeed(this, scrollSpeedPx)
            }
        }

        ui.fontSlider.addOnChangeListener { _, v, fromUser ->
            if (fromUser) {
                ui.txtScript.textSize = v
                lblFont.text = "Font: ${v.toInt()}sp"
                Prefs.saveFont(this, v)
            }
        }

        ui.fabPlayPause.setOnClickListener { togglePlayPause() }
        ui.btnTheme.setOnClickListener { toggleTheme() }

        ui.scrlScript.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_DOWN) showControlsTemporarily()
            false
        }

        togglePlayPause(forcePlay = true)
    }

    override fun onDestroy() {
        super.onDestroy()
        scrollHandler.removeCallbacks(scrollRunnable)
    }

    /* ── script I/O ──────────────────────────── */
    private fun loadScript(uri: Uri) = CoroutineScope(Dispatchers.IO).launch {
        val text = FileTextExtractor.extractText(this@TeleprompterActivity, uri)
        withContext(Dispatchers.Main) { ui.txtScript.text = text }
    }

    /* ── play / pause ────────────────────────── */
    private fun togglePlayPause(forcePlay: Boolean? = null) {
        val shouldPlay = forcePlay ?: !isPlaying
        isPlaying = shouldPlay
        ui.fabPlayPause.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
        if (isPlaying) scrollHandler.post(scrollRunnable)
    }

    /* ── overlay auto-hide ───────────────────── */
    private val hideRunnable = Runnable { ui.overlayControls.visibility = View.INVISIBLE }

    private fun showControlsTemporarily() {
        ui.overlayControls.visibility = View.VISIBLE
        hideSystemBars()
        ui.overlayControls.removeCallbacks(hideRunnable)
        ui.overlayControls.postDelayed(hideRunnable, 3000)
    }

    /* ── theme toggle ────────────────────────── */
    private fun toggleTheme() {
        val dark = !Prefs.isDark(this)
        Prefs.saveDark(this, dark)
        applyTheme(dark)
    }

    private fun applyTheme(dark: Boolean) {
        val bg = if (dark) Color.BLACK else Color.WHITE
        val fg = if (dark) Color.WHITE else Color.BLACK

        ui.root.setBackgroundColor(bg)
        ui.txtScript.setTextColor(fg)
        lblSpeed.setTextColor(fg)
        lblFont.setTextColor(fg)
        ui.overlayControls.setBackgroundColor(
            if (dark) 0x66000000 else 0x66FFFFFF
        )
        ui.btnTheme.text = if (dark) "LIGHT" else "DARK"
    }

    /* ── immersive helpers ───────────────────── */
    private fun hideSystemBars() {
        window.insetsController?.let {
            it.hide(WindowInsets.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
