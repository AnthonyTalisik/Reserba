package com.example.reserba.ui

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.reserba.R
import com.example.reserba.model.ReservationStatus

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/** Dialog that asks for required remarks (used for rejections and admin cancellations). */
fun Context.promptRemarks(title: String, hint: String, onSubmit: (String) -> Unit) {
    val input = EditText(this).apply {
        this.hint = hint
        minLines = 2
        setPadding(56, 32, 56, 32)
    }
    AlertDialog.Builder(this)
        .setTitle(title)
        .setView(input)
        .setPositiveButton("Submit") { _, _ ->
            val text = input.text.toString().trim()
            if (text.isEmpty()) toast("Remarks are required.") else onSubmit(text)
        }
        .setNegativeButton("Back", null)
        .show()
}

/** Colors a status badge TextView according to the reservation status. */
fun TextView.applyStatus(status: ReservationStatus) {
    text = status.name.lowercase().replaceFirstChar { it.uppercase() }
    val (bg, fg) = when (status) {
        ReservationStatus.PENDING -> R.drawable.bg_badge_amber to R.color.amber
        ReservationStatus.APPROVED -> R.drawable.bg_badge_green to R.color.green
        ReservationStatus.REJECTED, ReservationStatus.CANCELLED -> R.drawable.bg_badge_red to R.color.red
        ReservationStatus.COMPLETED -> R.drawable.bg_badge_blue to R.color.primary
    }
    setBackgroundResource(bg)
    setTextColor(ContextCompat.getColor(context, fg))
}
