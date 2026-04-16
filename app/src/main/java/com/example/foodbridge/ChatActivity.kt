package com.example.foodbridge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
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
        val senderName: String,
        val timestamp: Timestamp,
        val isMe: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatId        = intent.getStringExtra("chatId")       ?: ""
        otherUserName = intent.getStringExtra("otherName")    ?: "User"

        recyclerView = findViewById(R.id.recyclerView)
        etMessage    = findViewById(R.id.etMessage)
        btnSend      = findViewById(R.id.btnSend)
        tvChatTitle  = findViewById(R.id.tvChatTitle)

        tvChatTitle.text = otherUserName
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        adapter = ChatAdapter(messages, FirebaseHelper.currentUid ?: "")
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = adapter

        listenForMessages()
        setupSend()
    }

    private fun listenForMessages() {
        if (chatId.isEmpty()) return
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                messages.clear()
                val uid = FirebaseHelper.currentUid ?: ""
                snapshot.documents.forEach { doc ->
                    messages.add(ChatMessage(
                        text        = doc.getString("text")       ?: "",
                        senderUid   = doc.getString("senderUid")  ?: "",
                        senderName  = doc.getString("senderName") ?: "",
                        timestamp   = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                        isMe        = doc.getString("senderUid") == uid
                    ))
                }
                adapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messages.size - 1)
            }
    }

    private fun setupSend() {
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isEmpty() || chatId.isEmpty()) return@setOnClickListener

            val uid = FirebaseHelper.currentUid ?: return@setOnClickListener
            etMessage.setText("")

            db.collection("chats").document(chatId)
                .collection("messages")
                .add(mapOf(
                    "text"       to text,
                    "senderUid"  to uid,
                    "timestamp"  to Timestamp.now()
                ))

            // Update last message on chat doc
            db.collection("chats").document(chatId)
                .update(mapOf(
                    "lastMessage" to text,
                    "lastUpdated" to Timestamp.now()
                ))
        }
    }
}

class ChatAdapter(
    private val messages: List<ChatActivity.ChatMessage>,
    private val myUid: String
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    companion object {
        const val VIEW_ME    = 1
        const val VIEW_OTHER = 2
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tvMessageText)
        val tvTime: TextView = view.findViewById(R.id.tvMessageTime)
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].senderUid == myUid) VIEW_ME else VIEW_OTHER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = if (viewType == VIEW_ME)
            R.layout.item_chat_me else R.layout.item_chat_other
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
