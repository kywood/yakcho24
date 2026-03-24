package com.gamenuri.yakcho24.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecognitionResult(
    val name: String,
    val confidence: Float,
    val description: String,
)

class CameraViewModel : ViewModel() {

    private val _recognitionResult = MutableStateFlow<RecognitionResult?>(null)
    val recognitionResult: StateFlow<RecognitionResult?> = _recognitionResult.asStateFlow()

    // TODO: ML 모델 연동 후 실제 결과로 교체
    fun updateResult(result: RecognitionResult?) {
        _recognitionResult.value = result
    }
}
