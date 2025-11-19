package com.devs.i210396_i211384.services

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.devs.i210396_i211384.models.CallRequest

object CallService {
    private val database = FirebaseDatabase.getInstance()

    fun initiateCall(
        callRequest: CallRequest,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        database.reference
            .child("calls")
            .child(callRequest.callId)
            .setValue(callRequest)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Failed to initiate call") }
    }

    fun getCallData(callId: String, onComplete: (CallRequest?) -> Unit) {
        database.reference
            .child("calls")
            .child(callId)
            .get()
            .addOnSuccessListener { snapshot ->
                val call = snapshot.getValue(CallRequest::class.java)
                onComplete(call)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    fun listenForIncomingCalls(
        userId: String,
        onCallReceived: (CallRequest) -> Unit
    ) {
        database.reference
            .child("calls")
            .orderByChild("receiverId")
            .equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (callSnapshot in snapshot.children) {
                        val call = callSnapshot.getValue(CallRequest::class.java)
                        if (call != null && call.status == "ringing") {
                            onCallReceived(call)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    fun updateCallStatus(
        callId: String,
        status: String,
        onComplete: () -> Unit = {}
    ) {
        database.reference
            .child("calls")
            .child(callId)
            .child("status")
            .setValue(status)
            .addOnSuccessListener { onComplete() }
    }

    fun endCall(callId: String, onComplete: () -> Unit = {}) {
        database.reference
            .child("calls")
            .child(callId)
            .removeValue()
            .addOnSuccessListener { onComplete() }
    }

    fun listenToCallStatus(
        callId: String,
        onStatusChanged: (String) -> Unit
    ) {
        database.reference
            .child("calls")
            .child(callId)
            .child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(String::class.java)
                    if (status != null) {
                        onStatusChanged(status)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
}
