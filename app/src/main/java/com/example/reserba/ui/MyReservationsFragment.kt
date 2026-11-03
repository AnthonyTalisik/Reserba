package com.example.reserba.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reserba.adapter.ReservationAdapter
import com.example.reserba.data.MockData
import com.example.reserba.databinding.FragmentMyReservationsBinding
import com.example.reserba.model.Reservation
import com.example.reserba.model.ReservationStatus
import com.google.android.material.tabs.TabLayout

/** My Reservations: Pending / Approved / History tabs with cancellation (FR-08, FR-09). */
class MyReservationsFragment : Fragment() {

    private var _b: FragmentMyReservationsBinding? = null
    private val b get() = _b!!

    private lateinit var adapter: ReservationAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentMyReservationsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ReservationAdapter { confirmCancel(it) }
        b.rvReservations.layoutManager = LinearLayoutManager(requireContext())
        b.rvReservations.adapter = adapter

        b.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) = refresh()
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        if (_b == null) return
        val user = MockData.currentUser ?: return
        MockData.refreshStatuses()

        val mine = MockData.reservations.filter { it.requesterId == user.uid }
        val list: List<Reservation> = when (b.tabs.selectedTabPosition) {
            0 -> mine.filter { it.status == ReservationStatus.PENDING }.sortedBy { it.startMillis }
            1 -> mine.filter { it.status == ReservationStatus.APPROVED }.sortedBy { it.startMillis }
            else -> mine.filter {
                it.status == ReservationStatus.REJECTED ||
                    it.status == ReservationStatus.CANCELLED ||
                    it.status == ReservationStatus.COMPLETED
            }.sortedByDescending { it.startMillis }
        }
        adapter.submit(list)
        b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmCancel(r: Reservation) {
        val user = MockData.currentUser ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel reservation?")
            .setMessage("${r.roomName} on ${com.example.reserba.util.TimeUtils.formatDate(r.date)}. " +
                "The slot will become available to others right away.")
            .setPositiveButton("Cancel reservation") { _, _ ->
                val error = MockData.cancel(r.reservationId, user, null)
                requireContext().toast(error ?: "Reservation cancelled.")
                refresh()
            }
            .setNegativeButton("Keep it", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
