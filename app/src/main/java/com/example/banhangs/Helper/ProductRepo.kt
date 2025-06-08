package com.example.banhangs.Helper

import com.example.banhangs.Model.ApiCommentModel
import com.example.banhangs.Model.PostCommentRequest
import com.example.banhangs.Network.ApiService
import com.example.banhangs.model.*
import com.example.banhangs.network.ApiService

class ProductRepository(private val apiService: ApiService) {

    // ... other product-related functions (getProductDetails, etc.)

    suspend fun getProductComments(productId: String): Result<List<ApiCommentModel>> {
        return try {
            val response = apiService.getProductComments(productId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to get comments: ${response.message()} - Code: ${response.code()} - RetCode: ${response.body()?.retCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postProductComment(productId: String, commentText: String, rating: Float?): Result<ApiCommentModel> {
        return try {
            val request = PostCommentRequest(productId, commentText, rating)
            val response = apiService.postProductComment(productId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception("Failed to post comment: ${response.message()} - Code: ${response.code()} - RetCode: ${response.body()?.retCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}