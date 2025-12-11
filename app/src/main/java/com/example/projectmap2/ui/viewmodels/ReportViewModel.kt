package com.example.projectmap2.ui.viewmodels

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmap2.ui.models.BudgetComparison
import com.example.projectmap2.ui.models.BudgetUpdateState
import com.example.projectmap2.ui.models.ReportData
import com.example.projectmap2.ui.models.ReportState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ReportViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()

    private val _budgetUpdateState = MutableStateFlow<BudgetUpdateState>(BudgetUpdateState.Idle)
    val budgetUpdateState: StateFlow<BudgetUpdateState> = _budgetUpdateState.asStateFlow()

    private val _userBudget = MutableStateFlow(0L)
    val userBudget: StateFlow<Long> = _userBudget.asStateFlow()

    private val _totalExpense = MutableStateFlow(0L)
    val totalExpense: StateFlow<Long> = _totalExpense.asStateFlow()

    private val _monthYear = MutableStateFlow("")
    val monthYear: StateFlow<String> = _monthYear.asStateFlow()

    private val _budgetText = MutableStateFlow("Belum diatur")
    val budgetText: StateFlow<String> = _budgetText.asStateFlow()

    private val _comparison = MutableStateFlow(
        BudgetComparison(
            title = "⚙️ Budget belum diatur",
            color = Color.Gray,
            detail = "",
            recommendation = ""
        )
    )
    val comparison: StateFlow<BudgetComparison> = _comparison.asStateFlow()

    fun loadReportData(userId: String) {
        viewModelScope.launch {
            try {
                _reportState.value = ReportState.Loading

                // Set month year
                val calendar = Calendar.getInstance()
                _monthYear.value = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(calendar.time)

                // Load budget
                val doc = db.collection("User").document(userId).get().await()
                if (doc.exists()) {
                    _userBudget.value = doc.getLong("monthly_budget") ?: 0L
                    if (_userBudget.value > 0) {
                        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                        _budgetText.value = formatter.format(_userBudget.value).replace("Rp", "Rp ")
                    }
                }

                // Load expenses
                _totalExpense.value = loadExpenseData(userId)

                // Calculate comparison
                _comparison.value = calculateComparison(_totalExpense.value, _userBudget.value)

                _reportState.value = ReportState.Success(
                    ReportData(
                        monthYear = _monthYear.value,
                        userBudget = _userBudget.value,
                        totalExpense = _totalExpense.value,
                        budgetText = _budgetText.value,
                        comparison = _comparison.value
                    )
                )
            } catch (e: Exception) {
                _reportState.value = ReportState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateBudget(userId: String, budget: Long) {
        if (budget <= 0) {
            _budgetUpdateState.value = BudgetUpdateState.Error("Budget harus lebih dari 0!")
            return
        }

        viewModelScope.launch {
            try {
                _budgetUpdateState.value = BudgetUpdateState.Updating

                db.collection("User").document(userId)
                    .update("monthly_budget", budget)
                    .await()

                _userBudget.value = budget
                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                _budgetText.value = formatter.format(budget).replace("Rp", "Rp ")

                // Refresh comparison
                _comparison.value = calculateComparison(_totalExpense.value, _userBudget.value)

                _budgetUpdateState.value = BudgetUpdateState.Success("Budget berhasil disimpan!")
            } catch (e: Exception) {
                _budgetUpdateState.value = BudgetUpdateState.Error("Gagal: ${e.message}")
            }
        }
    }

    private suspend fun loadExpenseData(userId: String): Long {
        return try {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = Timestamp(calendar.time)

            calendar.set(currentYear, currentMonth + 1, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val endOfMonth = Timestamp(calendar.time)

            val documents = db.collection("Transactions")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("type", "expense")
                .get()
                .await()

            var totalExpense = 0L

            for (doc in documents) {
                val timestamp = doc.getTimestamp("date")
                if (timestamp != null) {
                    if (timestamp.toDate().after(startOfMonth.toDate()) &&
                        timestamp.toDate().before(endOfMonth.toDate())) {
                        val amount = doc.getLong("amount") ?: 0
                        totalExpense += amount
                    }
                }
            }

            totalExpense
        } catch (e: Exception) {
            0L
        }
    }

    private fun calculateComparison(
        userExpense: Long,
        userBudget: Long
    ): BudgetComparison {
        return when {
            userBudget == 0L -> {
                BudgetComparison(
                    title = "⚙️ Budget belum diatur",
                    color = Color.Gray,
                    detail = "Atur budget bulananmu terlebih dahulu untuk melihat perbandingan",
                    recommendation = "💡 Klik tombol 'Atur Budget' di atas untuk mulai mengelola keuanganmu!"
                )
            }
            userExpense == 0L -> {
                BudgetComparison(
                    title = "Belum ada pengeluaran",
                    color = Color.Gray,
                    detail = "Kamu belum mencatat pengeluaran bulan ini",
                    recommendation = "💡 Mulai catat pengeluaranmu untuk manajemen keuangan yang lebih baik!"
                )
            }
            userExpense < userBudget -> {
                val remaining = userBudget - userExpense
                val usedPercentage = ((userExpense.toDouble() / userBudget) * 100).toInt()
                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val formattedRemaining = formatter.format(remaining).replace("Rp", "Rp ")

                BudgetComparison(
                    title = "🎉 Kamu masih di jalur yang benar!",
                    color = Color(0xFF4CAF50),
                    detail = "Kamu sudah menggunakan $usedPercentage% dari budget. Sisa budget: $formattedRemaining",
                    recommendation = "💰 Bagus! Pertahankan kebiasaan ini hingga akhir bulan. Sisihkan sisanya untuk tabungan!"
                )
            }
            userExpense == userBudget -> {
                BudgetComparison(
                    title = "👌 Budget habis tepat!",
                    color = Color(0xFF2196F3),
                    detail = "Pengeluaranmu sama persis dengan budget yang diatur",
                    recommendation = "📊 Coba sisihkan sebagian untuk dana darurat atau tabungan!"
                )
            }
            else -> {
                val overbudget = userExpense - userBudget
                val overPercentage = (((userExpense - userBudget).toDouble() / userBudget) * 100).toInt()
                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val formattedOver = formatter.format(overbudget).replace("Rp", "Rp ")

                BudgetComparison(
                    title = "⚠️ Pengeluaran melebihi budget",
                    color = Color(0xFFF44336),
                    detail = "Kamu sudah over budget $formattedOver ($overPercentage% lebih tinggi dari target)",
                    recommendation = "💡 Evaluasi pengeluaranmu! Kurangi pengeluaran tidak penting dan pertimbangkan untuk menyesuaikan budget bulan depan."
                )
            }
        }
    }

    fun resetBudgetUpdateState() {
        _budgetUpdateState.value = BudgetUpdateState.Idle
    }
}