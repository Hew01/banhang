package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels // For by viewModels()
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.banhangs.Adapter.CommentAdapter // You'll need to adapt this for ApiCommentModel
import com.example.banhangs.Adapter.PicAdapter
import com.example.banhangs.Adapter.SelectedModelAdapter
import com.example.banhangs.Helper.formatNumberToShortForm
// Import your repositories, ViewModel, ViewModelFactory, and new ApiCommentModel
import com.example.banhangs.R
import com.example.banhangs.Repository.CartRepository
import com.example.banhangs.Repository.ProductRepository
import com.example.banhangs.ViewModel.DetailViewModel
import com.example.banhangs.ViewModel.DetailViewModelFactory
import com.example.banhangs.databinding.ActivityDetailBinding
import com.example.banhangs.Model.ApiCommentModel // Use the new API comment model
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.Network.ApiService
import com.example.banhangs.Network.RetrofitClient
import com.example.banhangs.Repository.UserPreferencesRepository

private val TAG_ACTIVITY = "DetailActivity_Observe"

class DetailActivity : BaseActivity() { // Assuming BaseActivity handles common setup

    private lateinit var binding: ActivityDetailBinding
    private lateinit var currentItemId: String // Product ID, ensure this is correctly passed and retrieved
    private lateinit var userPreferencesRepository: UserPreferencesRepository


    // Lazily initialize repositories (or use Hilt/Koin for DI)
    private val apiService: ApiService by lazy { RetrofitClient.instance } // Example: Get ApiService instance
    private val cartRepository by lazy { CartRepository(apiService, userPreferencesRepository) }
    private val productRepository by lazy { ProductRepository(apiService) }

    private val viewModel: DetailViewModel by viewModels {
        if (!::currentItemId.isInitialized || currentItemId.isBlank()) {
            Log.e("DetailActivity", "ViewModelFactory: currentItemId not initialized!")
        DetailViewModelFactory(cartRepository, productRepository, "INVALID_ID_FALLBACK")
    } else {
        DetailViewModelFactory(cartRepository, productRepository, currentItemId)
    }}


    private lateinit var commentAdapter: CommentAdapter // Needs to be updated for ApiCommentModel
    private var numberOrder = 1 // For quantity to add to cart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPreferencesRepository = UserPreferencesRepository(applicationContext)

        val receivedItem = intent.getParcelableExtra<ProductDetailsModel>("object")
        val receivedItemIdString = intent.getStringExtra("itemKey")

        if (receivedItem == null || receivedItemIdString.isNullOrBlank()) {
            Log.e("DetailActivity", "Product object or itemKey is null or blank from intent.")
            handleInvalidState("Error: Product data missing from intent.")
            return // Exit early
        }

        currentItemId = receivedItemIdString
        Log.d("DetailActivity", "Received item: ${receivedItem.name}, itemId: $currentItemId")

        // Pass the initially loaded item to the ViewModel
        // This assumes 'item' is the full ProductDetailsModel.
        // If 'item' is a summary and 'itemId' is used to fetch full details,
        // then the ViewModel would handle fetching product details.
        viewModel.setInitialProductData(receivedItem)


        updateProductUI(receivedItem)
        setupEventHandlersAndAdapters()
        setupCommentRecyclerView()
        observeViewModel()

        // Load comments (ViewModel handles this now, triggered by init or setInitialProductDetails)
        // viewModel.loadComments() // Already called in ViewModel or triggered by product ID availability
    }

    private fun handleInvalidState(message: String) {
        Toast.makeText(this, "Error: Product data missing.", Toast.LENGTH_LONG).show()
        Log.e("DetailActivity", "Item or ItemId not properly initialized.")
        finish() // Exit if essential data is missing
    }


    override fun onResume() {
        super.onResume()
        // Potentially refresh comments if needed, though ViewModel should handle state
        // viewModel.loadComments()
    }

    private fun setupEventHandlersAndAdapters() {
        // --- Event Listeners ---
        binding.backBtn.setOnClickListener { finish() }
        binding.cartBtn.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        binding.addToCartBtn.setOnClickListener {
            viewModel.productDetails.value?.let { currentProduct ->
                viewModel.addToCart(currentProduct, numberOrder)
            } ?: run {
                // Fallback: If productDetails is null, try using the initially passed item
                // This situation should be rare if setInitialProductData and observers are working.
                val initialItemFromIntent = intent.getParcelableExtra<ProductDetailsModel>("object")
                initialItemFromIntent?.let {
                    Log.w("DetailActivity", "addToCart using initial item from intent as fallback.")
                    viewModel.addToCart(it, numberOrder)
                } ?: Toast.makeText(this, "Product details not available to add to cart.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.submitCommentBtn.setOnClickListener {
            val commentText = binding.commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                viewModel.postComment(commentText)
                binding.commentInput.text.clear()
            } else {
                Toast.makeText(this, "Please enter your comment.", Toast.LENGTH_SHORT).show()
            }
        }

        // Example: Quantity selector (if you add one)
        // binding.plusBtn.setOnClickListener { numberOrder++; binding.quantityTxt.text = numberOrder.toString() }
        // binding.minusBtn.setOnClickListener { if (numberOrder > 1) { numberOrder--; binding.quantityTxt.text = numberOrder.toString() } }

        // Initialize PicAdapter for the product image gallery (if it's static or updated in updateProductUI)
        // If picList is dynamic based on productDetails, its adapter setup/update should be in updateProductUI
        binding.picList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // The adapter for picList will be set in updateProductUI since picList data comes from ProductDetailsModel
    }

    private fun updateProductUI(product: ProductDetailsModel?) {
        product?.let { currentProduct ->
            Log.i("DetailActivity", "updateProductUI called with product: ${currentProduct.name}")
            binding.titleTxt.text = currentProduct.name
            binding.derscriptionTxt.text = currentProduct.description ?: "No description available."
            binding.priceTxt.text = "$${formatNumberToShortForm(currentProduct.price)}" // Format as needed
            binding.raitingTxt.text = "${currentProduct.averageRating ?: 0.0} Rating"

            if (!currentProduct.categoryName.isNullOrBlank()) {
                binding.categoryNameTxt.text = currentProduct.categoryName
                binding.categoryNameTxt.visibility = View.VISIBLE
            } else {
                binding.categoryNameTxt.text = "N/A"
                // Consider hiding if N/A: binding.categoryNameTxt.visibility = View.GONE
            }

            val picList = ArrayList<String>()
            currentProduct.galleryImageUrls?.let { urls -> picList.addAll(urls) }

            if (picList.isNotEmpty()) {
                Glide.with(this).load(picList[0])
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error_placeholder)
                    .into(binding.img)
                // Set or update the adapter for the picture list
                binding.picList.adapter = PicAdapter(picList) { selectedImageUrl ->
                    Glide.with(this).load(selectedImageUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_error_placeholder)
                        .into(binding.img)
                }
                binding.picList.visibility = View.VISIBLE
            } else if (!currentProduct.mainImageUrl.isNullOrEmpty()) {
                Glide.with(this).load(currentProduct.mainImageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error_placeholder)
                    .into(binding.img)
                binding.picList.visibility = View.GONE // Hide picList if only main image
            } else {
                binding.img.setImageResource(R.drawable.ic_placeholder)
                binding.picList.visibility = View.GONE
            }
            // Ensure description visibility is also handled
            binding.derscriptionTxt.visibility = if (currentProduct.description.isNullOrBlank()) View.GONE else View.VISIBLE

        } ?: run {
            // Handle case where product is null (e.g., after an error or if initial data was null)
            Log.w("DetailActivity", "updateProductUI called with null product. Clearing UI fields.")
            binding.titleTxt.text = "Product Not Available"
            binding.derscriptionTxt.text = ""
            binding.priceTxt.text = ""
            binding.raitingTxt.text = ""
            binding.categoryNameTxt.text = ""
            binding.img.setImageResource(R.drawable.ic_error_placeholder) // Show an error placeholder
            binding.picList.adapter = null // Clear the adapter
            binding.picList.visibility = View.GONE
            binding.derscriptionTxt.visibility = View.GONE
            binding.categoryNameTxt.visibility = View.GONE
            // You might want to show a specific error message to the user here
        }
    }

    private fun setupCommentRecyclerView() {
        commentAdapter = CommentAdapter(ArrayList()) // Initialize with an empty list
        binding.modelComment.apply {
            layoutManager = LinearLayoutManager(this@DetailActivity)
            adapter = commentAdapter
            // isNestedScrollingEnabled = false // Consider if inside a ScrollView and needs to expand
        }
    }

    private fun observeViewModel() {
        Log.d(TAG_ACTIVITY, "Setting up ViewModel observers.")
        viewModel.productDetails.observe(this) { product ->
            if (product != null) {
                Log.i(TAG_ACTIVITY, "productDetails LiveData observed. Product Name: ${product.name}. Calling updateProductUI.")
                updateProductUI(product) // <<<<<<----- THIS IS THE CRUCIAL FIX
            } else {
                Log.w(TAG_ACTIVITY, "productDetails LiveData observed with a null product. UI will be updated to reflect this.")
                updateProductUI(null) // Update UI to show "not available" or clear fields
            }
        }

        viewModel.comments.observe(this) { comments ->
            Log.d("DetailActivity", "Observed comments: ${comments.size}")
            commentAdapter.updateComments(comments)
            if (comments.isEmpty()) {
                binding.modelComment.visibility = View.GONE // Hide RecyclerView
                binding.noCommentsTextView.visibility = View.VISIBLE // Show "No comments" text
            } else {
                binding.modelComment.visibility = View.VISIBLE // Show RecyclerView
                binding.noCommentsTextView.visibility = View.GONE // Hide "No comments" text
            }
        }

        viewModel.isLoadingComments.observe(this) { isLoadingComments ->
            // Handle loading state for comments
            // Example: binding.commentsProgressBar.visibility = if (isLoadingComments) View.VISIBLE else View.GONE
            val isProductLoading = viewModel.isLoadingProduct.value ?: false
            binding.addToCartBtn.isEnabled = !isLoadingComments && !isProductLoading
            binding.submitCommentBtn.isEnabled = !isLoadingComments && !isProductLoading
        }


        viewModel.isLoadingProduct.observe(this) { isLoadingProduct ->
            // You might want to disable buttons during loading
            val isCommentsLoading = viewModel.isLoadingComments.value ?: false
            binding.addToCartBtn.isEnabled = !isLoadingProduct && !isCommentsLoading
            binding.submitCommentBtn.isEnabled = !isLoadingProduct && !isCommentsLoading
        }

        viewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                Log.e("DetailActivity", "Error: $it")
                viewModel.onErrorShown() // Reset error message after showing
            }
        }

        viewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.onToastShown() // Reset toast message
            }
        }

        viewModel.addToCartSuccess.observe(this) { success ->
            // You can add further actions here if needed, e.g., navigate to cart
            // For now, the toast message from the ViewModel handles user feedback.
            if (success) {
                Log.d("DetailActivity", "Add to cart successful observer triggered.")
                // Optionally, navigate or update UI further
            }
        }
    }
}