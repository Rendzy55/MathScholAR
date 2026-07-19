package com.explorebyte.ar.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Representasi data soal individu (untuk Quiz, Pertanyaan Anak, dan Evaluasi).
 */
@Serializable
data class QuestionItem(
    @SerialName("id") val id: Int,
    @SerialName("pertanyaan") val pertanyaan: String,
    @SerialName("kata_kunci") val kataKunci: List<String>,
    @SerialName("feedback") val feedback: FeedbackSet,
    @SerialName("jawaban_benar") val jawabanBenar: String? = null
)

/**
 * Set feedback untuk 3 kategori jawaban.
 */
@Serializable
data class FeedbackSet(
    @SerialName("benar") val benar: String,
    @SerialName("sebagian") val sebagian: String,
    @SerialName("salah") val salah: String
)

/**
 * Data lengkap per bangun ruang, berisi 2 tipe sesi.
 */
@Serializable
data class ShapeData(
    @SerialName("pertanyaan_anak") val pertanyaanAnak: List<QuestionItem> = emptyList(),
    @SerialName("evaluasi") val evaluasi: List<QuestionItem> = emptyList()
)
