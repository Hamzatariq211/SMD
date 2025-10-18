package com.hamzatariq.i210396.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hamzatariq.i210396.R
import com.hamzatariq.i210396.models.UserStoryCollection
import com.hamzatariq.i210396.utils.ImageUtils

class StoryAdapter(
    private val context: Context,
    private val userStories: List<UserStoryCollection>,
    private val onStoryClick: (UserStoryCollection) -> Unit
) : RecyclerView.Adapter<StoryAdapter.StoryViewHolder>() {

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val storyUserImage: ImageView = itemView.findViewById(R.id.storyUserImage)
        val storyUsername: TextView = itemView.findViewById(R.id.storyUsername)

        fun bind(userStory: UserStoryCollection) {
            // Load user profile image
            if (userStory.userProfileImage.isNotEmpty()) {
                ImageUtils.loadBase64Image(storyUserImage, userStory.userProfileImage)
            }

            // Set username
            storyUsername.text = userStory.username

            // Click listener
            itemView.setOnClickListener {
                onStoryClick(userStory)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(userStories[position])
    }

    override fun getItemCount(): Int = userStories.size
}
