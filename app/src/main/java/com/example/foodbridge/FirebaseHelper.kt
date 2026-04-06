package com.example.foodbridge

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object FirebaseHelper {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ─── Auth ────────────────────────────────────────────────────────────────

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUid: String?
        get() = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        role: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user!!

            val userDoc = mapOf(
                "name"      to name,
                "email"     to email,
                "role"      to role,
                "location"  to "",
                "contact"   to "",
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            db.collection("users").document(user.uid).set(userDoc).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() = auth.signOut()

    // ─── User Profile ─────────────────────────────────────────────────────────

    suspend fun getUserProfile(uid: String): Result<Map<String, Any>> {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                Result.success(doc.data ?: emptyMap())
            } else {
                Result.failure(Exception("User profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Food Listings ────────────────────────────────────────────────────────

    suspend fun postFoodListing(listing: Map<String, Any>): Result<String> {
        return try {
            val docRef = db.collection("food_listings").add(listing).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableListings(): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = db.collection("food_listings")
                .whereEqualTo("status", "available")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val listings = snapshot.documents.map { doc ->
                (doc.data ?: emptyMap()) + mapOf("id" to doc.id)
            }
            Result.success(listings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyListings(uid: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = db.collection("food_listings")
                .whereEqualTo("donorUid", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val listings = snapshot.documents.map { doc ->
                (doc.data ?: emptyMap()) + mapOf("id" to doc.id)
            }
            Result.success(listings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
