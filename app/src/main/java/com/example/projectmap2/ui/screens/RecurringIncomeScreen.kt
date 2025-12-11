package com.example.projectmap2.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projectmap2.R
import com.example.projectmap2.ui.models.SaveRecurringIncomeState
import com.example.projectmap2.ui.theme.*
import com.example.projectmap2.ui.viewmodels.RecurringIncomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringIncomeScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: RecurringIncomeViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val statusText by viewModel.statusText.collectAsState()
    val statusColor by viewModel.statusColor.collectAsState()
    val showDeleteButton by viewModel.showDeleteButton.collectAsState()
    val jobTitle by viewModel.jobTitle.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val days = (1..31).map { it.toString() }
    val isSaving = saveState is SaveRecurringIncomeState.Saving

    // Load existing recurring income
    LaunchedEffect(userId) {
        viewModel.loadRecurringIncome(userId)
    }

    // Handle save state changes
    LaunchedEffect(saveState) {
        when (val state = saveState) {
            is SaveRecurringIncomeState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                if (state.shouldNavigateBack) {
                    scope.launch {
                        delay(1500)
                        onNavigateBack()
                    }
                }
                viewModel.resetSaveState()
            }
            is SaveRecurringIncomeState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            else -> {}
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
                onValueChange = { viewModel.updateJobTitle(it) },
                placeholder = { Text("contoh: Software Engineer") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !isSaving,
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
                onValueChange = { if (it.all { char -> char.isDigit() }) viewModel.updateAmount(it) },
                placeholder = { Text("contoh: 5000000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                enabled = !isSaving,
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
                onExpandedChange = { if (!isSaving) expanded = !expanded }
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
                    enabled = !isSaving,
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
                                viewModel.updateSelectedDay(day)
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
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = !isSaving
                    ) {
                        Text("Hapus", color = Color.Red)
                    }
                }

                Button(
                    onClick = {
                        viewModel.saveRecurringIncome(userId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBlue
                    ),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Simpan")
                    }
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
                        viewModel.deleteRecurringIncome()
                        showDeleteDialog = false
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