package com.example.projectmap2.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.projectmap2.R
import com.example.projectmap2.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var userBudget by remember { mutableLongStateOf(0L) }
    var totalExpense by remember { mutableLongStateOf(0L) }
    var monthYear by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("Belum diatur") }
    var comparisonTitle by remember { mutableStateOf("⚙️ Budget belum diatur") }
    var comparisonColor by remember { mutableStateOf(Color.Gray) }
    var comparisonDetail by remember { mutableStateOf("") }
    var recommendation by remember { mutableStateOf("") }
    var showBudgetDialog by remember { mutableStateOf(false) }

    // Load data
    LaunchedEffect(userId) {
        // Set month year
        val calendar = Calendar.getInstance()
        monthYear = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(calendar.time)

        // Load budget
        try {
            val doc = db.collection("User").document(userId).get().await()
            if (doc.exists()) {
                userBudget = doc.getLong("monthly_budget") ?: 0L
                if (userBudget > 0) {
                    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                    budgetText = formatter.format(userBudget).replace("Rp", "Rp ")
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // Load expenses
        loadExpenseData(db, userId) { expense ->
            totalExpense = expense
            calculateComparison(
                totalExpense,
                userBudget
            ) { title, color, detail, rec ->
                comparisonTitle = title
                comparisonColor = color
                comparisonDetail = detail
                recommendation = rec
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Laporan Transaksi",
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
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Laporan Bulan $monthYear",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = "Analisis Pengeluaran vs Budget",
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Total Expense Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(DarkBlue, LightBlue)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Total Pengeluaran Bulan Ini",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.alpha(0.9f)
                        )
                        Text(
                            text = formatCurrency(totalExpense),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Budget Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "💰 Budget Bulanan Kamu",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                            Text(
                                text = budgetText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Button(
                            onClick = { showBudgetDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkBlue.copy(alpha = 0.1f),
                                contentColor = DarkBlue
                            )
                        ) {
                            Text("Atur Budget", fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "per bulan",
                        fontSize = 12.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Comparison Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = comparisonTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = comparisonColor
                    )
                    Text(
                        text = comparisonDetail,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Recommendation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Rekomendasi",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57C00)
                    )
                    Text(
                        text = recommendation,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Footer Info
            Text(
                text = "* Pantau pengeluaranmu agar tetap sesuai dengan budget",
                fontSize = 11.sp,
                color = Color(0xFF999999),
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }

    // Budget Dialog
    if (showBudgetDialog) {
        SetBudgetDialog(
            currentBudget = if (userBudget > 0) userBudget.toString() else "",
            onDismiss = { showBudgetDialog = false },
            onSave = { budget ->
                scope.launch {
                    try {
                        db.collection("User").document(userId)
                            .update("monthly_budget", budget)
                            .await()

                        userBudget = budget
                        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                        budgetText = formatter.format(budget).replace("Rp", "Rp ")

                        Toast.makeText(context, "Budget berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        showBudgetDialog = false

                        // Refresh comparison
                        calculateComparison(
                            totalExpense,
                            userBudget
                        ) { title, color, detail, rec ->
                            comparisonTitle = title
                            comparisonColor = color
                            comparisonDetail = detail
                            recommendation = rec
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun SetBudgetDialog(
    currentBudget: String,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var budget by remember { mutableStateOf(currentBudget) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Budget Bulanan", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Masukkan target budget pengeluaran bulananmu",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = budget,
                    onValueChange = { if (it.all { char -> char.isDigit() }) budget = it },
                    label = { Text("Budget") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBlue
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (budget.isEmpty()) return@Button
                    val amt = budget.toLongOrNull()
                    if (amt != null && amt > 0) {
                        onSave(amt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

private suspend fun loadExpenseData(
    db: FirebaseFirestore,
    userId: String,
    onResult: (Long) -> Unit
) {
    try {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = Timestamp(calendar.time)

        calendar.set(currentYear, currentMonth + 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val endOfMonth = Timestamp(calendar.time)

        val documents = db.collection("Transactions")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("type", "expense")
            .get()
            .await()

        var totalExpense = 0L

        for (doc in documents) {
            val timestamp = doc.getTimestamp("date")
            if (timestamp != null) {
                if (timestamp.toDate().after(startOfMonth.toDate()) &&
                    timestamp.toDate().before(endOfMonth.toDate())) {
                    val amount = doc.getLong("amount") ?: 0
                    totalExpense += amount
                }
            }
        }

        onResult(totalExpense)
    } catch (e: Exception) {
        onResult(0L)
    }
}

private fun calculateComparison(
    userExpense: Long,
    userBudget: Long,
    onResult: (String, Color, String, String) -> Unit
) {
    when {
        userBudget == 0L -> {
            onResult(
                "⚙️ Budget belum diatur",
                Color.Gray,
                "Atur budget bulananmu terlebih dahulu untuk melihat perbandingan",
                "💡 Klik tombol 'Atur Budget' di atas untuk mulai mengelola keuanganmu!"
            )
        }
        userExpense == 0L -> {
            onResult(
                "Belum ada pengeluaran",
                Color.Gray,
                "Kamu belum mencatat pengeluaran bulan ini",
                "💡 Mulai catat pengeluaranmu untuk manajemen keuangan yang lebih baik!"
            )
        }
        userExpense < userBudget -> {
            val remaining = userBudget - userExpense
            val usedPercentage = ((userExpense.toDouble() / userBudget) * 100).toInt()
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val formattedRemaining = formatter.format(remaining).replace("Rp", "Rp ")

            onResult(
                "🎉 Kamu masih di jalur yang benar!",
                Color(0xFF4CAF50),
                "Kamu sudah menggunakan $usedPercentage% dari budget. Sisa budget: $formattedRemaining",
                "💰 Bagus! Pertahankan kebiasaan ini hingga akhir bulan. Sisihkan sisanya untuk tabungan!"
            )
        }
        userExpense == userBudget -> {
            onResult(
                "👌 Budget habis tepat!",
                Color(0xFF2196F3),
                "Pengeluaranmu sama persis dengan budget yang diatur",
                "📊 Coba sisihkan sebagian untuk dana darurat atau tabungan!"
            )
        }
        else -> {
            val overbudget = userExpense - userBudget
            val overPercentage = (((userExpense - userBudget).toDouble() / userBudget) * 100).toInt()
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            val formattedOver = formatter.format(overbudget).replace("Rp", "Rp ")

            onResult(
                "⚠️ Pengeluaran melebihi budget",
                Color(0xFFF44336),
                "Kamu sudah over budget $formattedOver ($overPercentage% lebih tinggi dari target)",
                "💡 Evaluasi pengeluaranmu! Kurangi pengeluaran tidak penting dan pertimbangkan untuk menyesuaikan budget bulan depan."
            )
        }
    }
}

private fun formatCurrency(amount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ")
}