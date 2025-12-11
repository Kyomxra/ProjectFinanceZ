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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projectmap2.R
import com.example.projectmap2.ui.models.Goal
import com.example.projectmap2.ui.models.GoalOperationState
import com.example.projectmap2.ui.theme.*
import com.example.projectmap2.ui.viewmodels.GoalsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: GoalsViewModel = viewModel()

    val goals by viewModel.goals.collectAsState()
    val operationState by viewModel.operationState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf<Goal?>(null) }
    var showOptionsDialog by remember { mutableStateOf(false) }

    // Load goals
    LaunchedEffect(userId) {
        viewModel.loadGoals(userId)
    }

    // Handle operation state
    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is GoalOperationState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetOperationState()
            }
            is GoalOperationState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetOperationState()
            }
            else -> {}
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
            viewModel = viewModel,
            context = context
        )
    }

    // Goal Options Dialog
    if (showOptionsDialog && selectedGoal != null) {
        GoalOptionsDialog(
            goal = selectedGoal!!,
            userId = userId,
            onDismiss = { showOptionsDialog = false },
            viewModel = viewModel,
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
    viewModel: GoalsViewModel,
    context: Context
) {
    var goalName by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
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

                OutlinedTextField(
                    value = dateText,
                    onValueChange = {},
                    label = { Text("Deadline") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
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
                        val amount = targetAmount.toLongOrNull() ?: 0
                        viewModel.addGoal(userId, goalName, amount, calendar.time)
                        onDismiss()
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
    viewModel: GoalsViewModel,
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
                            viewModel.markAsCompleted(goal.id)
                            onDismiss()
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
        EditGoalDialog(goal, onDismiss = { showEditDialog = false }, viewModel, context)
    }

    if (showDeleteDialog) {
        DeleteGoalDialog(goal, onDismiss = { showDeleteDialog = false; onDismiss() }, viewModel)
    }

    if (showAddSavingDialog) {
        AddSavingToGoalDialog(goal, userId, onDismiss = { showAddSavingDialog = false }, viewModel)
    }
}

@Composable
fun EditGoalDialog(goal: Goal, onDismiss: () -> Unit, viewModel: GoalsViewModel, context: Context) {
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
                        val amount = targetAmount.toLongOrNull() ?: 0
                        viewModel.updateGoal(goal.id, goalName, amount, calendar.time)
                        onDismiss()
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
fun DeleteGoalDialog(goal: Goal, onDismiss: () -> Unit, viewModel: GoalsViewModel) {
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
                    viewModel.deleteGoal(goal)
                    onDismiss()
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
fun AddSavingToGoalDialog(goal: Goal, userId: String, onDismiss: () -> Unit, viewModel: GoalsViewModel) {
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
                    val amt = amount.toLongOrNull() ?: 0
                    viewModel.addSavingToGoal(userId, goal, amt)
                    onDismiss()
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