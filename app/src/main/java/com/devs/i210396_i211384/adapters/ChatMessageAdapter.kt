package com.devs.i210396_i211384.adapters

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.devs.i210396_i211384.PostDetailActivity
import com.devs.i210396_i211384.R
import com.devs.i210396_i211384.models.ChatMessage
import com.devs.i210396_i211384.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageAdapter(
    private val context: Context,
    private val messages: MutableList<ChatMessage>,
    private val currentUserId: String,
    private val onEditMessage: ((ChatMessage) -> Unit)? = null,
    private val onDeleteMessage: ((ChatMessage) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val MESSAGE_TYPE_SENT = 1
    private val MESSAGE_TYPE_RECEIVED = 2

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val messageImage: ImageView = itemView.findViewById(R.id.messageImage)
        val postPreview: View = itemView.findViewById(R.id.postPreview)
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        val editedIndicator: TextView = itemView.findViewById(R.id.editedIndicator)

        fun bind(message: ChatMessage) {
            if (message.isDeleted) {
                messageText.visibility = View.VISIBLE
                messageText.text = "Message deleted"
                messageText.setTextColor(context.getColor(android.R.color.darker_gray))
                messageText.isEnabled = false
                messageImage.visibility = View.GONE
                postPreview.visibility = View.GONE
                editedIndicator.visibility = View.GONE
                return
            }

            when (message.messageType) {
                "text" -> {
                    messageText.visibility = View.VISIBLE
                    messageText.text = message.messageText
                    messageImage.visibility = View.GONE
                    postPreview.visibility = View.GONE
                }
                "image" -> {
                    messageText.visibility = View.GONE
                    messageImage.visibility = View.VISIBLE
                    postPreview.visibility = View.GONE
                    if (message.imageBase64.isNotEmpty()) {
                        ImageUtils.loadBase64Image(messageImage, message.imageBase64)
                    }
                }
                "post" -> {
                    messageText.visibility = View.GONE
                    messageImage.visibility = View.GONE
                    postPreview.visibility = View.VISIBLE
                    // Load post preview
                    postPreview.setOnClickListener {
                        val intent = Intent(context, PostDetailActivity::class.java)
                        intent.putExtra("postId", message.postId)
                        context.startActivity(intent)
                    }
                }
            }

            timestamp.text = formatTime(message.timestamp)
            editedIndicator.visibility = if (message.isEdited) View.VISIBLE else View.GONE

            // Long press to edit/delete
            itemView.setOnLongClickListener {
                if (message.canEdit()) {
                    showEditDeleteDialog(message)
                } else {
                    Toast.makeText(context, "Can only edit/delete within 5 minutes", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val messageImage: ImageView = itemView.findViewById(R.id.messageImage)
        val postPreview: View = itemView.findViewById(R.id.postPreview)
        val timestamp: TextView = itemView.findViewById(R.id.timestamp)
        val editedIndicator: TextView = itemView.findViewById(R.id.editedIndicator)

        fun bind(message: ChatMessage) {
            if (message.isDeleted) {
                messageText.visibility = View.VISIBLE
                messageText.text = "Message deleted"
                messageText.setTextColor(context.getColor(android.R.color.darker_gray))
                messageImage.visibility = View.GONE
                postPreview.visibility = View.GONE
                editedIndicator.visibility = View.GONE
                return
            }

            when (message.messageType) {
                "text" -> {
                    messageText.visibility = View.VISIBLE
                    messageText.text = message.messageText
                    messageImage.visibility = View.GONE
                    postPreview.visibility = View.GONE
                }
                "image" -> {
                    messageText.visibility = View.GONE
                    messageImage.visibility = View.VISIBLE
                    postPreview.visibility = View.GONE
                    if (message.imageBase64.isNotEmpty()) {
                        ImageUtils.loadBase64Image(messageImage, message.imageBase64)
                    }
                }
                "post" -> {
                    messageText.visibility = View.GONE
                    messageImage.visibility = View.GONE
                    postPreview.visibility = View.VISIBLE
                    postPreview.setOnClickListener {
                        val intent = Intent(context, PostDetailActivity::class.java)
                        intent.putExtra("postId", message.postId)
                        context.startActivity(intent)
                    }
                }
            }

            timestamp.text = formatTime(message.timestamp)
            editedIndicator.visibility = if (message.isEdited) View.VISIBLE else View.GONE
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) {
            MESSAGE_TYPE_SENT
        } else {
            MESSAGE_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == MESSAGE_TYPE_SENT) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is SentMessageViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedMessageViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun showEditDeleteDialog(message: ChatMessage) {
        val options = arrayOf("Edit Message", "Delete Message", "Cancel")
        AlertDialog.Builder(context)
            .setTitle("Message Options")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> onEditMessage?.invoke(message)
                    1 -> onDeleteMessage?.invoke(message)
                }
            }
            .show()
    }
}
