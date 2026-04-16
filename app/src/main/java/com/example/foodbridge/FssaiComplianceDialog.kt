package com.example.foodbridge

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object FssaiComplianceDialog {

    fun show(context: Context, onConfirmed: () -> Unit) {
        val checks = arrayOf(
            "Food is freshly prepared",
            "Packed in food-grade containers",
            "Temperature maintained",
            "Hands washed/Hygiene kept",
            "Allergens labeled",
            "Expiry date is accurate"
        )

        val checkedItems = BooleanArray(checks.size) { false }

        MaterialAlertDialogBuilder(context)
            .setTitle("Confirm Safety Standards")
            // Removed setMessage to ensure list visibility
            .setMultiChoiceItems(checks, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Confirm & Post") { dialog, _ ->
                if (checkedItems.all { it }) {
                    onConfirmed()
                } else {
                    // Show a simple error toast or nested alert
                    showError(context)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showError(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Incomplete")
            .setMessage("You must check all boxes to proceed.")
            .setPositiveButton("OK", null)
            .show()
    }
}