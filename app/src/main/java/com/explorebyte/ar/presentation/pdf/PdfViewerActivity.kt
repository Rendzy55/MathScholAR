package com.explorebyte.ar.presentation.pdf

import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.explorebyte.ar.core.utils.ArCoreCheckHelper
import com.explorebyte.ar.R
import com.explorebyte.ar.presentation.chatbot.ChatbotActivity
import java.io.File
import java.io.FileOutputStream

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tvPageNumber: TextView
    private lateinit var tvPdfTitle: TextView
    private lateinit var btnAction: Button
    
    private var pdfRenderer: PdfRenderer? = null
    private var parcelFileDescriptor: ParcelFileDescriptor? = null
    private var lastActionType: ActionType? = null
    private var pdfFileName: String = ""

    enum class ActionType { AR, AI, NONE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        pdfFileName = intent.getStringExtra("PDF_FILE") ?: "kubus.pdf"
        val title = intent.getStringExtra("PDF_TITLE") ?: "Modul Geometri"

        viewPager = findViewById(R.id.viewPagerPdf)
        tvPageNumber = findViewById(R.id.tvPageNumber)
        tvPdfTitle = findViewById(R.id.tvPdfTitle)
        tvPdfTitle.text = title
        btnAction = findViewById(R.id.btnLihatAr)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        setupPdfRenderer(pdfFileName)
        setupViewPager()

        findViewById<ImageButton>(R.id.btnPrevPage).setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }

        findViewById<ImageButton>(R.id.btnNextPage).setOnClickListener {
            val count = viewPager.adapter?.itemCount ?: 0
            if (viewPager.currentItem < count - 1) {
                viewPager.currentItem += 1
            }
        }
    }

    private fun setupPdfRenderer(fileName: String) {
        try {
            val file = File(cacheDir, fileName)
            // Selalu override file di cache agar jika ada update di asset langsung terbaca
            assets.open("pdf/$fileName").use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupViewPager() {
        pdfRenderer?.let {
            val adapter = PdfPagerAdapter(it)
            viewPager.adapter = adapter
            viewPager.setPageTransformer(BookFlipTransformer())
            
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    android.util.Log.d("PDF_DEBUG", "Target Page: ${position + 1}")
                    updateActionButton(position + 1)
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    // Update the text ONLY when the page has fully settled (IDLE)
                    // This fixes the bug where text changes before the image
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        val settledPage = viewPager.currentItem + 1
                        tvPageNumber.text = "$settledPage dari ${it.pageCount}"
                        updateActionButton(settledPage)
                        viewPager.invalidate()
                    }
                }
            })
            
            // Initial state
            tvPageNumber.text = "1 dari ${it.pageCount}"
            updateActionButton(1)
        }
    }

    private fun updateActionButton(currentPage: Int) {
        val targetAction = when {
            currentPage == 24 -> ActionType.AI
            pdfFileName.contains("kubus", ignoreCase = true) && currentPage == 11 -> ActionType.AR
            pdfFileName.contains("balok", ignoreCase = true) && currentPage == 10 -> ActionType.AR
            pdfFileName.contains("prisma", ignoreCase = true) && currentPage == 10 -> ActionType.AR
            else -> ActionType.NONE
        }

        // Only update if the type has changed to avoid flickering
        if (lastActionType == targetAction) return
        lastActionType = targetAction

        when (targetAction) {
            ActionType.AI -> {
                btnAction.visibility = View.VISIBLE
                btnAction.text = "Tanya AI Asisten"
                btnAction.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_chatbot, 0, 0, 0)
                btnAction.setOnClickListener {
                    val intent = Intent(this, ChatbotActivity::class.java)
                    val shapeType = when {
                        pdfFileName.contains("kubus", ignoreCase = true) -> "KUBUS"
                        pdfFileName.contains("balok", ignoreCase = true) -> "BALOK"
                        pdfFileName.contains("prisma", ignoreCase = true) -> "PRISMA"
                        else -> "KUBUS"
                    }
                    intent.putExtra("SHAPE_TYPE", shapeType)
                    startActivity(intent)
                }
            }
            ActionType.AR -> {
                btnAction.visibility = View.VISIBLE
                btnAction.text = "Lihat AR"
                btnAction.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_nav_ar, 0, 0, 0)
                btnAction.setOnClickListener {
                    val shapeType = when {
                        pdfFileName.contains("kubus", ignoreCase = true) -> "KUBUS"
                        pdfFileName.contains("balok", ignoreCase = true) -> "BALOK"
                        pdfFileName.contains("prisma", ignoreCase = true) -> "PRISMA"
                        else -> "KUBUS"
                    }
                    ArCoreCheckHelper.openARFeature(this, shapeType)
                }
            }
            ActionType.NONE -> {
                btnAction.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close()
        parcelFileDescriptor?.close()
    }
}
