package com.devs.i210396_i211384.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.devs.i210396_i211384.R
import com.devs.i210396_i211384.SociallyOtherUser
import com.devs.i210396_i211384.chatScreen
import com.devs.i210396_i211384.network.ApiService
import com.devs.i210396_i211384.network.FollowRequest
import com.devs.i210396_i211384.network.UserListItem
import com.devs.i210396_i211384.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserListAdapter(
    private val context: Context,
    private val users: MutableList<UserListItem>
) : RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {

    private val apiService = ApiService.create()

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.userProfileImage)
        val username: TextView = itemView.findViewById(R.id.userUsername)
        val fullName: TextView = itemView.findViewById(R.id.userFullName)
        val followButton: Button = itemView.findViewById(R.id.followButton)
        val messageButton: ImageView = itemView.findViewById(R.id.messageButton)
        val onlineIndicator: View? = itemView.findViewById(R.id.onlineIndicator)

        fun bind(user: UserListItem) {
            username.text = user.username
            val fullNameText = "${user.firstName ?: ""} ${user.lastName ?: ""}".trim()
            fullName.text = if (fullNameText.isNotEmpty()) fullNameText else user.username

            // Load profile image
            if (!user.profileImageUrl.isNullOrEmpty()) {
                ImageUtils.loadBase64Image(profileImage, user.profileImageUrl)
            } else {
                profileImage.setImageResource(R.drawable.profile)
            }

            // Show/hide online indicator based on user status
            onlineIndicator?.visibility = if (user.isOnline) View.VISIBLE else View.GONE

            // Update follow button state
            updateFollowButton(user)

            // Click to open user profile
            itemView.setOnClickListener {
                val intent = Intent(context, SociallyOtherUser::class.java)
                intent.putExtra("userId", user.id)
                intent.putExtra("username", user.username)
                context.startActivity(intent)
            }

            // Follow button click
            followButton.setOnClickListener {
                toggleFollow(user)
            }

            // Message button click - open chat
            messageButton.setOnClickListener {
                val intent = Intent(context, chatScreen::class.java)
                intent.putExtra("userId", user.id)
                intent.putExtra("username", user.username)
                intent.putExtra("profileImageUrl", user.profileImageUrl ?: "")
                context.startActivity(intent)
            }
        }

        private fun updateFollowButton(user: UserListItem) {
            when {
                user.isFollowing -> {
                    followButton.text = "Following"
                    followButton.setBackgroundResource(R.drawable.button_background_secondary)
                    followButton.setTextColor(context.getColor(android.R.color.black))
                }
                user.hasPendingRequest -> {
                    followButton.text = "Requested"
                    followButton.setBackgroundResource(R.drawable.button_background_secondary)
                    followButton.setTextColor(context.getColor(android.R.color.black))
                }
                else -> {
                    followButton.text = "Follow"
                    followButton.setBackgroundResource(R.drawable.button_background)
                    followButton.setTextColor(context.getColor(android.R.color.white))
                }
            }
        }

        private fun toggleFollow(user: UserListItem) {
            (context as? LifecycleOwner)?.lifecycleScope?.launch {
                try {
                    val response = if (user.isFollowing) {
                        // Unfollow
                        withContext(Dispatchers.IO) {
                            apiService.unfollowUser(FollowRequest(user.id))
                        }
                    } else {
                        // Follow
                        withContext(Dispatchers.IO) {
                            apiService.followUser(FollowRequest(user.id))
                        }
                    }

                    if (response.isSuccessful) {
                        val message = response.body()?.get("message") ?: "Success"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                        // Update local state
                        val index = users.indexOf(user)
                        if (index != -1) {
                            if (user.isFollowing) {
                                // Was following, now unfollowed
                                users[index] = user.copy(
                                    isFollowing = false,
                                    hasPendingRequest = false,
                                    followersCount = user.followersCount - 1
                                )
                            } else {
                                // Was not following, now following or requested
                                if (user.isPrivate) {
                                    // Private account - request sent
                                    users[index] = user.copy(
                                        hasPendingRequest = true
                                    )
                                } else {
                                    // Public account - directly followed
                                    users[index] = user.copy(
                                        isFollowing = true,
                                        followersCount = user.followersCount + 1
                                    )
                                }
                            }
                            notifyItemChanged(index)
                        }
                    } else {
                        Toast.makeText(context, "Failed to update follow status", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user_list, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    // Add method to update the list
    fun updateList(newUsers: List<UserListItem>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }
}
