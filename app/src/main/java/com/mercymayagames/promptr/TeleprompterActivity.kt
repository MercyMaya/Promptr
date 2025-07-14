package com.mercymayagames.promptr

import android.graphics.Color
import android.net.Uri
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.slider.Slider
import com.mercymayagames.promptr.databinding.ActivityTeleprompterBinding
import kotlinx.coroutines.*
import java.util.Locale

class TeleprompterActivity : AppCompatActivity() {

    companion object { const val EXTRA_FILE_URI = "file_uri" }

    private lateinit var ui: ActivityTeleprompterBinding

    /* ───────── Scrolling engine ───────── */
    private val handler = Handler(Looper.getMainLooper())
    private var sliderUnits = 5                 // 1–40  ➜ 0.1–4.0 px / frame
    private val pxPerTick get() = sliderUnits / 10f
    private var accumulator = 0f
    private var playing = false
    private val ticker = object : Runnable {
        override fun run() {
            if (playing) {
                accumulator += pxPerTick
                val delta = accumulator.toInt()
                if (delta > 0) {
                    ui.scrlScript.smoothScrollBy(0, delta)
                    accumulator -= delta
                }
                handler.postDelayed(this, 16)
            }
        }
    }

    /* ───────── Lifecycle ───────── */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityTeleprompterBinding.inflate(layoutInflater)
        setContentView(ui.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        intent.getStringExtra(EXTRA_FILE_URI)?.let { loadScript(Uri.parse(it)) }

        sliderUnits = Prefs.loadSpeed(this).coerceIn(1, 40)
        ui.speedSlider.value = sliderUnits.toFloat()
        ui.lblSpeed.text = getString(R.string.speed_label, pxPerTick.format1())

        val fontSp = Prefs.loadFont(this).coerceIn(12f, 96f)
        ui.fontSlider.value = fontSp
        ui.txtScript.textSize = fontSp
        ui.lblFont.text = getString(R.string.font_label, fontSp.toInt())

        applyTheme(Prefs.isDark(this))

        /* speed slider */
        ui.speedSlider.addOnChangeListener { _, v, user ->
            if (user) {
                sliderUnits = v.toInt()
                ui.lblSpeed.text = getString(R.string.speed_label, pxPerTick.format1())
                Prefs.saveSpeed(this, sliderUnits)
            }
        }
        ui.speedSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) { showOverlay() }
            override fun onStopTrackingTouch(slider: Slider) { scheduleHide() }
        })

        /* font slider */
        ui.fontSlider.addOnChangeListener { _, v, user ->
            if (user) {
                ui.txtScript.textSize = v
                ui.lblFont.text = getString(R.string.font_label, v.toInt())
                Prefs.saveFont(this, v)
            }
        }
        ui.fontSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) { showOverlay() }
            override fun onStopTrackingTouch(slider: Slider) { scheduleHide() }
        })

        /* play buttons */
        ui.fabPlayNow.setOnClickListener { togglePlayPause() }
        ui.fabCountdown.setOnClickListener { startCountdown() }
        ui.btnTheme.setOnClickListener   { toggleTheme() }

        /* tap script to reveal controls */
        ui.scrlScript.setOnTouchListener { v, e ->
            if (e.action == MotionEvent.ACTION_DOWN) {
                v.performClick()
                showOverlay(); scheduleHide()
            }
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(ticker)
    }

    /* ───────── Script loading ───────── */
    private fun loadScript(uri: Uri) = CoroutineScope(Dispatchers.IO).launch {
        val txt = FileTextExtractor.extractText(this@TeleprompterActivity, uri)
        withContext(Dispatchers.Main) { ui.txtScript.text = txt }
    }

    /* ───────── Play / Pause ───────── */
    private fun togglePlayPause(forcePlay: Boolean? = null) {
        val newState = forcePlay ?: !playing
        playing = newState
        ui.fabPlayNow.setImageResource(
            if (newState) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )
        if (newState) {
            handler.post(ticker)
            scheduleHide()
        } else {
            handler.removeCallbacks(ticker)
            showOverlay()
        }
    }

    /* ───────── Countdown ───────── */
    private fun startCountdown() {
        if (playing) return
        showOverlay()
        ui.tvCountdown.apply {
            text = "3"
            scaleX = 1f; scaleY = 1f; alpha = 1f
            isVisible = true
        }

        CoroutineScope(Dispatchers.Main).launch {
            for (n in 3 downTo 1) {
                ui.tvCountdown.text = n.toString()
                ui.tvCountdown.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(120).withEndAction {
                        ui.tvCountdown.animate()
                            .alpha(0f).scaleX(4f).scaleY(4f)
                            .setDuration(880).start()
                    }.start()
                delay(1000)
            }
            ui.tvCountdown.isVisible = false
            togglePlayPause(forcePlay = true)
        }
    }

    /* ───────── Overlay helpers ───────── */
    private val hideRunnable = Runnable { ui.overlayControls.visibility = View.INVISIBLE }
    private fun showOverlay() {
        ui.overlayControls.visibility = View.VISIBLE
        hideSystemBars()
        ui.overlayControls.removeCallbacks(hideRunnable)
    }
    private fun scheduleHide() = ui.overlayControls.postDelayed(hideRunnable, 3000)

    /* ───────── Theme ───────── */
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
        ui.lblSpeed.setTextColor(fg)
        ui.lblFont.setTextColor(fg)
        ui.tvCountdown.setTextColor(fg)
        ui.overlayControls.setBackgroundColor(if (dark) 0x66000000 else 0x66FFFFFF)
        ui.btnTheme.text = if (dark) getString(R.string.light) else getString(R.string.dark)
    }

    /* ───────── Immersive helper ───────── */
    private fun hideSystemBars() {
        window.insetsController?.let {
            it.hide(WindowInsets.Type.systemBars())
            it.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /* ───────── util ───────── */
    private fun Float.format1(): String =
        String.format(Locale.US, "%.1f", this)
}
