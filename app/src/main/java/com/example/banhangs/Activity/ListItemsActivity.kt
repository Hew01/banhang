package com.example.banhangs.Activity

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.banhangs.Adapter.ListItemsAdapter
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.ViewModel.MainViewModel
import com.example.banhangs.databinding.ActivityListItemsBinding

class ListItemsActivity : BaseActivity() {
    private lateinit var binding: ActivityListItemsBinding
    private val viewModel: MainViewModel by viewModels()

    private var categoryId: String = ""
    private var categoryTitle: String = ""
    private var searchQuery: String = ""

    private val TAG = "ListItemsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getIntentData()
        setupUI()
        observeViewModel()
        loadData()
    }

    private fun getIntentData() {
        categoryId = intent.getStringExtra("id") ?: ""
        categoryTitle = intent.getStringExtra("title") ?: ""
        searchQuery = intent.getStringExtra("searchQuery") ?: ""

        if (searchQuery.isEmpty() && categoryTitle.isNotEmpty()) {
            binding.categoryTxt.text = categoryTitle
        } else if (searchQuery.isNotEmpty()) {
            binding.categoryTxt.text = "Đang tìm kiếm..."
        } else {
            binding.categoryTxt.text = "Sản phẩm"
        }
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
            val searchResults = intent.getParcelableArrayListExtra<ProductDetailsModel>("searchResults")
            Log.d(TAG, "Received searchResults for query '$searchQuery': size=${searchResults?.size ?: 0}")

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

    private fun observeViewModel() {
        viewModel.recommended.observe(this) { items ->
            if (searchQuery.isEmpty()) { // Only update if not in search mode from intent
                Log.d(TAG, "Observed items for category '$categoryId': size=${items.size}")
                if (items.isNotEmpty()) {
                    updateAdapter(items.toMutableList())
                    // binding.emptyListTxt.visibility = View.GONE // REMOVED
                } else {
                    // If items are empty, categoryTxt will show the category title or "No products..."
                    // No separate empty text view to manage.
                    // You might want to update categoryTxt here if items are empty for a category.
                    binding.categoryTxt.text = "Không có sản phẩm trong mục: $categoryTitle"
                    updateAdapter(mutableListOf())
                    // binding.emptyListTxt.visibility = View.VISIBLE // REMOVED
                }
                binding.progressBarList.visibility = View.GONE
            }
        }
    }

    private fun updateAdapter(items: MutableList<ProductDetailsModel>) {
        // Ensure adapter is not null and is of the correct type before casting
        if (binding.viewList.adapter == null) {
            binding.viewList.adapter = ListItemsAdapter(items)
        } else if (binding.viewList.adapter is ListItemsAdapter) {
            (binding.viewList.adapter as ListItemsAdapter).updateData(items)
        } else {
            // If adapter exists but is of a different type (should not happen in this context)
            // Re-create it.
            binding.viewList.adapter = ListItemsAdapter(items)
        }

        // If the list is empty, the RecyclerView will simply show nothing.
        // The categoryTxt will display the relevant message ("No products found...", etc.)
    }
}