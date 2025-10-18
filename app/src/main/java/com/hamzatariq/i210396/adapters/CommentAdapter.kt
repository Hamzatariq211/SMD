package com.hamzatariq.i210396.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hamzatariq.i210396.R
import com.hamzatariq.i210396.models.Comment
import com.hamzatariq.i210396.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val context: Context,
    private val comments: List<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val commentUserImage: ImageView = itemView.findViewById(R.id.commentUserImage)
        val commentUsername: TextView = itemView.findViewById(R.id.commentUsername)
        val commentText: TextView = itemView.findViewById(R.id.commentText)
        val commentTime: TextView = itemView.findViewById(R.id.commentTime)
        val btnLikeComment: ImageView = itemView.findViewById(R.id.btnLikeComment)

        fun bind(comment: Comment) {
            // Load user profile image
            if (comment.userProfileImage.isNotEmpty()) {
                ImageUtils.loadBase64Image(commentUserImage, comment.userProfileImage)
            }

            // Set username and comment text
            commentUsername.text = comment.username
            commentText.text = comment.commentText

            // Format timestamp
            commentTime.text = getTimeAgo(comment.timestamp)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m"
            diff < 86400000 -> "${diff / 3600000}h"
            diff < 604800000 -> "${diff / 86400000}d"
            else -> "${diff / 604800000}w"
        }
    }
}

