package com.example.projectmap2.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import com.example.projectmap2.R
import com.example.projectmap2.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class Goal(
    val id: String,
    val goalName: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val deadline: Timestamp,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var goals by remember { mutableStateOf(listOf<Goal>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf<Goal?>(null) }
    var showOptionsDialog by remember { mutableStateOf(false) }

    // Load goals
    LaunchedEffect(userId) {
        db.collection("Goals")
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    goals = snapshots.documents.mapNotNull { doc ->
                        Goal(
                            id = doc.id,
                            goalName = doc.getString("goal_name") ?: "",
                            targetAmount = doc.getLong("target_amount") ?: 0,
                            currentAmount = doc.getLong("current_amount") ?: 0,
                            deadline = doc.getTimestamp("deadline") ?: Timestamp.now(),
                            status = doc.getString("status") ?: "active"
                        )
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Target Tabungan",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DarkBlue
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_input_add),
                    contentDescription = "Add Goal",
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            items(goals) { goal ->
                GoalItem(
                    goal = goal,
                    onClick = {
                        selectedGoal = goal
                        showOptionsDialog = true
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Add Goal Dialog
    if (showAddDialog) {
        AddGoalDialog(
            userId = userId,
            onDismiss = { showAddDialog = false },
            db = db,
            context = context
        )
    }

    // Goal Options Dialog
    if (showOptionsDialog && selectedGoal != null) {
        GoalOptionsDialog(
            goal = selectedGoal!!,
            userId = userId,
            onDismiss = { showOptionsDialog = false },
            db = db,
            context = context
        )
    }
}

@Composable
fun GoalItem(goal: Goal, onClick: () -> Unit) {
    val progress = if (goal.targetAmount > 0) {
        ((goal.currentAmount.toDouble() / goal.targetAmount) * 100).toInt()
    } else 0

    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val currentFormatted = formatter.format(goal.currentAmount).replace("Rp", "Rp ")
    val targetFormatted = formatter.format(goal.targetAmount).replace("Rp", "Rp ")

    val now = Calendar.getInstance().time
    val deadline = goal.deadline.toDate()
    val daysRemaining = TimeUnit.MILLISECONDS.toDays(deadline.time - now.time)
    val deadlineStr = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(deadline)
    val statusText = if (progress >= 100) "✅ SELESAI" else "🔄 ${goal.status.uppercase()}"

    val motivation = when {
        progress >= 100 -> "🎉 Target tercapai! Selamat!"
        progress >= 75 && daysRemaining > 30 -> "💪 Hebat! Hampir sampai target!"
        progress >= 50 && daysRemaining > 30 -> "👍 Bagus! Terus semangat menabung!"
        progress >= 25 && daysRemaining > 30 -> "🌟 Kamu bisa! Terus konsisten!"
        daysRemaining <= 7 && progress < 100 -> "⏰ Deadline tinggal $daysRemaining hari! Ayo semangat!"
        daysRemaining <= 30 && progress < 50 -> "⚠️ Tingkatkan tabunganmu! Deadline mendekat!"
        daysRemaining <= 0 && progress < 100 -> "⏱️ Deadline terlewat. Perpanjang atau sesuaikan target?"
        else -> "💰 Ayo mulai menabung untuk mencapai targetmu!"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = goal.goalName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Text(
                text = "$currentFormatted / $targetFormatted ($progress%)",
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(8.dp),
                color = if (progress >= 100) Color(0xFF4CAF50) else Purple500,
                trackColor = Color.LightGray
            )

            Text(
                text = "Deadline: $deadlineStr • $statusText",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = motivation,
                    fontSize = 14.sp,
                    color = Color(0xFF1976D2),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    userId: String,
    onDismiss: () -> Unit,
    db: FirebaseFirestore,
    context: Context
) {
    var goalName by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }

    // PERBAIKAN: Buat calendar instance yang proper
    val calendar = remember { Calendar.getInstance() }

    var dateText by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tambah Target Tabungan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = goalName,
                    onValueChange = { goalName = it },
                    label = { Text("Nama Target (e.g. Beli Laptop)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) targetAmount = it },
                    label = { Text("Jumlah Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // PERBAIKAN: TextField untuk deadline
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Deadline") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // PERBAIKAN: DatePickerDialog yang benar
                            val datePickerDialog = DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(year, month, day)
                                    dateText = String.format("%02d/%02d/%04d", day, month + 1, year)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )

                            // PERBAIKAN: Set minDate ke besok (hari ini + 1)
                            val tomorrow = Calendar.getInstance()
                            tomorrow.add(Calendar.DAY_OF_MONTH, 1)
                            tomorrow.set(Calendar.HOUR_OF_DAY, 0)
                            tomorrow.set(Calendar.MINUTE, 0)
                            tomorrow.set(Calendar.SECOND, 0)
                            tomorrow.set(Calendar.MILLISECOND, 0)
                            datePickerDialog.datePicker.minDate = tomorrow.timeInMillis

                            datePickerDialog.show()
                        },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (goalName.isEmpty()) {
                            Toast.makeText(context, "Masukkan nama target!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (targetAmount.isEmpty()) {
                            Toast.makeText(context, "Masukkan jumlah target!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val amount = targetAmount.toLongOrNull()
                        if (amount == null || amount <= 0) {
                            Toast.makeText(context, "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val goal = hashMapOf(
                            "user_id" to userId,
                            "goal_name" to goalName,
                            "target_amount" to amount,
                            "current_amount" to 0L,
                            "deadline" to Timestamp(calendar.time),
                            "status" to "active",
                            "created_at" to Timestamp(Date())
                        )

                        db.collection("Goals")
                            .add(goal)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Target tabungan ditambahkan!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                ) {
                    Text("Tambahkan")
                }
            }
        }
    }
}

@Composable
fun GoalOptionsDialog(
    goal: Goal,
    userId: String,
    onDismiss: () -> Unit,
    db: FirebaseFirestore,
    context: Context
) {
    val progress = if (goal.targetAmount > 0) {
        ((goal.currentAmount.toDouble() / goal.targetAmount) * 100).toInt()
    } else 0

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSavingDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(goal.goalName) },
        text = {
            Column {
                TextButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit", modifier = Modifier.fillMaxWidth())
                }
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Hapus", modifier = Modifier.fillMaxWidth())
                }
                TextButton(
                    onClick = { showAddSavingDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tambah Tabungan", modifier = Modifier.fillMaxWidth())
                }
                if (progress >= 100 && goal.status != "completed") {
                    TextButton(
                        onClick = {
                            db.collection("Goals").document(goal.id)
                                .update("status", "completed")
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Target ditandai selesai! 🎉", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tandai Selesai", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    if (showEditDialog) {
        EditGoalDialog(goal, onDismiss = { showEditDialog = false }, db, context)
    }

    if (showDeleteDialog) {
        DeleteGoalDialog(goal, onDismiss = { showDeleteDialog = false; onDismiss() }, db, context)
    }

    if (showAddSavingDialog) {
        AddSavingToGoalDialog(goal, userId, onDismiss = { showAddSavingDialog = false }, db, context)
    }
}

@Composable
fun EditGoalDialog(goal: Goal, onDismiss: () -> Unit, db: FirebaseFirestore, context: Context) {
    var goalName by remember { mutableStateOf(goal.goalName) }
    var targetAmount by remember { mutableStateOf(goal.targetAmount.toString()) }
    val calendar = Calendar.getInstance().apply { time = goal.deadline.toDate() }
    var dateText by remember {
        mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text("Edit Target Tabungan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = goalName,
                    onValueChange = { goalName = it },
                    label = { Text("Nama Target") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { if (it.all { c -> c.isDigit() }) targetAmount = it },
                    label = { Text("Jumlah Target") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Deadline") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    calendar.set(y, m, d)
                                    dateText = "$d/${m + 1}/$y"
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val amount = targetAmount.toLongOrNull()
                        if (goalName.isEmpty() || amount == null || amount <= 0) {
                            Toast.makeText(context, "Lengkapi semua field!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        db.collection("Goals").document(goal.id)
                            .update(mapOf(
                                "goal_name" to goalName,
                                "target_amount" to amount,
                                "deadline" to Timestamp(calendar.time)
                            ))
                            .addOnSuccessListener {
                                Toast.makeText(context, "Target berhasil diupdate!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
                ) {
                    Text("Update")
                }
            }
        }
    }
}

@Composable
fun DeleteGoalDialog(goal: Goal, onDismiss: () -> Unit, db: FirebaseFirestore, context: Context) {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val savedAmount = formatter.format(goal.currentAmount).replace("Rp", "Rp ")
    val message = if (goal.currentAmount > 0) {
        "Yakin ingin menghapus \"${goal.goalName}\"?\n\n$savedAmount yang sudah ditabung akan dikembalikan ke saldo."
    } else {
        "Yakin ingin menghapus \"${goal.goalName}\"?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus Target?") },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = {
                    db.collection("Transactions")
                        .whereEqualTo("goal_id", goal.id)
                        .whereEqualTo("type", "saving")
                        .get()
                        .addOnSuccessListener { docs ->
                            val batch = db.batch()
                            docs.forEach { batch.delete(it.reference) }
                            batch.delete(db.collection("Goals").document(goal.id))
                            batch.commit().addOnSuccessListener {
                                val msg = if (goal.currentAmount > 0) {
                                    "Target dihapus! $savedAmount dikembalikan ke saldo."
                                } else "Target dihapus!"
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                onDismiss()
                            }
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Hapus")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun AddSavingToGoalDialog(goal: Goal, userId: String, onDismiss: () -> Unit, db: FirebaseFirestore, context: Context) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah ke ${goal.goalName}") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() }) amount = it },
                label = { Text("Nominal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toLongOrNull()
                    if (amt == null || amt <= 0) {
                        Toast.makeText(context, "Jumlah harus lebih dari 0!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val transaction = hashMapOf(
                        "user_id" to userId,
                        "type" to "saving",
                        "goal_id" to goal.id,
                        "category" to goal.goalName,
                        "amount" to amt,
                        "date" to Timestamp(Date()),
                        "created_at" to Timestamp(Date()),
                        "note" to "Tabungan untuk ${goal.goalName}"
                    )

                    db.collection("Transactions").add(transaction).addOnSuccessListener {
                        val goalRef = db.collection("Goals").document(goal.id)
                        db.runTransaction { trans ->
                            val current = trans.get(goalRef).getLong("current_amount") ?: 0L
                            trans.update(goalRef, "current_amount", current + amt)
                        }.addOnSuccessListener {
                            Toast.makeText(context, "Tabungan ditambahkan!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
            ) {
                Text("Tambahkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
