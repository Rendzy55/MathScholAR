package com.explorebyte.ar.presentation.chatbot

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.explorebyte.ar.BuildConfig
import com.explorebyte.ar.data.remote.ApiService
import com.explorebyte.ar.data.remote.ChatMessage
import com.explorebyte.ar.data.remote.GroqRequest
import com.explorebyte.ar.data.remote.GroqResponse
import com.explorebyte.ar.domain.model.KeywordMatcher
import com.explorebyte.ar.domain.model.QuestionItem
import com.explorebyte.ar.domain.model.ShapeData
import com.explorebyte.ar.domain.usecase.TutorSessionManager
import com.explorebyte.ar.domain.usecase.TutorSessionManager.SessionPhase
import io.ktor.client.call.body
import io.ktor.http.isSuccess
import io.sentry.Sentry
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

data class Message(val text: String, val isUser: Boolean)

class ChatbotViewModel(application: Application) : AndroidViewModel(application) {

    // ─── UI State ────────────────────────────────────────────────────────
    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _sessionPhase = MutableLiveData<SessionPhase>(SessionPhase.IDLE)
    val sessionPhase: LiveData<SessionPhase> = _sessionPhase

    private val _progress = MutableLiveData<String>("")
    val progress: LiveData<String> = _progress

    private val _score = MutableLiveData<Pair<Int, Int>?>(null)
    val score: LiveData<Pair<Int, Int>?> = _score

    // ─── Core Components ─────────────────────────────────────────────────
    private val sessionManager = TutorSessionManager()
    private val conversationHistory = mutableListOf<ChatMessage>()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ─── Initialization ──────────────────────────────────────────────────

    /**
     * Memuat data bangun ruang dari assets dan inisialisasi session manager.
     */
    fun setShape(shape: String) {
        try {
            val jsonString = getApplication<Application>().assets
                .open("data/evaluation_data.json")
                .bufferedReader().use { it.readText() }

            val allData: Map<String, ShapeData> = json.decodeFromString(jsonString)
            val shapeData = allData[shape.lowercase()]

            if (shapeData != null) {
                sessionManager.loadShapeData(shapeData)
            } else {
                _error.value = "Data untuk bangun '$shape' tidak ditemukan."
            }
        } catch (e: Exception) {
            _error.value = "Gagal memuat data soal: ${e.localizedMessage}"
            Sentry.captureException(e)
        }
    }

    /**
     * Memulai sesi dari awal (Quiz → Pertanyaan Anak → Evaluasi).
     */
    fun startSession() {
        conversationHistory.clear()
        val firstQuestion = sessionManager.startSession()

        updatePhaseState()

        if (firstQuestion != null) {
            val phaseLabel = sessionManager.getPhaseLabel()
            val greeting = buildGreetingMessage(firstQuestion, phaseLabel)
            appendAiMessage(greeting)
            conversationHistory.add(ChatMessage(role = "assistant", content = greeting))
        } else {
            appendAiMessage("Maaf, tidak ada soal yang tersedia untuk bangun ruang ini.")
        }
    }

    // ─── Message Handling ────────────────────────────────────────────────

    /**
     * Mengirim jawaban siswa. Proses:
     * 1. Validasi lokal (Keyword Matching)
     * 2. Context Injection ke system prompt
     * 3. Kirim ke Groq API untuk respons natural
     */
    fun sendMessage(userPrompt: String) {
        if (sessionManager.currentPhase == SessionPhase.SELESAI ||
            sessionManager.currentPhase == SessionPhase.IDLE) {
            return
        }

        // Tambahkan pesan siswa ke UI
        appendUserMessage(userPrompt)

        // 1. Evaluasi lokal
        val answerResult = sessionManager.evaluateAnswer(userPrompt)
        if (answerResult == null) {
            appendAiMessage("Terjadi kesalahan. Sesi tidak dapat dilanjutkan.")
            return
        }

        // 2. Build context injection berdasarkan hasil lokal
        val currentQuestion = sessionManager.getCurrentQuestion()
        val fullSystemPrompt = buildSystemPrompt(currentQuestion, answerResult)

        // Update system prompt di conversation history
        if (conversationHistory.isEmpty()) {
            conversationHistory.add(ChatMessage(role = "system", content = fullSystemPrompt))
        } else {
            conversationHistory[0] = ChatMessage(role = "system", content = fullSystemPrompt)
        }
        conversationHistory.add(ChatMessage(role = "user", content = userPrompt))

        // 3. Kirim ke Groq API
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = ApiService.getGroqChatCompletions(
                    token = "Bearer ${BuildConfig.GROQ_API_KEY}",
                    request = GroqRequest(messages = conversationHistory.toList())
                )

                if (response.status.isSuccess()) {
                    val groqResponse = response.body<GroqResponse>()
                    val aiContent = groqResponse.choices?.firstOrNull()?.message?.content
                    if (!aiContent.isNullOrBlank()) {
                        processAiResponse(aiContent, answerResult)
                    } else {
                        // Fallback: gunakan feedback lokal jika AI tidak merespons
                        handleWithLocalFeedback(answerResult)
                    }
                } else {
                    // Fallback ke feedback lokal saat API error
                    handleWithLocalFeedback(answerResult)
                    _error.value = "Gangguan layanan AI (${response.status.value}). Menggunakan feedback lokal."
                }
            } catch (e: Exception) {
                // Fallback ke feedback lokal saat network error
                handleWithLocalFeedback(answerResult)
                _error.value = "Koneksi bermasalah. Menggunakan feedback lokal."
                Sentry.captureException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ─── AI Response Processing ──────────────────────────────────────────

    private fun processAiResponse(
        aiContent: String,
        answerResult: TutorSessionManager.AnswerResult
    ) {
        // Bersihkan tag kontrol dari respons AI
        val cleanContent = aiContent
            .replace("BENAR_NEXT", "")
            .replace("RETRY", "")
            .replace("MAX_ATTEMPT", "")
            .trim()

        appendAiMessage(cleanContent)
        conversationHistory.add(ChatMessage(role = "assistant", content = aiContent))

        // (Jawaban benar sekarang disertakan langsung oleh AI berkat instruksi di system prompt)

        // Tentukan langkah selanjutnya berdasarkan hasil lokal (bukan AI)
        if (answerResult.shouldAdvance) {
            advanceSession()
        }
    }

    /**
     * Fallback ketika API tidak tersedia — gunakan feedback dari JSON.
     */
    private fun handleWithLocalFeedback(answerResult: TutorSessionManager.AnswerResult) {
        appendAiMessage(answerResult.feedback)

        if (answerResult.isMaxAttempts && answerResult.jawabanBenar != null) {
            appendAiMessage("📖 Jawaban yang benar:\n${answerResult.jawabanBenar}")
        }

        if (answerResult.shouldAdvance) {
            advanceSession()
        }
    }

    // ─── Session Flow ────────────────────────────────────────────────────

    private fun advanceSession() {
        val previousPhase = sessionManager.currentPhase
        val nextQuestion = sessionManager.advanceToNext()
        val currentPhase = sessionManager.currentPhase
        updatePhaseState()

        if (nextQuestion != null) {
            // Cek apakah ada transisi fase
            if (previousPhase != currentPhase) {
                val transitionMsg = getPhaseTransitionMessage()
                if (transitionMsg != null) {
                    appendAiMessage(transitionMsg)
                }
            }

            val questionMsg = "📝 ${nextQuestion.pertanyaan}"
            appendAiMessage(questionMsg)
            conversationHistory.add(ChatMessage(role = "assistant", content = questionMsg))
        } else {
            // Sesi selesai
            handleSessionComplete()
        }
    }

    private fun handleSessionComplete() {
        val scoreData = sessionManager.getEvaluationScore()
        _score.value = scoreData

        val completionMsg = buildCompletionMessage(scoreData)
        appendAiMessage(completionMsg)
    }

    // ─── System Prompt Builder (Context Injection) ───────────────────────

    /**
     * Membangun system prompt yang HANYA berisi data soal aktif + hasil keyword matching.
     * Ini adalah inti dari "Context Injection" — hemat token karena tidak kirim seluruh JSON.
     */
    private fun buildSystemPrompt(
        question: QuestionItem?,
        answerResult: TutorSessionManager.AnswerResult
    ): String {
        val basePrompt = """
            Kamu adalah Tutor AI bernama "MathScholAR" untuk siswa SD kelas 5-6.
            Tugasmu saat ini adalah menjalankan sesi ${sessionManager.getPhaseLabel()}.
            
            ATURAN PENTING:
            1. Kamu adalah PENGUJI, bukan pembimbing. Jangan memberikan bimbingan Polya yang panjang.
            2. Fokus pada VALIDASI jawaban siswa. 
            3. Gunakan bahasa Indonesia yang ramah dan menyemangati.
            4. Jangan pernah memberikan jawaban langsung kecuali siswa sudah gagal 2 kali percobaan.
            5. Respons harus singkat (2-3 kalimat), tidak bertele-tele.
            6. Gunakan cetak tebal (**bold**) pada kata kunci penting agar lebih menonjol di aplikasi.
        """.trimIndent()

        if (question == null) {
            return "$basePrompt\n\nSesi telah selesai. Berikan apresiasi kepada siswa."
        }

        val questionJson = json.encodeToString(question)
        val matchResultLabel = when (answerResult.matchResult) {
            KeywordMatcher.MatchResult.BENAR -> "BENAR"
            KeywordMatcher.MatchResult.SEBAGIAN -> "SEBAGIAN"
            KeywordMatcher.MatchResult.SALAH -> "SALAH"
        }

        val contextPrompt = """
            
            KONTEKS SOAL AKTIF:
            Saat ini siswa sedang mengerjakan soal dengan data berikut: $questionJson
            
            HASIL KEYWORD MATCHING LOKAL:
            - Kategori: $matchResultLabel
            - Kata kunci cocok: ${answerResult.matchCount}/${answerResult.totalKeywords}
            - Percobaan ke: ${sessionManager.attempts}/${TutorSessionManager.MAX_ATTEMPTS}
            
            INSTRUKSI FEEDBACK:
            ${buildFeedbackInstruction(answerResult)}
        """.trimIndent()

        return "$basePrompt\n$contextPrompt"
    }

    private fun buildFeedbackInstruction(answerResult: TutorSessionManager.AnswerResult): String {
        return when {
            answerResult.matchResult == KeywordMatcher.MatchResult.BENAR -> {
                """
                Jawaban siswa BENAR. Berikan feedback positif sesuai skrip: "${answerResult.feedback}"
                Tambahkan sedikit variasi agar terasa natural, tapi JANGAN ubah makna inti feedbacknya.
                Akhiri responmu dengan kata BENAR_NEXT.
                """.trimIndent()
            }
            answerResult.isMaxAttempts -> {
                val correctAnswer = answerResult.jawabanBenar ?: ""
                """
                Siswa sudah mencoba ${TutorSessionManager.MAX_ATTEMPTS} kali dan masih salah.
                Sampaikan secara ramah bahwa jawabannya belum tepat, lalu berikan jawaban yang benar berikut ini:
                "$correctAnswer"
                Akhiri responmu dengan kata MAX_ATTEMPT.
                """.trimIndent()
            }
            answerResult.matchResult == KeywordMatcher.MatchResult.SEBAGIAN -> {
                """
                Jawaban siswa SEBAGIAN BENAR. Berikan pertanyaan pemantik sesuai skrip: "${answerResult.feedback}"
                Beri tahu bahwa siswa masih punya 1 kesempatan lagi.
                Akhiri responmu dengan kata RETRY.
                """.trimIndent()
            }
            else -> {
                """
                Jawaban siswa SALAH. Berikan hint/petunjuk materi sesuai skrip: "${answerResult.feedback}"
                Beri tahu bahwa siswa masih punya 1 kesempatan lagi.
                Akhiri responmu dengan kata RETRY.
                """.trimIndent()
            }
        }
    }

    // ─── UI Message Helpers ──────────────────────────────────────────────

    private fun appendUserMessage(text: String) {
        val updated = _messages.value.orEmpty().toMutableList()
        updated.add(Message(text, isUser = true))
        _messages.value = updated
    }

    private fun appendAiMessage(text: String) {
        val updated = _messages.value.orEmpty().toMutableList()
        updated.add(Message(text, isUser = false))
        _messages.value = updated
    }

    private fun updatePhaseState() {
        _sessionPhase.value = sessionManager.currentPhase
        _progress.value = sessionManager.getProgressText()
    }

    // ─── Message Builders ────────────────────────────────────────────────

    private fun buildGreetingMessage(question: QuestionItem, phaseLabel: String): String {
        return """
            Halo! 👋 Selamat datang di sesi belajar MathScholAR.
            
            Kita akan mulai dari $phaseLabel.
            
            📝 ${question.pertanyaan}
        """.trimIndent()
    }

    private fun getPhaseTransitionMessage(): String? {
        return when (sessionManager.currentPhase) {
            SessionPhase.PERTANYAAN_ANAK -> {
                "📊 Baik! Sekarang saatnya Evaluasi. Kamu akan menjawab beberapa soal untuk mengukur pemahamanmu. Semangat! 💪"
            }
            SessionPhase.EVALUASI -> {
                "Halo, teman hebat! 😊 Yuk, kita selesaikan masalah pada cerita tadi bersama-sama. Kita akan mengerjakannya langkah demi langkah. Jawablah setiap pertanyaan dengan bahasamu sendiri ya. Kamu memiliki maksimal dua kesempatan untuk menjawab setiap pertanyaan."
            }
            else -> null
        }
    }

    private fun buildCompletionMessage(scoreData: Pair<Int, Int>?): String {
        val sb = StringBuilder()
        sb.appendLine("🎉 Selamat! Kamu telah menyelesaikan seluruh sesi belajar!")

        if (scoreData != null) {
            val (correct, total) = scoreData
            val percentage = sessionManager.getEvaluationPercentage() ?: 0
            sb.appendLine()
            sb.appendLine("📊 Hasil Evaluasi:")
            sb.appendLine("   Benar: $correct dari $total soal ($percentage%)")
            sb.appendLine()

            when {
                percentage >= 80 -> sb.appendLine("🌟 Luar biasa! Pemahamanmu sangat baik!")
                percentage >= 60 -> sb.appendLine("👍 Bagus! Terus tingkatkan pemahamanmu ya!")
                else -> sb.appendLine("💪 Jangan menyerah! Coba pelajari lagi materinya dan ulangi evaluasinya.")
            }
        }

        return sb.toString().trim()
    }

    // ─── Legacy Compatibility ────────────────────────────────────────────

    /**
     * @deprecated Gunakan startSession() untuk alur lengkap.
     */
    fun startEvaluation() {
        startSession()
    }
}
