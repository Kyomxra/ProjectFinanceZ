package com.example.projectmap2.ui.viewmodels

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmap2.ui.models.LocationState
import com.example.projectmap2.ui.models.LocationStatistics
import com.example.projectmap2.ui.models.TransactionWithLocation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LocationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionWithLocation>>(emptyList())
    val transactions: StateFlow<List<TransactionWithLocation>> = _transactions.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    fun updateCurrentLocation(location: Location?) {
        _currentLocation.value = location
    }

    fun loadTransactionsWithLocation(userId: String) {
        viewModelScope.launch {
            try {
                _locationState.value = LocationState.Loading

                // TEMPORARY: Load ALL expenses (no date filter)
                db.collection("Transactions")
                    .whereEqualTo("user_id", userId)
                    .whereEqualTo("type", "expense")
                    // COMMENT OUT date filters temporarily
                    // .whereGreaterThanOrEqualTo("date", startOfMonth)
                    // .whereLessThan("date", startOfNextMonth)
                    .addSnapshotListener { snapshots, error ->
                        if (error != null) {
                            Log.e("LocationViewModel", "Error loading transactions", error)
                            _locationState.value = LocationState.Error(error.message ?: "Unknown error")
                            return@addSnapshotListener
                        }

                        val loadedTransactions = mutableListOf<TransactionWithLocation>()
                        snapshots?.documents?.forEach { doc ->
                            try {
                                val name = doc.getString("note") ?: doc.getString("category") ?: "Transaksi"
                                val category = doc.getString("category") ?: "Lainnya"
                                val amount = (doc.getLong("amount") ?: 0L).toDouble()
                                val timestamp = doc.getTimestamp("date")
                                val latitude = doc.getDouble("latitude")
                                val longitude = doc.getDouble("longitude")

                                if (latitude != null && longitude != null) {
                                    val dateStr = if (timestamp != null) {
                                        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                                            .format(timestamp.toDate())
                                    } else "Unknown date"

                                    loadedTransactions.add(
                                        TransactionWithLocation(
                                            id = doc.id,
                                            name = name,
                                            latitude = latitude,
                                            longitude = longitude,
                                            amount = amount,
                                            category = category,
                                            date = dateStr,
                                            userId = userId,
                                            type = "expense"
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("LocationViewModel", "Error parsing transaction", e)
                            }
                        }

                        _transactions.value = loadedTransactions

                        // Calculate statistics
                        val statistics = calculateStatistics(loadedTransactions)

                        _locationState.value = LocationState.Success(loadedTransactions, statistics)
                        Log.d("LocationViewModel", "Loaded ${loadedTransactions.size} transactions for this month")
                    }
            } catch (e: Exception) {
                Log.e("LocationViewModel", "Error in loadTransactionsWithLocation", e)
                _locationState.value = LocationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun calculateStatistics(transactions: List<TransactionWithLocation>): LocationStatistics {
        val totalSpent = transactions.sumOf { it.amount }
        val locationCounts = transactions.groupingBy { it.name }.eachCount()
        val mostFrequent = locationCounts.maxByOrNull { it.value }

        // Calculate location frequency by lat/lng
        val locationFrequency = mutableMapOf<String, Int>()
        transactions.forEach { transaction ->
            val locationKey = "${transaction.latitude},${transaction.longitude}"
            locationFrequency[locationKey] = locationFrequency.getOrDefault(locationKey, 0) + 1
        }

        return LocationStatistics(
            totalSpent = totalSpent,
            totalTransactions = transactions.size,
            mostFrequentLocation = mostFrequent?.key,
            locationFrequency = locationFrequency
        )
    }
}
