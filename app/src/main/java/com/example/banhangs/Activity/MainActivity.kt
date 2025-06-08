package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView // Keep this for the SearchView widget
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.example.banhangs.Adapter.CategoryAdapter
import com.example.banhangs.Adapter.RecommendedAdapter
import com.example.banhangs.Adapter.SliderAdapter
import com.example.banhangs.Helper.SessionManager // Import SessionManager
// import com.example.banhangs.Model.CategoryModel // Already imported if CategoryAdapter uses it
// import com.example.banhangs.Model.ProductDetailsModel // Already imported if RecommendedAdapter uses it
// import com.example.banhangs.Model.SliderModel // Already imported if SliderAdapter uses it
import com.example.banhangs.R
import com.example.banhangs.ViewModel.MainViewModel
import com.example.banhangs.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth // Keep for auth state, but not for profile name directly

// Remove FirebaseDatabase if only used for profile name
// import com.google.firebase.database.FirebaseDatabase

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    // private lateinit var tinyDB: TinyDB // Remove TinyDB if SessionManager handles all needed persistence
    private val viewModel: MainViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var sessionManager: SessionManager
    // In MainActivity.kt (continued)

    private val TAG = "MainActivity"

    private lateinit var recommendedAdapter: RecommendedAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var sliderAdapter: SliderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // tinyDB = TinyDB(this) // Remove if not used for other purposes
        auth = FirebaseAuth.getInstance() // Keep for checking login status
        sessionManager = SessionManager(this) // Initialize SessionManager

        // Check login status using SessionManager's token and Firebase Auth if needed for verification
        if (sessionManager.fetchAuthToken() == null /* || auth.currentUser == null || !auth.currentUser!!.isEmailVerified */) {
            // If you still want to use Firebase for email verification check, uncomment the auth part.
            // Otherwise, just checking for the token from your API might be sufficient.
            navigateToLogin()
            return // Important to return if navigating away
        }

        setupViews()
        observeViewModel()
        loadInitialData()
        loadProfileNameFromSession() // Load profile name from SessionManager
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile name in case it changed in ProfileActivity
        if (sessionManager.fetchAuthToken() != null) {
            loadProfileNameFromSession()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun setupViews() {
        // Recommended Adapter
        recommendedAdapter = RecommendedAdapter(mutableListOf())
        binding.viewRecommendation.layoutManager = GridLayoutManager(this, 2)
        binding.viewRecommendation.adapter = recommendedAdapter

        // Category Adapter
        categoryAdapter = CategoryAdapter(mutableListOf())
        binding.viewCategory.layoutManager =
                // In MainActivity.kt (continued)

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
            // Example of another transformer:
            // addTransformer { page, position ->
            //     val r = 1 - Math.abs(position)
            //     page.scaleY = 0.85f + r * 0.15f
            // }
        }
        binding.viewPager2.setPageTransformer(compositePageTransformer)

        initBottomMenu()
        initSearch()
    }

    private fun loadInitialData() {
        viewModel.loadBanners()
        viewModel.loadCategories()
        viewModel.loadRecommendedItems()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this, Observer { isLoading ->
            // This is a general loading state. You might want more granular control.
            binding.progressBarSlider.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.progressBarCategory.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.progressBarRecommend.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                Log.e(TAG, "ViewModel Error: $it")
                // Potentially hide all progress bars on error too
                binding.progressBarSlider.visibility = View.GONE
                binding.progressBarCategory.visibility = View.GONE
                binding.progressBarRecommend.visibility = View.GONE
            }
        })

        viewModel.banners.observe(this, Observer { banners ->
            binding.progressBarSlider.visibility = View.GONE // Hide specific progress bar
            if (banners.isNullOrEmpty()) {
                Log.w(TAG, "Banners are empty or null")
                binding.dotIncator.visibility = View.GONE
            } else {
                Log.d(TAG, "Updating banners: ${banners.size}")
                sliderAdapter.updateData(banners.toMutableList()) // Adapter has updateData now
                if (banners.size > 1) {
                    binding.dotIncator.visibility = View.VISIBLE
                    binding.dotIncator.attachTo(binding.viewPager2)
                } else {
                    binding.dotIncator.visibility = View.GONE
                }
            }
        })

        viewModel.categories.observe(this, Observer { categories ->
            binding.progressBarCategory.visibility = View.GONE // Hide specific progress bar
            if (categories.isNullOrEmpty()) {
                Log.w(TAG, "Categories are empty or null")
                // Toast.makeText(this, "No categories available", Toast.LENGTH_SHORT).show() // Optional
                categoryAdapter.updateData(emptyList())
            } else {
                Log.d(TAG, "Updating categories: ${categories.size}")
                categoryAdapter.updateData(categories.toMutableList()) // Adapter has updateData now
            }
        })

        viewModel.recommendedItems.observe(this, Observer { items ->
            binding.progressBarRecommend.visibility = View.GONE // Hide specific progress bar
            if (items.isNullOrEmpty()) {
                Log.w(TAG, "Recommended items are empty or null")
                // Toast.makeText(this, "No recommended items available", Toast.LENGTH_SHORT).show() // Optional
                recommendedAdapter.updateData(emptyList())
            } else {
                Log.d(TAG, "Updating recommended items: ${items.size}")
                recommendedAdapter.updateData(items.toMutableList()) // Adapter has updateData now
            }
        })

        // Observer for search results (primarily for navigation after search)
        // The isLoading observer within performSearch handles the direct navigation logic.
        viewModel.searchedItems.observe(this, Observer { searchResults ->
            // This observer can be used if MainActivity needs to react to search results
            // even if navigation happens elsewhere. For now, just logging.
            if (viewModel.isLoading.value == false) { // Ensure loading is complete
                Log.d(TAG, "Searched items LiveData updated, count: ${searchResults?.size ?: "null"}")
            }
        })
    }

    private fun initSearch() {
        binding.btnSearch.setOnClickListener {
            binding.searchView.setQuery("", false)
            binding.btnSearch.visibility = View.GONE
            binding.searchView.visibility = View.VISIBLE
            binding.btnSearchSubmit.visibility = View.VISIBLE
            binding.searchView.requestFocus()
            // Consider showing keyboard explicitly if needed
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.trim().let {
                    if (!it.isNullOrEmpty()) {
                        performSearch(it)
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.please_enter_product_name), Toast.LENGTH_SHORT).show()
                    }
                }
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Implement live suggestions if desired, but be mindful of API call frequency.
                return true
            }
        })

        binding.btnSearchSubmit.setOnClickListener {
            val query = binding.searchView.query.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            } else {
                Toast.makeText(this, getString(R.string.please_enter_product_name), Toast.LENGTH_SHORT).show()
            }
            binding.searchView.clearFocus()
        }
    }

    private fun performSearch(query: String) {
        Log.d(TAG, "Performing search for: $query")
        // You could show a specific search progress bar here
        // binding.searchProgressBar.visibility = View.VISIBLE

        viewModel.searchProductsByName(query) // Call ViewModel

        // Use a one-time observer for the isLoading state related to this search action
        // to handle navigation or displaying "no results" message.
        val searchLoadingObserver = object : Observer<Boolean> {
            override fun onChanged(isLoadingValue: Boolean) {
                if (!isLoadingValue) { // When loading is finished for the search
                    viewModel.isLoading.removeObserver(this) // Important: remove the observer

                    val searchResults = viewModel.searchedItems.value
                    if (!searchResults.isNullOrEmpty()) {
                        Log.d(TAG, "Search successful for '$query', found ${searchResults.size} items. Navigating.")
                        val intent = Intent(this@MainActivity, ListItemsActivity::class.java).apply {
                            putExtra("searchQuery", query)
                            // Pass the actual search results (ProductDetailsModel should be Parcelable)
                            putParcelableArrayListExtra("searchResults", ArrayList(searchResults))
                        }
                        startActivity(intent)
                    } else {
                        // Check if there was an error message from the ViewModel for this specific search
                        val lastError = viewModel.errorMessage.value
                        if (lastError != null && lastError.contains("search", ignoreCase = true)) {
                            // Error already shown by the general error observer
                            Log.d(TAG, "Search for '$query' failed or returned no results with error: $lastError")
                        } else {
                            Log.d(TAG, "No results found for query: $query")
                            Toast.makeText(this@MainActivity, getString(R.string.no_products_found_for_query, query), Toast.LENGTH_SHORT).show()
                        }
                    }
                    resetSearchUI()
                    // binding.searchProgressBar.visibility = View.GONE
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
        // Hide specific search progress bar if you have one
        // binding.searchProgressBar.visibility = View.GONE
    }

    // In MainActivity.kt (continued)

    private fun loadProfileNameFromSession() {
        // Fetch user's name from SessionManager (assuming it was saved during login)
        val profileName = sessionManager.fetchUserFullName() // Assuming SessionManager has getUserName()

        if (!profileName.isNullOrEmpty()) {
            binding.nametitle.text = profileName
            Log.d(TAG, "Profile name loaded from SessionManager: $profileName")
        } else {
            // Fallback if no name is stored in SessionManager
            binding.nametitle.text = getString(R.string.default_customer_name) // Use a string resource
            Log.d(TAG, "Profile name from SessionManager was null/empty, used default.")
            // Optionally, you could try to fetch it from an API endpoint if not in session,
            // but typically it's fetched once at login.
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
            // Assuming MyChatActivity exists and is set up
            startActivity(Intent(this@MainActivity, MyChatActivity::class.java))
        }
        // Example: Home button (if your current activity isn't the primary "home")
        // binding.homeBtn.setOnClickListener {
        //    // If MainActivity is already home, this might refresh or do nothing
        //    // Or, if you have a different main landing activity:
        //    // startActivity(Intent(this@MainActivity, HomeActivity::class.java))
        // }
    }
}

// Ensure you have these string resources in res/values/strings.xml:
// <string name="please_enter_product_name">Vui lòng nhập tên sản phẩm</string>
// <string name="no_products_found_for_query">Không tìm thấy sản phẩm nào cho \'%1$s\'</string>
// <string name="default_customer_name">Khách hàng</string>