package com.example.foodbridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvChatTitle: TextView

    private val db = FirebaseFirestore.getInstance()
    private var chatId = ""
    private var otherUserName = ""
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    data class ChatMessage(
        val text: String,
        val senderUid: String,
        val timestamp: Timestamp,
        val isMe: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatId        = intent.getStringExtra("chatId")    ?: ""
        otherUserName = intent.getStringExtra("otherName") ?: "Donor"

        recyclerView = findViewById(R.id.recyclerView)
        etMessage    = findViewById(R.id.etMessage)
        btnSend      = findViewById(R.id.btnSend)
        tvChatTitle  = findViewById(R.id.tvChatTitle)

        tvChatTitle.text = "💬 $otherUserName"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        if (chatId.isEmpty()) {
            Toast.makeText(this, "Could not open chat", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager

        adapter = ChatAdapter(messages, FirebaseHelper.currentUid ?: "")
        recyclerView.adapter = adapter

        // Create chat document if it doesn't exist yet
        db.collection("chats").document(chatId)
            .set(mapOf("createdAt" to Timestamp.now()), com.google.firebase.firestore.SetOptions.merge())

        listenForMessages()
        setupSend()
    }

    private fun listenForMessages() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot ?: return@addSnapshotListener

                val uid = FirebaseHelper.currentUid ?: ""
                messages.clear()
                snapshot.documents.forEach { doc ->
                    messages.add(ChatMessage(
                        text      = doc.getString("text")      ?: "",
                        senderUid = doc.getString("senderUid") ?: "",
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                        isMe      = doc.getString("senderUid") == uid
                    ))
                }
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun setupSend() {
        btnSend.setOnClickListener { sendMessage() }
        etMessage.setOnEditorActionListener { _, _, _ ->
            sendMessage(); true
        }
    }

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val uid = FirebaseHelper.currentUid ?: run {
            Toast.makeText(this, "Please sign in again", Toast.LENGTH_SHORT).show()
            return
        }

        etMessage.setText("")

        val message = mapOf(
            "text"      to text,
            "senderUid" to uid,
            "timestamp" to Timestamp.now()
        )

        db.collection("chats").document(chatId)
            .collection("messages")
            .add(message)

        // Update last message on chat document
        db.collection("chats").document(chatId)
            .update(mapOf(
                "lastMessage" to text,
                "lastUpdated" to Timestamp.now()
            ))
    }
}

class ChatAdapter(
    private val messages: List<ChatActivity.ChatMessage>,
    private val myUid: String
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvMessageText)
        val tvTime: TextView = view.findViewById(R.id.tvMessageTime)
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].isMe) 1 else 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = if (viewType == 1) R.layout.item_chat_me else R.layout.item_chat_other
        return VH(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val msg = messages[position]
        holder.tvText.text = msg.text
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        holder.tvTime.text = sdf.format(msg.timestamp.toDate())
    }
}