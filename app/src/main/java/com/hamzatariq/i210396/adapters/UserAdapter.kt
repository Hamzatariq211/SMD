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
import com.hamzatariq.i210396.SociallyOtherUser
import com.hamzatariq.i210396.chatScreen
import com.hamzatariq.i210396.models.User
import com.hamzatariq.i210396.utils.ImageUtils
import com.hamzatariq.i210396.utils.OnlineStatusManager

class UserAdapter(
    private val context: Context,
    private val users: MutableList<User>
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.userProfileImage)
        val username: TextView = itemView.findViewById(R.id.userUsername)
        val fullName: TextView = itemView.findViewById(R.id.userFullName)
        val messageButton: ImageView = itemView.findViewById(R.id.messageButton)
        val onlineIndicator: View? = itemView.findViewById(R.id.onlineIndicator)

        fun bind(user: User) {
            username.text = user.username
            fullName.text = "${user.firstName} ${user.lastName}"

            // Load profile image
            if (user.profileImageUrl.isNotEmpty()) {
                ImageUtils.loadBase64Image(profileImage, user.profileImageUrl)
            } else {
                profileImage.setImageResource(R.drawable.profile)
            }

            // Show/hide online indicator based on user status
            onlineIndicator?.visibility = if (user.isOnline) View.VISIBLE else View.GONE

            // Listen to real-time status updates
            OnlineStatusManager.listenToUserStatus(user.uid) { isOnline, _ ->
                onlineIndicator?.visibility = if (isOnline) View.VISIBLE else View.GONE
            }

            // Click to open user profile
            itemView.setOnClickListener {
                val intent = Intent(context, SociallyOtherUser::class.java)
                intent.putExtra("userId", user.uid)
                intent.putExtra("username", user.username)
                intent.putExtra("firstName", user.firstName)
                intent.putExtra("lastName", user.lastName)
                intent.putExtra("profileImageUrl", user.profileImageUrl)
                intent.putExtra("bio", user.bio)
                context.startActivity(intent)
            }

            // Message button click - open chat
            messageButton.setOnClickListener {
                val intent = Intent(context, chatScreen::class.java)
                intent.putExtra("userId", user.uid)
                intent.putExtra("username", user.username)
                intent.putExtra("profileImageUrl", user.profileImageUrl)
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }
}
