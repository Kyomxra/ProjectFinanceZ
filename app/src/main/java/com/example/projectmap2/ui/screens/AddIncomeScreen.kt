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
import androidx.compose.ui.Alignment
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
import com.example.projectmap2.ui.theme.LightBlue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val calendar = Calendar.getInstance()

    // Income categories
    val incomeCategories = listOf(
        "Gaji",
        "Bonus",
        "Investasi",
        "Freelance",
        "Hadiah",
        "Lainnya"
    )

    var selectedCategory by remember { mutableStateOf(incomeCategories[0]) }
    var expanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(calendar.time) }
    var dateText by remember {
        mutableStateOf(SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(calendar.time))
    }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Date Picker Dialog
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
                        "Tambah Pemasukan",
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
                    containerColor = DarkBlue
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Category Dropdown
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kategori",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkBlue,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            incomeCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
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
                                tint = DarkBlue
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkBlue,
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
                            focusedBorderColor = DarkBlue,
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
                            focusedBorderColor = DarkBlue,
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
                        "type" to "income",
                        "category" to selectedCategory,
                        "amount" to amountLong,
                        "date" to Timestamp(selectedDate),
                        "note" to note,
                        "created_at" to Timestamp.now()
                    )

                    db.collection("Transactions")
                        .add(transactionData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Pemasukan berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
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
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
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
                        "Tambahkan Pemasukan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}