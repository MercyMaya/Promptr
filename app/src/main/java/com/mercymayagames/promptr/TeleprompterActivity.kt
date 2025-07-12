package com.mercymayagames.promptr

import android.net.Uri
import android.os.*
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mercymayagames.promptr.databinding.ActivityTeleprompterBinding
import kotlinx.coroutines.*

/**
 * TeleprompterActivity – displays the script and auto-scrolls it.
 * We’ll wire fancy controls later; for now text just loads & starts scrolling.
 */
class TeleprompterActivity : AppCompatActivity() {

    companion object { const val EXTRA_FILE_URI = "file_uri" }

    private lateinit var ui: ActivityTeleprompterBinding

    private val scrollHandler = Handler(Looper.getMainLooper())
    private val scrollRunnable = object : Runnable {
        override fun run() {
            ui.scrlScript.smoothScrollBy(0, 2)      // 2px per tick
            scrollHandler.postDelayed(this, 16)     // ~60fps
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityTeleprompterBinding.inflate(layoutInflater)
        setContentView(ui.root)

        intent.getStringExtra(EXTRA_FILE_URI)?.let { uriString ->
            loadScript(Uri.parse(uriString))
        }

        // Toggle overlay on tap (not wired yet, placeholder)
        ui.scrlScript.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                ui.overlayControls.visibility =
                    if (ui.overlayControls.visibility == View.VISIBLE) View.INVISIBLE
                    else View.VISIBLE
            }
            false
        }
    }

    private fun loadScript(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            val text = FileTextExtractor.extractText(this@TeleprompterActivity, uri)
            withContext(Dispatchers.Main) {
                ui.txtScript.text = text
                startScrolling()
            }
        }
    }

    private fun startScrolling() = scrollHandler.post(scrollRunnable)
    override fun onDestroy() { super.onDestroy(); scrollHandler.removeCallbacks(scrollRunnable) }
}
