package com.example.projectmap2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmap2.ui.models.Goal
import com.example.projectmap2.ui.models.GoalOperationState
import com.example.projectmap2.ui.models.GoalsState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.*

class GoalsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _goalsState = MutableStateFlow<GoalsState>(GoalsState.Idle)
    val goalsState: StateFlow<GoalsState> = _goalsState.asStateFlow()

    private val _operationState = MutableStateFlow<GoalOperationState>(GoalOperationState.Idle)
    val operationState: StateFlow<GoalOperationState> = _operationState.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    fun loadGoals(userId: String) {
        viewModelScope.launch {
            try {
                _goalsState.value = GoalsState.Loading

                db.collection("Goals")
                    .whereEqualTo("user_id", userId)
                    .addSnapshotListener { snapshots, e ->
                        if (e != null) {
                            _goalsState.value = GoalsState.Error(e.message ?: "Unknown error")
                            return@addSnapshotListener
                        }

                        if (snapshots != null) {
                            val goalsList = snapshots.documents.mapNotNull { doc ->
                                Goal(
                                    id = doc.id,
                                    goalName = doc.getString("goal_name") ?: "",
                                    targetAmount = doc.getLong("target_amount") ?: 0,
                                    currentAmount = doc.getLong("current_amount") ?: 0,
                                    deadline = doc.getTimestamp("deadline") ?: Timestamp.now(),
                                    status = doc.getString("status") ?: "active"
                                )
                            }
                            _goals.value = goalsList
                            _goalsState.value = GoalsState.Success(goalsList)
                        }
                    }
            } catch (e: Exception) {
                _goalsState.value = GoalsState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addGoal(
        userId: String,
        goalName: String,
        targetAmount: Long,
        deadline: Date
    ) {
        if (goalName.isEmpty()) {
            _operationState.value = GoalOperationState.Error("Masukkan nama target!")
            return
        }

        if (targetAmount <= 0) {
            _operationState.value = GoalOperationState.Error("Jumlah harus lebih dari 0!")
            return
        }

        viewModelScope.launch {
            try {
                _operationState.value = GoalOperationState.Processing

                val goal = hashMapOf(
                    "user_id" to userId,
                    "goal_name" to goalName,
                    "target_amount" to targetAmount,
                    "current_amount" to 0L,
                    "deadline" to Timestamp(deadline),
                    "status" to "active",
                    "created_at" to Timestamp(Date())
                )

                db.collection("Goals")
                    .add(goal)
                    .await()

                _operationState.value = GoalOperationState.Success("Target tabungan ditambahkan!")
            } catch (e: Exception) {
                _operationState.value = GoalOperationState.Error("Gagal: ${e.message}")
            }
        }
    }

    fun updateGoal(
        goalId: String,
        goalName: String,
        targetAmount: Long,
        deadline: Date
    ) {
        if (goalName.isEmpty() || targetAmount <= 0) {
            _operationState.value = GoalOperationState.Error("Lengkapi semua field!")
            return
        }

        viewModelScope.launch {
            try {
                _operationState.value = GoalOperationState.Processing

                db.collection("Goals").document(goalId)
                    .update(mapOf(
                        "goal_name" to goalName,
                        "target_amount" to targetAmount,
                        "deadline" to Timestamp(deadline)
                    ))
                    .await()

                _operationState.value = GoalOperationState.Success("Target berhasil diupdate!")
            } catch (e: Exception) {
                _operationState.value = GoalOperationState.Error("Gagal: ${e.message}")
            }
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            try {
                _operationState.value = GoalOperationState.Processing

                val docs = db.collection("Transactions")
                    .whereEqualTo("goal_id", goal.id)
                    .whereEqualTo("type", "saving")
                    .get()
                    .await()

                val batch = db.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.delete(db.collection("Goals").document(goal.id))
                batch.commit().await()

                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                val savedAmount = formatter.format(goal.currentAmount).replace("Rp", "Rp ")
                val msg = if (goal.currentAmount > 0) {
                    "Target dihapus! $savedAmount dikembalikan ke saldo."
                } else "Target dihapus!"

                _operationState.value = GoalOperationState.Success(msg)
            } catch (e: Exception) {
                _operationState.value = GoalOperationState.Error("Gagal: ${e.message}")
            }
        }
    }

    fun markAsCompleted(goalId: String) {
        viewModelScope.launch {
            try {
                db.collection("Goals").document(goalId)
                    .update("status", "completed")
                    .await()

                _operationState.value = GoalOperationState.Success("Target ditandai selesai! 🎉")
            } catch (e: Exception) {
                _operationState.value = GoalOperationState.Error("Gagal: ${e.message}")
            }
        }
    }

    fun addSavingToGoal(
        userId: String,
        goal: Goal,
        amount: Long
    ) {
        if (amount <= 0) {
            _operationState.value = GoalOperationState.Error("Jumlah harus lebih dari 0!")
            return
        }

        viewModelScope.launch {
            try {
                _operationState.value = GoalOperationState.Processing

                val transaction = hashMapOf(
                    "user_id" to userId,
                    "type" to "saving",
                    "goal_id" to goal.id,
                    "category" to goal.goalName,
                    "amount" to amount,
                    "date" to Timestamp(Date()),
                    "created_at" to Timestamp(Date()),
                    "note" to "Tabungan untuk ${goal.goalName}"
                )

                db.collection("Transactions")
                    .add(transaction)
                    .await()

                val goalRef = db.collection("Goals").document(goal.id)
                db.runTransaction { trans ->
                    val current = trans.get(goalRef).getLong("current_amount") ?: 0L
                    trans.update(goalRef, "current_amount", current + amount)
                }.await()

                _operationState.value = GoalOperationState.Success("Tabungan ditambahkan!")
            } catch (e: Exception) {
                _operationState.value = GoalOperationState.Error("Gagal: ${e.message}")
            }
        }
    }

    fun resetOperationState() {
        _operationState.value = GoalOperationState.Idle
    }
}