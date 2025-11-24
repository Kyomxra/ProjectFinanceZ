package com.example.projectmap2.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.projectmap2.R
import com.example.projectmap2.ui.theme.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    val userId = prefs.getString("userId", null)
    val db = FirebaseFirestore.getInstance()

    var userName by remember { mutableStateOf("Loading...") }
    var userEmail by remember { mutableStateOf("Loading...") }
    var userDob by remember { mutableStateOf("Belum diisi") }
    var showEditDob by remember { mutableStateOf(false) }
    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var bannerBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showImagePickerDialog by remember { mutableStateOf(false) }
    var showDobDialog by remember { mutableStateOf(false) }
    var isBannerUpload by remember { mutableStateOf(false) }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uploadImageToFirebase(context, userId, it, isBannerUpload, db) { bitmap ->
                if (isBannerUpload) {
                    bannerBitmap = bitmap
                } else {
                    profileBitmap = bitmap
                }
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { uri ->
                uploadImageToFirebase(context, userId, uri, isBannerUpload, db) { bitmap ->
                    if (isBannerUpload) {
                        bannerBitmap = bitmap
                    } else {
                        profileBitmap = bitmap
                    }
                }
            }
        }
    }

    // Load user data
    LaunchedEffect(userId) {
        if (userId != null) {
            try {
                val doc = db.collection("User").document(userId).get().await()
                if (doc.exists()) {
                    val fName = doc.getString("FName") ?: ""
                    val lName = doc.getString("LName") ?: ""
                    userName = "$fName $lName"
                    userEmail = doc.getString("Email") ?: ""

                    // Perbaikan untuk DOB
                    val dob = doc.get("DOB")
                    when {
                        dob == null -> {
                            userDob = "Belum diisi"
                            showEditDob = true
                        }
                        dob is String -> {
                            if (dob.isEmpty()) {
                                userDob = "Belum diisi"
                                showEditDob = true
                            } else {
                                userDob = dob
                                showEditDob = false
                            }
                        }
                        dob is com.google.firebase.Timestamp -> {
                            // Format Timestamp ke string yang readable
                            val date = dob.toDate()
                            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                            userDob = sdf.format(date)
                            showEditDob = false
                        }
                        else -> {
                            userDob = dob.toString()
                            showEditDob = false
                        }
                    }

                    // Load profile image
                    val imageBase64 = doc.getString("imageURL")
                    if (!imageBase64.isNullOrEmpty()) {
                        profileBitmap = decodeBase64ToBitmap(imageBase64)
                    }

                    // Load banner image
                    val bannerBase64 = doc.getString("bannerURL")
                    if (!bannerBase64.isNullOrEmpty()) {
                        bannerBitmap = decodeBase64ToBitmap(bannerBase64)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                // Banner Section with Toolbar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    // Toolbar (always on top with dark background)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .zIndex(2f),
                        color = Color.Black.copy(alpha = 0.8f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    painter = painterResource(R.drawable.left),
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Banner Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .offset(y = 56.dp)
                    ) {
                        if (bannerBitmap != null) {
                            Image(
                                bitmap = bannerBitmap!!.asImageBitmap(),
                                contentDescription = "Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(DarkBlue, LightBlue)
                                        )
                                    )
                            )
                        }

                        // Edit Banner Button
                        FloatingActionButton(
                            onClick = {
                                isBannerUpload = true
                                showImagePickerDialog = true
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(40.dp),
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_camera),
                                contentDescription = "Edit Banner",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Profile Picture (overlapping banner)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 65.dp)
                            .zIndex(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            if (profileBitmap != null) {
                                Image(
                                    bitmap = profileBitmap!!.asImageBitmap(),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.profile),
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    tint = DarkBlue
                                )
                            }

                            // Edit button overlay
                            IconButton(
                                onClick = {
                                    isBannerUpload = false
                                    showImagePickerDialog = true
                                },
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(60.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.edit),
                                    contentDescription = "Edit Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            item {
                // Personal Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Informasi Pribadi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkBlue
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // User ID
                        ProfileInfoRow(
                            icon = R.drawable.profile,
                            label = "User ID",
                            value = userId ?: "N/A"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFF0F0F0)
                        )

                        // Name
                        ProfileInfoRow(
                            icon = R.drawable.profile,
                            label = "Nama Lengkap",
                            value = userName
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFF0F0F0)
                        )

                        // Email
                        ProfileInfoRow(
                            icon = R.drawable.inbox,
                            label = "Email",
                            value = userEmail
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color(0xFFF0F0F0)
                        )

                        // DOB with Edit button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.home),
                                    contentDescription = "DOB",
                                    tint = DarkBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Tanggal Lahir",
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666),
                                        modifier = Modifier.alpha(0.7f)
                                    )
                                    Text(
                                        text = userDob,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }

                            if (showEditDob) {
                                TextButton(onClick = { showDobDialog = true }) {
                                    Text("Edit", color = DarkBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Image Picker Dialog
    if (showImagePickerDialog) {
        ImagePickerDialog(
            onDismiss = { showImagePickerDialog = false },
            onGalleryClick = {
                showImagePickerDialog = false
                galleryLauncher.launch("image/*")
            },
            onCameraClick = {
                showImagePickerDialog = false
                val permission = Manifest.permission.CAMERA
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    val photoFile = File.createTempFile("profile_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
                    tempPhotoUri = uri
                    cameraLauncher.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(permission)
                }
            }
        )
    }

    // DOB Picker Dialog
    if (showDobDialog) {
        DobPickerDialog(
            onDismiss = { showDobDialog = false },
            onSave = { day, month, year ->
                val formattedDate = "$day $month $year"
                if (userId != null) {
                    db.collection("User").document(userId)
                        .update("DOB", formattedDate)
                        .addOnSuccessListener {
                            userDob = formattedDate
                            showEditDob = false
                            Toast.makeText(context, "Tanggal lahir disimpan!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal menyimpan DOB", Toast.LENGTH_SHORT).show()
                        }
                }
                showDobDialog = false
            }
        )
    }
}

@Composable
fun ProfileInfoRow(icon: Int, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = DarkBlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF666666),
                modifier = Modifier.alpha(0.7f)
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Sumber Foto") },
        text = {
            Column {
                TextButton(
                    onClick = onGalleryClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Gallery", modifier = Modifier.fillMaxWidth())
                }
                TextButton(
                    onClick = onCameraClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Camera", modifier = Modifier.fillMaxWidth())
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
}

@Composable
fun DobPickerDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    val days = (1..31).map { it.toString().padStart(2, '0') }
    val months = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (1950..currentYear).map { it.toString() }.reversed()

    var selectedDay by remember { mutableStateOf(days[0]) }
    var selectedMonth by remember { mutableStateOf(months[0]) }
    var selectedYear by remember { mutableStateOf(years[0]) }

    var expandedDay by remember { mutableStateOf(false) }
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Tanggal Lahir", fontWeight = FontWeight.Bold) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Day Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandedDay = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedDay, fontSize = 14.sp)
                    }
                    DropdownMenu(
                        expanded = expandedDay,
                        onDismissRequest = { expandedDay = false }
                    ) {
                        days.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    selectedDay = day
                                    expandedDay = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Month Dropdown
                Box(modifier = Modifier.weight(1.5f)) {
                    OutlinedButton(
                        onClick = { expandedMonth = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedMonth, fontSize = 12.sp)
                    }
                    DropdownMenu(
                        expanded = expandedMonth,
                        onDismissRequest = { expandedMonth = false }
                    ) {
                        months.forEach { month ->
                            DropdownMenuItem(
                                text = { Text(month) },
                                onClick = {
                                    selectedMonth = month
                                    expandedMonth = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Year Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandedYear = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedYear, fontSize = 14.sp)
                    }
                    DropdownMenu(
                        expanded = expandedYear,
                        onDismissRequest = { expandedYear = false }
                    ) {
                        years.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year) },
                                onClick = {
                                    selectedYear = year
                                    expandedYear = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedDay, selectedMonth, selectedYear) },
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

private fun uploadImageToFirebase(
    context: Context,
    userId: String?,
    uri: Uri,
    isBanner: Boolean,
    db: FirebaseFirestore,
    onSuccess: (Bitmap) -> Unit
) {
    if (userId == null) return

    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val resizedBitmap = if (isBanner) {
            resizeBitmap(bitmap, 1200, 400)
        } else {
            resizeBitmap(bitmap, 800, 800)
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)

        val fieldName = if (isBanner) "bannerURL" else "imageURL"

        db.collection("User").document(userId)
            .update(fieldName, base64String)
            .addOnSuccessListener {
                onSuccess(resizedBitmap)
                Toast.makeText(
                    context,
                    if (isBanner) "Banner diperbarui!" else "Foto profil diperbarui!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal update foto", Toast.LENGTH_SHORT).show()
            }
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
    }
}

private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    return if (maxWidth > maxHeight) {
        // Banner mode
        val targetAspect = maxWidth.toFloat() / maxHeight.toFloat()
        val currentAspect = width.toFloat() / height.toFloat()

        val (cropWidth, cropHeight) = if (currentAspect > targetAspect) {
            val calculatedWidth = (height * targetAspect).toInt()
            Pair(calculatedWidth, height)
        } else {
            val calculatedHeight = (width / targetAspect).toInt()
            Pair(width, calculatedHeight)
        }

        val xOffset = (width - cropWidth) / 2
        val yOffset = (height - cropHeight) / 2

        val croppedBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, cropWidth, cropHeight)
        Bitmap.createScaledBitmap(croppedBitmap, maxWidth, maxHeight, true)
    } else {
        // Profile picture mode
        val size = minOf(width, height)
        val xOffset = (width - size) / 2
        val yOffset = (height - size) / 2

        val squareBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size)
        Bitmap.createScaledBitmap(squareBitmap, maxWidth, maxHeight, true)
    }
}

private fun decodeBase64ToBitmap(base64: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}
