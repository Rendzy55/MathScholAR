package com.explorebyte.ar.domain.model

/**
 * Logic Engine untuk validasi jawaban siswa secara lokal menggunakan Keyword Matching.
 *
 * Aturan Penilaian:
 * - Benar (≥80% kata kunci cocok): Penguatan positif
 * - Sebagian (50–79% kata kunci cocok): Pertanyaan pemantik + 1 kesempatan
 * - Salah (<50% kata kunci cocok): Hint materi + 1 kesempatan
 */
object KeywordMatcher {

    enum class MatchResult {
        BENAR,
        SEBAGIAN,
        SALAH
    }

    /**
     * Evaluasi jawaban siswa terhadap daftar kata kunci soal.
     *
     * @param answer Jawaban siswa (free text)
     * @param keywords Daftar kata kunci yang harus ada dalam jawaban
     * @return [MatchResult] berdasarkan persentase kata kunci yang cocok
     */
    fun evaluate(answer: String, keywords: List<String>): MatchResult {
        if (keywords.isEmpty()) return MatchResult.BENAR

        val normalizedAnswer = normalizeText(answer)
        val matchCount = keywords.count { keyword ->
            matchKeyword(normalizedAnswer, keyword)
        }

        val percentage = (matchCount.toDouble() / keywords.size) * 100.0

        return when {
            percentage >= 80.0 -> MatchResult.BENAR
            percentage >= 50.0 -> MatchResult.SEBAGIAN
            else -> MatchResult.SALAH
        }
    }

    /**
     * Menghitung jumlah kata kunci yang cocok (untuk konteks AI).
     */
    fun countMatches(answer: String, keywords: List<String>): Int {
        if (keywords.isEmpty()) return 0
        val normalizedAnswer = normalizeText(answer)
        return keywords.count { keyword ->
            matchKeyword(normalizedAnswer, keyword)
        }
    }

    /**
     * Mendapatkan feedback yang sesuai berdasarkan hasil matching.
     */
    fun getFeedback(result: MatchResult, feedback: FeedbackSet): String {
        return when (result) {
            MatchResult.BENAR -> feedback.benar
            MatchResult.SEBAGIAN -> feedback.sebagian
            MatchResult.SALAH -> feedback.salah
        }
    }

    /**
     * Normalisasi teks: lowercase, hapus tanda baca berlebih, normalisasi spasi.
     */
    private fun normalizeText(text: String): String {
        return text
            .lowercase()
            .replace("×", "x")
            .replace("✕", "x")
            .replace("*", "x")
            .replace("kali", "x")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Cocokkan kata kunci dengan jawaban.
     * Mendukung pencocokan fleksibel untuk variasi penulisan umum di matematika.
     */
    private fun matchKeyword(normalizedAnswer: String, keyword: String): Boolean {
        val normalizedKeyword = normalizeText(keyword)

        // Direct substring match
        if (normalizedAnswer.contains(normalizedKeyword)) return true

        // Cek sinonim umum untuk kata kunci matematika
        val synonyms = getSynonyms(normalizedKeyword)
        return synonyms.any { synonym ->
            normalizedAnswer.contains(synonym)
        }
    }

    /**
     * Daftar sinonim untuk kata kunci matematika yang umum.
     * Membantu mencocokkan jawaban yang menggunakan istilah berbeda tapi bermakna sama.
     */
    private fun getSynonyms(keyword: String): List<String> {
        val synonymMap = mapOf(
            "bangun ruang" to listOf("bangun 3 dimensi", "bangun tiga dimensi", "bentuk 3d", "bentuk tiga dimensi"),
            "persegi" to listOf("kotak", "segi empat sama sisi"),
            "sama besar" to listOf("sama ukuran", "ukuran sama", "sama panjang", "kongruen", "sama luas"),
            "6 sisi" to listOf("enam sisi", "6 buah sisi", "enam buah sisi"),
            "12 rusuk" to listOf("dua belas rusuk", "12 buah rusuk"),
            "8 titik sudut" to listOf("delapan titik sudut", "8 buah titik sudut", "8 sudut", "delapan sudut", "8 buah sudut"),
            "s x s" to listOf("sisi x sisi", "s pangkat 2", "s kuadrat", "s²"),
            "s x s x s" to listOf("sisi x sisi x sisi", "s pangkat 3", "s kubik", "s³"),
            "6 x s x s" to listOf("6s²", "6 s²", "enam x s x s", "6 x sisi x sisi"),
            "panjang x lebar x tinggi" to listOf("p x l x t", "panjang kali lebar kali tinggi"),
            "sisi sama panjang" to listOf("semua sisi sama", "ukuran sisi sama", "rusuk sama panjang"),
            "semua sisi kubus terlihat" to listOf("sisi terlihat semua", "seluruh sisi terlihat", "semua sisi terlihat", "semua sisi terbuka"),
            "luas seluruh permukaan dapat dihitung" to listOf("bisa dihitung luas", "menghitung luas semua sisi", "luas semua sisi dihitung", "menghitung seluruh luas permukaan"),
            "persegi panjang" to listOf("kotak panjang", "segi empat"),
            "pasangan berhadapan sama besar" to listOf("sisi yang berhadapan sama", "berhadapan ukurannya sama", "pasangan sisi sama besar", "ukuran berhadapannya sama"),
            "semua sisi balok terlihat" to listOf("semua sisi terlihat", "sisi terlihat semua", "seluruh sisi terbuka", "dibuka semua sisinya"),
            "p, l, t" to listOf("panjang, lebar, dan tinggi", "panjang, lebar, tinggi", "p, l, dan t"),
            "3 pasang sisi" to listOf("tiga pasang sisi", "tiga sisi berpasangan"),
            "2 x ((p x l) + (l x t) + (p x t))" to listOf("2 x (pl + lt + pt)", "2x(p x l + l x t + p x t)", "2*(p*l + l*t + p*t)", "2 x (p x l + l x t + p x t)"),
            "berjejer ke samping, belakang, dan atas" to listOf("berjejer ke samping, ke belakang, dan ke atas", "tumpukan ke samping, ke belakang, dan ke atas", "menyamping, ke belakang, dan ke atas"),
            "kapasitas muatan" to listOf("isi kotak", "isi ruang", "muatan di dalam kotak", "isi dalamnya")
        )

        return synonymMap[keyword] ?: emptyList()
    }
}
