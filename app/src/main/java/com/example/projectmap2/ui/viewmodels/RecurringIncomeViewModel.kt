package com.example.projectmap2.ui.viewmodels

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmap2.ui.models.RecurringIncomeData
import com.example.projectmap2.ui.models.RecurringIncomeState
import com.example.projectmap2.ui.models.SaveRecurringIncomeState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.*

class RecurringIncomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _recurringIncomeState = MutableStateFlow<RecurringIncomeState>(RecurringIncomeState.Idle)
    val recurringIncomeState: StateFlow<RecurringIncomeState> = _recurringIncomeState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveRecurringIncomeState>(SaveRecurringIncomeState.Idle)
    val saveState: StateFlow<SaveRecurringIncomeState> = _saveState.asStateFlow()

    private val _recurringIncomeId = MutableStateFlow<String?>(null)
    val recurringIncomeId: StateFlow<String?> = _recurringIncomeId.asStateFlow()

    private val _jobTitle = MutableStateFlow("")
    val jobTitle: StateFlow<String> = _jobTitle.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _selectedDay = MutableStateFlow("25")
    val selectedDay: StateFlow<String> = _selectedDay.asStateFlow()

    private val _statusText = MutableStateFlow("Belum ada pendapatan pokok")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _statusColor = MutableStateFlow(Color.Gray)
    val statusColor: StateFlow<Color> = _statusColor.asStateFlow()

    private val _showDeleteButton = MutableStateFlow(false)
    val showDeleteButton: StateFlow<Boolean> = _showDeleteButton.asStateFlow()

    fun updateJobTitle(value: String) {
        _jobTitle.value = value
    }

    fun updateAmount(value: String) {
        _amount.value = value
    }

    fun updateSelectedDay(value: String) {
        _selectedDay.value = value
    }

    fun loadRecurringIncome(userId: String) {
        viewModelScope.launch {
            try {
                _recurringIncomeState.value = RecurringIncomeState.Loading

                val docs = db.collection("RecurringIncome")
                    .whereEqualTo("user_id", userId)
                    .whereEqualTo("is_active", true)
                    .get()
                    .await()

                if (!docs.isEmpty) {
                    val doc = docs.documents[0]
                    _recurringIncomeId.value = doc.id
                    _jobTitle.value = doc.getString("job_title") ?: ""
                    _amount.value = (doc.getLong("amount") ?: 0).toString()
                    _selectedDay.value = (doc.getLong("day_of_month") ?: 25).toString()

                    _statusText.value = "✓ Sudah ada pendapatan pokok aktif"
                    _statusColor.value = Color(0xFF4CAF50)
                    _showDeleteButton.value = true
                } else {
                    _statusText.value = "Belum ada pendapatan pokok"
                    _statusColor.value = Color.Gray
                    _showDeleteButton.value = false
                }

                _recurringIncomeState.value = RecurringIncomeState.Success(
                    RecurringIncomeData(
                        recurringIncomeId = _recurringIncomeId.value,
                        jobTitle = _jobTitle.value,
                        amount = _amount.value,
                        selectedDay = _selectedDay.value,
                        statusText = _statusText.value,
                        statusColor = _statusColor.value,
                        showDeleteButton = _showDeleteButton.value
                    )
                )
            } catch (e: Exception) {
                _recurringIncomeState.value = RecurringIncomeState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun saveRecurringIncome(userId: String) {
        if (_jobTitle.value.isEmpty()) {
            _saveState.value = SaveRecurringIncomeState.Error("Masukkan nama pekerjaan!")
            return
        }

        if (_amount.value.isEmpty()) {
            _saveState.value = SaveRecurringIncomeState.Error("Masukkan gaji bulanan!")
            return
        }

        val amt = _amount.value.toLongOrNull()
        if (amt == null || amt <= 0) {
            _saveState.value = SaveRecurringIncomeState.Error("Gaji harus lebih dari 0!")
            return
        }

        val dayOfMonth = _selectedDay.value.toInt()

        viewModelScope.launch {
            try {
                _saveState.value = SaveRecurringIncomeState.Saving

                val now = Timestamp.now()
                val data = hashMapOf(
                    "user_id" to userId,
                    "job_title" to _jobTitle.value,
                    "amount" to amt,
                    "day_of_month" to dayOfMonth,
                    "is_active" to true,
                    "created_at" to now,
                    "updated_at" to now
                )

                val newId = if (_recurringIncomeId.value != null) {
                    // Update
                    db.collection("RecurringIncome")
                        .document(_recurringIncomeId.value!!)
                        .update(data as Map<String, Any>)
                        .await()

                    _recurringIncomeId.value!!
                } else {
                    // Create
                    val doc = db.collection("RecurringIncome")
                        .add(data)
                        .await()

                    doc.id
                }

                _recurringIncomeId.value = newId
                _statusText.value = "✓ Sudah ada pendapatan pokok aktif"
                _statusColor.value = Color(0xFF4CAF50)
                _showDeleteButton.value = true

                // Check and add today's income
                checkAndAddTodayIncome(userId, newId, _jobTitle.value, amt, dayOfMonth)

            } catch (e: Exception) {
                _saveState.value = SaveRecurringIncomeState.Error("Gagal: ${e.message}")
            }
        }
    }

    fun deleteRecurringIncome() {
        viewModelScope.launch {
            try {
                if (_recurringIncomeId.value != null) {
                    db.collection("RecurringIncome")
                        .document(_recurringIncomeId.value!!)
                        .update(
                            mapOf(
                                "is_active" to false,
                                "updated_at" to Timestamp.now()
                            )
                        )
                        .await()

                    _recurringIncomeId.value = null
                    _statusText.value = "Belum ada pendapatan pokok"
                    _statusColor.value = Color.Gray
                    _jobTitle.value = ""
                    _amount.value = ""
                    _selectedDay.value = "25"
                    _showDeleteButton.value = false

                    _saveState.value = SaveRecurringIncomeState.Success("Pendapatan pokok dihapus!")
                }
            } catch (e: Exception) {
                _saveState.value = SaveRecurringIncomeState.Error("Gagal: ${e.message}")
            }
        }
    }

    private suspend fun checkAndAddTodayIncome(
        userId: String,
        recurringIncomeId: String,
        jobTitle: String,
        amount: Long,
        dayOfMonth: Int
    ) {
        val today = Calendar.getInstance()
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)

        if (currentDay != dayOfMonth) {
            _saveState.value = SaveRecurringIncomeState.Success(
                "Pendapatan akan otomatis ditambahkan setiap tanggal $dayOfMonth",
                shouldNavigateBack = true
            )
            return
        }

        // Check existing transaction this month
        val monthStart = Calendar.getInstance().apply {
            set(currentYear, currentMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthEnd = Calendar.getInstance().apply {
            set(currentYear, currentMonth + 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        try {
            val docs = db.collection("Transactions")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("recurring_income_id", recurringIncomeId)
                .whereGreaterThanOrEqualTo("date", Timestamp(monthStart.time))
                .whereLessThan("date", Timestamp(monthEnd.time))
                .get()
                .await()

            if (docs.isEmpty) {
                createTransaction(userId, recurringIncomeId, jobTitle, amount)
            } else {
                _saveState.value = SaveRecurringIncomeState.Success(
                    "Gaji bulan ini sudah ditambahkan",
                    shouldNavigateBack = true
                )
            }
        } catch (e: Exception) {
            // Try to create anyway
            createTransaction(userId, recurringIncomeId, jobTitle, amount)
        }
    }

    private suspend fun createTransaction(
        userId: String,
        recurringIncomeId: String,
        jobTitle: String,
        amount: Long
    ) {
        val now = Calendar.getInstance()
        val transaction = hashMapOf(
            "user_id" to userId,
            "type" to "income",
            "category" to jobTitle,
            "amount" to amount,
            "date" to Timestamp(now.time),
            "created_at" to Timestamp.now(),
            "note" to "Gaji bulanan - $jobTitle",
            "recurring_income_id" to recurringIncomeId
        )

        try {
            db.collection("Transactions")
                .add(transaction)
                .await()

            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val formatted = formatter.format(amount).replace("Rp", "Rp ")

            _saveState.value = SaveRecurringIncomeState.Success(
                "🎉 Gaji $formatted berhasil ditambahkan!",
                shouldNavigateBack = true
            )
        } catch (e: Exception) {
            _saveState.value = SaveRecurringIncomeState.Error(
                "Gagal menambahkan transaksi: ${e.message}"
            )
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveRecurringIncomeState.Idle
    }
}