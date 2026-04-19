package com.example.foodbridge

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class ChatListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private val db = FirebaseFirestore.getInstance()

    data class ChatPreview(
        val chatId: String,
        val otherUid: String,
        val otherName: String,
        val lastMessage: String,
        val lastUpdated: Timestamp?,
        val unread: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        recyclerView = findViewById(R.id.recyclerView)
        layoutEmpty  = findViewById(R.id.layoutEmpty)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadChats()
    }

    override fun onResume() {
        super.onResume()
        loadChats()
    }

    private fun loadChats() {
        val myUid = FirebaseHelper.currentUid ?: return

        // Listen in real-time for chat updates
        db.collection("chats")
            .whereArrayContains("participants", myUid)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener

                val chats = mutableListOf<ChatPreview>()
                snapshot.documents.forEach { doc ->
                    val participants = doc.get("participants") as? List<*> ?: return@forEach
                    val otherUid = participants.firstOrNull { it != myUid } as? String ?: return@forEach
                    val names = doc.get("participantNames") as? Map<*, *>
                    val otherName = names?.get(otherUid) as? String ?: "User"
                    chats.add(ChatPreview(
                        chatId      = doc.id,
                        otherUid    = otherUid,
                        otherName   = otherName,
                        lastMessage = doc.getString("lastMessage") ?: "No messages yet",
                        lastUpdated = doc.getTimestamp("lastUpdated"),
                        unread      = false
                    ))
                }

                if (chats.isEmpty()) {
                    layoutEmpty.visibility  = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    layoutEmpty.visibility  = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = ChatListAdapter(chats) { chat ->
                        startActivity(Intent(this, ChatActivity::class.java).apply {
                            putExtra("chatId",    chat.chatId)
                            putExtra("otherName", chat.otherName)
                        })
                    }
                }
            }
    }
}

class ChatListAdapter(
    private val chats: List<ChatListActivity.ChatPreview>,
    private val onClick: (ChatListActivity.ChatPreview) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar:      TextView = view.findViewById(R.id.tvAvatar)
        val tvName:        TextView = view.findViewById(R.id.tvName)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val tvTime:        TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_chat_list, parent, false))

    override fun getItemCount() = chats.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val chat = chats[position]
        holder.tvAvatar.text      = chat.otherName.firstOrNull()?.uppercase() ?: "?"
        holder.tvName.text        = chat.otherName
        holder.tvLastMessage.text = chat.lastMessage
        holder.tvTime.text = chat.lastUpdated?.let {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(it.toDate())
        } ?: ""
        holder.itemView.setOnClickListener { onClick(chat) }
    }
}
