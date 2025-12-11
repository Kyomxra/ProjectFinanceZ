package com.example.projectmap2.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.projectmap2.ui.models.RegisterState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RegisterViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    fun registerUser(
        userId: String,
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        context: Context,
        onRegisterSuccess: () -> Unit
    ) {
        // Validation
        if (userId.isEmpty() || firstName.isEmpty() || lastName.isEmpty() ||
            email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            _registerState.value = RegisterState.Error("Isi semua data")
            return
        }

        if (password != confirmPassword) {
            _registerState.value = RegisterState.Error("Password tidak sama")
            return
        }

        _registerState.value = RegisterState.Loading

        // Check if user_id already exists
        db.collection("User").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    _registerState.value = RegisterState.Error("User ID sudah dipakai!")
                } else {
                    // Create user data
                    val user = hashMapOf(
                        "user_id" to userId,
                        "FName" to firstName,
                        "LName" to lastName,
                        "Email" to email,
                        "Password" to password,
                        "DOB" to "",
                        "imageURL" to ""
                    )

                    // Save with userId as documentId
                    db.collection("User").document(userId)
                        .set(user)
                        .addOnSuccessListener {
                            // Save userId to SharedPreferences
                            val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("userId", userId).apply()

                            _registerState.value = RegisterState.Success
                            onRegisterSuccess()
                        }
                        .addOnFailureListener { e ->
                            _registerState.value = RegisterState.Error("Gagal: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                _registerState.value = RegisterState.Error("Error: ${e.message}")
            }
    }

    fun resetState() {
        _registerState.value = RegisterState.Idle
    }
}