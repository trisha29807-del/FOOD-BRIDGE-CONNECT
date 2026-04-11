package com.example.foodbridge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.*

class FoodListingAdapter(
    private var listings: MutableList<DocumentSnapshot>,
    private val onClaimClick: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<FoodListingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFoodName: TextView = view.findViewById(R.id.tvFoodName)
        val tvDonorName: TextView = view.findViewById(R.id.tvDonorName)
        val tvFoodTypeBadge: TextView = view.findViewById(R.id.tvFoodTypeBadge)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvExpiry: TextView = view.findViewById(R.id.tvExpiry)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val btnClaim: MaterialButton = view.findViewById(R.id.btnClaim)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = listings[position]

        holder.tvFoodName.text = doc.getString("foodName") ?: "Unnamed"
        holder.tvDonorName.text = "By: ${doc.getString("donorName") ?: "Unknown"}"
        holder.tvFoodTypeBadge.text = doc.getString("foodType") ?: "Food"
        holder.tvQuantity.text = "Qty: ${doc.getString("quantity") ?: "-"}"
        holder.tvLocation.text = "📍 ${doc.getString("location") ?: "Unknown"}"
        holder.tvDescription.text = doc.getString("description") ?: ""

        // Format expiry date
        val expiryTs = doc.getTimestamp("expiryDate")
        if (expiryTs != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            holder.tvExpiry.text = "⏰ Expires: ${sdf.format(expiryTs.toDate())}"
        } else {
            holder.tvExpiry.text = "Expiry: Not specified"
        }

        holder.btnClaim.setOnClickListener {
            onClaimClick(doc)
        }
    }

    override fun getItemCount() = listings.size

    /** Replace full list (used when filter chip changes or search runs) */
    fun submitList(newList: MutableList<DocumentSnapshot>) {
        listings = newList
        notifyDataSetChanged()
    }

    /** Remove a single item after it has been claimed */
    fun removeItem(docId: String) {
        val index = listings.indexOfFirst { it.id == docId }
        if (index != -1) {
            listings.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
