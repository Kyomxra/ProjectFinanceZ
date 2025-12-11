package com.example.projectmap2.ui.models

data class Transaction(
    val title: String,
    val amount: String,
    val date: String,
    val timestamp: Long
)

data class DashboardData(
    val userName: String = "",
    val monthName: String = "Loading...",
    val totalIncome: Int = 0,
    val totalExpense: Int = 0,
    val transactions: List<Transaction> = emptyList()
)

sealed class DashboardState {
    object Idle : DashboardState()
    object Loading : DashboardState()
    data class Success(val data: DashboardData) : DashboardState()
    data class Error(val message: String) : DashboardState()
}