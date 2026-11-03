package com.example.reserba.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reserba.R
import com.example.reserba.adapter.AdminRequestAdapter
import com.example.reserba.data.MockData
import com.example.reserba.databinding.FragmentAdminRequestsBinding
import com.example.reserba.model.Reservation
import com.example.reserba.model.ReservationStatus
import com.example.reserba.util.TimeUtils

/** Pending Requests (Admin): queue with room / date / status filters and overlap flags (FR-06). */
class AdminRequestsFragment : Fragment() {

    private var _b: FragmentAdminRequestsBinding? = null
    private val b get() = _b!!

    private lateinit var adapter: AdminRequestAdapter
    private var spinnerRoomCount = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentAdminRequestsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminRequestAdapter(
            onReview = { r ->
                startActivity(
                    Intent(requireContext(), ReviewActivity::class.java)
                        .putExtra(Extras.RESERVATION_ID, r.reservationId)
                )
            },
            onReject = { r -> rejectWithRemarks(r) }
        )
        b.rvRequests.layoutManager = LinearLayoutManager(requireContext())
        b.rvRequests.adapter = adapter

        b.spRoom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = refresh()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        b.chipGroupStatus.setOnCheckedStateChangeListener { _, _ -> refresh() }
        b.chipToday.setOnCheckedChangeListener { _, _ -> refresh() }
    }

    override fun onResume() {
        super.onResume()
        setupRoomSpinnerIfNeeded()
        refresh()
    }

    private fun setupRoomSpinnerIfNeeded() {
        if (spinnerRoomCount == MockData.rooms.size) return
        spinnerRoomCount = MockData.rooms.size
        val names = listOf("All rooms") + MockData.rooms.map { it.name }
        b.spRoom.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
    }

    private fun refresh() {
        if (_b == null) return
        MockData.refreshStatuses()

        val pendingAll = MockData.reservations.filter { it.status == ReservationStatus.PENDING }
        b.tvWaiting.text = pendingAll.size.toString()
        b.tvOverlapping.text = pendingAll.count { MockData.overlappingPending(it).isNotEmpty() }.toString()

        val roomPos = b.spRoom.selectedItemPosition
        val roomId = if (roomPos <= 0) null else MockData.rooms.getOrNull(roomPos - 1)?.roomId
        val status = when (b.chipGroupStatus.checkedChipId) {
            R.id.chipPending -> ReservationStatus.PENDING
            R.id.chipApproved -> ReservationStatus.APPROVED
            else -> null
        }
        val today = TimeUtils.dateOffset(0)

        val list = MockData.reservations.filter {
            (roomId == null || it.roomId == roomId) &&
                (status == null || it.status == status) &&
                (!b.chipToday.isChecked || it.date == today)
        }.sortedBy { it.startMillis }

        adapter.submit(list)
        b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun rejectWithRemarks(r: Reservation) {
        val admin = MockData.currentUser ?: return
        requireContext().promptRemarks("Reject request", "Reason for rejection (required)") { remarks ->
            val error = MockData.reject(r.reservationId, remarks, admin)
            requireContext().toast(error ?: "Request rejected.")
            refresh()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
        spinnerRoomCount = -1
    }
}
