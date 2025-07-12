package com.mercymayagames.promptr

import android.net.Uri
import android.os.Bundle
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.mercymayagames.promptr.databinding.ActivityMainBinding

/**
 * MainActivity – the “lobby” where we let the user pick a script file.
 * Storage Access Framework = hassle-free file picking.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding

    // Modern Activity-Result API for file picking
    private val pickFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { openTeleprompter(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.btnPickFile.setOnClickListener { launchPicker() }
    }

    private fun launchPicker() {
        pickFile.launch(
            arrayOf(
                "text/plain",
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
        )
    }

    private fun openTeleprompter(fileUri: Uri) {
        startActivity(
            Intent(this, TeleprompterActivity::class.java)
                .putExtra(TeleprompterActivity.EXTRA_FILE_URI, fileUri.toString())
        )
    }
}
