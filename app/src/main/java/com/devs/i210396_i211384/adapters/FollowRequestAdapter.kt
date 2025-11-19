package com.devs.i210396_i211384.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.devs.i210396_i211384.R
import com.devs.i210396_i211384.models.FollowRequest
import com.devs.i210396_i211384.utils.ImageUtils

class FollowRequestAdapter(
    private val context: Context,
    private val requests: MutableList<FollowRequest>,
    private val onAccept: (FollowRequest) -> Unit,
    private val onReject: (FollowRequest) -> Unit
) : RecyclerView.Adapter<FollowRequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val username: TextView = itemView.findViewById(R.id.username)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)

        fun bind(request: FollowRequest) {
            username.text = request.fromUsername

            // Load profile image
            if (request.fromProfileImageUrl.isNotEmpty()) {
                ImageUtils.loadBase64Image(profileImage, request.fromProfileImageUrl)
            } else {
                profileImage.setImageResource(R.drawable.profile)
            }

            // Accept button
            btnAccept.setOnClickListener {
                onAccept(request)
            }

            // Reject button
            btnReject.setOnClickListener {
                onReject(request)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_follow_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount(): Int = requests.size
}

