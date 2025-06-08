package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels // For by viewModels()
import androidx.compose.ui.semantics.text
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.banhangs.Adapter.CommentAdapter // You'll need to adapt this for ApiCommentModel
import com.example.banhangs.Adapter.PicAdapter
import com.example.banhangs.Adapter.SelectedModelAdapter
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

class DetailActivity : BaseActivity() { // Assuming BaseActivity handles common setup

    private lateinit var binding: ActivityDetailBinding
    private lateinit var item: ProductDetailsModel // This is the product being displayed
    private lateinit var itemId: String // Product ID, ensure this is correctly passed and retrieved

    // Lazily initialize repositories (or use Hilt/Koin for DI)
    private val apiService: ApiService by lazy { RetrofitClient.instance } // Example: Get ApiService instance
    private val cartRepository by lazy { CartRepository(apiService) }
    private val productRepository by lazy { ProductRepository(apiService) }

    private val viewModel: DetailViewModel by viewModels {
        DetailViewModelFactory(cartRepository, productRepository, itemId)
    }

    private lateinit var commentAdapter: CommentAdapter // Needs to be updated for ApiCommentModel
    private var numberOrder = 1 // For quantity to add to cart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!::item.isInitialized || itemId.isBlank()) { // Basic check
            handleInvalidState()
            return
        }

        // Pass the initially loaded item to the ViewModel
        // This assumes 'item' is the full ProductDetailsModel.
        // If 'item' is a summary and 'itemId' is used to fetch full details,
        // then the ViewModel would handle fetching product details.
        viewModel.setInitialProductDetails(item)


        setupUI()
        setupCommentRecyclerView()
        observeViewModel()

        // Load comments (ViewModel handles this now, triggered by init or setInitialProductDetails)
        // viewModel.loadComments() // Already called in ViewModel or triggered by product ID availability
    }

    private fun handleInvalidState() {
        Toast.makeText(this, "Error: Product data missing.", Toast.LENGTH_LONG).show()
        Log.e("DetailActivity", "Item or ItemId not properly initialized.")
        finish() // Exit if essential data is missing
    }


    override fun onResume() {
        super.onResume()
        // Potentially refresh comments if needed, though ViewModel should handle state
        // viewModel.loadComments()
    }

    private fun setupUI() {
        // Get product details from intent (as before)
        // Ensure 'object' key matches what you use in Intent
        val receivedItem = intent.getParcelableExtra<ProductDetailsModel>("object")
        val receivedItemId = intent.getStringExtra("itemKey")

        if (receivedItem == null || receivedItemId == null) {
            Log.e("DetailActivity", "Product object or itemKey is null from intent.")
            handleInvalidState()
            return
        }
        item = receivedItem
        itemId = receivedItemId
        Log.d("DetailActivity", "itemId from intent: $itemId")


        binding.titleTxt.text = item.name // Assuming 'name' from ProductDetailsModel
        binding.derscriptionTxt.text = item.description
        binding.priceTxt.text = "$${item.price}"
        binding.raitingTxt.text = "${item.averageRating ?: 0.0} Rating" // Use averageRating

        // Image loading (Picasso/Glide for main image and picList)
        val picList = ArrayList<String>().apply { item.galleryImageUrls?.let { addAll(it) } }
        if (picList.isNotEmpty()) {
            Glide.with(this).load(picList[0]).into(binding.img)
        } else if (!item.mainImageUrl.isNullOrEmpty()) {
            Glide.with(this).load(item.mainImageUrl).into(binding.img)
        } else {
            binding.img.setImageResource(R.drawable.ic_placeholder) // Set a placeholder
        }


        binding.picList.adapter = PicAdapter(picList) { selectedImageUrl ->
            Glide.with(this).load(selectedImageUrl).into(binding.img)
        }
        binding.picList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Model list (if this is still relevant, e.g., product variants)
        // val modelList = ArrayList<String>().apply { addAll(item.model) } // Assuming 'model' is a property
        // binding.modelList.adapter = SelectedModelAdapter(modelList)
        // binding.modelList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        // If 'modelList' is not part of ProductDetailsModel, remove or adapt this.
        // For now, I'll hide it if 'model' isn't a direct property of ProductDetailsModel
        // binding.modelList.visibility = View.GONE // Or handle appropriately


        // --- Event Listeners ---
        binding.addToCartBtn.setOnClickListener {
            // numberOrder should ideally be from a quantity selector UI element
            // For now, using the class variable 'numberOrder'
            viewModel.addToCart(item, numberOrder)
        }

        binding.submitCommentBtn.setOnClickListener {
            val commentText = binding.commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                viewModel.postComment(commentText) // Rating is optional in ViewModel
                binding.commentInput.text.clear() // Clear input after attempting to post
            } else {
                Toast.makeText(this, "Please enter your comment.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backBtn.setOnClickListener { finish() }
        binding.cartBtn.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

// Example: Quantity selector (if you add one)
// binding.plusBtn.setOnClickListener { numberOrder++; binding.quantityTxt.text = numberOrder.toString() }
// binding.minusBtn.setOnClickListener { if (numberOrder > 1) { numberOrder--; binding.quantityTxt.text = numberOrder.toString() } }
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
        viewModel.productDetails.observe(this) { product ->
            // This is useful if ViewModel fetches/updates product details.
            // For now, we set it initially. If it changes, update UI here.
            // e.g., if stock changes after an attempted cart add.
            product?.let {
                // Update UI elements if they can change dynamically based on ViewModel
                // binding.titleTxt.text = it.name
                // binding.priceTxt.text = "$${it.price}"
                // ...
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

        viewModel.isLoading.observe(this) { isLoading ->
            // You might want to disable buttons during loading
            binding.addToCartBtn.isEnabled = !isLoading
            binding.submitCommentBtn.isEnabled = !isLoading
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