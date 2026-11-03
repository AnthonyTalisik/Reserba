package com.example.reserba.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.reserba.R
import com.example.reserba.data.MockData
import com.example.reserba.databinding.ActivityReviewBinding
import com.example.reserba.model.Reservation
import com.example.reserba.model.ReservationStatus

class ReviewActivity : AppCompatActivity() {

    private lateinit var b: ActivityReviewBinding
    private lateinit var reservation: Reservation

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(b.root)

        val r = MockData.reservationById(intent.getStringExtra(Extras.RESERVATION_ID) ?: "")
        if (r == null || MockData.currentUser == null) {
            finish()
            return
        }
        reservation = r

        b.btnBack.setOnClickListener { finish() }
        b.btnApprove.setOnClickListener { approve() }
        b.btnReject.setOnClickListener { reject() }
        b.btnCancelRes.setOnClickListener { cancelReservation() }

        bindDetails()
    }

    private fun bindDetails() {
        val r = reservation
        val room = MockData.roomById(r.roomId)

        b.tvRvRequester.text = "${r.organization}  -  ${r.requesterName}"
        b.tvRvStatus.applyStatus(r.status)
        b.tvRvRoom.text = "Room: ${r.roomName}"
        b.tvRvWhen.text = "When: ${com.example.reserba.util.TimeUtils.formatDate(r.date)}, " +
            com.example.reserba.util.TimeUtils.formatRange(r.startTime, r.endTime)
        b.tvRvPeople.text = "People: ${r.attendees} of ${room?.capacity ?: "?"} seats"
        b.tvRvPurpose.text = "Purpose: ${r.purpose}"
        b.tvRvEquipment.text = "Equipment: " + r.equipmentNeeded.ifEmpty { listOf("None") }.joinToString(", ")

        val pending = r.status == ReservationStatus.PENDING
        val approved = r.status == ReservationStatus.APPROVED
        val notStarted = System.currentTimeMillis() < r.startMillis

        b.rowDecision.visibility = if (pending) View.VISIBLE else View.GONE
        b.btnCancelRes.visibility = if (approved && notStarted) View.VISIBLE else View.GONE
        b.tilRemarks.visibility = if ((pending || approved) && notStarted) View.VISIBLE else View.GONE

        if (!pending) {
            b.tvConflictResult.visibility = View.GONE
            b.tvOverlapWarning.visibility = View.GONE
            if (r.adminRemarks.isNotBlank()) {
                b.tvError.text = "Remarks: ${r.adminRemarks}"
                b.tvError.visibility = View.VISIBLE
            }
            return
        }

        // Conflict check result (BR-01, BR-10)
        val block = MockData.findBlockConflict(r.roomId, r.startMillis, r.endMillis)
        val clash = MockData.findApprovedConflict(r.roomId, r.startMillis, r.endMillis, r.reservationId)
        b.tvConflictResult.visibility = View.VISIBLE
        when {
            block != null -> showConflictResult(
                false, "Blocked date: ${block.reason}. This request cannot be approved."
            )
            clash != null -> showConflictResult(
                false,
                "Clashes with an approved reservation by ${clash.organization} " +
                    "(${com.example.reserba.util.TimeUtils.formatRange(clash.startTime, clash.endTime)}). " +
                    "This request cannot be approved."
            )
            else -> showConflictResult(
                true,
                "No clash with approved bookings. Checked approved reservations, blocked dates and the " +
                    "${room?.bufferMinutes ?: 20}-minute gap."
            )
        }

        // BR-08: warn that approving will auto-reject overlapping pending requests
        val others = MockData.overlappingPending(r)
        if (others.isEmpty()) {
            b.tvOverlapWarning.visibility = View.GONE
        } else {
            b.tvOverlapWarning.visibility = View.VISIBLE
            b.tvOverlapWarning.text = "Approving rejects ${others.size} other pending request(s) for the same time: " +
                others.joinToString(", ") { it.organization }
        }
    }

    private fun showConflictResult(ok: Boolean, text: String) {
        b.tvConflictResult.text = text
        b.tvConflictResult.setBackgroundResource(if (ok) R.drawable.bg_badge_green else R.drawable.bg_badge_red)
        b.tvConflictResult.setTextColor(ContextCompat.getColor(this, if (ok) R.color.green else R.color.red))
    }

    private fun remarks() = b.etRemarks.text.toString().trim()

    private fun showError(message: String) {
        b.tvError.text = message
        b.tvError.visibility = View.VISIBLE
    }

    private fun approve() {
        val admin = MockData.currentUser ?: return
        val error = MockData.approve(reservation.reservationId, remarks(), admin)
        if (error != null) {
            showError(error)
            return
        }
        toast("Approved. Overlapping pending requests were rejected.")
        finish()
    }

    private fun reject() {
        val admin = MockData.currentUser ?: return
        b.tilRemarks.error = null
        val error = MockData.reject(reservation.reservationId, remarks(), admin)
        if (error != null) {
            b.tilRemarks.error = error
            return
        }
        toast("Request rejected.")
        finish()
    }

    private fun cancelReservation() {
        val admin = MockData.currentUser ?: return
        b.tilRemarks.error = null
        val error = MockData.cancel(reservation.reservationId, admin, remarks())
        if (error != null) {
            b.tilRemarks.error = error
            return
        }
        toast("Reservation cancelled. The slot is available again.")
        finish()
    }
}
