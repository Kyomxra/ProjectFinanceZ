package com.example.projectmap2.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.projectmap2.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    // Gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBlue, LightBlue)
                )
            )
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 250.dp, y = (-50).dp)
                .alpha(0.1f)
                .background(Color.White, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = (-30).dp, y = 600.dp)
                .alpha(0.08f)
                .background(Color.White, CircleShape)
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "💰",
                        fontSize = 60.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "FinanceZ",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Slogan
            Text(
                text = "Manage Your Finance Wisely",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.alpha(0.9f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Feature highlights
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                FeatureRow(emoji = "💰", text = "Kelola Keuangan dengan Mudah")
                Spacer(modifier = Modifier.height(16.dp))
                FeatureRow(emoji = "🎯", text = "Capai Target Tabungan Impian")
                Spacer(modifier = Modifier.height(16.dp))
                FeatureRow(emoji = "📊", text = "Pantau Pengeluaran Real-time")
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Get Started button
            Button(
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(8.dp, RoundedCornerShape(30.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DarkBlue
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    // Login Dialog
    if (showDialog) {
        LoginDialog(
            onDismiss = { showDialog = false },
            onNavigateToRegister = {
                showDialog = false
                onNavigateToRegister()
            },
            onLoginSuccess = onLoginSuccess,
            context = context
        )
    }
}

@Composable
fun FeatureRow(emoji: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            modifier = Modifier.size(32.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.alpha(0.9f)
        )
    }
}

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    context: Context
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val db = FirebaseFirestore.getInstance()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Login",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Email Label
                Text(
                    text = "Email",
                    fontSize = 18.sp,
                    color = Grey,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("you@gmail.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Label
                Text(
                    text = "Kata Sandi",
                    fontSize = 18.sp,
                    color = Grey,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Masukkan kata sandi anda") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Login Button
                Button(
                    onClick = {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            checkLogin(email, password, context, onLoginSuccess, onDismiss, db)
                        } else {
                            Toast.makeText(context, "Isi semua data", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Masuk",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Register Link
                TextButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = "Register here",
                        color = Purple500,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun checkLogin(
    email: String,
    password: String,
    context: Context,
    onLoginSuccess: (String) -> Unit,
    onDismiss: () -> Unit,
    db: FirebaseFirestore
) {
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

                Toast.makeText(context, "Login berhasil!", Toast.LENGTH_SHORT).show()
                onDismiss()
                onLoginSuccess(userId)
            } else {
                Toast.makeText(context, "Email atau password salah", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}