package com.example.banhangs.Helper

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CloudinaryHelper {
    private val cloudinary: Cloudinary

    init {
        cloudinary = Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dyalhwoyy",
            "api_key", "687572861125667",
            "api_secret", "IJR4Sr5uzay35VcPUKrW6z7hmhM"
        ))
    }

    // Hàm upload hình ảnh
    suspend fun uploadImage(imageFile: File): String? {
        return withContext(Dispatchers.IO) {
            try {
                val uploadResult = cloudinary.uploader().upload(imageFile, ObjectUtils.emptyMap())
                return@withContext uploadResult["secure_url"].toString()
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    // Hàm upload video
    suspend fun uploadVideo(videoFile: File): String? {
        return withContext(Dispatchers.IO) {
            try {
                val uploadResult = cloudinary.uploader().upload(videoFile, ObjectUtils.asMap("resource_type", "video"))
                return@withContext uploadResult["secure_url"].toString()
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }
}
