package com.explorebyte.ar.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Representasi data soal individu (untuk Quiz, Pertanyaan Anak, dan Evaluasi).
 */
data class QuestionItem(
    @SerializedName("id") val id: Int,
    @SerializedName("pertanyaan") val pertanyaan: String,
    @SerializedName("kata_kunci") val kataKunci: List<String>,
    @SerializedName("feedback") val feedback: FeedbackSet,
    @SerializedName("jawaban_benar") val jawabanBenar: String? = null
)

/**
 * Set feedback untuk 3 kategori jawaban.
 */
data class FeedbackSet(
    @SerializedName("benar") val benar: String,
    @SerializedName("sebagian") val sebagian: String,
    @SerializedName("salah") val salah: String
)

/**
 * Data lengkap per bangun ruang, berisi 3 tipe sesi.
 */
data class ShapeData(
    @SerializedName("quiz") val quiz: QuestionItem?,
    @SerializedName("pertanyaan_anak") val pertanyaanAnak: List<QuestionItem>,
    @SerializedName("evaluasi") val evaluasi: List<QuestionItem>
)
