package com.explorebyte.ar.presentation.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.explorebyte.ar.R

class PdfPagerAdapter(private val renderer: PdfRenderer) :
    RecyclerView.Adapter<PdfPagerAdapter.PdfViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_page, parent, false)
        return PdfViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.bind(position, renderer)
    }

    override fun onViewRecycled(holder: PdfViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    override fun getItemCount(): Int = renderer.pageCount

    class PdfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPage: ImageView = itemView.findViewById(R.id.ivPdfPage)
        private var currentBitmap: Bitmap? = null

        fun bind(position: Int, renderer: PdfRenderer) {
            // Clean up previous state before binding new one
            recycle()
            
            try {
                val page = renderer.openPage(position)
                
                // Render with slightly optimized quality to prevent OOM on fast scroll
                val bitmap = Bitmap.createBitmap(
                    (page.width * 1.5).toInt(), 
                    (page.height * 1.5).toInt(), 
                    Bitmap.Config.ARGB_8888
                )
                
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                ivPage.setImageBitmap(bitmap)
                currentBitmap = bitmap
                page.close()
                
                // Force view refresh as requested
                ivPage.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun recycle() {
            ivPage.setImageBitmap(null)
            currentBitmap?.recycle()
            currentBitmap = null
        }
    }
}
