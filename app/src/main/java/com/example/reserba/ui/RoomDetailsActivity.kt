package com.example.reserba.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reserba.R
import com.example.reserba.adapter.SlotAdapter
import com.example.reserba.data.MockData
import com.example.reserba.databinding.ActivityRoomDetailsBinding
import com.example.reserba.model.Role
import com.example.reserba.model.Room
import com.example.reserba.util.TimeUtils

class RoomDetailsActivity : AppCompatActivity() {

    private lateinit var b: ActivityRoomDetailsBinding
    private lateinit var room: Room
    private lateinit var selectedDate: String
    private val slotAdapter = SlotAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRoomDetailsBinding.inflate(layoutInflater)
        setContentView(b.root)

        val found = MockData.roomById(intent.getStringExtra(Extras.ROOM_ID) ?: "")
        if (found == null) {
            finish()
            return
        }
        room = found
        selectedDate = TimeUtils.dateOffset(2) // first comfortably bookable day (24 h rule)

        b.tvRoomTitle.text = room.name
        b.tvRoomLocation.text = "${room.building}, ${room.floor}"
        b.tvRoomInfo.text = "${room.type.label}  •  ${room.capacity} seats  •  " +
            "Open ${TimeUtils.formatRange(room.openTime, room.closeTime)}"
        b.tvRoomEquipment.text = "Equipment: " + room.equipment.joinToString(", ")

        b.rvSlots.layoutManager = LinearLayoutManager(this)
        b.rvSlots.adapter = slotAdapter

        b.btnBack.setOnClickListener { finish() }
        b.btnRequest.setOnClickListener {
            startActivity(
                Intent(this, RequestFormActivity::class.java)
                    .putExtra(Extras.ROOM_ID, room.roomId)
                    .putExtra(Extras.DATE, selectedDate)
            )
        }
        if (MockData.currentUser?.role == Role.ADMIN || !room.isActive) {
            b.btnRequest.isEnabled = false
            b.btnRequest.text = if (room.isActive) "Administrators don't request rooms" else "Room is inactive"
        }

        buildDateStrip()
        loadSlots()
    }

    override fun onResume() {
        super.onResume()
        if (::room.isInitialized) loadSlots()
    }

    private fun buildDateStrip() {
        b.dateStrip.removeAllViews()
        for (i in 0..14) {
            val date = TimeUtils.dateOffset(i)
            val tv = layoutInflater.inflate(R.layout.item_day, b.dateStrip, false) as TextView
            tv.text = TimeUtils.dayLabel(date)
            tv.tag = date
            tv.setOnClickListener {
                selectedDate = date
                styleDays()
                loadSlots()
            }
            b.dateStrip.addView(tv)
        }
        styleDays()
    }

    private fun styleDays() {
        for (i in 0 until b.dateStrip.childCount) {
            val tv = b.dateStrip.getChildAt(i) as TextView
            val selected = tv.tag == selectedDate
            tv.setBackgroundResource(if (selected) R.drawable.bg_day_selected else R.drawable.bg_card)
            tv.setTextColor(ContextCompat.getColor(this, if (selected) R.color.white else R.color.ink))
        }
    }

    private fun loadSlots() {
        b.tvDateLabel.text = TimeUtils.formatDate(selectedDate)
        slotAdapter.submit(MockData.buildSlots(room, selectedDate))
    }
}
