package com.explorebyte.ar.presentation.chatbot

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.explorebyte.ar.R
import com.explorebyte.ar.core.utils.NetworkUtils
import com.explorebyte.ar.domain.usecase.TutorSessionManager.SessionPhase

class ChatbotActivity : AppCompatActivity() {

    private val viewModel: ChatbotViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        val shapeType = intent.getStringExtra("SHAPE_TYPE") ?: "kubus"
        val sessionType = intent.getStringExtra("SESSION_TYPE") ?: "SOAL"
        viewModel.setShape(shapeType)

        setupUI()
        setupObservers()

        // Mulai sesi khusus (Evaluasi atau Soal)
        viewModel.startSession(sessionType)
    }

    private fun setupUI() {
        val rvChat = findViewById<RecyclerView>(R.id.rvChat)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        adapter = ChatAdapter()
        rvChat.adapter = adapter

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                if (NetworkUtils.isInternetAvailable(this)) {
                    viewModel.sendMessage(text)
                    etMessage.text.clear()
                } else {
                    // Fallback: kirim pesan tetap, akan dihandle oleh local feedback
                    viewModel.sendMessage(text)
                    etMessage.text.clear()
                }
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun setupObservers() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val rvChat = findViewById<RecyclerView>(R.id.rvChat)
        val sessionProgressLayout = findViewById<LinearLayout>(R.id.sessionProgressLayout)
        val tvPhaseLabel = findViewById<TextView>(R.id.tvPhaseLabel)
        val tvProgressText = findViewById<TextView>(R.id.tvProgressText)

        // Observe chat messages
        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                rvChat.scrollToPosition(messages.size - 1)
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe errors
        viewModel.error.observe(this) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        // Observe session phase untuk progress indicator
        viewModel.sessionPhase.observe(this) { phase ->
            when (phase) {
                SessionPhase.IDLE -> {
                    sessionProgressLayout.visibility = View.GONE
                }
                SessionPhase.SELESAI -> {
                    sessionProgressLayout.visibility = View.VISIBLE
                    tvPhaseLabel.text = "✅ Selesai"
                }
                else -> {
                    sessionProgressLayout.visibility = View.VISIBLE
                }
            }
        }

        // Observe progress text
        viewModel.progress.observe(this) { progressText ->
            tvProgressText.text = progressText
        }

        // Observe session phase label
        viewModel.sessionPhase.observe(this) { phase ->
            val label = when (phase) {
                SessionPhase.PERTANYAAN_ANAK -> "📊 Evaluasi"
                SessionPhase.EVALUASI -> "📝 Soal"
                SessionPhase.SELESAI -> "✅ Selesai"
                else -> ""
            }
            tvPhaseLabel.text = label
        }

        // Observe evaluation score
        viewModel.score.observe(this) { scoreData ->
            scoreData?.let { (correct, total) ->
                val percentage = if (total > 0) (correct * 100) / total else 0
                tvProgressText.text = "Skor: $correct/$total ($percentage%)"
            }
        }
    }
}
