package com.example.reserba.data

import com.example.reserba.model.AccountStatus
import com.example.reserba.model.AppNotification
import com.example.reserba.model.Reservation
import com.example.reserba.model.ReservationStatus
import com.example.reserba.model.Role
import com.example.reserba.model.Room
import com.example.reserba.model.RoomBlock
import com.example.reserba.model.RoomType
import com.example.reserba.model.Slot
import com.example.reserba.model.SlotStatus
import com.example.reserba.model.User
import com.example.reserba.util.TimeUtils

data class LoginResult(val user: User?, val error: String?)

/**
 * Phase 1: in-memory mock data + all the business rules (BR-01 .. BR-15).
 * In Phase 2 this object is replaced by Firebase Authentication + Cloud Firestore,
 * keeping the same function names so the screens barely change.
 */
object MockData {

    const val MAX_PENDING = 3                 // BR-11
    const val MAX_DURATION_HOURS = 4L         // BR-04
    const val MIN_ADVANCE_HOURS = 24L         // BR-03
    const val MAX_ADVANCE_DAYS = 30L          // BR-03

    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 86_400_000L

    val users = mutableListOf<User>()
    val rooms = mutableListOf<Room>()
    val reservations = mutableListOf<Reservation>()
    val blocks = mutableListOf<RoomBlock>()
    val notifications = mutableListOf<AppNotification>()

    var currentUser: User? = null

    private var seq = 1000
    fun nextId(prefix: String) = "$prefix${seq++}"

    // ---------------------------------------------------------------- lookups

    fun roomById(id: String): Room? = rooms.firstOrNull { it.roomId == id }
    fun reservationById(id: String): Reservation? = reservations.firstOrNull { it.reservationId == id }

    // ------------------------------------------------------------ auth (FR-01)

    fun login(email: String, password: String): LoginResult {
        val u = users.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }
            ?: return LoginResult(null, "No account found for that email.")
        if (u.password != password) return LoginResult(null, "Incorrect password.")
        return when (u.accountStatus) {
            AccountStatus.PENDING ->
                LoginResult(null, "Your account is still waiting for administrator verification.")
            AccountStatus.REJECTED -> {
                val why = if (u.adminRemarks.isBlank()) "" else " Reason: ${u.adminRemarks}."
                LoginResult(null, "Your registration was rejected.$why Please register again with corrected information.")
            }
            AccountStatus.ACTIVE -> {
                currentUser = u
                LoginResult(u, null)
            }
        }
    }

    fun logout() {
        currentUser = null
    }

    /** Returns an error message, or null when the account was created (as Pending). BR-12, BR-13, BR-15. */
    fun register(
        fullName: String, idNumber: String, organization: String,
        email: String, password: String, role: Role
    ): String? {
        if (role == Role.ADMIN) return "Administrator accounts cannot be self-registered."
        // A rejected account must be able to register again with corrected information.
        users.removeAll {
            it.accountStatus == AccountStatus.REJECTED &&
                (it.email.equals(email.trim(), true) || it.idNumber.equals(idNumber.trim(), true))
        }
        if (users.any { it.email.equals(email.trim(), true) }) return "That email is already registered."
        if (users.any { it.idNumber.equals(idNumber.trim(), true) }) {
            return "That ID number is already linked to another account." // BR-15
        }
        val u = User(
            uid = nextId("u"),
            fullName = fullName.trim(),
            email = email.trim(),
            password = password,
            role = role,
            idNumber = idNumber.trim(),
            organization = organization.trim(),
            accountStatus = AccountStatus.PENDING
        )
        users += u
        adminIds().forEach {
            pushNotification(it, "New account to verify", "${u.fullName} (${u.idNumber}) registered.", "")
        }
        return null
    }

    private fun adminIds() = users.filter { it.role == Role.ADMIN }.map { it.uid }

    // ------------------------------------------------- availability and conflicts

    fun findApprovedConflict(
        roomId: String, startMillis: Long, endMillis: Long, excludeId: String? = null
    ): Reservation? {
        val room = roomById(roomId) ?: return null
        return reservations.firstOrNull {
            it.roomId == roomId &&
                it.status == ReservationStatus.APPROVED &&
                it.reservationId != excludeId &&
                TimeUtils.conflicts(startMillis, endMillis, it.startMillis, it.endMillis, room.bufferMinutes)
        }
    }

    fun findBlockConflict(roomId: String, startMillis: Long, endMillis: Long): RoomBlock? =
        blocks.firstOrNull {
            it.roomId == roomId && TimeUtils.overlaps(
                startMillis, endMillis,
                TimeUtils.toMillis(it.date, it.startTime), TimeUtils.toMillis(it.date, it.endTime)
            )
        }

    /** Other pending requests that clash with this one (flagged for the admin, BR-08). */
    fun overlappingPending(r: Reservation): List<Reservation> {
        val room = roomById(r.roomId) ?: return emptyList()
        return reservations.filter {
            it.status == ReservationStatus.PENDING &&
                it.reservationId != r.reservationId &&
                it.roomId == r.roomId &&
                TimeUtils.conflicts(r.startMillis, r.endMillis, it.startMillis, it.endMillis, room.bufferMinutes)
        }
    }

    fun isFreeNow(room: Room): Boolean = availabilityLabel(room).second

    /** (label, isFree) shown as the badge on the Room List. */
    fun availabilityLabel(room: Room): Pair<String, Boolean> {
        if (!room.isActive) return "Unavailable" to false
        val now = System.currentTimeMillis()
        blocks.firstOrNull {
            it.roomId == room.roomId &&
                now in TimeUtils.toMillis(it.date, it.startTime)..TimeUtils.toMillis(it.date, it.endTime)
        }?.let { return "Blocked" to false }
        reservations.firstOrNull {
            it.roomId == room.roomId && it.status == ReservationStatus.APPROVED &&
                now in it.startMillis..it.endMillis
        }?.let { return "In use until ${TimeUtils.format12(it.endTime)}" to false }
        return "Free now" to true
    }

    /** Hourly day timeline for one room: available / pending / taken / blocked. */
    fun buildSlots(room: Room, date: String): List<Slot> {
        val open = TimeUtils.timeToMinutes(room.openTime)
        val close = TimeUtils.timeToMinutes(room.closeTime)
        val out = mutableListOf<Slot>()
        var m = open
        while (m < close) {
            val e = minOf(m + 60, close)
            val st = TimeUtils.minutesToTime(m)
            val en = TimeUtils.minutesToTime(e)
            val s = TimeUtils.toMillis(date, st)
            val en2 = TimeUtils.toMillis(date, en)

            val block = blocks.firstOrNull {
                it.roomId == room.roomId && TimeUtils.overlaps(
                    s, en2, TimeUtils.toMillis(it.date, it.startTime), TimeUtils.toMillis(it.date, it.endTime)
                )
            }
            val taken = reservations.firstOrNull {
                it.roomId == room.roomId && it.status == ReservationStatus.APPROVED &&
                    TimeUtils.overlaps(s, en2, it.startMillis, it.endMillis)
            }
            val pending = reservations.firstOrNull {
                it.roomId == room.roomId && it.status == ReservationStatus.PENDING &&
                    TimeUtils.overlaps(s, en2, it.startMillis, it.endMillis)
            }
            out += when {
                block != null -> Slot(st, en, SlotStatus.BLOCKED, "Blocked - ${block.reason}")
                taken != null -> Slot(st, en, SlotStatus.TAKEN, "Taken - ${taken.organization}")
                pending != null -> Slot(st, en, SlotStatus.PENDING, "Pending - ${pending.organization}")
                else -> Slot(st, en, SlotStatus.AVAILABLE, "Available")
            }
            m += 60
        }
        return out
    }

    /** BR-14: active, free (incl. buffer and blocked dates), enough seats, same type and equipment. */
    fun suggestRooms(
        original: Room, date: String, start: String, end: String,
        attendees: Int, equipmentNeeded: List<String>
    ): List<Room> {
        val s = TimeUtils.toMillis(date, start)
        val e = TimeUtils.toMillis(date, end)
        val sMin = TimeUtils.timeToMinutes(start)
        val eMin = TimeUtils.timeToMinutes(end)
        val seats = maxOf(attendees, 1)
        return rooms.filter {
            it.roomId != original.roomId &&
                it.isActive &&
                it.type == original.type &&
                it.capacity >= seats &&
                it.equipment.containsAll(equipmentNeeded) &&
                sMin >= TimeUtils.timeToMinutes(it.openTime) &&
                eMin <= TimeUtils.timeToMinutes(it.closeTime) &&
                findBlockConflict(it.roomId, s, e) == null &&
                findApprovedConflict(it.roomId, s, e) == null
        }.sortedWith(
            compareBy<Room>({ it.capacity - seats }, { if (it.building == original.building) 0 else 1 })
        )
    }

    // ------------------------------------------------------ request validation

    fun pendingCount(userId: String) =
        reservations.count { it.requesterId == userId && it.status == ReservationStatus.PENDING }

    /** BR-02, 03, 04, 05, 10, 11, 12. Returns an error message or null when valid. */
    fun validateRequest(
        room: Room, date: String, startTime: String, endTime: String,
        attendees: Int?, purpose: String, userId: String
    ): String? {
        if (!room.isActive) return "This room is inactive and cannot be requested."

        val s = TimeUtils.toMillis(date, startTime)
        val e = TimeUtils.toMillis(date, endTime)
        if (e <= s) return "End time must be later than start time."

        val sMin = TimeUtils.timeToMinutes(startTime)
        val eMin = TimeUtils.timeToMinutes(endTime)
        if (sMin < TimeUtils.timeToMinutes(room.openTime) || eMin > TimeUtils.timeToMinutes(room.closeTime)) {
            return "Time must fall within the room's operating hours (${TimeUtils.formatRange(room.openTime, room.closeTime)})."
        }
        if (e - s > MAX_DURATION_HOURS * HOUR_MS) return "A single reservation may not exceed $MAX_DURATION_HOURS hours."

        val now = System.currentTimeMillis()
        if (s < now) return "The reservation date cannot be in the past."
        if (s < now + MIN_ADVANCE_HOURS * HOUR_MS) return "Requests must be made at least $MIN_ADVANCE_HOURS hours ahead."
        if (s > now + MAX_ADVANCE_DAYS * DAY_MS) return "Requests may be made at most $MAX_ADVANCE_DAYS days ahead."

        if (attendees == null || attendees < 1) return "Enter at least 1 attendee."
        if (attendees > room.capacity) return "Attendees exceed the room capacity (${room.capacity})."

        if (purpose.trim().length < 10) return "Purpose must be at least 10 characters."
        if (pendingCount(userId) >= MAX_PENDING) return "You already have $MAX_PENDING pending requests. Wait for a decision or cancel one."
        return null
    }

    fun createRequest(
        room: Room, user: User, organization: String, date: String,
        start: String, end: String, attendees: Int, purpose: String, equipment: List<String>
    ): Reservation {
        val r = Reservation(
            reservationId = nextId("res"),
            roomId = room.roomId,
            roomName = room.name,
            requesterId = user.uid,
            requesterName = user.fullName,
            organization = organization,
            date = date,
            startTime = start,
            endTime = end,
            startMillis = TimeUtils.toMillis(date, start),
            endMillis = TimeUtils.toMillis(date, end),
            purpose = purpose.trim(),
            equipmentNeeded = equipment,
            attendees = attendees,
            status = ReservationStatus.PENDING
        )
        reservations += r
        pushNotification(
            user.uid, "Request submitted",
            "${room.name} on ${TimeUtils.formatDate(date)} is waiting for approval.", r.reservationId
        )
        adminIds().forEach {
            pushNotification(
                it, "New reservation request",
                "${user.fullName} requested ${room.name} on ${TimeUtils.formatDate(date)}.", r.reservationId
            )
        }
        return r
    }

    // ------------------------------------------------------ admin decisions

    /**
     * BR-01, BR-06, BR-07, BR-08.
     * Re-runs the conflict check at the moment of approval (Phase 2: inside a Firestore transaction),
     * locks the slot and auto-rejects overlapping pending requests.
     */
    @Synchronized
    fun approve(reservationId: String, remarks: String, admin: User): String? {
        val r = reservationById(reservationId) ?: return "Reservation not found."
        if (r.status != ReservationStatus.PENDING) return "Only pending requests can be approved."
        val room = roomById(r.roomId) ?: return "Room not found."
        if (!room.isActive) return "This room is inactive."

        findBlockConflict(r.roomId, r.startMillis, r.endMillis)?.let {
            return "The room is blocked at that time (${it.reason})."
        }
        findApprovedConflict(r.roomId, r.startMillis, r.endMillis, r.reservationId)?.let {
            return "Conflicts with an approved reservation by ${it.organization} " +
                "(${TimeUtils.formatRange(it.startTime, it.endTime)}, ${room.bufferMinutes}-minute gap included)."
        }

        r.status = ReservationStatus.APPROVED
        r.adminRemarks = remarks
        r.reviewedBy = admin.uid
        r.updatedAt = System.currentTimeMillis()
        pushNotification(
            r.requesterId, "Reservation approved",
            "${r.roomName} on ${TimeUtils.formatDate(r.date)}, ${TimeUtils.formatRange(r.startTime, r.endTime)}." +
                if (remarks.isBlank()) "" else " Remarks: $remarks",
            r.reservationId
        )

        // BR-08: auto-reject the other pending requests that overlap the slot just locked.
        overlappingPending(r).forEach { other ->
            other.status = ReservationStatus.REJECTED
            other.adminRemarks = "Slot no longer available"
            other.reviewedBy = admin.uid
            other.updatedAt = System.currentTimeMillis()
            pushNotification(
                other.requesterId, "Reservation rejected",
                "${other.roomName} on ${TimeUtils.formatDate(other.date)}: Slot no longer available.",
                other.reservationId
            )
        }
        return null
    }

    fun reject(reservationId: String, remarks: String, admin: User): String? {
        if (remarks.isBlank()) return "Remarks are required when rejecting a request." // BR-07
        val r = reservationById(reservationId) ?: return "Reservation not found."
        if (r.status != ReservationStatus.PENDING) return "Only pending requests can be rejected."
        r.status = ReservationStatus.REJECTED
        r.adminRemarks = remarks.trim()
        r.reviewedBy = admin.uid
        r.updatedAt = System.currentTimeMillis()
        pushNotification(
            r.requesterId, "Reservation rejected",
            "${r.roomName} on ${TimeUtils.formatDate(r.date)}. Reason: ${r.adminRemarks}", r.reservationId
        )
        return null
    }

    /** BR-09: owners cancel their own reservations before the start time; admins need a reason. */
    fun cancel(reservationId: String, by: User, reason: String?): String? {
        val r = reservationById(reservationId) ?: return "Reservation not found."
        val isAdmin = by.role == Role.ADMIN
        if (!isAdmin && r.requesterId != by.uid) return "You can only cancel your own reservations."
        if (r.status != ReservationStatus.PENDING && r.status != ReservationStatus.APPROVED) {
            return "This reservation can no longer be cancelled."
        }
        if (System.currentTimeMillis() >= r.startMillis) return "A reservation that has started cannot be cancelled."
        if (isAdmin && reason.isNullOrBlank()) return "A reason is required to cancel a reservation."

        r.status = ReservationStatus.CANCELLED
        if (isAdmin) {
            r.adminRemarks = reason!!.trim()
            r.reviewedBy = by.uid
        }
        r.updatedAt = System.currentTimeMillis()

        if (isAdmin) {
            pushNotification(
                r.requesterId, "Reservation cancelled",
                "${r.roomName} on ${TimeUtils.formatDate(r.date)} was cancelled by the administrator. Reason: ${r.adminRemarks}",
                r.reservationId
            )
        } else {
            adminIds().forEach {
                pushNotification(
                    it, "Reservation cancelled",
                    "${r.requesterName} cancelled ${r.roomName} on ${TimeUtils.formatDate(r.date)}.", r.reservationId
                )
            }
        }
        return null
    }

    /** Approved reservations whose end time has passed become Completed (BR-06). */
    fun refreshStatuses() {
        val now = System.currentTimeMillis()
        reservations.filter { it.status == ReservationStatus.APPROVED && it.endMillis < now }
            .forEach { it.status = ReservationStatus.COMPLETED }
    }

    // ------------------------------------------------------------ accounts (FR-11)

    fun pendingAccounts(): List<User> = users.filter { it.accountStatus == AccountStatus.PENDING }

    fun approveAccount(uid: String, admin: User) {
        users.firstOrNull { it.uid == uid }?.let {
            it.accountStatus = AccountStatus.ACTIVE
            it.verifiedBy = admin.uid
        }
    }

    fun rejectAccount(uid: String, remarks: String, admin: User): String? {
        if (remarks.isBlank()) return "Remarks are required when rejecting an account."
        users.firstOrNull { it.uid == uid }?.let {
            it.accountStatus = AccountStatus.REJECTED
            it.adminRemarks = remarks.trim()
            it.verifiedBy = admin.uid
        }
        return null
    }

    // ----------------------------------------------------- rooms and blocks (FR-10)

    fun addRoom(name: String, building: String, type: RoomType, capacity: Int): Room {
        val room = Room(
            roomId = nextId("r"), name = name, building = building, floor = "",
            type = type, capacity = capacity, equipment = listOf("Whiteboard")
        )
        rooms += room
        return room
    }

    fun blockWholeDay(room: Room, date: String, reason: String, admin: User) {
        blocks += RoomBlock(nextId("blk"), room.roomId, date, room.openTime, room.closeTime, reason, admin.uid)
    }

    // -------------------------------------------------------------- notifications

    fun pushNotification(userId: String, title: String, message: String, reservationId: String) {
        notifications += AppNotification(nextId("n"), userId, title, message, reservationId)
    }

    fun notificationsFor(userId: String): List<AppNotification> =
        notifications.filter { it.userId == userId }.sortedByDescending { it.createdAt }

    // ------------------------------------------------------------------ seed data

    private fun seedReservation(
        id: String, roomId: String, requesterId: String, requesterName: String, org: String,
        dayOffset: Int, start: String, end: String, purpose: String, attendees: Int,
        status: ReservationStatus, remarks: String = ""
    ) {
        val room = roomById(roomId)!!
        val date = TimeUtils.dateOffset(dayOffset)
        reservations += Reservation(
            reservationId = id, roomId = roomId, roomName = room.name,
            requesterId = requesterId, requesterName = requesterName, organization = org,
            date = date, startTime = start, endTime = end,
            startMillis = TimeUtils.toMillis(date, start), endMillis = TimeUtils.toMillis(date, end),
            purpose = purpose, equipmentNeeded = listOf("Projector"), attendees = attendees,
            status = status, adminRemarks = remarks,
            reviewedBy = if (status == ReservationStatus.PENDING) "" else "u_admin"
        )
    }

    private fun seed() {
        // Demo accounts (also listed on the Login screen)
        users += User("u_admin", "Room Administrator", "admin@reserba.edu", "admin123", Role.ADMIN, "ADMIN-001", "Facilities Office", AccountStatus.ACTIVE)
        users += User("u_student", "Maria Santos", "student@school.edu", "123456", Role.STUDENT, "24-0124", "CS Society", AccountStatus.ACTIVE)
        users += User("u_faculty", "Prof. Reyes", "faculty@school.edu", "123456", Role.FACULTY, "F-1020", "Computer Studies Dept.", AccountStatus.ACTIVE)
        users += User("u_p1", "Juan D. Cruz", "juan@school.edu", "123456", Role.STUDENT, "24-0311", "Student Council", AccountStatus.PENDING)
        users += User("u_p2", "Ana Lopez", "ana@school.edu", "123456", Role.STUDENT, "23-1180", "BSIT 3", AccountStatus.PENDING)

        rooms += Room("r301", "Room 301 - CCS Lab", "Main Building", "3rd Floor", RoomType.CLASSROOM, 40, listOf("Projector", "Whiteboard"), description = "Computer lab with 40 workstations.")
        rooms += Room("r302", "Room 302", "Main Building", "3rd Floor", RoomType.CLASSROOM, 35, listOf("Projector", "Whiteboard"))
        rooms += Room("r305", "Room 305", "Main Building", "3rd Floor", RoomType.CLASSROOM, 45, listOf("Projector", "Whiteboard", "Microphone"))
        rooms += Room("mrA", "Meeting Room A", "Main Building", "2nd Floor", RoomType.MEETING, 12, listOf("Projector", "Whiteboard"))
        rooms += Room("mrB", "Meeting Room B", "Annex Building", "1st Floor", RoomType.MEETING, 20, listOf("Projector", "Whiteboard"))
        rooms += Room("hall", "Activity Hall", "Student Center", "Ground Floor", RoomType.ACTIVITY, 120, listOf("Projector", "Microphone"))

        // A few overlapping cases to demonstrate conflict detection and the status workflow.
        seedReservation("s1", "r301", "u_seed", "Student Council", "Student Council", 2, "11:00", "13:00", "Officers planning meeting", 30, ReservationStatus.APPROVED, "Approved. Please tidy up.")
        seedReservation("s2", "r301", "u_faculty", "Prof. Reyes", "Computer Studies Dept.", 2, "14:00", "15:30", "Review session for midterms", 35, ReservationStatus.PENDING)
        seedReservation("s3", "r301", "u_student", "Maria Santos", "CS Society", 2, "14:30", "16:00", "General meeting of members", 40, ReservationStatus.PENDING)
        seedReservation("s4", "mrA", "u_student", "Maria Santos", "CS Society", 3, "09:00", "10:30", "Officers' weekly sync", 8, ReservationStatus.PENDING)
        seedReservation("s5", "hall", "u_student", "Maria Santos", "CS Society", 4, "13:00", "16:00", "Freshmen orientation workshop", 80, ReservationStatus.APPROVED, "Approved. Sound check at 12:30.")
        seedReservation("s6", "r302", "u_student", "Maria Santos", "CS Society", -3, "10:00", "12:00", "Hackathon planning session", 25, ReservationStatus.COMPLETED)
        seedReservation("s7", "mrB", "u_student", "Maria Santos", "CS Society", -2, "15:00", "17:00", "Alumni talk preparation", 15, ReservationStatus.REJECTED, "Room reserved for a school event.")

        blocks += RoomBlock("b1", "r302", TimeUtils.dateOffset(5), "07:00", "21:00", "Maintenance", "u_admin")

        pushNotification("u_student", "Reservation approved", "Activity Hall was approved. Remarks: Approved. Sound check at 12:30.", "s5")
        pushNotification("u_student", "Reservation rejected", "Meeting Room B was rejected. Reason: Room reserved for a school event.", "s7")
        pushNotification("u_admin", "New reservation request", "Prof. Reyes requested Room 301 - CCS Lab.", "s2")
        pushNotification("u_admin", "New account to verify", "Juan D. Cruz (24-0311) registered.", "")
    }

    init {
        seed()
    }
}
