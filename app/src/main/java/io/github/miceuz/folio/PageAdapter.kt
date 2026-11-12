package io.github.miceuz.folio

import android.graphics.Color
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min
import kotlin.math.sqrt

class PageAdapter(private val doc: PdfDoc, private val gap: Int) :
    RecyclerView.Adapter<PageAdapter.Holder>() {

    class Holder(val image: ImageView) : RecyclerView.ViewHolder(image) {
        @Volatile var token = 0
        var shownPage = -1
        var shownWidth = 0
    }

    /** On-screen page width in px at zoom 1. */
    var pageWidth = 0

    /** Bitmaps are rendered at pageWidth * renderScale. */
    var renderScale = 1f

    override fun getItemCount() = doc.pageCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val iv = ImageView(parent.context)
        iv.scaleType = ImageView.ScaleType.FIT_XY
        iv.setBackgroundColor(Color.WHITE)
        iv.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
        return Holder(iv)
    }

    fun pageHeight(page: Int) = (pageWidth * doc.aspect[page]).toInt()

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val lp = holder.image.layoutParams as RecyclerView.LayoutParams
        lp.height = pageHeight(position)
        lp.setMargins(gap, gap / 2, gap, gap / 2)
        holder.image.layoutParams = lp

        if (holder.shownPage != position) {
            holder.image.setImageBitmap(null)
            holder.shownPage = -1
            holder.shownWidth = 0
        }
        val width = targetWidth(position)
        if (width <= 0 || (holder.shownPage == position && holder.shownWidth == width)) return

        val token = ++holder.token
        doc.render(position, width, { holder.token == token }) { bmp ->
            holder.image.setImageBitmap(bmp)
            holder.shownPage = position
            holder.shownWidth = width
        }
    }

    override fun onViewRecycled(holder: Holder) {
        holder.token++
        holder.image.setImageBitmap(null)
        holder.shownPage = -1
        holder.shownWidth = 0
    }

    private fun targetWidth(page: Int): Int {
        val w = pageWidth * renderScale
        val maxW = sqrt(MAX_PIXELS / doc.aspect[page])
        return min(w, maxW).toInt()
    }

    private companion object {
        const val MAX_PIXELS = 8_000_000f
    }
}
