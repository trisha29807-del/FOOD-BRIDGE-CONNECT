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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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

        recyclerView   = findViewById(R.id.recyclerView)
        etMessage      = findViewById(R.id.etMessage)
        btnSend        = findViewById(R.id.btnSend)
        tvChatTitle    = findViewById(R.id.tvChatTitle)
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus)

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

        // ── KEY FIX: init document first, THEN start listening ───────────────
        initChatDocumentAndListen()
        setupSend()
    }

    override fun onStart() {
        super.onStart()
        // Listener is started inside initChatDocumentAndListen() after doc is ready
        // but if activity resumes after being stopped we re-attach
        if (messageListener == null && chatId.isNotEmpty()) {
            startListening()
        }
    }

    override fun onStop() {
        super.onStop()
        messageListener?.remove()
        messageListener = null
    }

    /**
     * Ensures the chat document exists in Firestore with BOTH participant UIDs
     * and BOTH participant names before we start listening for messages.
     *
     * Root cause of the "receiver can't receive" bug:
     *   - The sender created the chat doc with only their own UID / name.
     *   - The receiver opened the chat, but their UID was missing from
     *     `participants`, so Firestore security rules blocked them from reading
     *     the messages sub-collection.
     *
     * Fix: always merge BOTH UIDs and look up BOTH names.
     */
    private fun initChatDocumentAndListen() {
        val myUid = FirebaseHelper.currentUid ?: return

        // chatId is always "<smallerUid>_<largerUid>"
        val uids = chatId.split("_")
        if (uids.size != 2) { startListening(); return }

        val otherUid = uids.firstOrNull { it != myUid } ?: run { startListening(); return }
        val chatRef  = db.collection("chats").document(chatId)

        // Step 1: ensure participants list has BOTH uids
        chatRef.set(
            mapOf(
                "participants" to FieldValue.arrayUnion(myUid, otherUid),
                "lastUpdated"  to Timestamp.now()
            ),
            SetOptions.merge()
        ).addOnCompleteListener {
            // Step 2: write MY display name into participantNames map
            lifecycleScope.launch {
                val result = FirebaseHelper.getUserProfile(myUid)
                val myName = result.getOrNull()?.get("name") as? String ?: "User"

                chatRef.set(
                    mapOf(
                        "participantNames" to mapOf(myUid to myName)
                    ),
                    SetOptions.merge()
                ).addOnCompleteListener {
                    // Step 3: ONLY start listening once the doc is properly set up
                    startListening()
                }
            }
        }
    }

    private fun startListening() {
        if (chatId.isEmpty()) return
        if (messageListener != null) return   // already listening

        messageListener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Surface the real error so you can see Firestore rule denials in logcat
                    Toast.makeText(
                        this,
                        "Chat error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }
                snapshot ?: return@addSnapshotListener

                val uid = FirebaseHelper.currentUid ?: ""
                messages.clear()
                snapshot.documents.forEach { doc ->
                    messages.add(
                        ChatMessage(
                            text      = doc.getString("text")      ?: "",
                            senderUid = doc.getString("senderUid") ?: "",
                            timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                            isMe      = doc.getString("senderUid") == uid
                        )
                    )
                }
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    recyclerView.post {
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
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

        val message = hashMapOf(
            "text"      to text,
            "senderUid" to uid,
            "timestamp" to Timestamp.now()
        )

        db.collection("chats").document(chatId)
            .collection("messages")
            .add(message)
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send", Toast.LENGTH_SHORT).show()
                etMessage.setText(text)
            }

        // Update last-message preview for ChatListActivity
        db.collection("chats").document(chatId)
            .set(
                mapOf(
                    "lastMessage" to text,
                    "lastUpdated" to Timestamp.now()
                ),
                SetOptions.merge()
            )
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