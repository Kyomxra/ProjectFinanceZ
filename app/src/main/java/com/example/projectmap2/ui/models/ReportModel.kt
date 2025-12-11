package com.example.projectmap2.ui.models

import androidx.compose.ui.graphics.Color

data class BudgetComparison(
    val title: String,
    val color: Color,
    val detail: String,
    val recommendation: String
)

data class ReportData(
    val monthYear: String,
    val userBudget: Long,
    val totalExpense: Long,
    val budgetText: String,
    val comparison: BudgetComparison
)

sealed class ReportState {
    object Idle : ReportState()
    object Loading : ReportState()
    data class Success(val data: ReportData) : ReportState()
    data class Error(val message: String) : ReportState()
}

sealed class BudgetUpdateState {
    object Idle : BudgetUpdateState()
    object Updating : BudgetUpdateState()
    data class Success(val message: String) : BudgetUpdateState()
    data class Error(val message: String) : BudgetUpdateState()
}