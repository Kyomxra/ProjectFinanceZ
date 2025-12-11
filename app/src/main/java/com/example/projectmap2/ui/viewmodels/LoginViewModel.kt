package com.example.projectmap2.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.projectmap2.ui.models.LoginState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun checkLogin(
        email: String,
        password: String,
        context: Context,
        onLoginSuccess: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        if (email.isEmpty() || password.isEmpty()) {
            _loginState.value = LoginState.Error("Isi semua data")
            return
        }

        _loginState.value = LoginState.Loading

        db.collection("User")
            .get()
            .addOnSuccessListener { result ->
                var isValidUser = false
                var userId: String? = null

                for (document in result) {
                    val dbEmail = document.getString("Email")
                    val dbPassword = document.getString("Password")

                    if (email == dbEmail && password == dbPassword) {
                        isValidUser = true
                        userId = document.id
                        break
                    }
                }

                if (isValidUser && userId != null) {
                    // Save userId to SharedPreferences
                    val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("userId", userId).apply()

                    _loginState.value = LoginState.Success(userId)
                    onDismiss()
                    onLoginSuccess(userId)
                } else {
                    _loginState.value = LoginState.Error("Email atau password salah")
                }
            }
            .addOnFailureListener { e ->
                _loginState.value = LoginState.Error("Error: ${e.message}")
            }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}