package com.example.projectmap2.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projectmap2.R
import com.example.projectmap2.ui.theme.DarkBlue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSavingScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val calendar = Calendar.getInstance()

    // Load savings goals from Firebase
    var savingGoals by remember { mutableStateOf(listOf<String>()) }
    var isLoadingGoals by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        try {
            val docs = db.collection("Goals")
                .whereEqualTo("user_id", userId)
                .get()
                .await()

            val goals = docs.documents.mapNotNull { it.getString("name") }
            savingGoals = if (goals.isNotEmpty()) goals else listOf("Tabungan Umum")
            isLoadingGoals = false
        } catch (e: Exception) {
            savingGoals = listOf("Tabungan Umum")
            isLoadingGoals = false
        }
    }

    var selectedGoal by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(calendar.time) }
    var dateText by remember {
        mutableStateOf(SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(calendar.time))
    }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Set default goal when loaded
    LaunchedEffect(savingGoals) {
        if (savingGoals.isNotEmpty() && selectedGoal.isEmpty()) {
            selectedGoal = savingGoals[0]
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            dateText = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(selectedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tambah Tabungan",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.left),
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50)  // Green for saving
                )
            )
        }
    ) { padding ->
        if (isLoadingGoals) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F7FA))
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Goal Dropdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Target Tabungan",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = selectedGoal,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    unfocusedBorderColor = Color.LightGray
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                savingGoals.forEach { goal ->
                                    DropdownMenuItem(
                                        text = { Text(goal) },
                                        onClick = {
                                            selectedGoal = goal
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Date Picker
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tanggal",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = dateText,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.home),
                                    contentDescription = "Calendar",
                                    tint = Color(0xFF4CAF50)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { datePickerDialog.show() },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Nominal",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            placeholder = { Text("Masukkan nominal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Text("Rp", fontWeight = FontWeight.Bold)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Note Input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Catatan (Opsional)",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = { Text("Tambahkan catatan") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (amount.isEmpty()) {
                            Toast.makeText(context, "Nominal harus diisi!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        val amountLong = amount.toLongOrNull() ?: 0L

                        val transactionData = hashMapOf(
                            "user_id" to userId,
                            "type" to "saving",
                            "category" to selectedGoal,
                            "amount" to amountLong,
                            "date" to Timestamp(selectedDate),
                            "note" to note,
                            "created_at" to Timestamp.now()
                        )

                        db.collection("Transactions")
                            .add(transactionData)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Tabungan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            "Tambahkan Tabungan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}