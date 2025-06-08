package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels // For by viewModels()
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.example.banhangs.Adapter.CategoryAdapter
import com.example.banhangs.Adapter.RecommendedAdapter
import com.example.banhangs.Adapter.SliderAdapter
import androidx.appcompat.widget.SearchView
import com.example.banhangs.Model.CategoryModel // Your CategoryModel
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.Model.SliderModel
import com.example.banhangs.R // Assuming your R file is here
import com.example.banhangs.ViewModel.MainViewModel
import com.example.banhangs.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var tinyDB: TinyDB
    private val viewModel: MainViewModel by viewModels() // Modern ViewModel instantiation
    private lateinit var auth: FirebaseAuth
    private val TAG = "MainActivity"

    private lateinit var recommendedAdapter: RecommendedAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var sliderAdapter: SliderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tinyDB = TinyDB(this)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null || !auth.currentUser!!.isEmailVerified) {
            navigateToLogin()
            return
        }

        setupViews()
        observeViewModel()

        loadInitialData()
        loadProfileName() // Load profile name after ensuring user is logged in
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile name in case it changed in ProfileActivity
        if (auth.currentUser != null && auth.currentUser!!.isEmailVerified) {
            loadProfileName()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupViews() {
        // Recommended Adapter
        recommendedAdapter = RecommendedAdapter(mutableListOf()) // Initialize with empty list
        binding.viewRecommendation.layoutManager = GridLayoutManager(this, 2)
        binding.viewRecommendation.adapter = recommendedAdapter

        // Category Adapter
        categoryAdapter = CategoryAdapter(mutableListOf()) // Initialize with empty list
        binding.viewCategory.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.viewCategory.adapter = categoryAdapter

        // Slider Adapter
        sliderAdapter = SliderAdapter(mutableListOf(), binding.viewPager2)
        binding.viewPager2.adapter = sliderAdapter
        binding.viewPager2.clipToPadding = false
        binding.viewPager2.clipChildren = false
        binding.viewPager2.offscreenPageLimit = 3
        binding.viewPager2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        val compositePageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(40))
            // You can add other transformers like ScaleInTransformer if needed
            // addTransformer(ViewPager2.PageTransformer { page, position ->
            //     val r = 1 - Math.abs(position)
            //     page.scaleY = 0.85f + r * 0.15f
            // })
        }
        binding.viewPager2.setPageTransformer(compositePageTransformer)

        initBottomMenu()
        initSearch()
    }

    private fun loadInitialData() {
        viewModel.loadBanners()
        viewModel.loadCategories()
        viewModel.loadRecommendedItems() // Use the correct method from ViewModel
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this, Observer { isLoading ->
            // Show a general loading indicator if needed, or handle per section
            binding.progressBarSlider.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.progressBarCategory.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.progressBarRecommend.visibility = if (isLoading) View.VISIBLE else View.GONE
            // More granular loading indicators can be tied to specific LiveData if desired
        })

        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                Log.e(TAG, "ViewModel Error: $it")
            }
        })

        viewModel.banners.observe(this, Observer { banners ->
            binding.progressBarSlider.visibility = View.GONE
            if (banners.isNullOrEmpty()) {
                Log.w(TAG, "Banners are empty or null")
                // Optionally show a placeholder or hide the ViewPager
                binding.dotIncator.visibility = View.GONE
            } else {
                Log.d(TAG, "Updating banners: ${banners.size}")
                sliderAdapter.updateData(banners.toMutableList()) // Assuming SliderAdapter has updateData
                if (banners.size > 1) {
                    binding.dotIncator.visibility = View.VISIBLE
                    binding.dotIncator.attachTo(binding.viewPager2)
                } else {
                    binding.dotIncator.visibility = View.GONE
                }
            }
        })

        viewModel.categories.observe(this, Observer { categories ->
            binding.progressBarCategory.visibility = View.GONE
            if (categories.isNullOrEmpty()) {
                Log.w(TAG, "Categories are empty or null")
                Toast.makeText(this, "No categories available", Toast.LENGTH_SHORT).show()
                categoryAdapter.updateData(emptyList()) // Update with empty list
            } else {
                Log.d(TAG, "Updating categories: ${categories.size}")
                // Ensure your CategoryAdapter can handle CategoryModel from your API
                categoryAdapter.updateData(categories.toMutableList())
            }
        })

        // Observe recommendedItems (which should be List<ProductDetailsModel>)
        viewModel.recommendedItems.observe(this, Observer { items ->
            binding.progressBarRecommend.visibility = View.GONE
            if (items.isNullOrEmpty()) {
                Log.w(TAG, "Recommended items are empty or null")
                Toast.makeText(this, "No recommended items available", Toast.LENGTH_SHORT).show()
                recommendedAdapter.updateData(emptyList()) // Update with empty list
            } else {
                Log.d(TAG, "Updating recommended items: ${items.size}")
                recommendedAdapter.updateData(items.toMutableList())
            }
        })

        // Observer for search results
        viewModel.searchedItems.observe(this, Observer { searchResults ->
            // This observer is primarily for when the search results are ready
            // The actual navigation/display logic is handled in initSearch after viewModel.searchProductsByName is called
            if (viewModel.isLoading.value == false)
                if (viewModel.isLoading.value == false && binding.searchView.visibility == View.VISIBLE) { // Check if search was active
                    // The actual navigation to ListItemsActivity is now handled within the search button's OnClickListener
                    // This observer is more for reacting to search data changes if MainActivity itself were to display them.
                    // For now, we'll just log.
                    if (searchResults != null) {
                        Log.d(TAG, "Searched items updated, count: ${searchResults.size}")
                    }
                    // If you wanted to update something on MainActivity directly based on search results, do it here.
                }
        })
    }

    private fun initSearch() {
        binding.btnSearch.setOnClickListener {
            binding.searchView.setQuery("", false) // Clear previous query
            binding.btnSearch.visibility = View.GONE
            binding.searchView.visibility = View.VISIBLE
            binding.btnSearchSubmit.visibility = View.VISIBLE
            binding.searchView.requestFocus()
            // Consider showing keyboard:
            // val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // imm.showSoftInput(binding.searchView, InputMethodManager.SHOW_IMPLICIT)
        }

        // Handle search submission from keyboard (optional but good UX)
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener { // Explicit type
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.trim().isNotEmpty()) {
                        performSearch(it.trim())
                    } else {
                        Toast.makeText(this@MainActivity, "Vui lòng nhập tên sản phẩm", Toast.LENGTH_SHORT).show()
                    }
                }
                binding.searchView.clearFocus() // Hide keyboard
                return true // Indicate the action was handled
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // You could implement live search suggestions here if desired
                // For example, call viewModel.searchProductsByName(newText) but be mindful of API call frequency
                return true // Indicate the action was handled (or false if default processing should occur)
            }
        })

        binding.btnSearchSubmit.setOnClickListener {
            val query = binding.searchView.query.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                Toast.makeText(this, "Vui lòng nhập tên sản phẩm", Toast.LENGTH_SHORT).show()
            }
            binding.searchView.clearFocus() // Hide keyboard
        }
    }

    private fun performSearch(query: String) {
        Log.d(TAG, "Performing search for: $query")
        // Show loading indicator for search specifically if you have one
        // binding.searchProgressBar.visibility = View.VISIBLE

        // Call ViewModel to search
        viewModel.searchProductsByName(query)

        // Observe isLoading and then searchedItems to navigate
        // We need a one-time observer for this action
        val searchLoadingObserver = object : Observer<Boolean> {
            override fun onChanged(isLoading: Boolean) {
                if (!isLoading) {
                    // Data is ready (or search failed), now check results
                    viewModel.isLoading.removeObserver(this) // Clean up this observer

                    val searchResults = viewModel.searchedItems.value
                    if (!searchResults.isNullOrEmpty()) {
                        Log.d(TAG, "Search successful for '$query', found ${searchResults.size} items. Navigating.")
                        val intent = Intent(this@MainActivity, ListItemsActivity::class.java).apply {
                            putExtra("searchQuery", query) // Pass the original query
                            // Pass the actual search results
                            putParcelableArrayListExtra("searchResults", ArrayList(searchResults))
                        }
                        startActivity(intent)
                    } else {
                        Log.d(TAG, "No results found for query: $query")
                        Toast.makeText(this@MainActivity, "Không tìm thấy sản phẩm nào cho '$query'", Toast.LENGTH_SHORT).show()
                    }
                    resetSearchUI()
                }
            }
        }
        viewModel.isLoading.observe(this, searchLoadingObserver)
    }


    private fun resetSearchUI() {
        binding.searchView.setQuery("", false)
        binding.searchView.visibility = View.GONE
        binding.btnSearchSubmit.visibility = View.GONE
        binding.btnSearch.visibility = View.VISIBLE
        // binding.searchProgressBar.visibility = View.GONE
    }


    private fun loadProfileName() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(userId).child("profile_name")
                .get().addOnSuccessListener { snapshot ->
                    val profileName = snapshot.getValue(String::class.java)
                    if (profileName != null && profileName.isNotEmpty()) {
                        tinyDB.putString("profile_name", profileName)
                        binding.nametitle.text = profileName
                        Log.d(TAG, "Profile name loaded from Firebase: $profileName")
                    } else {
                        // Fallback to TinyDB or default if Firebase has no name or it's empty
                        val storedName = tinyDB.getString("profile_name")
                        binding.nametitle.text = if (!storedName.isNullOrEmpty()) storedName else "Khách hàng"
                        Log.d(TAG, "Profile name from Firebase was null/empty, used TinyDB/default: ${binding.nametitle.text}")
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to load profile name: ${e.message}")
                    val storedName = tinyDB.getString("profile_name")
                    binding.nametitle.text = if (!storedName.isNullOrEmpty()) storedName else "Khách hàng"
                    Toast.makeText(this, "Failed to load profile name", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Should not happen if auth check passed, but as a safeguard
            val storedName = tinyDB.getString("profile_name")
            binding.nametitle.text = if (!storedName.isNullOrEmpty()) storedName else "Khách hàng"
            Log.w(TAG, "User ID was null when trying to load profile name.")
        }
    }

    private fun initBottomMenu() {
        binding.cartBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, CartActivity::class.java))
        }
        binding.profileBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
        }
        binding.orderBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, MyOrderActivity::class.java))
        }
        binding.chatBtn.setOnClickListener {
            // Assuming MyChatActivity exists
            startActivity(Intent(this@MainActivity, MyChatActivity::class.java))
        }
    }

    // Make sure your adapters have an `updateData` method
    // Example for RecommendedAdapter (similar for CategoryAdapter, SliderAdapter):
    // class RecommendedAdapter(private var items: MutableList<ProductDetailsModel>) : RecyclerView.Adapter<...>() {
    //    fun updateData(newItems: List<ProductDetailsModel>) {
    //        items.clear()
    //        items.addAll(newItems)
    //        notifyDataSetChanged() // Or use DiffUtil for better performance
    //    }
    //    // ... other adapter methods
    // }
}