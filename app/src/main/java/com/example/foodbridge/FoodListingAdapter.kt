package com.example.foodbridge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.*

class FoodListingAdapter(
    private val listings: MutableList<DocumentSnapshot>,
    private val onClaim: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<FoodListingAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFoodName:   TextView      = view.findViewById(R.id.tvFoodName)
        val tvFoodType:   TextView      = view.findViewById(R.id.tvFoodType)
        val tvDonorName:  TextView      = view.findViewById(R.id.tvDonorName)
        val tvLocation:   TextView      = view.findViewById(R.id.tvLocation)
        val tvQuantity:   TextView      = view.findViewById(R.id.tvQuantity)
        val tvExpiry:     TextView      = view.findViewById(R.id.tvExpiry)
        val tvDescription:TextView      = view.findViewById(R.id.tvDescription)
        val btnClaim:     MaterialButton= view.findViewById(R.id.btnClaim)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = listings[position]

        holder.tvFoodName.text  = doc.getString("foodName")  ?: "Unknown"
        holder.tvFoodType.text  = doc.getString("foodType")  ?: ""
        holder.tvDonorName.text = doc.getString("donorName") ?: "Anonymous"
        holder.tvLocation.text  = doc.getString("location")  ?: "Location not set"
        holder.tvQuantity.text  = doc.getString("quantity")  ?: ""

        // Format expiry date
        val expiry = doc.getTimestamp("expiryDate")
        if (expiry != null) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            holder.tvExpiry.text = sdf.format(expiry.toDate())
        } else {
            holder.tvExpiry.text = "N/A"
        }

        // Show description if available
        val desc = doc.getString("description")
        if (!desc.isNullOrEmpty()) {
            holder.tvDescription.text = desc
            holder.tvDescription.visibility = View.VISIBLE
        } else {
            holder.tvDescription.visibility = View.GONE
        }

        holder.btnClaim.setOnClickListener {
            holder.btnClaim.isEnabled = false
            holder.btnClaim.text = "Claiming..."
            onClaim(doc)
        }
    }

    override fun getItemCount(): Int = listings.size

    // Called from BrowseActivity after filter/search
    fun submitList(newList: MutableList<DocumentSnapshot>) {
        listings.clear()
        listings.addAll(newList)
        notifyDataSetChanged()
    }
}
