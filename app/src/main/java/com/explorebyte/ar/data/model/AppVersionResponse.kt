package com.explorebyte.ar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppVersionResponse(
    val id: Long,
    val version_code: Int,
    val version_name: String? = null,
    val message: String? = null,
    val apk_url: String? = null,
    val created_at: String? = null
)
