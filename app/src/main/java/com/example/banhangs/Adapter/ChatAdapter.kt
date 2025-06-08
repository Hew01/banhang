package com.example.banhangs.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.banhangs.Model.ChatModel
import com.example.banhangs.databinding.ItemChatLeftBinding

import com.example.banhangs.databinding.ItemChatRightBinding
class ChatAdapter(private val chatList: ArrayList<ChatModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_RIGHT = 1
        private const val VIEW_TYPE_LEFT = 2
    }

    inner class ChatRightViewHolder(val binding: ItemChatRightBinding) : RecyclerView.ViewHolder(binding.root)
    inner class ChatLeftViewHolder(val binding: ItemChatLeftBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return if (chatList[position].senderId == "1") VIEW_TYPE_RIGHT else VIEW_TYPE_LEFT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_RIGHT) {
            val binding = ItemChatRightBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ChatRightViewHolder(binding)
        } else {
            val binding = ItemChatLeftBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ChatLeftViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = chatList[position]
        if (holder is ChatRightViewHolder) {
            holder.binding.titleTxt.text = chat.messageText
        } else if (holder is ChatLeftViewHolder) {
            holder.binding.titleTxt.text = chat.messageText
        }
    }

    override fun getItemCount(): Int = chatList.size
}
