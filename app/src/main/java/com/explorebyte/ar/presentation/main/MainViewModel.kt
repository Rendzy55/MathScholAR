package com.explorebyte.ar.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.explorebyte.ar.domain.model.ArObject
import com.explorebyte.ar.domain.repository.ArRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ArRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<MainState>(MainState.Loading)
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    fun fetchArObjects() {
        viewModelScope.launch {
            repository.getArObjects().collect { objects ->
                _uiState.value = MainState.Success(objects)
            }
        }
    }
}

sealed class MainState {
    object Loading : MainState()
    data class Success(val objects: List<ArObject>) : MainState()
    data class Error(val message: String) : MainState()
}

