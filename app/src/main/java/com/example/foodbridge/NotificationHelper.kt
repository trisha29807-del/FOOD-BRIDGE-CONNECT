package com.example.foodbridge

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object NotificationHelper {

    // Call this on login to save the device token
    fun saveFcmToken() {
        val uid = FirebaseHelper.currentUid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token)
        }
    }

    // Send notification when food is claimed (call from PlaceOrderActivity)
    suspend fun notifyDonorFoodClaimed(donorUid: String, foodName: String, buyerName: String) {
        try {
            val donorDoc = FirebaseFirestore.getInstance()
                .collection("users").document(donorUid).get().await()
            val token = donorDoc.getString("fcmToken") ?: return

            // Save notification to Firestore — a Cloud Function will send the push
            FirebaseFirestore.getInstance().collection("notifications").add(
                mapOf(
                    "toUid"     to donorUid,
                    "fcmToken"  to token,
                    "title"     to "Your food was claimed! 🎉",
                    "body"      to "$buyerName claimed your '$foodName' listing",
                    "type"      to "food_claimed",
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "sent"      to false
                )
            )
        } catch (e: Exception) { /* silent */ }
    }

    // Send notification when new food is listed (saved to Firestore for Cloud Function)
    fun notifyNewListing(foodName: String, location: String) {
        FirebaseFirestore.getInstance().collection("notifications").add(
            mapOf(
                "toAll"     to true,
                "title"     to "New food available nearby! 🍱",
                "body"      to "$foodName available at $location",
                "type"      to "new_listing",
                "createdAt" to com.google.firebase.Timestamp.now(),
                "sent"      to false
            )
        )
    }
}
