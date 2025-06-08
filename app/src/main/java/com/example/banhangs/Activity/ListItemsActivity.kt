package com.example.banhangs.Activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.compose.ui.semantics.text
import androidx.recyclerview.widget.GridLayoutManager
import com.example.banhangs.Adapter.ListItemsAdapter
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.ViewModel.MainViewModel
import com.example.banhangs.databinding.ActivityListItemsBinding

class ListItemsActivity : BaseActivity() {
    private lateinit var binding: ActivityListItemsBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var listItemsAdapter: ListItemsAdapter

    private var mode: ActivityMode = ActivityMode.NONE
    private var categoryId: String = ""
    private var categoryTitle: String = ""
    private var searchQuery: String = ""

    private enum class ActivityMode {
        CATEGORY, SEARCH, NONE // Could add RECOMMENDATIONS if that's a mode
    }

    private val TAG = "ListItemsActivity"

    companion object {
        const val EXTRA_CATEGORY_ID = "CATEGORY_ID"
        const val EXTRA_CATEGORY_TITLE = "CATEGORY_TITLE"
        const val EXTRA_SEARCH_QUERY = "SEARCH_QUERY"
        private const val TAG = "ListItemsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentData()
        setupUI()
        setupRecyclerView()
        observeViewModel()
        loadDataBasedOnMode()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBarList.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                // Show error message (e.g., in a Toast or a TextView)
                // For now, we'll update the categoryTxt, but a dedicated error view is better
                showError(it)
                binding.viewList.visibility = View.GONE // Hide list on error
            }
        }

        // Observe items by category
        viewModel.itemsByCategoryId.observe(this) { items ->
            if (mode == ActivityMode.CATEGORY) {
                Log.d(TAG, "Observed items for category '$categoryId': size=${items?.size ?: 0}")
                if (items != null) {
                    updateAdapter(items)
                    if (items.isEmpty()) {
                        binding.categoryTxt.text =
                            "Không có sản phẩm trong mục: ${categoryTitle}"
                        binding.viewList.visibility = View.GONE

                    } else {
                        binding.categoryTxt.text =
                            categoryTitle // Reset title if items found
                        binding.viewList.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Observe searched items
        viewModel.searchedItems.observe(this) { items ->
            if (mode == ActivityMode.SEARCH) {
                Log.d(TAG, "Observed search results for '$searchQuery': size=${items?.size ?: 0}")
                if (items != null) {
                    updateAdapter(items)
                    if (items.isEmpty()) {
                        binding.categoryTxt.text = "Không tìm thấy sản phẩm cho: \"$searchQuery\""
                        binding.viewList.visibility = View.GONE
                    } else {
                        binding.categoryTxt.text = "Kết quả tìm kiếm: \"$searchQuery\""
                        binding.viewList.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
            private fun getIntentData() {
                intent.getStringExtra(EXTRA_CATEGORY_ID)?.let {
                    if (it.isNotEmpty()) {
                        categoryId = it
                        mode = ActivityMode.CATEGORY
                    }
                }
                intent.getStringExtra(EXTRA_CATEGORY_TITLE)?.let { categoryTitle = it }
                intent.getStringExtra(EXTRA_SEARCH_QUERY)?.let {
                    if (it.isNotEmpty()) {
                        searchQuery = it
                        // If both categoryId and searchQuery are passed, decide priority.
                        // Here, search query might override category.
                        mode = ActivityMode.SEARCH
                    }
                }

                // Set initial title based on mode
                when (mode) {
                    ActivityMode.CATEGORY -> binding.categoryTxt.text = categoryTitle
                    ActivityMode.SEARCH -> binding.categoryTxt.text =
                        "Đang tìm kiếm \"$searchQuery\"..."

                    ActivityMode.NONE -> binding.categoryTxt.text =
                        "Sản phẩm" // Or an error/default state
                }
                Log.d(TAG, "Mode: $mode, CategoryId: $categoryId, SearchQuery: $searchQuery")
            }

            private fun setupUI() {
                binding.backBtn.setOnClickListener {
                    finish()
                }
                binding.viewList.layoutManager = GridLayoutManager(this, 2)
            }

            private fun loadData() {
                binding.progressBarList.visibility = View.VISIBLE
                // binding.emptyListTxt.visibility = View.GONE // REMOVED: emptyListTxt does not exist

                if (searchQuery.isNotEmpty()) {
                    val searchResults =
                        intent.getParcelableArrayListExtra<ProductDetailsModel>("searchResults")
                    Log.d(
                        TAG,
                        "Received searchResults for query '$searchQuery': size=${searchResults?.size ?: 0}"
                    )

                    if (!searchResults.isNullOrEmpty()) {
                        updateAdapter(searchResults.toMutableList())
                        binding.categoryTxt.text = "Kết quả tìm kiếm: \"$searchQuery\""
                    } else {
                        binding.categoryTxt.text = "Không tìm thấy sản phẩm cho: \"$searchQuery\""
                        // binding.emptyListTxt.visibility = View.VISIBLE // REMOVED
                        updateAdapter(mutableListOf())
                    }
                    binding.progressBarList.visibility = View.GONE
                } else if (categoryId.isNotEmpty()) {
                    viewModel.loadItemsByCategoryId(categoryId)
                } else {
                    Log.w(TAG, "No category ID or search query provided.")
                    binding.categoryTxt.text = "Không có thông tin để tải sản phẩm"
                    // binding.emptyListTxt.visibility = View.VISIBLE // REMOVED
                    binding.progressBarList.visibility = View.GONE
                    updateAdapter(mutableListOf())
                }
            }

            private fun setupRecyclerView() {
                listItemsAdapter = ListItemsAdapter(mutableListOf())
                binding.viewList.apply {
                    layoutManager = GridLayoutManager(this@ListItemsActivity, 2)
                    adapter = listItemsAdapter
                }
            }

            private fun loadDataBasedOnMode() {
                when (mode) {
                    ActivityMode.CATEGORY -> {
                        categoryId.let {
                            Log.d(TAG, "Loading items for category ID: $it")
                            viewModel.loadItemsByCategoryId(it)
                        }
                    }

                    ActivityMode.SEARCH -> {
                        searchQuery.let {
                            Log.d(TAG, "Searching for query: $it")
                            viewModel.searchProductsByName(it)
                        }
                    }

                    ActivityMode.NONE -> {
                        Log.w(TAG, "No valid mode (Category or Search) to load data.")
                        // Optionally, load recommended items or show a specific message
                        // viewModel.loadRecommendedItems() // If this is a desired default
                        showError("Không có thông tin để tải sản phẩm.")
                        updateAdapter(emptyList()) // Ensure list is cleared
                    }
                }
            }


// If you have a separate LiveData for recommended items and a mode for it:
// viewModel.recommendedItems.observe(this) { items ->
//     if (mode == ActivityMode.RECOMMENDATIONS) { // Assuming you add this mode
//         Log.d(TAG, "Observed recommended items: size=${items?.size ?: 0}")
//         if (items != null) {
//             updateAdapter(items)
//             if (items.isEmpty()) {
//                 binding.categoryTxt.text = "Không có sản phẩm gợi ý"
//                 binding.viewList.visibility = View.GONE
//                 binding.emptyListTxt.visibility = View.VISIBLE
//                 binding.emptyListTxt.text = "Không có sản phẩm gợi ý"
//             } else {
//                 binding.categoryTxt.text = "Sản phẩm gợi ý"
//                 binding.viewList.visibility = View.VISIBLE
//                 binding.emptyListTxt.visibility = View.GONE
//             }
//         }
//     }
// }


            private fun updateAdapter(items: List<ProductDetailsModel>) {
                listItemsAdapter.updateData(items.toMutableList())
                // The visibility of the list and empty text is handled in the observers
            }

            private fun showError(message: String) {
                // A more robust error display would be a dedicated TextView or a Snackbar/Toast
                binding.categoryTxt.text = message // Temporarily using categoryTxt for errors
                binding.progressBarList.visibility = View.GONE // Hide progress bar on error
                binding.viewList.visibility = View.GONE // Hide list on error
                Log.e(TAG, "Error displayed: $message")
            }
        }