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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvChatTitle: TextView
    private lateinit var tvOnlineStatus: TextView

    private val db = FirebaseFirestore.getInstance()
    private var chatId = ""
    private var otherUserName = ""
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter
    private var messageListener: ListenerRegistration? = null

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
        otherUserName = intent.getStringExtra("otherName") ?: "User"

        recyclerView    = findViewById(R.id.recyclerView)
        etMessage       = findViewById(R.id.etMessage)
        btnSend         = findViewById(R.id.btnSend)
        tvChatTitle     = findViewById(R.id.tvChatTitle)
        tvOnlineStatus  = findViewById(R.id.tvOnlineStatus)

        tvChatTitle.text    = otherUserName
        tvOnlineStatus.text = "FoodBridge Chat"

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

        // Initialize chat document with participant info
        initChatDocument()
        setupSend()
    }

    override fun onStart() {
        super.onStart()
        // Start real-time listener when activity is visible
        startListening()
    }

    override fun onStop() {
        super.onStop()
        // Remove listener when activity goes to background to save reads
        messageListener?.remove()
    }

    private fun initChatDocument() {
        val myUid = FirebaseHelper.currentUid ?: return
        // Extract other UID from chatId (format: uid1_uid2)
        val uids = chatId.split("_")
        val otherUid = uids.firstOrNull { it != myUid } ?: return

        // Save participant info so ChatListActivity can show names
        db.collection("chats").document(chatId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // Create chat document with participants list
                    db.collection("chats").document(chatId).set(mapOf(
                        "participants"     to listOf(myUid, otherUid),
                        "participantNames" to mapOf(myUid to "Me"),
                        "lastMessage"      to "",
                        "lastUpdated"      to Timestamp.now(),
                        "createdAt"        to Timestamp.now()
                    ))
                }
                // Update my name in the chat
                lifecycleScope.launch {
                    val result = FirebaseHelper.getUserProfile(myUid)
                    val name = result.getOrNull()?.get("name") as? String ?: "User"
                    db.collection("chats").document(chatId)
                        .update("participantNames.$myUid", name)
                }
            }
    }

    private fun startListening() {
        if (chatId.isEmpty()) return

        messageListener = db.collection("chats").document(chatId)
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
                // Auto scroll to latest message
                if (messages.isNotEmpty()) {
                    recyclerView.post {
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }
    }

    private fun setupSend() {
        btnSend.setOnClickListener { sendMessage() }
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

        // Add message to subcollection
        db.collection("chats").document(chatId)
            .collection("messages")
            .add(message)
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                etMessage.setText(text) // restore text on failure
            }

        // Update last message on chat document for ChatListActivity
        db.collection("chats").document(chatId)
            .update(mapOf(
                "lastMessage" to text,
                "lastUpdated" to Timestamp.now()
            ))
    }
}

// ── Adapter ───────────────────────────────────────────────────────────────────

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
