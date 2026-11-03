package com.example.reserba.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reserba.adapter.ManageRoomAdapter
import com.example.reserba.data.MockData
import com.example.reserba.databinding.DialogAddRoomBinding
import com.example.reserba.databinding.FragmentManageRoomsBinding
import com.example.reserba.model.Room
import com.example.reserba.model.RoomType
import com.example.reserba.util.TimeUtils
import java.util.Calendar

/** Manage Rooms & Blocked Dates (Admin) (FR-10). */
class ManageRoomsFragment : Fragment() {

    private var _b: FragmentManageRoomsBinding? = null
    private val b get() = _b!!

    private lateinit var adapter: ManageRoomAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentManageRoomsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ManageRoomAdapter(
            onToggle = { room, active ->
                room.isActive = active
                requireContext().toast("${room.name} is now ${if (active) "active" else "inactive"}.")
            },
            onBlock = { room -> pickBlockDate(room) }
        )
        b.rvManageRooms.layoutManager = LinearLayoutManager(requireContext())
        b.rvManageRooms.adapter = adapter
        b.btnAddRoom.setOnClickListener { showAddRoomDialog() }
        refresh()
    }

    private fun refresh() {
        if (_b == null) return
        adapter.submit(MockData.rooms.toList())
    }

    private fun pickBlockDate(room: Room) {
        val c = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                val date = TimeUtils.dateFromParts(y, m, d)
                requireContext().promptRemarks("Block ${room.name} on ${TimeUtils.formatDate(date)}", "Reason, e.g. Maintenance") { reason ->
                    val admin = MockData.currentUser ?: return@promptRemarks
                    MockData.blockWholeDay(room, date, reason, admin)
                    requireContext().toast("Blocked ${TimeUtils.formatDate(date)}. New requests are not allowed that day.")
                }
            },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showAddRoomDialog() {
        val d = DialogAddRoomBinding.inflate(layoutInflater)
        d.spRoomType.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, RoomType.values().map { it.label }
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add room")
            .setView(d.root)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()

        // Override the positive button so invalid input keeps the dialog open.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = d.etRoomName.text.toString().trim()
            val building = d.etBuilding.text.toString().trim()
            val capacity = d.etCapacity.text.toString().trim().toIntOrNull()
            when {
                name.isEmpty() -> d.etRoomName.error = "Required"
                building.isEmpty() -> d.etBuilding.error = "Required"
                capacity == null || capacity < 1 -> d.etCapacity.error = "Enter a number of at least 1"
                else -> {
                    val type = RoomType.values()[d.spRoomType.selectedItemPosition]
                    MockData.addRoom(name, building, type, capacity)
                    refresh()
                    dialog.dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
