package com.example.projectmap2.ui.models

data class CategoryPrediction(
    val category: String,
    val confidence: Float,
    val allPredictions: Map<String, Float>
)

sealed class MLState {
    object Idle : MLState()
    object Loading : MLState()
    data class Success(val prediction: CategoryPrediction) : MLState()
    data class Error(val message: String) : MLState()
}