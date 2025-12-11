package com.example.projectmap2.ui.models

import com.google.firebase.Timestamp

data class Goal(
    val id: String,
    val goalName: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val deadline: Timestamp,
    val status: String
)

sealed class GoalsState {
    object Idle : GoalsState()
    object Loading : GoalsState()
    data class Success(val goals: List<Goal>) : GoalsState()
    data class Error(val message: String) : GoalsState()
}

sealed class GoalOperationState {
    object Idle : GoalOperationState()
    object Processing : GoalOperationState()
    data class Success(val message: String) : GoalOperationState()
    data class Error(val message: String) : GoalOperationState()
}