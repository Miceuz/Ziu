package io.github.miceuz.folio

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import java.util.concurrent.Executors

/** PdfRenderer allows one open page at a time, so all rendering goes through one thread. */
class PdfDoc(resolver: ContentResolver, uri: Uri) : AutoCloseable {
    private val fd: ParcelFileDescriptor =
        resolver.openFileDescriptor(uri, "r") ?: error("no file descriptor")
    private val renderer = try {
        PdfRenderer(fd)
    } catch (e: Exception) {
        fd.close()
        throw e
    }
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    val pageCount = renderer.pageCount

    /** Page widths in points. */
    val widthPt = IntArray(pageCount)

    /** Page aspect ratios, height / width. */
    val aspect = FloatArray(pageCount) { i ->
        renderer.openPage(i).use {
            widthPt[i] = it.width
            it.height.toFloat() / it.width
        }
    }

    fun render(index: Int, width: Int, wanted: () -> Boolean, done: (Bitmap) -> Unit) {
        worker.execute {
            if (!wanted()) return@execute
            val height = (width * aspect[index]).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            renderer.openPage(index).use {
                it.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
            main.post { if (wanted()) done(bmp) }
        }
    }

    /** Render part of a page: [m] maps page points to pixels of a [w] x [h] bitmap. */
    fun renderRegion(index: Int, w: Int, h: Int, m: Matrix, wanted: () -> Boolean, done: (Bitmap) -> Unit) {
        worker.execute {
            if (!wanted()) return@execute
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            renderer.openPage(index).use {
                it.render(bmp, null, m, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
            main.post { if (wanted()) done(bmp) }
        }
    }

    override fun close() {
        worker.execute {
            renderer.close()
            fd.close()
        }
        worker.shutdown()
    }
}
