package com.explorebyte.ar.domain.repository

import com.explorebyte.ar.domain.model.ArObject
import kotlinx.coroutines.flow.Flow

interface ArRepository {
    fun getArObjects(): Flow<List<ArObject>>
}

