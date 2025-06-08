package com.example.banhangs.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.banhangs.R
import com.example.banhangs.databinding.ViewholderModelBinding

class SelectedModelAdapter(val items:MutableList<String>) : RecyclerView.Adapter<SelectedModelAdapter.ViewHolder>() {
    private var selectedPosition = -1
    private var lastSelectedPosition = -1
    private  lateinit var context:Context
    inner class ViewHolder(val bingding : ViewholderModelBinding) : RecyclerView.ViewHolder(bingding.root){

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SelectedModelAdapter.ViewHolder {
        context = parent.context
        val bingding = ViewholderModelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return  ViewHolder(bingding)
    }

    override fun onBindViewHolder(holder: SelectedModelAdapter.ViewHolder, position: Int) {
        holder.bingding.modelTxt.text = items[position]


        holder.bingding.root.setOnClickListener{
            lastSelectedPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(lastSelectedPosition)
            notifyItemChanged(selectedPosition)
        }
        if(selectedPosition == position){
            holder.bingding.modelLayout.setBackgroundResource(R.drawable.green_bg_selected)
            holder.bingding.modelTxt.setTextColor(context.resources.getColor(R.color.green))
        }else{
            holder.bingding.modelLayout.setBackgroundResource(R.drawable.grey_bg)
            holder.bingding.modelTxt.setTextColor(context.resources.getColor(R.color.black))
        }
    }

    override fun getItemCount(): Int {
       return items.size
    }
}