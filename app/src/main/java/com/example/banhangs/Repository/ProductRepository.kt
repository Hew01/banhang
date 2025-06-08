package com.example.banhangs.Repository

import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.banhangs.Model.* // Import your models including ProductDetailsModel
import com.example.banhangs.Network.ApiService // Your Retrofit ApiService interface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(private val apiService: ApiService) {

    /**
     * Fetches the detailed information for a specific product.
     * @param productId The ID of the product to fetch.
     */
    suspend fun getProductDetails(productId: String): Result<ProductDetailsModel> {
        return withContext(Dispatchers.IO) {
            try {
                // Assuming your ApiService has:
                // suspend fun getProductDetails(@Path("id") productId: String): Response<ProductDetailsApiResponse>
                // And ProductDetailsApiResponse is typealias ProductDetailsApiResponse = ApiResponse<ProductDetailData>
                // And ProductDetailData is the DTO that needs mapping to ProductDetailsModel (domain model)

                val response = apiService.getProductDetails(productId) // This should return Response<ApiResponse<ProductDetailData>>
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data != null) {
                        // Here, apiResponse.data is ProductDetailData (DTO)
                        // You need to map it to ProductDetailsModel (your domain/UI model)
                        // For simplicity, if ProductDetailData and ProductDetailsModel are identical in structure
                        // and ProductDetailsModel is what your API returns directly in `data`, this is fine.
                        // However, typically you'd have a DTO from the API and map it.
                        // Let's assume for now apiService.getProductDetails directly gives ProductDetailsModel
                        // or that ProductDetailData is directly usable as ProductDetailsModel.
                        // If they are different, you'd do:
                        // val productDto = apiResponse.data
                        // Result.success(productDto.toDomainModel()) // Assuming an extension function
                        Result.success(apiResponse.data) // If ProductDetailsModel is directly in ApiResponse.data
                    } else {
                        val errorMessage = "Failed to get product details: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    Result.failure(Exception("Failed to get product details: Network Error - Code: ${response.code()}, Message: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to get product details: Exception - ${e.message}", e))
            } as Result<ProductDetailsModel>
        }
    }

    /**
     * Fetches a list of products belonging to a specific category.
     * @param categoryId The ID of the category.
     */
    suspend fun getProductsByCategory(categoryId: String): Result<List<ProductDetailsModel>> {
        return withContext(Dispatchers.IO) {
            try {
                // Assuming ApiService has:
                // suspend fun getProductsByCategory(@Query("categoryId") categoryId: String): Response<ProductsByCategoryApiResponse>
                // And ProductsByCategoryApiResponse is typealias for ApiResponse<List<ProductSummaryData>>
                // And ProductSummaryData needs mapping to ProductDetailsModel or a similar summary domain model.

                val response = apiService.getProductsByCategory(categoryId) // This should return Response<ApiResponse<List<ProductSummaryData>>>
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data != null) {
                        // apiResponse.data is List<ProductSummaryData> (DTOs)
                        // You need to map this list to List<ProductDetailsModel> or List<ProductSummaryModel>
                        // val productSummariesDto = apiResponse.data
                        // Result.success(productSummariesDto.map { it.toDomainModel() })
                        Result.success(apiResponse.data) // If ProductDetailsModel is directly in ApiResponse.data
                    } else {
                        val errorMessage = "Failed to get products by category: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    Result.failure(Exception("Failed to get products by category: Network Error - Code: ${response.code()}, Message: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to get products by category: Exception - ${e.message}", e))
            } as Result<List<ProductDetailsModel>>
        }
    }

    /**
     * Searches for products based on a query string.
     * @param query The search term.
     */
    @OptIn(UnstableApi::class)
    suspend fun searchProducts(query: String): Result<List<ProductDetailsModel>> {
        return withContext(Dispatchers.IO) {
            try {
                // Assuming ApiService has:
                // suspend fun searchProducts(@Query("q") query: String): Response<SearchProductsApiResponse>
                // And SearchProductsApiResponse is typealias for ApiResponse<List<ProductSummaryData>>

                val response = apiService.searchProductsByName(query) // Pass the query to the correct parameter name

                if (response.isSuccessful) {
                    val listOfApiResponses: List<ProductDetailsApiResponse>? = response.body()

                    if (listOfApiResponses != null) {
                        val successfulProducts = mutableListOf<ProductDetailData>()
                        var firstErrorRetCode: Int? = null
                        var firstErrorMessage: String? = null
                        var allSuccessful = true

                        for (apiResponseItem in listOfApiResponses) {
                            // Now, apiResponseItem is ApiResponse<ProductDetailData>
                            // So, apiResponseItem.retCode and apiResponseItem.data are valid
                            if (apiResponseItem.retCode == 0 && apiResponseItem.data != null) {
                                successfulProducts.add(apiResponseItem.data)
                            } else {
                                allSuccessful = false
                                if (firstErrorRetCode == null) { // Capture the first error encountered
                                    firstErrorRetCode = apiResponseItem.retCode
                                    firstErrorMessage = apiResponseItem.systemMessage ?: "Search item failed with no message."
                                }
                                Log.e("ProductRepository", "Search item failed: RetCode=${apiResponseItem.retCode}, Message='${apiResponseItem.systemMessage}' for a product in search results.")
                                // Decide if you want to stop processing or collect all successful ones
                            }
                        }

                        if (successfulProducts.isNotEmpty()) {
                            // Map the successful ProductDetailData (DTOs) to ProductDetailsModel (Domain Models)
                            val domainModels = successfulProducts.map { productDetailData ->
                                // Assuming ProductDetailsModel is your domain model and you have a mapping
                                ProductDetailsModel(
                                    productId = productDetailData.productId,
                                    name = productDetailData.name,
                                    description = productDetailData.description
                                        ?: productDetailData.shortDescription ?: "",
                                    price = productDetailData.price,
                                    averageRating = productDetailData.averageRating ?: 0.0,
                                    ratedCount = productDetailData.ratedCount ?: 0,
                                    mainImageUrl = productDetailData.mainImageUrl,
                                    galleryImageUrls = productDetailData.galleryImageUrls
                                        ?: emptyList(),
                                    stock = productDetailData.stock ?: 0,
                                    categoryName = productDetailData.categoryName,
                                    brandName = productDetailData.brandName,
                                    salePrice = productDetailData.salePrice,
                                    isOnSale = productDetailData.isOnSale
                                        ?: (productDetailData.salePrice != null),
                                    shortDescription = null,
                                    soldCount = null,
                                    saleStart = null,
                                    saleEnd = null,
                                    categoryId = null,
                                    brandId = null,
                                    isFeatured = null
                                )
                            }
                            Result.success(domainModels)
                        } else if (!allSuccessful) {
                            // All items in the list failed or the list was empty but contained errors
                            val errorMessage = "Failed to search products: API Error on items - First Error RetCode: ${firstErrorRetCode}, Message: ${firstErrorMessage}"
                            Result.failure(Exception(errorMessage))
                        } else {
                            // List was empty and no errors (e.g., search returned no results but was successful)
                            Result.success(emptyList()) // No products found
                        }
                    } else {
                        // response.body() was null, which is unusual for a successful response but possible
                        Result.failure(Exception("Failed to search products: Empty response body."))
                    }
                } else {
                    Result.failure(Exception("Failed to search products: Network Error - Code: ${response.code()}, Message: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to search products: Exception - ${e.message}", e))
            }
        }
    }


    /**
     * Fetches comments for a specific product.
     * @param productId The ID of the product for which to fetch comments.
     */
    suspend fun getProductComments(productId: String): Result<List<ApiCommentModel>> {
        return withContext(Dispatchers.IO) {
            try {
                // Assuming ApiService has:
                // suspend fun getProductComments(@Path("productId") productId: String): Response<CommentsListApiResponse>
                // And CommentsListApiResponse is typealias for ApiResponse<List<ApiCommentModel>>
                val response = apiService.getProductComments(productId)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data != null) {
                        Result.success(apiResponse.data)
                    } else {
                        val errorMessage = "Failed to get comments: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    Result.failure(Exception("Failed to get comments: Network Error - Code: ${response.code()}, Message: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to get comments: Exception - ${e.message}", e))
            }
        }
    }

    /**
     * Posts a new comment for a specific product.
     * @param productId The ID of the product to comment on.
     * @param commentText The text of the comment.
     * @param rating Optional rating associated with the comment.
     */
    suspend fun postProductComment(productId: String, commentText: String, rating: Float?): Result<ApiCommentModel> {
        return withContext(Dispatchers.IO) {
            try {
                val request = PostCommentRequest(productId, commentText, rating)
                // Assuming ApiService has:
                // suspend fun postProductComment(@Path("productId") productId: String, @Body commentRequest: PostCommentRequest): Response<ApiResponse<ApiCommentModel>>
                val response = apiService.postProductComment(productId, request)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.retCode == 0 && apiResponse.data != null) {
                        Result.success(apiResponse.data) // Assuming API returns the posted comment
                    } else {val errorMessage = "Failed to post comment: API Error - RetCode: ${apiResponse?.retCode}, Message: ${apiResponse?.systemMessage ?: response.message()}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    Result.failure(Exception("Failed to post comment: Network Error - Code: ${response.code()}, Message: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to post comment: Exception - ${e.message}", e))
            }
        }
    }

    // --- Helper for DTO to Domain Model Mapping (Illustrative) ---
    // If your API returns DTOs (Data Transfer Objects) like ProductDetailData or ProductSummaryData
    // that are different from your domain/UI model (ProductDetailsModel), you'd add mapping functions.
    //
    // Example (if ProductDetailData is a DTO and ProductDetailsModel is your domain model):
    // private fun ProductDetailData.toDomainModel(): ProductDetailsModel {
    //     return ProductDetailsModel(
    //         productId = this.productId,
    //         name = this.name,
    //         description = this.description ?: "",
    //         price = this.price,
    //         averageRating = this.averageRating ?: 0.0,
    //         ratedCount = this.ratedCount ?: 0,
    //         mainImageUrl = this.mainImageUrl,
    //         galleryImageUrls = this.galleryImageUrls ?: emptyList(),
    //         stock = this.stock ?: 0,
    //         categoryName = this.categoryName,
    //         brandName = this.brandName
    //         // ... map other fields ...
    //     )
    // }
    //
    // Example (if ProductSummaryData is a DTO):
    // private fun ProductSummaryData.toDomainModel(): ProductDetailsModel { // Or a specific ProductSummaryDomainModel
    //     return ProductDetailsModel(
    //         productId = this.productId,
    //         name = this.name,
    //         description = "", // Summary might not have full description
    //         price = this.price,
    //         averageRating = this.averageRating ?: 0.0,
    //         ratedCount = this.ratedCount ?: 0,
    //         mainImageUrl = this.mainImageUrl,
    //         galleryImageUrls = emptyList(), // Summary might not have gallery
    //         stock = this.stock ?: 0,
    //         categoryName = this.categoryName
    //         // ... map other fields ...
    //     )
    // }
    //
    // You would then use these in your repository methods:
    // e.g., Result.success(apiResponse.data.toDomainModel())
    // or   Result.success(apiResponse.data.map { it.toDomainModel() })
}