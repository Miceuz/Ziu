package io.github.miceuz.folio

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : Activity() {

    private lateinit var pages: ZoomRecyclerView
    private lateinit var openButton: Button
    private lateinit var counter: TextView
    private val layout by lazy { LinearLayoutManager(this) }

    private var doc: PdfDoc? = null
    private var adapter: PageAdapter? = null
    private var uri: Uri? = null

    private val hideCounter = Runnable { counter.animate().alpha(0f).setDuration(300) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
        pages = findViewById(R.id.pages)
        openButton = findViewById(R.id.open)
        counter = findViewById(R.id.counter)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            pages.setInsets(bars.top, bars.bottom)
            insets
        }

        pages.layoutManager = layout
        pages.setItemViewCacheSize(2)
        pages.onWidthChanged = ::onWidthChanged
        pages.onZoomSettled = { scale ->
            adapter?.let {
                it.renderScale = scale
                it.notifyItemRangeChanged(0, it.itemCount)
            }
        }
        pages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0) showCounter()
            }
        })

        openButton.setOnClickListener {
            val pick = Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/pdf")
            startActivityForResult(pick, REQ_OPEN)
        }

        val saved = savedInstanceState?.let { BundleCompat.getParcelable(it, KEY_URI, Uri::class.java) }
        (saved ?: intent?.data)?.let(::load)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OPEN && resultCode == RESULT_OK) data?.data?.let(::load)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        uri?.let { outState.putParcelable(KEY_URI, it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        doc?.close()
    }

    private fun load(u: Uri) {
        val d = try {
            PdfDoc(contentResolver, u)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.cant_open, Toast.LENGTH_LONG).show()
            return
        }
        doc?.close()
        doc = d
        uri = u
        val a = PageAdapter(d, resources.getDimensionPixelSize(R.dimen.page_gap))
        a.pageWidth = pageWidth(pages.width)
        adapter = a
        pages.resetZoom()
        pages.adapter = a
        pages.visibility = View.VISIBLE
        openButton.visibility = View.GONE
        showCounter()
    }

    private fun pageWidth(viewWidth: Int) =
        maxOf(0, viewWidth - 2 * resources.getDimensionPixelSize(R.dimen.page_gap))

    // Device rotated (or window resized): refit pages to the new width and keep
    // the same spot of the same page at the top of the screen.
    private fun onWidthChanged(width: Int) {
        val a = adapter ?: return
        val first = layout.findFirstVisibleItemPosition()
        var fraction = 0f
        if (first != RecyclerView.NO_POSITION) {
            val v = layout.findViewByPosition(first)
            if (v != null && v.height > 0) fraction = (pages.paddingTop - v.top).toFloat() / v.height
        }
        pages.resetZoom()
        a.renderScale = 1f
        a.pageWidth = pageWidth(width)
        a.notifyItemRangeChanged(0, a.itemCount)
        if (first != RecyclerView.NO_POSITION) {
            layout.scrollToPositionWithOffset(first, -(fraction * a.pageHeight(first)).toInt())
        }
    }

    private fun showCounter() {
        val first = layout.findFirstVisibleItemPosition()
        val count = adapter?.itemCount ?: return
        counter.text = "${maxOf(first, 0) + 1} / $count"
        counter.animate().cancel()
        counter.alpha = 1f
        counter.removeCallbacks(hideCounter)
        counter.postDelayed(hideCounter, 1200)
    }

    private companion object {
        const val REQ_OPEN = 1
        const val KEY_URI = "uri"
    }
}
