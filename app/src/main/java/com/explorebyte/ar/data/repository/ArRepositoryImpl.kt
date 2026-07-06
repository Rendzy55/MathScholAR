package com.explorebyte.ar.data.repository

import com.explorebyte.ar.domain.model.ArObject
import com.explorebyte.ar.domain.repository.ArRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ArRepositoryImpl : ArRepository {
    override fun getArObjects(): Flow<List<ArObject>> = flow {
        // Mock data
        val objects = listOf(
            ArObject("1", "Sample Model", null, "models/sample.glb")
        )
        emit(objects)
    }
}

