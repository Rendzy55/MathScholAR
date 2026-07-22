package com.explorebyte.ar.presentation.main

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.viewpager2.widget.ViewPager2
import com.explorebyte.ar.core.utils.ArCoreCheckHelper
import com.explorebyte.ar.R
import com.explorebyte.ar.presentation.chatbot.ChatbotActivity
import com.explorebyte.ar.presentation.pdf.PdfViewerActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * MainActivity sebagai Dashboard utama aplikasi MathScholAR.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var vpBanner: ViewPager2
    private val bannerHandler = Handler(Looper.getMainLooper())
    private val bannerRunnable = object : Runnable {
        override fun run() {
            val count = vpBanner.adapter?.itemCount ?: 0
            if (count > 0) {
                val nextItem = (vpBanner.currentItem + 1) % count
                vpBanner.currentItem = nextItem
                bannerHandler.postDelayed(this, 3000) // Slide every 3 seconds
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup UI Elements
        setupHeader()
        setupHeroBanner()
        setupContent()
    }

    private fun setupHeader() {
        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            // TODO: Sementara untuk testing PseudoARActivity — ganti kembali ke Profile nanti
            startActivity(Intent(this, com.explorebyte.ar.PseudoARActivity::class.java))
        }
    }

    private fun setupHeroBanner() {
        vpBanner = findViewById(R.id.vpBanner)
        
        // List images from assets/banners
        val bannerImages = assets.list("banners")?.toList() ?: emptyList()
        
        if (bannerImages.isNotEmpty()) {
            val adapter = BannerAdapter(bannerImages)
            vpBanner.adapter = adapter
            
            // Start auto slide
            bannerHandler.postDelayed(bannerRunnable, 3000)
        }

    }

    override fun onPause() {
        super.onPause()
        bannerHandler.removeCallbacks(bannerRunnable)
    }

    override fun onResume() {
        super.onResume()
        if (::vpBanner.isInitialized && vpBanner.adapter != null) {
            bannerHandler.postDelayed(bannerRunnable, 3000)
        }
    }

    private fun setupContent() {
        // Load illustrations from assets
        try {
            // Kubus
            val kubusStream = assets.open("ilustration/kubus_ils.jpg")
            findViewById<ImageView>(R.id.ivKubusIls).setImageBitmap(BitmapFactory.decodeStream(kubusStream))
            
            // Balok
            val balokStream = assets.open("ilustration/balok_ils.jpg")
            findViewById<ImageView>(R.id.ivBalokIls).setImageBitmap(BitmapFactory.decodeStream(balokStream))
            
            // Prisma
            val prismaStream = assets.open("ilustration/prisma_ils.jpg")
            findViewById<ImageView>(R.id.ivPrismaIls).setImageBitmap(BitmapFactory.decodeStream(prismaStream))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val overlayKubus = findViewById<android.view.View>(R.id.overlayKubus)
        val overlayBalok = findViewById<android.view.View>(R.id.overlayBalok)
        val overlayPrisma = findViewById<android.view.View>(R.id.overlayPrisma)

        val handleImageClick: (android.view.View, String, String) -> Unit = { overlay, pdfFile, title ->
            overlay.visibility = android.view.View.VISIBLE
            overlay.postDelayed({
                openFlipbook(pdfFile, title)
                overlay.visibility = android.view.View.GONE
            }, 500)
        }

        // Image Click Listeners (Show Overlay)
        findViewById<ImageView>(R.id.ivKubusIls).setOnClickListener { handleImageClick(overlayKubus, "Flipbook_kubus.pdf", "Mainan Kubus di Playground") }
        findViewById<ImageView>(R.id.ivBalokIls).setOnClickListener { handleImageClick(overlayBalok, "Flipbook_balok.pdf", "Misteri kotak Penyimpanan") }
        findViewById<ImageView>(R.id.ivPrismaIls).setOnClickListener { handleImageClick(overlayPrisma, "Flipbook_prisma.pdf", "Atap Unik di Taman Bermain") }

        // Set Click Listeners for Cards (Fallback for entire card area)
        findViewById<CardView>(R.id.cardKubus).setOnClickListener { openFlipbook("Flipbook_kubus.pdf", "Mainan Kubus di Playground") }
        findViewById<CardView>(R.id.cardBalok).setOnClickListener { openFlipbook("Flipbook_balok.pdf", "Misteri kotak Penyimpanan") }
        findViewById<CardView>(R.id.cardPrisma).setOnClickListener { openFlipbook("Flipbook_prisma.pdf", "Atap Unik di Taman Bermain") }
    }

    private fun openFlipbook(fileName: String, title: String) {
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra("PDF_FILE", fileName)
            putExtra("PDF_TITLE", title)
        }
        startActivity(intent)
    }
}
