package com.example.reserba.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reserba.R
import com.example.reserba.adapter.RoomAdapter
import com.example.reserba.data.MockData
import com.example.reserba.databinding.FragmentRoomsBinding
import com.example.reserba.model.RoomType

/** Room List (Home): search by name/building, filter by type, capacity and "free now". */
class RoomsFragment : Fragment() {

    private var _b: FragmentRoomsBinding? = null
    private val b get() = _b!!

    private lateinit var adapter: RoomAdapter

    private val capacityLabels = listOf("Any capacity", "10+ seats", "30+ seats", "50+ seats", "100+ seats")
    private val capacityMins = listOf(0, 10, 30, 50, 100)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentRoomsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.tvGreeting.text = "Hi, ${MockData.currentUser?.fullName?.substringBefore(" ") ?: ""}"

        adapter = RoomAdapter { room ->
            startActivity(
                Intent(requireContext(), RoomDetailsActivity::class.java)
                    .putExtra(Extras.ROOM_ID, room.roomId)
            )
        }
        b.rvRooms.layoutManager = LinearLayoutManager(requireContext())
        b.rvRooms.adapter = adapter

        b.spCapacity.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, capacityLabels
        )
        b.spCapacity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = refresh()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b4: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = refresh()
        })
        b.chipGroupType.setOnCheckedStateChangeListener { _, _ -> refresh() }
        b.chipFreeNow.setOnCheckedChangeListener { _, _ -> refresh() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        if (_b == null) return
        MockData.refreshStatuses()

        val query = b.etSearch.text.toString().trim().lowercase()
        val type = when (b.chipGroupType.checkedChipId) {
            R.id.chipClassroom -> RoomType.CLASSROOM
            R.id.chipMeeting -> RoomType.MEETING
            R.id.chipActivity -> RoomType.ACTIVITY
            else -> null
        }
        val minCapacity = capacityMins[b.spCapacity.selectedItemPosition.coerceAtLeast(0)]

        val result = MockData.rooms.filter { room ->
            room.isActive &&
                (query.isEmpty() || room.name.lowercase().contains(query) || room.building.lowercase().contains(query)) &&
                (type == null || room.type == type) &&
                room.capacity >= minCapacity &&
                (!b.chipFreeNow.isChecked || MockData.isFreeNow(room))
        }
        adapter.submit(result)
        b.tvEmpty.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
