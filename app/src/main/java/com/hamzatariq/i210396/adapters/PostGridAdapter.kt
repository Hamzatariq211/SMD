package com.hamzatariq.i210396.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.hamzatariq.i210396.PostDetailActivity
import com.hamzatariq.i210396.R
import com.hamzatariq.i210396.models.Post
import com.hamzatariq.i210396.utils.ImageUtils

class PostGridAdapter(
    private val context: Context,
    private val posts: List<Post>,
    private val onPostClick: (Post) -> Unit
) : RecyclerView.Adapter<PostGridAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.postImage)

        fun bind(post: Post) {
            // Load post image from Base64
            if (post.postImageBase64.isNotEmpty()) {
                ImageUtils.loadBase64Image(postImage, post.postImageBase64)
            }

            // Click listener - open PostDetailActivity
            itemView.setOnClickListener {
                val intent = Intent(context, PostDetailActivity::class.java)
                intent.putExtra("postId", post.postId)
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post_grid, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position])
    }

    override fun getItemCount(): Int = posts.size
}
