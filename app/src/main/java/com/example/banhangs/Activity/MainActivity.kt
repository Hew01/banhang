package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.example.banhangs.Adapter.SliderAdapter
import com.example.banhangs.Helper.TinyDB
import com.example.banhangs.Model.SliderModel
import com.example.banhangs.ViewModel.MainViewModel
import com.example.banhangs.databinding.ActivityMainBinding
import androidx.recyclerview.widget.GridLayoutManager
import com.example.banhangs.Adapter.CategoryAdapter
import com.example.banhangs.Adapter.RecommendedAdapter
import com.example.banhangs.Model.ProductDetailsModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var tinyDB: TinyDB
    private var viewModel = MainViewModel()
    private lateinit var auth: FirebaseAuth
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tinyDB = TinyDB(this)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null || !auth.currentUser!!.isEmailVerified) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        } else {
            loadProfileName()
        }

        initBanner()
        initCategory()
        initRecommended()
        initBottomMenu()
        initSearch()
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null && auth.currentUser!!.isEmailVerified) {
            loadProfileName()
        }
    }

    private fun initSearch() {
        // Khi nhấn icon tìm kiếm
        binding.btnSearch.setOnClickListener {
            binding.btnSearch.visibility = View.GONE
            binding.searchView.visibility = View.VISIBLE
            binding.btnSearchSubmit.visibility = View.VISIBLE
            binding.searchView.requestFocus()
        }

        // Khi nhấn nút "Tìm"
        binding.btnSearchSubmit.setOnClickListener {
            val query = binding.searchView.query.toString().trim()
            if (query.isNotEmpty()) {
                // Gọi hàm tìm kiếm trong ViewModel
                viewModel.searchProductsByName(query)

                // Định nghĩa resultsObserver
                val resultsObserver = object : Observer<List<ProductDetailsModel>> {
                    override fun onChanged(searchResults: List<ProductDetailsModel>) {
                        Log.d(TAG, "Observer received searchResults for query '$query': size=${searchResults.size}")
                        if (searchResults.isNotEmpty()) {
                            // Tạo Intent mới để chuyển sang ListItemsActivity
                            val intent = Intent(this@MainActivity, ListItemsActivity::class.java).apply {
                                putExtra("searchQuery", query)
                                putParcelableArrayListExtra("searchResults", ArrayList(searchResults))
                                // Xóa flags để giữ MainActivity trong stack
                            }
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@MainActivity, "Không tìm thấy sản phẩm nào", Toast.LENGTH_SHORT).show()
                        }
                        // Gỡ resultsObserver
                        viewModel.searchResults.removeObserver(this)
                    }
                }

                // Định nghĩa loadingObserver
                val loadingObserver = object : Observer<Boolean> {
                    override fun onChanged(isLoading: Boolean) {
                        if (!isLoading) {
                            // Dữ liệu đã sẵn sàng, quan sát searchResults
                            viewModel.searchResults.observe(this@MainActivity, resultsObserver)
                            // Gỡ loadingObserver
                            viewModel.isLoading.removeObserver(this)
                        }
                    }
                }

                // Quan sát isLoading
                viewModel.isLoading.observe(this@MainActivity, loadingObserver)
            } else {
                Toast.makeText(this, "Vui lòng nhập tên sản phẩm", Toast.LENGTH_SHORT).show()
            }

            // Reset giao diện tìm kiếm
            binding.searchView.setQuery("", false)
            binding.searchView.visibility = View.GONE
            binding.btnSearchSubmit.visibility = View.GONE
            binding.btnSearch.visibility = View.VISIBLE
        }
    }

    private fun loadProfileName() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            FirebaseDatabase.getInstance().getReference("Users/$userId/profile_name")
                .get().addOnSuccessListener { snapshot ->
                    val profileName = snapshot.getValue(String::class.java) ?: tinyDB.getString("profile_name") ?: "Khách hàng thân mến"
                    tinyDB.putString("profile_name", profileName)
                    binding.nametitle.text = profileName
                    Log.d(TAG, "Profile name loaded: $profileName")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to load profile name: ${e.message}")
                    binding.nametitle.text = tinyDB.getString("profile_name") ?: "Khách hàng thân mến"
                    Toast.makeText(this, "Failed to load profile name", Toast.LENGTH_SHORT).show()
                }
        } else {
            binding.nametitle.text = tinyDB.getString("profile_name") ?: "Khách hàng thân mến"
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
            startActivity(Intent(this@MainActivity, MyChatActivity::class.java))
        }
    }

    private fun initRecommended() {
        binding.progressBarRecommend.visibility = View.VISIBLE
        binding.viewRecommendation.layoutManager = GridLayoutManager(this@MainActivity, 2)

        viewModel.recommended.observe(this, Observer { items ->
            binding.progressBarRecommend.visibility = View.GONE
            if (items.isNullOrEmpty()) {
                Log.e(TAG, "Recommended items are empty")
                Toast.makeText(this, "No recommended items available", Toast.LENGTH_SHORT).show()
                val dummyItems = mutableListOf(
                    com.example.banhangs.Model.ProductDetailsModel(
                        name = "Phone",
                        description = "Smartphone",
                        galleryImageUrls = arrayListOf("phone_image"),
                        model = arrayListOf("Black", "White"),
                        price = 500.00,
                        rating = 4.5,
                        numberInCart = 0,
                        showRecommended = true,
                        categoryId = 1L
                    ),
                    com.example.banhangs.Model.ProductDetailsModel(
                        title = "Laptop",
                        description = "Gaming Laptop",
                        picUrl = arrayListOf("laptop_image"),
                        model = arrayListOf("Silver", "Black"),
                        price = 100L,
                        rating = 4.8,
                        numberInCart = 0,
                        showRecommended = true,
                        categoryId = 1L
                    )
                )
                binding.viewRecommendation.adapter = RecommendedAdapter(dummyItems)
            } else {
                Log.d(TAG, "Recommended items loaded: ${items.size}")
                binding.viewRecommendation.adapter = RecommendedAdapter(items.toMutableList())
            }
        })

        viewModel.loadRecommended()
    }

    private fun initCategory() {
        binding.progressBarCategory.visibility = View.VISIBLE
        binding.viewCategory.layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)

        viewModel.categories.observe(this, Observer { categories ->
            binding.progressBarCategory.visibility = View.GONE
            if (categories.isNullOrEmpty()) {
                Log.e(TAG, "Categories are empty")
                Toast.makeText(this, "No categories available", Toast.LENGTH_SHORT).show()
                val dummyCategories = mutableListOf(
                    com.example.banhangs.Model.CategoryModel(
                        title = "Electronics",
                        id = 1,
                        picUrl = "cat_electronics"
                    ),
                    com.example.banhangs.Model.CategoryModel(
                        title = "Fashion",
                        id = 2,
                        picUrl = "cat_fashion"
                    )
                )
                binding.viewCategory.adapter = CategoryAdapter(dummyCategories)
            } else {
                Log.d(TAG, "Categories loaded: ${categories.size}")
                binding.viewCategory.adapter = CategoryAdapter(categories.toMutableList())
            }
        })
        viewModel.loadCategory()
    }

    private fun banners(image: List<SliderModel>) {
        binding.progressBarSlider.visibility = View.GONE
        if (image.isEmpty()) {
            Log.e(TAG, "Banner images are empty")
            Toast.makeText(this, "No banner images available", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Banner images loaded: ${image.size}")
        binding.viewPager2.adapter = SliderAdapter(image.toMutableList(), binding.viewPager2)
        binding.viewPager2.clipToPadding = false
        binding.viewPager2.clipChildren = false
        binding.viewPager2.offscreenPageLimit = 3
        binding.viewPager2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        val compositePageTransformer = CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(40))
        }
        binding.viewPager2.setPageTransformer(compositePageTransformer)

        if (image.size > 1) {
            binding.dotIncator.visibility = View.VISIBLE
            binding.dotIncator.attachTo(binding.viewPager2)
        }
    }

    private fun initBanner() {
        binding.progressBarSlider.visibility = View.VISIBLE
        viewModel.banners.observe(this, Observer { banners ->
            if (banners.isNullOrEmpty()) {
                Log.e(TAG, "Banners are empty")
                Toast.makeText(this, "No banners available", Toast.LENGTH_SHORT).show()
                val dummyBanners = mutableListOf(
                    SliderModel("banner1_test"),
                    SliderModel("banner2_test")
                )
                banners(dummyBanners)
            } else {
                banners(banners.toMutableList())
            }
        })
        viewModel.loadBanners()
    }
}