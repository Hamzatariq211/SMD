package com.hamzatariq.i210396.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.hamzatariq.i210396.PostDetailActivity
import com.hamzatariq.i210396.R
import com.hamzatariq.i210396.models.Post
import com.hamzatariq.i210396.utils.ImageUtils

class PostFeedAdapter(
    private val context: Context,
    private val posts: MutableList<Post>
) : RecyclerView.Adapter<PostFeedAdapter.PostViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profilePic: ImageView = itemView.findViewById(R.id.profile_pic)
        val username: TextView = itemView.findViewById(R.id.username)
        val postImage: ImageView = itemView.findViewById(R.id.post_image)
        val likeIcon: ImageView = itemView.findViewById(R.id.like_icon)
        val commentIcon: ImageView = itemView.findViewById(R.id.comment_icon)
        val shareIcon: ImageView = itemView.findViewById(R.id.share_icon)
        val saveIcon: ImageView = itemView.findViewById(R.id.save_icon)
        val likesText: TextView = itemView.findViewById(R.id.likes_text)
        val captionText: TextView = itemView.findViewById(R.id.caption_text)

        fun bind(post: Post) {
            // Load user profile image
            if (post.userProfileImage.isNotEmpty()) {
                ImageUtils.loadBase64Image(profilePic, post.userProfileImage)
            }

            // Set username
            username.text = post.username

            // Load post image
            if (post.postImageBase64.isNotEmpty()) {
                ImageUtils.loadBase64Image(postImage, post.postImageBase64)
            }

            // Set like icon state
            val currentUserId = auth.currentUser?.uid ?: ""
            val isLiked = post.isLikedByUser(currentUserId)
            likeIcon.setImageResource(if (isLiked) R.drawable.likebold else R.drawable.like)

            // Set likes text
            val likeCount = post.getLikeCount()
            likesText.text = when {
                likeCount == 0 -> "Be the first to like this"
                likeCount == 1 -> "Liked by 1 person"
                else -> "Liked by $likeCount people"
            }

            // Set caption
            if (post.caption.isNotEmpty()) {
                captionText.visibility = View.VISIBLE
                captionText.text = "${post.username} ${post.caption}"
            } else {
                captionText.visibility = View.GONE
            }

            // Like button click
            likeIcon.setOnClickListener {
                toggleLike(post)
            }

            // Comment button click
            commentIcon.setOnClickListener {
                openPostDetail(post)
            }

            // Share button click
            shareIcon.setOnClickListener {
                // TODO: Implement share functionality
            }

            // Save button click
            saveIcon.setOnClickListener {
                // TODO: Implement save functionality
            }

            // Post image click - open detail
            postImage.setOnClickListener {
                openPostDetail(post)
            }
        }

        private fun toggleLike(post: Post) {
            val currentUserId = auth.currentUser?.uid ?: return
            val postRef = database.reference.child("posts").child(post.postId).child("likes")

            if (post.isLikedByUser(currentUserId)) {
                // Unlike
                postRef.child(currentUserId).removeValue()
                post.likes.remove(currentUserId)
                likeIcon.setImageResource(R.drawable.like)
            } else {
                // Like
                postRef.child(currentUserId).setValue(true)
                post.likes[currentUserId] = true
                likeIcon.setImageResource(R.drawable.likebold)
            }

            // Update likes text
            val likeCount = post.getLikeCount()
            likesText.text = when {
                likeCount == 0 -> "Be the first to like this"
                likeCount == 1 -> "Liked by 1 person"
                else -> "Liked by $likeCount people"
            }
        }

        private fun openPostDetail(post: Post) {
            val intent = Intent(context, PostDetailActivity::class.java)
            intent.putExtra("postId", post.postId)
            context.startActivity(intent)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post_feed, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size
}

