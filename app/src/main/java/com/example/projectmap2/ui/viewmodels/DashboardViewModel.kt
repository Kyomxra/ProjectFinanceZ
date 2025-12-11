package com.example.projectmap2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmap2.ui.models.DashboardData
import com.example.projectmap2.ui.models.DashboardState
import com.example.projectmap2.ui.models.Transaction
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

class DashboardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Idle)
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _monthName = MutableStateFlow("Loading...")
    val monthName: StateFlow<String> = _monthName.asStateFlow()

    private val _totalIncome = MutableStateFlow(0)
    val totalIncome: StateFlow<Int> = _totalIncome.asStateFlow()

    private val _totalExpense = MutableStateFlow(0)
    val totalExpense: StateFlow<Int> = _totalExpense.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    fun loadUserData(userId: String) {
        viewModelScope.launch {
            try {
                _dashboardState.value = DashboardState.Loading

                // Load user info
                val doc = db.collection("User").document(userId).get().await()
                if (doc.exists()) {
                    val fName = doc.getString("FName") ?: ""
                    val lName = doc.getString("LName") ?: ""
                    _userName.value = "$fName $lName"
                }

                // Load summary and transactions
                loadSummaryAndTransactions(userId)

            } catch (e: Exception) {
                _dashboardState.value = DashboardState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun reloadData(userId: String) {
        viewModelScope.launch {
            loadSummaryAndTransactions(userId)
        }
    }

    private suspend fun loadSummaryAndTransactions(userId: String) {
        try {
            val monthName = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())
            _monthName.value = monthName

            val documents = db.collection("Transactions")
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            var totalIncome = 0L
            var totalExpense = 0L
            val txnList = mutableListOf<Transaction>()

            for (doc in documents) {
                val type = doc.getString("type") ?: ""
                val amount = doc.getLong("amount") ?: 0
                val category = doc.getString("category") ?: "Transaksi"
                val timestamp = doc.getTimestamp("date")
                val note = doc.getString("note") ?: ""

                when (type) {
                    "income" -> totalIncome += amount
                    "expense" -> totalExpense += amount
                    "saving" -> totalExpense += amount
                }

                val dateStr = if (timestamp != null) {
                    formatDate(timestamp)
                } else "Unknown date"

                val amountStr = formatCurrency(amount, type)
                val title = if (note.isNotEmpty()) note else category

                txnList.add(Transaction(title, amountStr, dateStr, timestamp?.toDate()?.time ?: 0))
            }

            txnList.sortByDescending { it.timestamp }

            _totalIncome.value = totalIncome.toInt()
            _totalExpense.value = totalExpense.toInt()
            _transactions.value = txnList

            _dashboardState.value = DashboardState.Success(
                DashboardData(
                    userName = _userName.value,
                    monthName = monthName,
                    totalIncome = totalIncome.toInt(),
                    totalExpense = totalExpense.toInt(),
                    transactions = txnList
                )
            )
        } catch (e: Exception) {
            _dashboardState.value = DashboardState.Error(e.message ?: "Error loading data")
        }
    }

    private fun formatDate(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val calendar = Calendar.getInstance()
        val today = calendar.time

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = calendar.time

        return when {
            isSameDay(date, today) -> "Today"
            isSameDay(date, yesterday) -> "Yesterday"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(date)
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatCurrency(amount: Long, type: String = ""): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val formatted = formatter.format(amount).replace("Rp", "Rp ")
        return when (type) {
            "income" -> "+ $formatted"
            "expense" -> "- $formatted"
            "saving" -> "💰 $formatted"
            else -> formatted
        }
    }
}