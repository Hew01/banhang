package com.example.banhangs.ViewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.banhangs.Model.CategoryModel
import com.example.banhangs.Model.ProductDetailsModel
import com.example.banhangs.Model.SliderModel
import com.google.firebase.database.*

class MainViewModel : ViewModel() {

    private val firebaseDatabase = FirebaseDatabase.getInstance()

    private val _banner = MutableLiveData<List<SliderModel>>()
    val banners: LiveData<List<SliderModel>> = _banner

    private val _Category = MutableLiveData<MutableList<CategoryModel>>()
    val categories: LiveData<MutableList<CategoryModel>> = _Category

    private val _Recommended = MutableLiveData<MutableList<ProductDetailsModel>>()
    val recommended: LiveData<MutableList<ProductDetailsModel>> = _Recommended

    fun loadFiltered(id: String) {
        val ref = firebaseDatabase.getReference("Items")
        val categoryIdLong = id.toLongOrNull() ?: return // Nếu id không phải số, thoát hàm
        val query: Query = ref.orderByChild("categoryId").equalTo(categoryIdLong.toDouble()) // Firebase cần Double cho number

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<ProductDetailsModel>()
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(ProductDetailsModel::class.java)
                    if (item != null) {
                        item.productId = childSnapshot.key.toString()
                        lists.add(item)
                    }
                }
                Log.d("MainViewModel", "Filtered items for category $id: ${lists.size}")
                _Recommended.postValue(lists)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainViewModel", "Load filtered failed: ${error.message}")
            }
        })
    }


    fun loadRecommended() {
        val ref = firebaseDatabase.getReference("Items")
        val query: Query = ref.orderByChild("showRecommended").equalTo(true)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<ProductDetailsModel>()
                for (childSnapshot in snapshot.children) {
                    val item = childSnapshot.getValue(ProductDetailsModel::class.java)
                    if (item != null) {
                        item.productId = childSnapshot.key ?: ""
                        lists.add(item)
                    }
                }
                _Recommended.postValue(lists)
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý lỗi nếu cần
            }
        })
    }

    fun loadCategory() {
        val ref = firebaseDatabase.getReference("Category")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<CategoryModel>()
                for (childSnapshot in snapshot.children) {
                    val category = childSnapshot.getValue(CategoryModel::class.java)
                    if (category != null) {
                        lists.add(category)
                    }
                }
                _Category.postValue(lists)
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý lỗi nếu cần
            }
        })
    }

    fun loadBanners() {
        val ref = firebaseDatabase.getReference("Banner")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lists = mutableListOf<SliderModel>()
                for (childSnapshot in snapshot.children) {
                    val banner = childSnapshot.getValue(SliderModel::class.java)
                    if (banner != null) {
                        lists.add(banner)
                    }
                }
                _banner.postValue(lists)
            }

            override fun onCancelled(error: DatabaseError) {
                // Xử lý lỗi nếu cần
            }
        })
    }



    // Trong MainViewModel.kt
    fun searchProducts(query: String): LiveData<List<ProductDetailsModel>> {
        val result = MutableLiveData<List<ProductDetailsModel>>()

        FirebaseDatabase.getInstance().getReference("Products")
            .orderByChild("title")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val products = mutableListOf<ProductDetailsModel>()
                    for (productSnapshot in snapshot.children) {
                        productSnapshot.getValue(ProductDetailsModel::class.java)?.let {
                            products.add(it)
                        }
                    }
                    result.value = products
                }

                override fun onCancelled(error: DatabaseError) {
                    // Xử lý lỗi
                }
            })

        return result
    }
}
