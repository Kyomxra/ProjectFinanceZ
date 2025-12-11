package com.example.projectmap2.ui.models

import android.graphics.Bitmap

data class ProfileData(
    val userName: String = "Loading...",
    val userEmail: String = "Loading...",
    val userDob: String = "Belum diisi",
    val showEditDob: Boolean = true,
    val profileBitmap: Bitmap? = null,
    val bannerBitmap: Bitmap? = null
)

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val data: ProfileData) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class ImageUploadState {
    object Idle : ImageUploadState()
    object Uploading : ImageUploadState()
    data class Success(val bitmap: Bitmap) : ImageUploadState()
    data class Error(val message: String) : ImageUploadState()
}