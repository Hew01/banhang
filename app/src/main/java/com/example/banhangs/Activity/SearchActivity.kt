// SearchActivity.kt
package com.example.banhangs.Activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.banhangs.Adapter.SearchAdapter
import com.example.banhangs.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: SearchAdapter
    private lateinit var viewModel: com.example.banhangs.ViewModel.MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        setupBackButton()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewSearch.layoutManager = LinearLayoutManager(this)
        adapter = SearchAdapter(mutableListOf())
        binding.recyclerViewSearch.adapter = adapter
    }





    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }


}