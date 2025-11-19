package com.devs.i210396_i211384

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.devs.i210396_i211384.adapters.FollowRequestAdapter
import com.devs.i210396_i211384.models.FollowRequest

class FollowRequestsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var followRequestAdapter: FollowRequestAdapter
    private lateinit var emptyView: TextView

    private val followRequestsList = mutableListOf<FollowRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_follow_requests)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        followRequestAdapter = FollowRequestAdapter(
            this,
            followRequestsList,
            onAccept = { request -> acceptFollowRequest(request) },
            onReject = { request -> rejectFollowRequest(request) }
        )
        recyclerView.adapter = followRequestAdapter

        // Back button
        findViewById<ImageView>(R.id.backIcon).setOnClickListener {
            finish()
        }

        // Load follow requests
        loadFollowRequests()
    }

    private fun loadFollowRequests() {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(currentUserId)
            .collection("followRequests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                followRequestsList.clear()

                snapshot?.documents?.forEach { doc ->
                    val request = doc.toObject(FollowRequest::class.java)
                    request?.let {
                        followRequestsList.add(it.copy(requestId = doc.id))
                    }
                }

                followRequestAdapter.notifyDataSetChanged()

                // Show/hide empty view
                if (followRequestsList.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
    }

    private fun acceptFollowRequest(request: FollowRequest) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Add to followers/following
        val followData = hashMapOf(
            "userId" to request.fromUserId,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("users").document(currentUserId)
            .collection("followers")
            .document(request.fromUserId)
            .set(followData)

        val followingData = hashMapOf(
            "userId" to currentUserId,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("users").document(request.fromUserId)
            .collection("following")
            .document(currentUserId)
            .set(followingData)

        // Update request status
        firestore.collection("users").document(currentUserId)
            .collection("followRequests")
            .document(request.fromUserId)
            .update("status", "accepted")
            .addOnSuccessListener {
                // Remove from list
                followRequestsList.remove(request)
                followRequestAdapter.notifyDataSetChanged()
            }
    }

    private fun rejectFollowRequest(request: FollowRequest) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Delete the follow request
        firestore.collection("users").document(currentUserId)
            .collection("followRequests")
            .document(request.fromUserId)
            .delete()
            .addOnSuccessListener {
                // Remove from list
                followRequestsList.remove(request)
                followRequestAdapter.notifyDataSetChanged()
            }
    }
}

