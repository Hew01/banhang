package com.example.banhangs.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.banhangs.Adapter.ChatAdapter
import com.example.banhangs.Helper.TinyDB
import com.example.banhangs.Model.ChatModel
import com.example.banhangs.databinding.ActivityMyChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MyChatActivity : BaseActivity() {
    private lateinit var binding: ActivityMyChatBinding
    private lateinit var tinyDB: TinyDB
    private lateinit var firebaseDatabase: DatabaseReference
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = ArrayList<ChatModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ActivityMyChatBinding", "onCreate started")
        binding = ActivityMyChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MyOrderActivity", "Binding initialized: ${binding.backBtn != null}")

        tinyDB = TinyDB(this)
        firebaseDatabase = FirebaseDatabase.getInstance().reference
        setVariable()
        loadChatsRealtime()
        initAdapter()



        // Kiểm tra trạng thái nút Back
        binding.backBtn.post {
            Log.d("ActivityMyChatBinding", "Back button state: isShown=${binding.backBtn.isShown}, isEnabled=${binding.backBtn.isEnabled}, isClickable=${binding.backBtn.isClickable}")
        }

        // Xử lý nút Back
        binding.backBtn.setOnClickListener {
            Log.d("ActivityMyChatBinding", "Back button clicked")
            Toast.makeText(this, "Quay lại màn hình chính", Toast.LENGTH_SHORT).show()
            try {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
                Log.d("ActivityMyChatBinding", "Navigated to MainActivity")
            } catch (e: Exception) {
                Log.e("ActivityMyChatBinding", "Error starting MainActivity: ${e.message}")
                Toast.makeText(this, "Lỗi mở MainActivity: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Thêm sự kiện touch để debug
        binding.backBtn.setOnTouchListener { _, event ->
            Log.d("ActivityMyChatBinding", "Back button touched: ${event.action}")
            false // Cho phép sự kiện click tiếp tục
        }



    }
    private fun initAdapter() {
        chatAdapter = ChatAdapter(chatList)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MyChatActivity)
            adapter = chatAdapter
        }
    }





    private fun loadChatsRealtime() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid ?: return

        val chatRef = firebaseDatabase.child("Users").child(userId).child("chats")
        chatRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for(chatSnap in snapshot.children){
                    val chat = chatSnap.getValue(ChatModel::class.java)
                    if (chat != null) {
                        chatList.add(chat)

                    }
                }
                chatAdapter.notifyDataSetChanged()
                binding.chatRecyclerView.scrollToPosition(chatList.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MyChatActivity, "Không thể tải tin nhắn", Toast.LENGTH_SHORT).show()
            }

        })
    }




    private fun setVariable() {
        binding.apply {

            sendBtn.setOnClickListener {

                    saveChatToFirebase()
                }
            }
    }

    private fun saveChatToFirebase() {
        val chattext = binding.txtMess.text.toString().trim()
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userEmail = firebaseUser?.email ?: "unknown@email.com"
        val userId = firebaseUser?.uid ?: "unknown_uid"

        val usersRef = FirebaseDatabase.getInstance().getReference("Users")
        usersRef.get().addOnSuccessListener { dataSnapshot ->
            var userName = ""
            for(userSnapshot  in dataSnapshot.children){
                val authEmail = userSnapshot.child("auth_email").getValue(String::class.java)
                if(authEmail == userEmail){
                    userName = userSnapshot.child("profile_name").getValue(String::class.java) ?: ""
                    break
                }
            }

        val chatModel = ChatModel(
            senderId = "1",
            senderName = userName,
            messageText = chattext
        )

            val chatsRef = firebaseDatabase.child("Users").child(userId).child("chats")
            chatsRef.push().setValue(chatModel)
                .addOnSuccessListener {
                    Toast.makeText(this@MyChatActivity, "Bình luận đã được gửi", Toast.LENGTH_SHORT).show()
                    binding.txtMess.text.clear()
                }

                .addOnFailureListener {
                    Toast.makeText(this@MyChatActivity, "Lỗi khi gửi bình luận", Toast.LENGTH_SHORT).show()
                }

    }

    }

}