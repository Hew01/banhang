package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.example.banhangs.Adapter.CategoryAdapter
import com.example.banhangs.Adapter.RecommendedAdapter
import com.example.banhangs.Adapter.SliderAdapter
import com.example.banhangs.R
import com.example.banhangs.ViewModel.AuthViewModel // For observing login state
import com.example.banhangs.ViewModel.MainViewModel
import com.example.banhangs.databinding.ActivityMainBinding
import com.example.banhangs.Factory.AuthViewModelFactory // If you initialize AuthViewModel here
import com.example.banhangs.Network.RetrofitClient // For factory
import com.example.banhangs.Repository.AuthRepository // For factory
import com.example.banhangs.Repository.UserPreferencesRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

// Removed: import com.google.firebase.auth.FirebaseAuth
// Removed: import com.example.banhangs.Helper.SessionManager

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels() // Your existing MainViewModel
    private val authViewModel: AuthViewModel by viewModels { // For login status and user data access
        AuthViewModelFactory(
            AuthRepository(RetrofitClient.instance, UserPreferencesRepository(applicationContext)),
            UserPreferencesRepository(applicationContext)
        )
    }
    // Direct access to UserPreferencesRepository for observing flows not exposed by AuthViewModel
    private val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext)
    }

    private val TAG = "MainActivity"

    private lateinit var recommendedAdapter: RecommendedAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var sliderAdapter: SliderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // LoginActivity is responsible for navigating here only if logged in.
        // This is a fallback check.
        lifecycleScope.launch {
            if (userPreferencesRepository.getUserToken() == null) {
                Log.w(TAG, "No auth token found, navigating to LoginActivity.")
                navigateToLogin()
                return@launch // Stop further execution in onCreate if navigating away
            }

            // If token exists, proceed with setup
            Log.d(TAG, "Auth token found, proceeding with MainActivity setup.")
            setupViews()
            observeMainViewModel() // Renamed for clarity
            observeAuthData()    // New observer for auth-related data like profile name
            loadInitialData()
        }
    }

    override fun onResume() {
        super.onResume()
        // The profile name will update reactively via observeAuthData() if it changes.
        // You might want to re-check the token validity here if tokens have a short lifespan
        // and trigger a re-login or refresh if necessary, but that's a more advanced scenario.
        lifecycleScope.launch {
            if (userPreferencesRepository.getUserToken() == null && !isFinishing) {
                Log.w(TAG, "onResume: No auth token found, navigating to LoginActivity.")
                navigateToLogin()
            }
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
        }
        binding.viewPager2.setPageTransformer(compositePageTransformer)

        initBottomMenu()
        initSearch()
    }

    private fun loadInitialData() {
        // These API calls in MainViewModel should now internally use the token
        // obtained from UserPreferencesRepository (via AuthRepository or directly).
        mainViewModel.loadBanners()
        mainViewModel.loadCategories()
        mainViewModel.loadRecommendedItems()
    }

    private fun observeAuthData() {
        // Observe user's full name
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userPreferencesRepository.userFullNameFlow.collect { profileName ->
                    if (!profileName.isNullOrEmpty()) {
                        binding.nametitle.text = profileName
                        Log.d(TAG, "Profile name updated from DataStore: $profileName")
                    } else {
                        binding.nametitle.text = getString(R.string.default_customer_name)
                        Log.d(TAG, "Profile name from DataStore was null/empty, used default.")
                    }
                }
            }
        }

        // Optionally, observe isLoggedIn from AuthViewModel if MainActivity needs to
        // react to logout events initiated from within MainActivity itself (e.g., a logout button here)
        // or to double-check consistency.
        authViewModel.isLoggedIn.observe(this) { isLoggedIn ->
            Log.d(TAG, "isLoggedIn state observed in MainActivity: $isLoggedIn")
            if (!isLoggedIn && !isFinishing) { // Ensure not to navigate if activity is finishing
                // This would be a more forceful redirect if for some reason token got cleared
                // and the initial check in onCreate/onResume didn't catch it before UI setup.
                Log.w(TAG, "Observed logged out state, navigating to login.")
                navigateToLogin()
            }
        }
    }

    private fun observeMainViewModel() { // Renamed from observeViewModel
        mainViewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBarSlider.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.progressBarCategory.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.progressBarRecommend.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        mainViewModel.errorMessage.observe(this, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                Log.e(TAG, "MainViewModel Error: $it")
                binding.progressBarSlider.visibility = View.GONE
                binding.progressBarCategory.visibility = View.GONE
                binding.progressBarRecommend.visibility = View.GONE
            }
        })

        mainViewModel.banners.observe(this, Observer { banners ->
            binding.progressBarSlider.visibility = View.GONE
            if (banners.isNullOrEmpty()) {
                Log.w(TAG, "Banners are empty or null")
                binding.dotIncator.visibility = View.GONE
            } else {
                Log.d(TAG, "Updating banners: ${banners.size}")
                sliderAdapter.updateData(banners.toMutableList())
                if (banners.size > 1) {
                    binding.dotIncator.visibility = View.VISIBLE
                    binding.dotIncator.attachTo(binding.viewPager2)
                } else {
                    binding.dotIncator.visibility = View.GONE
                }
            }
        })

        mainViewModel.categories.observe(this, Observer { categories ->
            binding.progressBarCategory.visibility = View.GONE
            if (categories.isNullOrEmpty()) {
                Log.w(TAG, "Categories are empty or null")
                categoryAdapter.updateData(emptyList())
            } else {
                Log.d(TAG, "Updating categories: ${categories.size}")
                categoryAdapter.updateData(categories.toMutableList())
            }
        })

        mainViewModel.recommendedItems.observe(this, Observer { items ->
            binding.progressBarRecommend.visibility = View.GONE
            if (items.isNullOrEmpty()) {
                Log.w(TAG, "Recommended items are empty or null")
                recommendedAdapter.updateData(emptyList())
            } else {
                Log.d(TAG, "Updating recommended items: ${items.size}")
                recommendedAdapter.updateData(items.toMutableList())
            }
        })

        mainViewModel.searchedItems.observe(this, Observer { searchResults ->
            if (mainViewModel.isLoading.value == false) {
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
        mainViewModel.searchProductsByName(query) // Call MainViewModel

        val searchLoadingObserver = object : Observer<Boolean> {
            override fun onChanged(isLoadingValue: Boolean) {
                if (!isLoadingValue) {
                    mainViewModel.isLoading.removeObserver(this)
                    val searchResults = mainViewModel.searchedItems.value
                    if (!searchResults.isNullOrEmpty()) {
                        Log.d(TAG, "Search successful for '$query', found ${searchResults.size} items. Navigating.")
                        val intent = Intent(this@MainActivity, ListItemsActivity::class.java).apply {
                            putExtra("searchQuery", query)
                            putParcelableArrayListExtra("searchResults", ArrayList(searchResults))
                        }
                        startActivity(intent)
                    } else {
                        val lastError = mainViewModel.errorMessage.value
                        if (lastError != null && lastError.contains("search", ignoreCase = true)) {
                            Log.d(TAG, "Search for '$query' failed or returned no results with error: $lastError")
                        } else {
                            Log.d(TAG, "No results found for query: $query")
                            Toast.makeText(this@MainActivity, getString(R.string.no_products_found_for_query, query), Toast.LENGTH_SHORT).show()
                        }
                    }
                    resetSearchUI()
                }
            }
        }
        mainViewModel.isLoading.observe(this, searchLoadingObserver)
    }

    private fun resetSearchUI() {
        binding.searchView.setQuery("", false)
        binding.searchView.visibility = View.GONE
        binding.btnSearchSubmit.visibility = View.GONE
        binding.btnSearch.visibility = View.VISIBLE
    }

    private fun initBottomMenu() {
        binding.cartBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, CartActivity::class.java))
        }
        binding.profileBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
            // ProfileActivity should save any name changes to UserPreferencesRepository
            // so the userFullNameFlow in MainActivity gets the update.
        }
        binding.orderBtn.setOnClickListener {
            startActivity(Intent(this@MainActivity, MyOrderActivity::class.java))
        }
//        binding.chatBtn.setOnClickListener {
//            startActivity(Intent(this@MainActivity, MyChatActivity::class.java))
//        }

        // Example Logout Button (if you add one to activity_main.xml)
        // binding.btnLogout.setOnClickListener {
        //    Log.d(TAG, "Logout button clicked.")
        //    authViewModel.logoutUser() // This will trigger isLoggedIn observer and navigateToLogin
        // }
    }
}