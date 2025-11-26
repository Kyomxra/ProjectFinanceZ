package com.example.projectmap2.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.projectmap2.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringIncomeScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var recurringIncomeId by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("Belum ada pendapatan pokok") }
    var statusColor by remember { mutableStateOf(Color.Gray) }
    var showDeleteButton by remember { mutableStateOf(false) }

    var jobTitle by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf("25") }
    var expanded by remember { mutableStateOf(false) }

    val days = (1..31).map { it.toString() }

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load existing recurring income
    LaunchedEffect(userId) {
        try {
            val docs = db.collection("RecurringIncome")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("is_active", true)
                .get()
                .await()

            if (!docs.isEmpty) {
                val doc = docs.documents[0]
                recurringIncomeId = doc.id
                jobTitle = doc.getString("job_title") ?: ""
                amount = (doc.getLong("amount") ?: 0).toString()
                selectedDay = (doc.getLong("day_of_month") ?: 25).toString()

                statusText = "✓ Sudah ada pendapatan pokok aktif"
                statusColor = Color(0xFF4CAF50)
                showDeleteButton = true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memuat: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pendapatan Pokok",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
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
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Status Text
            Text(
                text = statusText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Job Title
            Text(
                text = "Nama Pekerjaan",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = jobTitle,
                onValueChange = { jobTitle = it },
                placeholder = { Text("contoh: Software Engineer") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkBlue,
                    unfocusedBorderColor = Color.Gray
                )
            )

            // Amount
            Text(
                text = "Gaji Bulanan",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                placeholder = { Text("contoh: 5000000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkBlue,
                    unfocusedBorderColor = Color.Gray
                )
            )

            // Day Selector
            Text(
                text = "Tanggal Gajian Setiap Bulan",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedDay,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .padding(bottom = 24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    days.forEach { day ->
                        DropdownMenuItem(
                            text = { Text(day) },
                            onClick = {
                                selectedDay = day
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Info Text
            Surface(
                color = Color(0xFFF5F5F5),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "ℹ️ Setiap bulan pada tanggal yang dipilih, pendapatan akan otomatis ditambahkan ke transaksi Anda.",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (showDeleteButton) {
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Hapus", color = Color.Red)
                    }
                }

                Button(
                    onClick = {
                        if (jobTitle.isEmpty()) {
                            Toast.makeText(context, "Masukkan nama pekerjaan!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (amount.isEmpty()) {
                            Toast.makeText(context, "Masukkan gaji bulanan!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val amt = amount.toLongOrNull()
                        if (amt == null || amt <= 0) {
                            Toast.makeText(context, "Gaji harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val dayOfMonth = selectedDay.toInt()

                        scope.launch {
                            saveRecurringIncome(
                                context = context,
                                db = db,
                                userId = userId,
                                recurringIncomeId = recurringIncomeId,
                                jobTitle = jobTitle,
                                amount = amt,
                                dayOfMonth = dayOfMonth,
                                onSuccess = { newId ->
                                    recurringIncomeId = newId
                                    statusText = "✓ Sudah ada pendapatan pokok aktif"
                                    statusColor = Color(0xFF4CAF50)
                                    showDeleteButton = true

                                    // Check and add today's income
                                    scope.launch {
                                        checkAndAddTodayIncome(
                                            context = context,
                                            db = db,
                                            userId = userId,
                                            recurringIncomeId = newId,
                                            jobTitle = jobTitle,
                                            amount = amt,
                                            dayOfMonth = dayOfMonth,
                                            onComplete = {
                                                scope.launch {
                                                    delay(1500)
                                                    onNavigateBack()
                                                }
                                            }
                                        )
                                    }
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue
                    )
                ) {
                    Text("Simpan")
                }
            }
        }
    }

    // Delete Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Pendapatan Pokok") },
            text = { Text("Yakin hapus? Transaksi yang sudah ada tidak akan terhapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (recurringIncomeId != null) {
                            db.collection("RecurringIncome")
                                .document(recurringIncomeId!!)
                                .update(
                                    mapOf(
                                        "is_active" to false,
                                        "updated_at" to Timestamp.now()
                                    )
                                )
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Pendapatan pokok dihapus!", Toast.LENGTH_SHORT).show()

                                    recurringIncomeId = null
                                    statusText = "Belum ada pendapatan pokok"
                                    statusColor = Color.Gray
                                    jobTitle = ""
                                    amount = ""
                                    selectedDay = "25"
                                    showDeleteButton = false
                                    showDeleteDialog = false
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

private suspend fun saveRecurringIncome(
    context: Context,
    db: FirebaseFirestore,
    userId: String,
    recurringIncomeId: String?,
    jobTitle: String,
    amount: Long,
    dayOfMonth: Int,
    onSuccess: (String) -> Unit
) {
    val now = Timestamp.now()
    val data = hashMapOf(
        "user_id" to userId,
        "job_title" to jobTitle,
        "amount" to amount,
        "day_of_month" to dayOfMonth,
        "is_active" to true,
        "created_at" to now,
        "updated_at" to now
    )

    try {
        if (recurringIncomeId != null) {
            // Update
            db.collection("RecurringIncome")
                .document(recurringIncomeId)
                .update(data as Map<String, Any>)
                .await()

            Toast.makeText(context, "Pendapatan pokok diperbarui!", Toast.LENGTH_SHORT).show()
            onSuccess(recurringIncomeId)
        } else {
            // Create
            val doc = db.collection("RecurringIncome")
                .add(data)
                .await()

            Toast.makeText(context, "Pendapatan pokok berhasil disimpan!", Toast.LENGTH_SHORT).show()
            onSuccess(doc.id)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun checkAndAddTodayIncome(
    context: Context,
    db: FirebaseFirestore,
    userId: String,
    recurringIncomeId: String,
    jobTitle: String,
    amount: Long,
    dayOfMonth: Int,
    onComplete: () -> Unit
) {
    val today = Calendar.getInstance()
    val currentDay = today.get(Calendar.DAY_OF_MONTH)
    val currentMonth = today.get(Calendar.MONTH)
    val currentYear = today.get(Calendar.YEAR)

    if (currentDay != dayOfMonth) {
        Toast.makeText(
            context,
            "Pendapatan akan otomatis ditambahkan setiap tanggal $dayOfMonth",
            Toast.LENGTH_LONG
        ).show()
        onComplete()
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
            createTransaction(context, db, userId, recurringIncomeId, jobTitle, amount, onComplete)
        } else {
            Toast.makeText(context, "Gaji bulan ini sudah ditambahkan", Toast.LENGTH_LONG).show()
            onComplete()
        }
    } catch (e: Exception) {
        // Try to create anyway
        createTransaction(context, db, userId, recurringIncomeId, jobTitle, amount, onComplete)
    }
}

private suspend fun createTransaction(
    context: Context,
    db: FirebaseFirestore,
    userId: String,
    recurringIncomeId: String,
    jobTitle: String,
    amount: Long,
    onComplete: () -> Unit
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

        Toast.makeText(
            context,
            "🎉 Gaji $formatted berhasil ditambahkan!",
            Toast.LENGTH_LONG
        ).show()

        onComplete()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Gagal menambahkan transaksi: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}