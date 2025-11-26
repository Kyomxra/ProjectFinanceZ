package com.example.projectmap2.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.projectmap2.R
import com.example.projectmap2.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.content.res.Configuration
import androidx.navigation.NavController
import com.example.projectmap2.ui.navigation.Screen

data class Transaction(
    val title: String,
    val amount: String,
    val date: String,
    val timestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userId: String,
    navController: NavController,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    val db = FirebaseFirestore.getInstance()

    val configuration = LocalContext.current.resources.configuration
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val circleWidth = if (isLandscape) 2050.dp else 1250.dp
    val circleHeight = if (isLandscape) 450.dp else 550.dp
    val circleOffsetY = if (isLandscape) (-160).dp else (-210).dp

    var userName by remember { mutableStateOf("") }
    var greeting by remember { mutableStateOf("Selamat siang,") }
    var monthName by remember { mutableStateOf("Loading...") }
    var totalIncome by remember { mutableIntStateOf(0) }
    var totalExpense by remember { mutableIntStateOf(0) }
    var transactions by remember { mutableStateOf(listOf<Transaction>()) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showSavingDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Load user data
    LaunchedEffect(userId) {
        try {
            val doc = db.collection("User").document(userId).get().await()
            if (doc.exists()) {
                val fName = doc.getString("FName") ?: ""
                val lName = doc.getString("LName") ?: ""
                userName = "$fName $lName"
            }

            loadSummaryAndTransactions(db, userId) { month, income, expense, txns ->
                monthName = month
                totalIncome = income
                totalExpense = expense
                transactions = txns
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                DrawerMenuItem(R.drawable.profile, "Profile") {
                    scope.launch {
                        drawerState.close()
                    }
                    navController.navigate(Screen.Profile.route)
                }

                DrawerMenuItem(R.drawable.inbox, "Inbox") {
                    Toast.makeText(context, "Inbox clicked", Toast.LENGTH_SHORT).show()
                }
                DrawerMenuItem(R.drawable.settings, "Settings") {
                    Toast.makeText(context, "Settings clicked", Toast.LENGTH_SHORT).show()
                }
                DrawerMenuItem(R.drawable.logout, "Logout") {
                    prefs.edit().remove("userId").apply()
                    Toast.makeText(context, "Logout berhasil!", Toast.LENGTH_SHORT).show()
                    onLogout()
                }
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                BottomAppBar(
                    containerColor = Color.White,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NavigationBarItem(
                            selected = true,
                            onClick = { },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.home),
                                    contentDescription = "Home",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { Text("Home", color = Color.Black) }
                        )

                        // Empty space for FAB
                        Spacer(modifier = Modifier.weight(1f))

                        NavigationBarItem(
                            selected = false,
                            onClick = {
                                // Navigate to Location
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.location),
                                    contentDescription = "Location",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = { Text("Location", color = Color.Black) }
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showBottomSheet = true },
                    containerColor = DarkBlue,
                    shape = CircleShape,
                    modifier = Modifier.offset(y = 50.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = "Tambah",
                        tint = Color.White
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8F9FA))
            ) {
                // Background Half Circle with gradient (bigger, covers sides)
                Box(
                    modifier = Modifier
                        .width(circleWidth)
                        .height(circleHeight)
                        .offset(y = circleOffsetY)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(DarkBlue, LightBlue)
                            ),
                            shape = CircleShape
                        )
                        .align(Alignment.TopCenter)
                        .zIndex(0f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                ) {
                    // Top App Bar (Manual)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(start = 10.dp, top = 10.dp)
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.menu),
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Scrollable Content
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp)
                    ) {
                        item {
                            // Welcome Text (Fixed at top)
                            Text(
                                text = greeting,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = userName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        item {
                            // Summary Fragment
                            SummaryCard(monthName, totalIncome, totalExpense)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item {
                            // Menu Cards (3 cards horizontal)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                MenuCard(
                                    iconRes = R.drawable.wallet,
                                    title = "Pendapatan Pokok",
                                    modifier = Modifier.weight(1f).padding(8.dp)
                                ) {
                                    // Navigate to RecurringIncome
                                }

                                MenuCard(
                                    iconRes = R.drawable.piggybank,
                                    title = "Target Tabungan",
                                    modifier = Modifier.weight(1f).padding(8.dp)
                                ) {
                                    navController.navigate(Screen.Goals.createRoute(userId))
                                }

                                MenuCard(
                                    iconRes = R.drawable.report,
                                    title = "Laporan Transaksi",
                                    modifier = Modifier.weight(1f).padding(8.dp)
                                ) {
                                    // Navigate to Report
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item {
                            Text(
                                text = "Transactions History",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }

                        items(transactions) { transaction ->
                            TransactionItem(transaction)
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet
    if (showBottomSheet) {
        BottomSheetOptions(
            onDismiss = { showBottomSheet = false },
            onIncomeClick = {
                showBottomSheet = false
                showIncomeDialog = true
            },
            onExpenseClick = {
                showBottomSheet = false
                showExpenseDialog = true
            },
            onSavingClick = {
                showBottomSheet = false
                showSavingDialog = true
            }
        )
    }

    if (showIncomeDialog) {
        AddIncomeDialog(
            userId = userId,
            onDismiss = { showIncomeDialog = false },
            onSuccess = {
                showIncomeDialog = false
                // Reload data
                scope.launch {
                    loadSummaryAndTransactions(db, userId) { month, income, expense, txns ->
                        monthName = month
                        totalIncome = income
                        totalExpense = expense
                        transactions = txns
                    }
                }
            }
        )
    }

    if (showExpenseDialog) {
        AddExpenseDialog(
            userId = userId,
            onDismiss = { showExpenseDialog = false },
            onSuccess = {
                showExpenseDialog = false
                // Reload data
                scope.launch {
                    loadSummaryAndTransactions(db, userId) { month, income, expense, txns ->
                        monthName = month
                        totalIncome = income
                        totalExpense = expense
                        transactions = txns
                    }
                }
            }
        )
    }

    if (showSavingDialog) {
        AddSavingDialog(
            userId = userId,
            onDismiss = { showSavingDialog = false },
            onSuccess = {
                showSavingDialog = false
                // Reload data
                scope.launch {
                    loadSummaryAndTransactions(db, userId) { month, income, expense, txns ->
                        monthName = month
                        totalIncome = income
                        totalExpense = expense
                        transactions = txns
                    }
                }
            }
        )
    }
}

@Composable
fun SummaryCard(month: String, income: Int, expense: Int) {
    val balance = income - expense
    val total = income + expense
    val incomePercent = if (total > 0) (income.toFloat() / total) else 0f
    val expensePercent = if (total > 0) (expense.toFloat() / total) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = month,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatCurrency(income.toLong()),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pemasukan",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.alpha(0.8f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatCurrency(expense.toLong()),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Pengeluaran",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.alpha(0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = formatCurrencyWithSign(balance.toLong()),
                    color = Color(0xFF00E676),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Saldo",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.alpha(0.8f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar: Green (Income) + Red (Expense)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    if (total > 0) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Green part (Income)
                            if (income > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(incomePercent)
                                        .background(
                                            color = Color.Green,
                                            shape = RoundedCornerShape(
                                                topStart = 6.dp,
                                                bottomStart = 6.dp,
                                                topEnd = if (expense == 0) 6.dp else 0.dp,
                                                bottomEnd = if (expense == 0) 6.dp else 0.dp
                                            )
                                        )
                                )
                            }
                            // Red part (Expense)
                            if (expense > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(expensePercent)
                                        .background(
                                            color = Color.Red,
                                            shape = RoundedCornerShape(
                                                topEnd = 6.dp,
                                                bottomEnd = 6.dp,
                                                topStart = if (income == 0) 6.dp else 0.dp,
                                                bottomStart = if (income == 0) 6.dp else 0.dp
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Hijau = Pemasukan | Merah = Pengeluaran",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .alpha(0.7f)
                        .padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
fun MenuCard(iconRes: Int, title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title,
                tint = DarkBlue,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Black,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = transaction.date,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = transaction.amount,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Purple500
            )
        }
    }
}

@Composable
fun DrawerMenuItem(iconRes: Int, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = title,
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, fontSize = 16.sp, color = Color.Black)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetOptions(
    onDismiss: () -> Unit,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onSavingClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            BottomSheetItem("💰 Pemasukan", onIncomeClick)
            HorizontalDivider()
            BottomSheetItem("💸 Pengeluaran", onExpenseClick)
            HorizontalDivider()
            BottomSheetItem("🏦 Tabungan", onSavingClick)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BottomSheetItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    )
}

private suspend fun loadSummaryAndTransactions(
    db: FirebaseFirestore,
    userId: String,
    onResult: (String, Int, Int, List<Transaction>) -> Unit
) {
    try {
        val calendar = Calendar.getInstance()
        val monthName = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())

        val documents = db.collection("Transactions")
            .whereEqualTo("user_id", userId)
            .get()
            .await()

        var totalIncome = 0L
        var totalExpense = 0L
        val txnList = mutableListOf<Transaction>()

        for (doc in documents) {
            val type = doc.getString("type") ?: ""
            val amount = doc.getLong("amount") ?: 0
            val category = doc.getString("category") ?: "Transaksi"
            val timestamp = doc.getTimestamp("date")
            val note = doc.getString("note") ?: ""

            when (type) {
                "income" -> totalIncome += amount
                "expense" -> totalExpense += amount
                "saving" -> totalExpense += amount
            }

            val dateStr = if (timestamp != null) {
                formatDate(timestamp)
            } else "Unknown date"

            val amountStr = formatCurrency(amount, type)
            val title = if (note.isNotEmpty()) note else category

            txnList.add(Transaction(title, amountStr, dateStr, timestamp?.toDate()?.time ?: 0))
        }

        txnList.sortByDescending { it.timestamp }

        onResult(monthName, totalIncome.toInt(), totalExpense.toInt(), txnList)
    } catch (e: Exception) {
        onResult("Error", 0, 0, emptyList())
    }
}

private fun formatDate(timestamp: Timestamp): String {
    val date = timestamp.toDate()
    val calendar = Calendar.getInstance()
    val today = calendar.time

    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = calendar.time

    return when {
        isSameDay(date, today) -> "Today"
        isSameDay(date, yesterday) -> "Yesterday"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(date)
    }
}

private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun formatCurrency(amount: Long, type: String = ""): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val formatted = formatter.format(amount).replace("Rp", "Rp ")
    return when (type) {
        "income" -> "+ $formatted"
        "expense" -> "- $formatted"
        "saving" -> "💰 $formatted"
        else -> formatted
    }
}

private fun formatCurrencyWithSign(amount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val formatted = formatter.format(Math.abs(amount)).replace("Rp", "Rp ")
    return if (amount >= 0) "+ $formatted" else "- $formatted"
}
