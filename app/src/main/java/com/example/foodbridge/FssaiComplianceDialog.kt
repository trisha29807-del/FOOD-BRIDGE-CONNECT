package com.example.foodbridge

import android.content.Context
import androidx.appcompat.app.AlertDialog

object FssaiComplianceDialog {

    // Call this before submitting food listing
    // onConfirmed is called only if user checks all boxes
    fun show(context: Context, onConfirmed: () -> Unit) {
        val checks = arrayOf(
            "✅  Food is freshly prepared or properly stored",
            "📦  Food is packed in clean, food-grade containers",
            "🌡️  Temperature has been maintained (below 5°C or above 60°C)",
            "🧴  Hands were washed before handling food",
            "⚠️  Common allergens are labeled (nuts, dairy, gluten)",
            "🏷️  Expiry/best-before date is accurate"
        )

        val checkedItems = BooleanArray(checks.size) { false }

        AlertDialog.Builder(context)
            .setTitle("FSSAI Food Safety Checklist")
            .setMessage("Please confirm all safety standards before listing:")
            .setMultiChoiceItems(checks, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Confirm & Post") { _, _ ->
                if (checkedItems.all { it }) {
                    onConfirmed()
                } else {
                    AlertDialog.Builder(context)
                        .setTitle("Incomplete Checklist")
                        .setMessage("Please confirm all food safety standards before listing.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
