package com.example.projectmap2.ui.models

import android.location.Location

data class TransactionWithLocation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val amount: Double,
    val category: String,
    val date: String,
    val userId: String,
    val type: String
)

data class LocationStatistics(
    val totalSpent: Double,
    val totalTransactions: Int,
    val mostFrequentLocation: String?,
    val locationFrequency: Map<String, Int>
)

sealed class LocationState {
    object Idle : LocationState()
    object Loading : LocationState()
    data class Success(
        val transactions: List<TransactionWithLocation>,
        val statistics: LocationStatistics
    ) : LocationState()
    data class Error(val message: String) : LocationState()
}

sealed class LocationPermissionState {
    object Idle : LocationPermissionState()
    object Granted : LocationPermissionState()
    object Denied : LocationPermissionState()
}