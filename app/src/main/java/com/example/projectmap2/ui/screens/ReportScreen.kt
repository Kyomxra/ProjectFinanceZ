package com.example.projectmap2.ui.screens

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projectmap2.R
import com.example.projectmap2.ui.models.BudgetUpdateState
import com.example.projectmap2.ui.theme.*
import com.example.projectmap2.ui.viewmodels.ReportViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ReportViewModel = viewModel()

    val userBudget by viewModel.userBudget.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val monthYear by viewModel.monthYear.collectAsState()
    val budgetText by viewModel.budgetText.collectAsState()
    val comparison by viewModel.comparison.collectAsState()
    val budgetUpdateState by viewModel.budgetUpdateState.collectAsState()

    var showBudgetDialog by remember { mutableStateOf(false) }

    // Load data
    LaunchedEffect(userId) {
        viewModel.loadReportData(userId)
    }

    // Handle budget update state
    LaunchedEffect(budgetUpdateState) {
        when (val state = budgetUpdateState) {
            is BudgetUpdateState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                showBudgetDialog = false
                viewModel.resetBudgetUpdateState()
            }
            is BudgetUpdateState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetBudgetUpdateState()
            }
            else -> {}
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
                        text = comparison.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = comparison.color
                    )
                    Text(
                        text = comparison.detail,
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
                        text = comparison.recommendation,
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
                viewModel.updateBudget(userId, budget)
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

private fun formatCurrency(amount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ")
}