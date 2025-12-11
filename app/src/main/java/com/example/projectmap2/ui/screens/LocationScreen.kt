package com.example.projectmap2.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projectmap2.ui.models.LocationState
import com.example.projectmap2.ui.models.TransactionWithLocation
import com.example.projectmap2.ui.viewmodels.LocationViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    userId: String,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val viewModel: LocationViewModel = viewModel()

    val transactions by viewModel.transactions.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val locationState by viewModel.locationState.collectAsState()

    var hasLocationPermission by remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    viewModel.updateCurrentLocation(location)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Request permission on first composition
    LaunchedEffect(Unit) {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            hasLocationPermission = true
        } else {
            locationPermissionLauncher.launch(permission)
        }
    }

    // Get current location
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    viewModel.updateCurrentLocation(location)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Load transactions
    LaunchedEffect(userId) {
        viewModel.loadTransactionsWithLocation(userId)
    }

    // Update map when transactions or location changes
    LaunchedEffect(transactions, currentLocation, googleMap) {
        googleMap?.let { map ->
            map.clear()

            // Show user location
            currentLocation?.let { location ->
                val userPosition = LatLng(location.latitude, location.longitude)
                map.addMarker(
                    MarkerOptions()
                        .position(userPosition)
                        .title("Lokasi Kamu")
                        .icon(createUserLocationMarker())
                        .zIndex(1000f)
                )
            }

            // Show transactions
            if (transactions.isNotEmpty()) {
                val boundsBuilder = LatLngBounds.Builder()
                val locationFrequency = mutableMapOf<String, Int>()

                currentLocation?.let {
                    boundsBuilder.include(LatLng(it.latitude, it.longitude))
                }

                transactions.forEach { transaction ->
                    val position = LatLng(transaction.latitude, transaction.longitude)
                    val markerColor = getCategoryColor(transaction.category)

                    map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(transaction.name)
                            .snippet("${formatCurrency(transaction.amount)} - ${transaction.category}\n${transaction.date}")
                            .icon(createCustomMarker(markerColor, transaction.amount))
                    )

                    val locationKey = "${transaction.latitude},${transaction.longitude}"
                    locationFrequency[locationKey] = locationFrequency.getOrDefault(locationKey, 0) + 1

                    boundsBuilder.include(position)
                }

                // Draw heatmap circles
                drawHeatmapCircles(map, locationFrequency)

                // Move camera
                try {
                    val bounds = boundsBuilder.build()
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                } catch (e: Exception) {
                    currentLocation?.let {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude), 14f
                        ))
                    }
                }
            } else {
                currentLocation?.let {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.latitude, it.longitude), 14f
                    ))
                } ?: run {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        LatLng(-6.200000, 106.816666), 12f
                    ))
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = false,
                    onClick = onNavigateToHome
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Lokasi") },
                    label = { Text("Lokasi") },
                    selected = true,
                    onClick = { }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Google Map
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        onCreate(null)
                        onResume()
                        getMapAsync { map ->
                            googleMap = map
                            map.uiSettings.isZoomControlsEnabled = true
                            map.uiSettings.isMyLocationButtonEnabled = false

                            if (hasLocationPermission) {
                                try {
                                    map.isMyLocationEnabled = true
                                } catch (e: SecurityException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    when (lifecycleOwner.lifecycle.currentState) {
                        androidx.lifecycle.Lifecycle.State.RESUMED -> view.onResume()
                        androidx.lifecycle.Lifecycle.State.STARTED -> view.onStart()
                        androidx.lifecycle.Lifecycle.State.CREATED -> view.onCreate(null)
                        androidx.lifecycle.Lifecycle.State.DESTROYED -> view.onDestroy()
                        else -> {}
                    }
                }
            )

            // Statistics Card
            when (val state = locationState) {
                is LocationState.Success -> {
                    StatisticsCard(
                        totalSpent = state.statistics.totalSpent,
                        totalTransactions = state.statistics.totalTransactions,
                        mostFrequentLocation = state.statistics.mostFrequentLocation,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                    )
                }
                else -> {
                    StatisticsCard(
                        totalSpent = 0.0,
                        totalTransactions = 0,
                        mostFrequentLocation = null,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                    )
                }
            }

            // Legend Card
            LegendCard(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView?.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView?.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDestroy()
        }
    }
}

@Composable
fun StatisticsCard(
    totalSpent: Double,
    totalTransactions: Int,
    mostFrequentLocation: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Pengeluaran Bulan Ini",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Text(
                text = formatCurrency(totalSpent),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6200EE),
                modifier = Modifier.padding(top = 4.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.LightGray
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total Transaksi",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "$totalTransactions transaksi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Lokasi Favorit",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = mostFrequentLocation ?: "-",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun LegendCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Kategori",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LegendItem(color = Color(0xFFFF9800), label = "Makanan")
            Spacer(modifier = Modifier.height(4.dp))
            LegendItem(color = Color(0xFF2196F3), label = "Transport")
            Spacer(modifier = Modifier.height(4.dp))
            LegendItem(color = Color(0xFF9C27B0), label = "Shopping")
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp)
    }
}

// Helper functions
fun createUserLocationMarker(): BitmapDescriptor {
    val size = 70
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val glowPaint = Paint().apply {
        color = android.graphics.Color.argb(80, 33, 150, 243)
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, glowPaint)

    val mainPaint = Paint().apply {
        color = android.graphics.Color.rgb(33, 150, 243)
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 3f, mainPaint)

    val borderPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 3f - 2, borderPaint)

    val dotPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 8f, dotPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun createCustomMarker(color: Int, amount: Double): BitmapDescriptor {
    val size = when {
        amount >= 1000000 -> 60
        amount >= 100000 -> 50
        else -> 40
    }

    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    paint.apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 2, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

fun getCategoryColor(category: String): Int {
    return when (category.lowercase()) {
        "makan", "makanan", "food & drink" -> android.graphics.Color.rgb(255, 152, 0)
        "transport", "transportasi" -> android.graphics.Color.rgb(33, 150, 243)
        "belanja", "shopping" -> android.graphics.Color.rgb(156, 39, 176)
        "hiburan", "entertainment" -> android.graphics.Color.rgb(233, 30, 99)
        "bills", "tagihan" -> android.graphics.Color.rgb(76, 175, 80)
        else -> android.graphics.Color.rgb(158, 158, 158)
    }
}

fun drawHeatmapCircles(map: GoogleMap, locationFrequency: Map<String, Int>) {
    val maxFrequency = locationFrequency.values.maxOrNull() ?: 1

    locationFrequency.forEach { (locationKey, frequency) ->
        if (frequency > 1) {
            val coords = locationKey.split(",")
            val position = LatLng(coords[0].toDouble(), coords[1].toDouble())

            val radius = 200.0 + (frequency * 100.0)
            val fillColor = android.graphics.Color.argb(
                (50 + (frequency.toFloat() / maxFrequency * 80)).toInt(),
                255, 0, 0
            )

            map.addCircle(
                CircleOptions()
                    .center(position)
                    .radius(radius)
                    .strokeColor(android.graphics.Color.argb(180, 255, 0, 0))
                    .strokeWidth(2f)
                    .fillColor(fillColor)
            )
        }
    }
}

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return formatter.format(amount).replace("Rp", "Rp ")
}