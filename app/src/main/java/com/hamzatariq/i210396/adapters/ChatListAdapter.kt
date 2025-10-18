package com.hamzatariq.i210396.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hamzatariq.i210396.R
import com.hamzatariq.i210396.chatScreen
import com.hamzatariq.i210396.models.ChatUser
import com.hamzatariq.i210396.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val context: Context,
    private val chatUsers: MutableList<ChatUser>
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val username: TextView = itemView.findViewById(R.id.username)
        val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        val unreadBadge: TextView = itemView.findViewById(R.id.unreadBadge)

        fun bind(chatUser: ChatUser) {
            username.text = chatUser.username
            lastMessage.text = chatUser.lastMessage.ifEmpty { "Tap to start chatting" }
            timestamp.text = formatTime(chatUser.lastMessageTime)

            // Load profile image
            if (chatUser.profileImageUrl.isNotEmpty()) {
                ImageUtils.loadBase64Image(profileImage, chatUser.profileImageUrl)
            }

            // Show unread badge
            if (chatUser.unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = if (chatUser.unreadCount > 99) "99+" else chatUser.unreadCount.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }

            // Click to open chat
            itemView.setOnClickListener {
                val intent = Intent(context, chatScreen::class.java)
                intent.putExtra("userId", chatUser.userId)
                intent.putExtra("username", chatUser.username)
                intent.putExtra("profileImageUrl", chatUser.profileImageUrl)
                context.startActivity(intent)
            }
        }

        private fun formatTime(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "Now"
                diff < 3600000 -> "${diff / 60000}m"
                diff < 86400000 -> "${diff / 3600000}h"
                diff < 604800000 -> "${diff / 86400000}d"
                else -> {
                    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatUsers[position])
    }

    override fun getItemCount(): Int = chatUsers.size
}

