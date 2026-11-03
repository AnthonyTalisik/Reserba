package com.example.reserba.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.reserba.R
import com.example.reserba.data.MockData
import com.example.reserba.databinding.ItemAccountBinding
import com.example.reserba.databinding.ItemAdminRequestBinding
import com.example.reserba.databinding.ItemManageRoomBinding
import com.example.reserba.databinding.ItemNotificationBinding
import com.example.reserba.databinding.ItemReservationBinding
import com.example.reserba.databinding.ItemRoomBinding
import com.example.reserba.databinding.ItemSlotBinding
import com.example.reserba.model.AppNotification
import com.example.reserba.model.Reservation
import com.example.reserba.model.ReservationStatus
import com.example.reserba.model.Role
import com.example.reserba.model.Room
import com.example.reserba.model.Slot
import com.example.reserba.model.SlotStatus
import com.example.reserba.model.User
import com.example.reserba.ui.applyStatus
import com.example.reserba.util.TimeUtils

class ViewHolderOf<B : androidx.viewbinding.ViewBinding>(val b: B) : RecyclerView.ViewHolder(b.root)


class RoomAdapter(private val onClick: (Room) -> Unit) : RecyclerView.Adapter<ViewHolderOf<ItemRoomBinding>>() {
    private var items: List<Room> = emptyList()

    fun submit(list: List<Room>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolderOf(ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolderOf<ItemRoomBinding>, position: Int) {
        val room = items[position]
        val b = holder.b
        val ctx = b.root.context
        b.tvRoomName.text = room.name
        b.tvRoomLocation.text = "${room.building}" + if (room.floor.isBlank()) "" else ", ${room.floor}"
        b.tvRoomMeta.text = "${room.capacity} seats  •  ${room.type.label}"

        val (label, free) = MockData.availabilityLabel(room)
        b.tvBadge.text = label
        b.tvBadge.setBackgroundResource(if (free) R.drawable.bg_badge_green else R.drawable.bg_badge_amber)
        b.tvBadge.setTextColor(ContextCompat.getColor(ctx, if (free) R.color.green else R.color.amber))
        b.root.setOnClickListener { onClick(room) }
    }
}

// ------------------------------------------------------------ Availability slots

class SlotAdapter : RecyclerView.Adapter<ViewHolderOf<ItemSlotBinding>>() {
    private var items: List<Slot> = emptyList()

    fun submit(list: List<Slot>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolderOf(ItemSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolderOf<ItemSlotBinding>, position: Int) {
        val slot = items[position]
        val b = holder.b
        val ctx = b.root.context
        b.tvSlotTime.text = TimeUtils.format12(slot.startTime)
        b.tvSlotStatus.text = slot.detail
        val (bg, fg) = when (slot.status) {
            SlotStatus.AVAILABLE -> R.drawable.bg_badge_green to R.color.green
            SlotStatus.PENDING -> R.drawable.bg_badge_amber to R.color.amber
            SlotStatus.TAKEN -> R.drawable.bg_badge_red to R.color.red
            SlotStatus.BLOCKED -> R.drawable.bg_card to R.color.muted
        }
        b.tvSlotStatus.setBackgroundResource(bg)
        b.tvSlotStatus.setTextColor(ContextCompat.getColor(ctx, fg))
    }
}

// -------------------------------------------------------------- My reservations

class ReservationAdapter(
    private val onCancel: (Reservation) -> Unit
) : RecyclerView.Adapter<ViewHolderOf<ItemReservationBinding>>() {
    private var items: List<Reservation> = emptyList()

    fun submit(list: List<Reservation>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolderOf(ItemReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolderOf<ItemReservationBinding>, position: Int) {
        val r = items[position]
        val b = holder.b
        b.tvStatus.applyStatus(r.status)
        b.tvResRoom.text = r.roomName
        b.tvResWhen.text = "${TimeUtils.formatDate(r.date)}  •  ${TimeUtils.formatRange(r.startTime, r.endTime)}"
        b.tvResPurpose.text = r.purpose
        if (r.adminRemarks.isBlank()) {
            b.tvResRemarks.visibility = View.GONE
        } else {
            b.tvResRemarks.visibility = View.VISIBLE
            b.tvResRemarks.text = "Administrator remarks: ${r.adminRemarks}"
        }
        val cancellable = (r.status == ReservationStatus.PENDING || r.status == ReservationStatus.APPROVED) &&
            System.currentTimeMillis() < r.startMillis
        b.btnCancel.visibility = if (cancellable) View.VISIBLE else View.GONE
        b.btnCancel.text = if (r.status == ReservationStatus.PENDING) "Cancel request" else "Cancel reservation"
        b.btnCancel.setOnClickListener { onCancel(r) }
    }
}

// ---------------------------------------------------------------- Notifications

class NotificationAdapter(
    private val onClick: (AppNotification) -> Unit
) : RecyclerView.Adapter<ViewHolderOf<ItemNotificationBinding>>() {
    private var items: List<AppNotification> = emptyList()

    fun submit(list: List<AppNotification>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolderOf(ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolderOf<ItemNotificationBinding>, position: Int) {
        val n = items[position]
        val b = holder.b
        b.tvNotifTitle.text = n.title
        b.tvNotifMessage.text = n.message
        b.tvNotifTime.text = TimeUtils.formatDateTime(n.createdAt)
        b.viewDot.visibility = if (n.isRead) View.INVISIBLE else View.VISIBLE
        b.root.setOnClickListener { onClick(n) }
    }
}

// ----------------------------------------------------------- Admin: requests

class AdminRequestAdapter(
    private val onReview: (Reservation) -> Unit,
    private val onReject: (Reservation) -> Unit
) : RecyclerView.Adapter<ViewHolderOf<ItemAdminRequestBinding>>() {
    private var items: List<Reservation> = emptyList()

    fun submit(list: List<Reservation>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolderOf(ItemAdminRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolderOf<ItemAdminRequestBinding>, position: Int) {
        val r = items[position]
        val b = holder.b
        b.tvReqRequester.text = "${r.organization}  -  ${r.requesterName}"
        b.tvReqStatus.applyStatus(r.status)
        b.tvReqRoom.text = r.roomName
        b.tvReqWhen.text = "${TimeUtils.formatDate(r.date)}  •  ${TimeUtils.formatRange(r.startTime, r.endTime)}  •  ${r.attendees} people"

        val pending = r.status == ReservationStatus.PENDING
        val overlap = pending && MockData.overlappingPending(r).isNotEmpty()
        b.tvOverlapFlag.visibility = if (overlap) View.VISIBLE else View.GONE
        b.btnReject.visibility = if (pending) View.VISIBLE else View.GONE
        b.btnReview.text = if (pending) "Review" else "Details"
        b.btnReview.setOnClickListener { onReview(r) }
        b.btnReject.setOnClickListener { onReject(r) }
    }
}

// ---------------------------------------------------------- Admin: accounts

class AccountAdapter(
    private val onApprove: (User) -> Unit,
    private val onReject: (User) -> Unit
) : RecyclerView.Adapter<ViewHolderOf<ItemAccountBinding>>() {
    private var items: List<User> = emptyList()

    fun submit(list: List<User>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolderOf(ItemAccountBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolderOf<ItemAccountBinding>, position: Int) {
        val u = items[position]
        val b = holder.b
        b.tvInitials.text = u.fullName.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }
        b.tvAccName.text = u.fullName
        b.tvAccId.text = "ID: ${u.idNumber}"
        b.tvAccRole.text = if (u.role == Role.FACULTY) "Faculty" else "Student"
        b.tvAccOrg.text = "Organization / department: ${u.organization}"
        b.btnApproveAcc.setOnClickListener { onApprove(u) }
        b.btnRejectAcc.setOnClickListener { onReject(u) }
    }
}

// ------------------------------------------------------------ Admin: rooms

class ManageRoomAdapter(
    private val onToggle: (Room, Boolean) -> Unit,
    private val onBlock: (Room) -> Unit
) : RecyclerView.Adapter<ViewHolderOf<ItemManageRoomBinding>>() {
    private var items: List<Room> = emptyList()

    fun submit(list: List<Room>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolderOf(ItemManageRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolderOf<ItemManageRoomBinding>, position: Int) {
        val room = items[position]
        val b = holder.b
        b.tvMrName.text = room.name
        b.tvMrDetails.text = "${room.building}  •  ${room.capacity} seats  •  " +
            "${TimeUtils.formatRange(room.openTime, room.closeTime)}  •  ${room.bufferMinutes}-min gap"
        b.switchActive.setOnCheckedChangeListener(null)
        b.switchActive.isChecked = room.isActive
        b.switchActive.setOnCheckedChangeListener { _, checked -> onToggle(room, checked) }
        b.btnBlock.setOnClickListener { onBlock(room) }
    }
}
