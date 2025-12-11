package com.example.projectmap2.ui.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmap2.ui.models.ImageUploadState
import com.example.projectmap2.ui.models.ProfileData
import com.example.projectmap2.ui.models.ProfileState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _imageUploadState = MutableStateFlow<ImageUploadState>(ImageUploadState.Idle)
    val imageUploadState: StateFlow<ImageUploadState> = _imageUploadState.asStateFlow()

    private val _userName = MutableStateFlow("Loading...")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("Loading...")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userDob = MutableStateFlow("Belum diisi")
    val userDob: StateFlow<String> = _userDob.asStateFlow()

    private val _showEditDob = MutableStateFlow(true)
    val showEditDob: StateFlow<Boolean> = _showEditDob.asStateFlow()

    private val _profileBitmap = MutableStateFlow<Bitmap?>(null)
    val profileBitmap: StateFlow<Bitmap?> = _profileBitmap.asStateFlow()

    private val _bannerBitmap = MutableStateFlow<Bitmap?>(null)
    val bannerBitmap: StateFlow<Bitmap?> = _bannerBitmap.asStateFlow()

    fun loadUserData(userId: String) {
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading

                val doc = db.collection("User").document(userId).get().await()
                if (doc.exists()) {
                    val fName = doc.getString("FName") ?: ""
                    val lName = doc.getString("LName") ?: ""
                    _userName.value = "$fName $lName"
                    _userEmail.value = doc.getString("Email") ?: ""

                    // Handle DOB
                    val dob = doc.get("DOB")
                    when {
                        dob == null -> {
                            _userDob.value = "Belum diisi"
                            _showEditDob.value = true
                        }
                        dob is String -> {
                            if (dob.isEmpty()) {
                                _userDob.value = "Belum diisi"
                                _showEditDob.value = true
                            } else {
                                _userDob.value = dob
                                _showEditDob.value = false
                            }
                        }
                        dob is com.google.firebase.Timestamp -> {
                            val date = dob.toDate()
                            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                            _userDob.value = sdf.format(date)
                            _showEditDob.value = false
                        }
                        else -> {
                            _userDob.value = dob.toString()
                            _showEditDob.value = false
                        }
                    }

                    // Load profile image
                    val imageBase64 = doc.getString("imageURL")
                    if (!imageBase64.isNullOrEmpty()) {
                        _profileBitmap.value = decodeBase64ToBitmap(imageBase64)
                    }

                    // Load banner image
                    val bannerBase64 = doc.getString("bannerURL")
                    if (!bannerBase64.isNullOrEmpty()) {
                        _bannerBitmap.value = decodeBase64ToBitmap(bannerBase64)
                    }

                    _profileState.value = ProfileState.Success(
                        ProfileData(
                            userName = _userName.value,
                            userEmail = _userEmail.value,
                            userDob = _userDob.value,
                            showEditDob = _showEditDob.value,
                            profileBitmap = _profileBitmap.value,
                            bannerBitmap = _bannerBitmap.value
                        )
                    )
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun uploadImage(
        context: Context,
        userId: String,
        uri: Uri,
        isBanner: Boolean
    ) {
        viewModelScope.launch {
            try {
                _imageUploadState.value = ImageUploadState.Uploading

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
                    .await()

                if (isBanner) {
                    _bannerBitmap.value = resizedBitmap
                } else {
                    _profileBitmap.value = resizedBitmap
                }

                _imageUploadState.value = ImageUploadState.Success(resizedBitmap)
            } catch (e: Exception) {
                _imageUploadState.value = ImageUploadState.Error(e.message ?: "Gagal upload gambar")
            }
        }
    }

    fun updateDob(userId: String, day: String, month: String, year: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val formattedDate = "$day $month $year"
                db.collection("User").document(userId)
                    .update("DOB", formattedDate)
                    .await()

                _userDob.value = formattedDate
                _showEditDob.value = false
                onSuccess()
            } catch (e: Exception) {
                onError()
            }
        }
    }

    fun resetImageUploadState() {
        _imageUploadState.value = ImageUploadState.Idle
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
}