package com.explorebyte.ar.domain.model

data class ArObject(
    val id: String,
    val name: String,
    val modelResId: Int? = null,
    val modelUri: String? = null
)

