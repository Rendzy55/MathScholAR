package com.explorebyte.ar.domain.usecase

import com.explorebyte.ar.domain.model.KeywordMatcher
import com.explorebyte.ar.domain.model.QuestionItem
import com.explorebyte.ar.domain.model.ShapeData

/**
 * State machine untuk mengelola alur sesi Tutor AI.
 *
 * Alur: QUIZ (1 soal) → PERTANYAAN_ANAK (N soal) → EVALUASI (5 soal) → SELESAI
 *
 * Setiap soal memiliki maksimal 2 percobaan. Jika percobaan ke-2 masih salah,
 * jawaban benar diberikan dan sesi lanjut ke soal berikutnya.
 */
class TutorSessionManager {

    enum class SessionPhase {
        IDLE,
        QUIZ,
        PERTANYAAN_ANAK,
        EVALUASI,
        SELESAI
    }

    /**
     * Hasil evaluasi jawaban siswa.
     */
    data class AnswerResult(
        val matchResult: KeywordMatcher.MatchResult,
        val feedback: String,
        val matchCount: Int,
        val totalKeywords: Int,
        val isMaxAttempts: Boolean,
        val jawabanBenar: String?,
        val shouldAdvance: Boolean
    )

    var currentPhase: SessionPhase = SessionPhase.IDLE
        private set

    var currentQuestionIndex: Int = 0
        private set

    var attempts: Int = 0
        private set

    private var shapeData: ShapeData? = null

    // Skor evaluasi
    private var evaluationCorrectCount: Int = 0
    private var evaluationTotalAttempted: Int = 0

    companion object {
        const val MAX_ATTEMPTS = 2
    }

    /**
     * Memuat data bangun ruang dan memulai sesi.
     */
    fun loadShapeData(data: ShapeData) {
        shapeData = data
        currentPhase = SessionPhase.IDLE
        currentQuestionIndex = 0
        attempts = 0
        evaluationCorrectCount = 0
        evaluationTotalAttempted = 0
    }

    /**
     * Memulai sesi dari fase pertama (QUIZ).
     * @return Pertanyaan pertama dari quiz, atau null jika data kosong.
     */
    fun startSession(): QuestionItem? {
        val data = shapeData ?: return null

        // Mulai dari quiz jika ada
        if (data.quiz != null) {
            currentPhase = SessionPhase.QUIZ
            currentQuestionIndex = 0
            attempts = 0
            return data.quiz
        }

        // Skip ke pertanyaan anak jika quiz kosong
        if (data.pertanyaanAnak.isNotEmpty()) {
            currentPhase = SessionPhase.PERTANYAAN_ANAK
            currentQuestionIndex = 0
            attempts = 0
            return data.pertanyaanAnak[0]
        }

        // Skip ke evaluasi jika pertanyaan anak juga kosong
        if (data.evaluasi.isNotEmpty()) {
            currentPhase = SessionPhase.EVALUASI
            currentQuestionIndex = 0
            attempts = 0
            return data.evaluasi[0]
        }

        currentPhase = SessionPhase.SELESAI
        return null
    }

    /**
     * Mendapatkan soal yang sedang aktif.
     */
    fun getCurrentQuestion(): QuestionItem? {
        val data = shapeData ?: return null
        return when (currentPhase) {
            SessionPhase.QUIZ -> data.quiz
            SessionPhase.PERTANYAAN_ANAK -> data.pertanyaanAnak.getOrNull(currentQuestionIndex)
            SessionPhase.EVALUASI -> data.evaluasi.getOrNull(currentQuestionIndex)
            else -> null
        }
    }

    /**
     * Evaluasi jawaban siswa dan tentukan langkah selanjutnya.
     *
     * @param answer Jawaban siswa
     * @return [AnswerResult] berisi kategori, feedback, dan apakah harus lanjut ke soal berikutnya
     */
    fun evaluateAnswer(answer: String): AnswerResult? {
        val question = getCurrentQuestion() ?: return null
        val matchResult = KeywordMatcher.evaluate(answer, question.kataKunci)
        val matchCount = KeywordMatcher.countMatches(answer, question.kataKunci)
        val feedback = KeywordMatcher.getFeedback(matchResult, question.feedback)

        attempts++

        val isMaxAttempts = attempts >= MAX_ATTEMPTS
        val shouldAdvance: Boolean

        when (matchResult) {
            KeywordMatcher.MatchResult.BENAR -> {
                // Jawaban benar → langsung lanjut
                if (currentPhase == SessionPhase.EVALUASI) {
                    evaluationCorrectCount++
                    evaluationTotalAttempted++
                }
                shouldAdvance = true
            }
            KeywordMatcher.MatchResult.SEBAGIAN, KeywordMatcher.MatchResult.SALAH -> {
                if (isMaxAttempts) {
                    // Sudah 2 percobaan → paksa lanjut, berikan jawaban benar
                    if (currentPhase == SessionPhase.EVALUASI) {
                        evaluationTotalAttempted++
                    }
                    shouldAdvance = true
                } else {
                    // Masih ada kesempatan
                    shouldAdvance = false
                }
            }
        }

        return AnswerResult(
            matchResult = matchResult,
            feedback = feedback,
            matchCount = matchCount,
            totalKeywords = question.kataKunci.size,
            isMaxAttempts = isMaxAttempts && matchResult != KeywordMatcher.MatchResult.BENAR,
            jawabanBenar = if (isMaxAttempts && matchResult != KeywordMatcher.MatchResult.BENAR) question.jawabanBenar else null,
            shouldAdvance = shouldAdvance
        )
    }

    /**
     * Pindah ke soal berikutnya atau fase berikutnya.
     * @return Soal berikutnya, atau null jika sesi sudah selesai
     */
    fun advanceToNext(): QuestionItem? {
        val data = shapeData ?: return null
        attempts = 0

        when (currentPhase) {
            SessionPhase.QUIZ -> {
                // Quiz hanya 1 soal → pindah ke pertanyaan anak
                return transitionToPhase(SessionPhase.PERTANYAAN_ANAK, data)
            }
            SessionPhase.PERTANYAAN_ANAK -> {
                currentQuestionIndex++
                if (currentQuestionIndex < data.pertanyaanAnak.size) {
                    return data.pertanyaanAnak[currentQuestionIndex]
                }
                // Pertanyaan anak habis → pindah ke evaluasi
                return transitionToPhase(SessionPhase.EVALUASI, data)
            }
            SessionPhase.EVALUASI -> {
                currentQuestionIndex++
                if (currentQuestionIndex < data.evaluasi.size) {
                    return data.evaluasi[currentQuestionIndex]
                }
                // Evaluasi selesai
                currentPhase = SessionPhase.SELESAI
                return null
            }
            else -> {
                currentPhase = SessionPhase.SELESAI
                return null
            }
        }
    }

    private fun transitionToPhase(nextPhase: SessionPhase, data: ShapeData): QuestionItem? {
        when (nextPhase) {
            SessionPhase.PERTANYAAN_ANAK -> {
                if (data.pertanyaanAnak.isNotEmpty()) {
                    currentPhase = SessionPhase.PERTANYAAN_ANAK
                    currentQuestionIndex = 0
                    return data.pertanyaanAnak[0]
                }
                // Skip ke evaluasi jika tidak ada pertanyaan anak
                return transitionToPhase(SessionPhase.EVALUASI, data)
            }
            SessionPhase.EVALUASI -> {
                if (data.evaluasi.isNotEmpty()) {
                    currentPhase = SessionPhase.EVALUASI
                    currentQuestionIndex = 0
                    return data.evaluasi[0]
                }
                currentPhase = SessionPhase.SELESAI
                return null
            }
            else -> {
                currentPhase = SessionPhase.SELESAI
                return null
            }
        }
    }

    /**
     * Mendapatkan teks progress untuk UI (contoh: "Evaluasi: Soal 3/5").
     */
    fun getProgressText(): String {
        val data = shapeData ?: return ""
        return when (currentPhase) {
            SessionPhase.IDLE -> "Siap memulai"
            SessionPhase.QUIZ -> "Quiz Utama"
            SessionPhase.PERTANYAAN_ANAK -> {
                val total = data.pertanyaanAnak.size
                "Pertanyaan Pendalaman: ${currentQuestionIndex + 1}/$total"
            }
            SessionPhase.EVALUASI -> {
                val total = data.evaluasi.size
                "Evaluasi: Soal ${currentQuestionIndex + 1}/$total"
            }
            SessionPhase.SELESAI -> "Sesi Selesai"
        }
    }

    /**
     * Mendapatkan label fase saat ini untuk UI.
     */
    fun getPhaseLabel(): String {
        return when (currentPhase) {
            SessionPhase.IDLE -> "Menunggu"
            SessionPhase.QUIZ -> "📝 Quiz"
            SessionPhase.PERTANYAAN_ANAK -> "💡 Pendalaman"
            SessionPhase.EVALUASI -> "📊 Evaluasi"
            SessionPhase.SELESAI -> "✅ Selesai"
        }
    }

    /**
     * Mendapatkan skor evaluasi akhir.
     * @return Pair(benar, total) atau null jika belum evaluasi
     */
    fun getEvaluationScore(): Pair<Int, Int>? {
        if (currentPhase != SessionPhase.SELESAI && currentPhase != SessionPhase.EVALUASI) return null
        val total = shapeData?.evaluasi?.size ?: return null
        return Pair(evaluationCorrectCount, total)
    }

    /**
     * Mendapatkan persentase skor evaluasi.
     */
    fun getEvaluationPercentage(): Int? {
        val score = getEvaluationScore() ?: return null
        if (score.second == 0) return 0
        return ((score.first.toDouble() / score.second) * 100).toInt()
    }

    /**
     * Reset sesi ke awal.
     */
    fun resetSession() {
        currentPhase = SessionPhase.IDLE
        currentQuestionIndex = 0
        attempts = 0
        evaluationCorrectCount = 0
        evaluationTotalAttempted = 0
    }
}
