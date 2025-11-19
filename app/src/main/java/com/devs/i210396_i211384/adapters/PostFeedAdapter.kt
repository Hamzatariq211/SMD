package com.devs.i210396_i211384.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.devs.i210396_i211384.PostDetailActivity
import com.devs.i210396_i211384.R
import com.devs.i210396_i211384.models.Post
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.SessionManager
import com.devs.i210396_i211384.utils.ImageUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostFeedAdapter(
    private val context: Context,
    private val posts: MutableList<Post>
) : RecyclerView.Adapter<PostFeedAdapter.PostViewHolder>() {

    private val apiService = ApiService.create()

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

            // Set like icon state based on MySQL data
            val isLiked = post.isLiked
            likeIcon.setImageResource(if (isLiked) R.drawable.likebold else R.drawable.like)

            // Set likes text using likeCount from MySQL
            val likeCount = post.likeCount
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
            // Get current user ID from session
            val currentUserId = SessionManager.getUserId() ?: return

            // Call MySQL API to toggle like
            (context as? LifecycleOwner)?.lifecycleScope?.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        apiService.likePost(com.devs.i210396_i211384.network.LikePostRequest(post.postId))
                    }

                    if (response.isSuccessful) {
                        // Toggle like state locally
                        val newLikeState = !post.isLiked
                        val newLikeCount = if (newLikeState) post.likeCount + 1 else post.likeCount - 1

                        // Update post object
                        val index = posts.indexOf(post)
                        if (index != -1) {
                            posts[index] = post.copy(
                                isLiked = newLikeState,
                                likeCount = newLikeCount
                            )
                        }

                        // Update UI
                        likeIcon.setImageResource(if (newLikeState) R.drawable.likebold else R.drawable.like)
                        likesText.text = when {
                            newLikeCount == 0 -> "Be the first to like this"
                            newLikeCount == 1 -> "Liked by 1 person"
                            else -> "Liked by $newLikeCount people"
                        }
                    }
                } catch (e: Exception) {
                    // Handle error silently or show toast
                }
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
