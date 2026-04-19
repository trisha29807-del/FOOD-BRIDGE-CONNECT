package com.example.foodbridge

import android.content.Context
import androidx.work.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class ExpiryCheckWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db  = FirebaseFirestore.getInstance()
            val now = Timestamp.now()

            val snapshot = db.collection("food_listings")
                .whereEqualTo("status", "available")
                .get().await()

            val batch = db.batch()
            var count = 0
            snapshot.documents.forEach { doc ->
                val expiry = doc.getTimestamp("expiryDate")
                if (expiry != null && expiry.compareTo(now) < 0) {
                    batch.update(doc.reference, "status", "expired")
                    count++
                }
            }
            if (count > 0) batch.commit().await()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        // Call this from MainActivity onCreate to schedule hourly checks
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
                1, TimeUnit.HOURS
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "expiry_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
