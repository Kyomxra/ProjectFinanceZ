package com.example.projectmap2.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import com.example.projectmap2.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// ==================== INCOME DIALOG ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    userId: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    val incomeTypes = listOf("Gaji", "Bonus", "Investasi", "Lainnya")
    var selectedType by remember { mutableStateOf(incomeTypes[0]) }
    var expanded by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(calendar.time) }
    var dateText by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time))
    }

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tambahkan Pemasukan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkBlue,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        incomeTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Tanggal") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDatePicker(context, calendar) { year, month, day ->
                                calendar.set(year, month, day)
                                selectedDate = calendar.time
                                dateText = "$day/${month + 1}/$year"
                            }
                        },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                    label = { Text("Nominal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Note Input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Add Button
                Button(
                    onClick = {
                        if (amount.isEmpty()) {
                            Toast.makeText(context, "Masukkan jumlah!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val amountLong = amount.toLongOrNull()
                        if (amountLong == null || amountLong <= 0) {
                            Toast.makeText(context, "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val transaction = hashMapOf(
                            "user_id" to userId,
                            "type" to "income",
                            "category" to selectedType,
                            "amount" to amountLong,
                            "date" to Timestamp(selectedDate),
                            "created_at" to Timestamp(Date()),
                            "note" to note
                        )

                        db.collection("Transactions")
                            .add(transaction)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Pemasukan ditambahkan!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue
                    )
                ) {
                    Text("Tambahkan", fontSize = 16.sp)
                }
            }
        }
    }
}

// ==================== EXPENSE DIALOG ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    userId: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    val expenseTypes = listOf("Makan", "Transport", "Belanja", "Hiburan", "Lainnya")
    var selectedType by remember { mutableStateOf(expenseTypes[0]) }
    var expanded by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(calendar.time) }
    var dateText by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time))
    }

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tambahkan Pengeluaran",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkBlue,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        expenseTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Tanggal") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDatePicker(context, calendar) { year, month, day ->
                                calendar.set(year, month, day)
                                selectedDate = calendar.time
                                dateText = "$day/${month + 1}/$year"
                            }
                        },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                    label = { Text("Nominal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Note Input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Add Button
                Button(
                    onClick = {
                        if (amount.isEmpty()) {
                            Toast.makeText(context, "Masukkan jumlah!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val amountLong = amount.toLongOrNull()
                        if (amountLong == null || amountLong <= 0) {
                            Toast.makeText(context, "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val transaction = hashMapOf(
                            "user_id" to userId,
                            "type" to "expense",
                            "category" to selectedType,
                            "amount" to amountLong,
                            "date" to Timestamp(selectedDate),
                            "created_at" to Timestamp(Date()),
                            "note" to note
                        )

                        db.collection("Transactions")
                            .add(transaction)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Pengeluaran ditambahkan!", Toast.LENGTH_SHORT).show()
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue
                    )
                ) {
                    Text("Tambahkan", fontSize = 16.sp)
                }
            }
        }
    }
}

// ==================== SAVING DIALOG ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSavingDialog(
    userId: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var goalNames by remember { mutableStateOf(listOf<String>()) }
    var goalIds by remember { mutableStateOf(listOf<String>()) }
    var selectedGoal by remember { mutableStateOf("Memuat tujuan...") }
    var selectedGoalIndex by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(calendar.time) }
    var dateText by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time))
    }

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // Load Goals
    LaunchedEffect(Unit) {
        db.collection("Goals")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                val names = mutableListOf<String>()
                val ids = mutableListOf<String>()

                for (doc in documents) {
                    val goalName = doc.getString("goal_name") ?: "Goal"
                    val currentAmount = doc.getLong("current_amount") ?: 0
                    val targetAmount = doc.getLong("target_amount") ?: 0
                    val progress = if (targetAmount > 0) {
                        ((currentAmount.toDouble() / targetAmount) * 100).toInt()
                    } else 0

                    names.add("$goalName ($progress%)")
                    ids.add(doc.id)
                }

                if (names.isEmpty()) {
                    goalNames = listOf("⚠️ Belum ada tujuan tabungan")
                    goalIds = emptyList()
                    selectedGoal = "⚠️ Belum ada tujuan tabungan"
                } else {
                    goalNames = names
                    goalIds = ids
                    selectedGoal = names[0]
                }
                isLoading = false
            }
            .addOnFailureListener {
                goalNames = listOf("❌ Gagal memuat tujuan")
                goalIds = emptyList()
                selectedGoal = "❌ Gagal memuat tujuan"
                isLoading = false
            }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tambahkan Tabungan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Goal Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isLoading && goalIds.isNotEmpty()) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedGoal,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilih Tujuan") },
                        trailingIcon = {
                            if (!isLoading && goalIds.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkBlue,
                            unfocusedBorderColor = Color.Gray
                        ),
                        enabled = !isLoading && goalIds.isNotEmpty()
                    )

                    if (goalIds.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            goalNames.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedGoal = name
                                        selectedGoalIndex = index
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (goalIds.isEmpty() && !isLoading) {
                    Text(
                        text = "Buat tujuan tabungan dulu di menu Target Tabungan!",
                        fontSize = 12.sp,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Tanggal") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDatePicker(context, calendar) { year, month, day ->
                                calendar.set(year, month, day)
                                selectedDate = calendar.time
                                dateText = "$day/${month + 1}/$year"
                            }
                        },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                    label = { Text("Nominal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Note Input
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Add Button
                Button(
                    onClick = {
                        if (goalIds.isEmpty()) {
                            Toast.makeText(context, "Buat tujuan tabungan dulu!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (amount.isEmpty()) {
                            Toast.makeText(context, "Masukkan jumlah!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val amountLong = amount.toLongOrNull()
                        if (amountLong == null || amountLong <= 0) {
                            Toast.makeText(context, "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val selectedGoalId = goalIds[selectedGoalIndex]
                        val selectedGoalName = goalNames[selectedGoalIndex].substringBefore(" (")

                        val transaction = hashMapOf(
                            "user_id" to userId,
                            "type" to "saving",
                            "goal_id" to selectedGoalId,
                            "category" to selectedGoalName,
                            "amount" to amountLong,
                            "date" to Timestamp(selectedDate),
                            "created_at" to Timestamp(Date()),
                            "note" to if (note.isNotEmpty()) note else "Tabungan untuk $selectedGoalName"
                        )

                        db.collection("Transactions")
                            .add(transaction)
                            .addOnSuccessListener {
                                // Update goal's current_amount
                                val goalRef = db.collection("Goals").document(selectedGoalId)
                                db.runTransaction { trans ->
                                    val goalSnapshot = trans.get(goalRef)
                                    val currentAmount = goalSnapshot.getLong("current_amount") ?: 0L
                                    trans.update(goalRef, "current_amount", currentAmount + amountLong)
                                }.addOnSuccessListener {
                                    Toast.makeText(context, "Tabungan ditambahkan!", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                }.addOnFailureListener {
                                    Toast.makeText(context, "Transaksi tersimpan tapi gagal update goal", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue
                    ),
                    enabled = !isLoading && goalIds.isNotEmpty()
                ) {
                    Text("Tambahkan", fontSize = 16.sp)
                }
            }
        }
    }
}

// ==================== DATE PICKER HELPER ====================
private fun showDatePicker(
    context: Context,
    calendar: Calendar,
    onDateSelected: (Int, Int, Int) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onDateSelected(year, month, day)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}