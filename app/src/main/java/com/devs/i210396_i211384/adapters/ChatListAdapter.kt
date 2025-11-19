package com.devs.i210396_i211384.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.devs.i210396_i211384.R
import com.devs.i210396_i211384.chatScreen
import com.devs.i210396_i211384.network.ChatListItem
import com.devs.i210396_i211384.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(
    private val context: Context,
    private val chatUsers: MutableList<ChatListItem>
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val username: TextView = itemView.findViewById(R.id.username)
        val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        val unreadBadge: TextView = itemView.findViewById(R.id.unreadBadge)
        val onlineIndicator: View? = itemView.findViewById(R.id.onlineIndicator)

        fun bind(chatUser: ChatListItem) {
            username.text = chatUser.username
            lastMessage.text = chatUser.lastMessage.ifEmpty { "Tap to start chatting" }
            timestamp.text = formatTime(chatUser.lastMessageTime)

            // Load profile image
            if (chatUser.profileImageUrl.isNotEmpty()) {
                ImageUtils.loadBase64Image(profileImage, chatUser.profileImageUrl)
            } else {
                profileImage.setImageResource(R.drawable.profile)
            }

            // Show online indicator
            onlineIndicator?.visibility = if (chatUser.isOnline) View.VISIBLE else View.GONE

            // Show unread badge
            if (chatUser.unreadCount > 0) {
                unreadBadge.visibility = View.VISIBLE
                unreadBadge.text = if (chatUser.unreadCount > 99) "99+" else chatUser.unreadCount.toString()
            } else {
                unreadBadge.visibility = View.GONE
            }

            // Click to open chat
            itemView.setOnClickListener {
                try {
                    val intent = Intent(context, chatScreen::class.java)

                    // Ensure we have valid data before opening chat
                    val userId = chatUser.otherUserId
                    val username = chatUser.username

                    if (userId.isEmpty()) {
                        android.widget.Toast.makeText(context, "Error: Invalid user ID", android.widget.Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // IMPORTANT: Don't pass large Base64 images through Intent
                    // This causes TransactionTooLargeException
                    intent.putExtra("userId", userId)
                    intent.putExtra("username", username)
                    // DON'T pass profileImageUrl - load it in the chat screen instead
                    intent.putExtra("isVanishMode", false)

                    context.startActivity(intent)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Error opening chat: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
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
