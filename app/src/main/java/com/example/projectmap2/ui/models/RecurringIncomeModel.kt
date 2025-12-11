package com.example.projectmap2.ui.models

data class RecurringIncomeData(
    val recurringIncomeId: String? = null,
    val jobTitle: String = "",
    val amount: String = "",
    val selectedDay: String = "25",
    val statusText: String = "Belum ada pendapatan pokok",
    val statusColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray,
    val showDeleteButton: Boolean = false
)

sealed class RecurringIncomeState {
    object Idle : RecurringIncomeState()
    object Loading : RecurringIncomeState()
    data class Success(val data: RecurringIncomeData) : RecurringIncomeState()
    data class Error(val message: String) : RecurringIncomeState()
}

sealed class SaveRecurringIncomeState {
    object Idle : SaveRecurringIncomeState()
    object Saving : SaveRecurringIncomeState()
    data class Success(val message: String, val shouldNavigateBack: Boolean = false) : SaveRecurringIncomeState()
    data class Error(val message: String) : SaveRecurringIncomeState()
}