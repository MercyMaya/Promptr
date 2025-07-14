package com.mercymayagames.promptr

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mercymayagames.promptr.databinding.ActivityMainBinding

/**
 * MainActivity – shows saved script files.
 * • FAB adds a script via SAF
 * • Center prompt for first-time users
 * • Bottom hint when list has items
 * • Long-press entry → confirmation dialog → remove
 */
class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding
    private lateinit var adapter: ScriptAdapter

    /* SAF picker */
    private val picker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { persistAndAdd(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        /* Recycler */
        adapter = ScriptAdapter { uri -> openTeleprompter(uri) }
        ui.recycler.apply {
            adapter = this@MainActivity.adapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            )
        }

        ui.fabPickFile.setOnClickListener { launchPicker() }
    }

    override fun onResume() {
        super.onResume()
        adapter.submit(Prefs.loadScripts(this).toList())
        updatePrompts()
    }

    /* ---------- UI state ---------- */
    private fun updatePrompts() {
        val empty = adapter.itemCount == 0
        ui.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        ui.tvHint.visibility  = if (empty) View.GONE   else View.VISIBLE
    }

    /* ---------- picker helpers ---------- */
    private fun launchPicker() = picker.launch(
        arrayOf(
            "text/plain",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    )

    private fun persistAndAdd(uri: Uri) {
        /* ask SAF to hold permission long-term */
        contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        Prefs.addScript(this, uri.toString())
        adapter.submit(Prefs.loadScripts(this).toList())
        updatePrompts()
        openTeleprompter(uri)
    }

    /* ---------- navigation ---------- */
    private fun openTeleprompter(uri: Uri) =
        startActivity(
            Intent(this, TeleprompterActivity::class.java)
                .putExtra(TeleprompterActivity.EXTRA_FILE_URI, uri.toString())
        )

    /* ---------- Recycler ---------- */
    private inner class ScriptAdapter(
        private val click: (Uri) -> Unit
    ) : RecyclerView.Adapter<ScriptAdapter.Holder>() {

        private val items = mutableListOf<String>()

        fun submit(list: List<String>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): Holder =
            Holder(layoutInflater.inflate(android.R.layout.simple_list_item_1, p, false))

        override fun onBindViewHolder(h: Holder, pos: Int) =
            h.bind(Uri.parse(items[pos]))

        override fun getItemCount(): Int = items.size

        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
            private val tv: TextView = v.findViewById(android.R.id.text1)
            fun bind(uri: Uri) {
                tv.text = prettyName(uri)
                tv.setOnClickListener { click(uri) }
                tv.setOnLongClickListener {
                    confirmRemoval(uri)
                    true
                }
            }
        }
    }

    /* ---------- confirmation dialog ---------- */
    private fun confirmRemoval(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(R.string.remove_title)
            .setMessage(getString(R.string.remove_msg, prettyName(uri)))
            .setPositiveButton(R.string.remove) { _, _ ->
                Prefs.removeScript(this, uri.toString())
                adapter.submit(Prefs.loadScripts(this).toList())
                updatePrompts()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /* readable label from SAF */
    private fun prettyName(uri: Uri): String =
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && c.moveToFirst()) c.getString(idx)
            else uri.lastPathSegment ?: uri.toString()
        } ?: uri.toString()
}
