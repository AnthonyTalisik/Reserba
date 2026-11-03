package com.example.reserba.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.reserba.data.MockData
import com.example.reserba.databinding.ActivityRequestFormBinding
import com.example.reserba.databinding.ItemSuggestionBinding
import com.example.reserba.model.Room
import com.example.reserba.util.TimeUtils

class RequestFormActivity : AppCompatActivity() {

    private lateinit var b: ActivityRequestFormBinding
    private lateinit var room: Room
    private lateinit var date: String
    private var startTimes: List<String> = emptyList()
    private var endTimes: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRequestFormBinding.inflate(layoutInflater)
        setContentView(b.root)

        val found = MockData.roomById(intent.getStringExtra(Extras.ROOM_ID) ?: "")
        val user = MockData.currentUser
        if (found == null || user == null) {
            finish()
            return
        }
        room = found
        date = intent.getStringExtra(Extras.DATE) ?: TimeUtils.dateOffset(2)

        b.etOrg.setText(user.organization)
        b.btnBack.setOnClickListener { finish() }
        b.btnDate.setOnClickListener { pickDate() }
        b.btnSend.setOnClickListener { onSend() }

        val timeListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                validateTimes()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        b.spStart.onItemSelectedListener = timeListener
        b.spEnd.onItemSelectedListener = timeListener

        bindRoom(null, null)
    }

    // ----------------------------------------------------------- form setup

    private fun bindRoom(keepStart: String?, keepEnd: String?) {
        b.tvFormRoom.text = "${room.name}  •  ${room.building}" +
            if (room.floor.isBlank()) "" else ", ${room.floor}"
        b.tvCapacityHint.text = "Maximum ${room.capacity} attendees"
        buildTimeLists(keepStart, keepEnd)
        updateDateButton()
    }

    /** BR-02: times are chosen in 30-minute steps within the room's operating hours. */
    private fun buildTimeLists(keepStart: String?, keepEnd: String?) {
        val open = TimeUtils.timeToMinutes(room.openTime)
        val close = TimeUtils.timeToMinutes(room.closeTime)
        startTimes = (open until close step 30).map { TimeUtils.minutesToTime(it) }
        endTimes = ((open + 30)..close step 30).map { TimeUtils.minutesToTime(it) }

        b.spStart.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, startTimes.map { TimeUtils.format12(it) }
        )
        b.spEnd.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, endTimes.map { TimeUtils.format12(it) }
        )

        val startIdx = startTimes.indexOf(keepStart ?: "09:00").let { if (it >= 0) it else 0 }
        val endIdx = endTimes.indexOf(keepEnd ?: "10:00").let { if (it >= 0) it else 0 }
        b.spStart.setSelection(startIdx)
        b.spEnd.setSelection(endIdx)
        validateTimes()
    }

    private fun selectedStart(): String = startTimes[b.spStart.selectedItemPosition]
    private fun selectedEnd(): String = endTimes[b.spEnd.selectedItemPosition]

    private fun validateTimes() {
        if (startTimes.isEmpty() || endTimes.isEmpty()) return
        val ok = TimeUtils.timeToMinutes(selectedEnd()) > TimeUtils.timeToMinutes(selectedStart())
        b.tvTimeError.visibility = if (ok) View.GONE else View.VISIBLE
        b.tvTimeError.text = "End time must be later than start time."
        b.btnSend.isEnabled = ok
        b.cardConflict.visibility = View.GONE
    }

    private fun updateDateButton() {
        b.btnDate.text = TimeUtils.formatDate(date)
    }

    private fun pickDate() {
        val p = date.split("-")
        val dialog = DatePickerDialog(
            this,
            { _, y, m, d ->
                date = TimeUtils.dateFromParts(y, m, d)
                updateDateButton()
                b.cardConflict.visibility = View.GONE
            },
            p[0].toInt(), p[1].toInt() - 1, p[2].toInt()
        )
        val now = System.currentTimeMillis()
        dialog.datePicker.minDate = now - 1000
        dialog.datePicker.maxDate = now + MockData.MAX_ADVANCE_DAYS * 86_400_000L
        dialog.show()
    }

    private fun selectedEquipment(): List<String> = buildList {
        if (b.chipProjector.isChecked) add("Projector")
        if (b.chipWhiteboard.isChecked) add("Whiteboard")
        if (b.chipMic.isChecked) add("Microphone")
    }

    // ------------------------------------------------------------ submit

    private fun showError(message: String) {
        b.tvError.text = message
        b.tvError.visibility = View.VISIBLE
    }

    private fun onSend() {
        val user = MockData.currentUser ?: return
        b.tvError.visibility = View.GONE
        b.cardConflict.visibility = View.GONE

        val attendees = b.etAttendees.text.toString().trim().toIntOrNull()
        val purpose = b.etPurpose.text.toString()
        val org = b.etOrg.text.toString().trim()
        val start = selectedStart()
        val end = selectedEnd()

        if (org.isEmpty()) {
            showError("Enter your organization or department.")
            return
        }
        // BR-02, 03, 04, 05, 10, 11, 12
        MockData.validateRequest(room, date, start, end, attendees, purpose, user.uid)?.let {
            showError(it)
            return
        }

        val s = TimeUtils.toMillis(date, start)
        val e = TimeUtils.toMillis(date, end)

        // BR-10 blocked dates
        MockData.findBlockConflict(room.roomId, s, e)?.let {
            showConflict("The room is blocked at that time (${it.reason}).", attendees!!)
            return
        }

        // BR-01 overlap + 20-minute gap against approved reservations
        MockData.findApprovedConflict(room.roomId, s, e)?.let { c ->
            val next = TimeUtils.roundUp30(TimeUtils.timeToMinutes(c.endTime) + room.bufferMinutes)
            val hint = if (next < TimeUtils.timeToMinutes(room.closeTime)) {
                " Earliest start after it: ${TimeUtils.format12(TimeUtils.minutesToTime(next))}."
            } else ""
            showConflict(
                "${c.organization} has this room ${TimeUtils.formatRange(c.startTime, c.endTime)} " +
                    "(a ${room.bufferMinutes}-minute gap is kept between reservations).$hint",
                attendees!!
            )
            return
        }

        MockData.createRequest(room, user, org, date, start, end, attendees!!, purpose, selectedEquipment())
        toast("Request sent. The administrator will review it.")
        finish()
    }

    /** FR-05: show the clash and offer other rooms that are free at the same time. */
    private fun showConflict(message: String, attendees: Int) {
        b.cardConflict.visibility = View.VISIBLE
        b.tvConflictMsg.text = message

        val suggestions = MockData.suggestRooms(
            room, date, selectedStart(), selectedEnd(), attendees, selectedEquipment()
        )
        b.llSuggestions.removeAllViews()
        b.tvNoSuggest.visibility = if (suggestions.isEmpty()) View.VISIBLE else View.GONE

        suggestions.forEach { s ->
            val item = ItemSuggestionBinding.inflate(layoutInflater, b.llSuggestions, false)
            item.tvSugName.text = s.name
            item.tvSugInfo.text = "${s.capacity} seats  •  ${s.equipment.joinToString(", ")}  •  ${s.building}"
            item.btnSwitch.setOnClickListener {
                val start = selectedStart()
                val end = selectedEnd()
                room = s
                bindRoom(start, end)
                toast("Switched to ${s.name}. Review and send.")
            }
            b.llSuggestions.addView(item.root)
        }
        b.root.post { b.root.smoothScrollTo(0, b.cardConflict.top) }
    }
}
