package com.example.reserba.model

enum class Role { STUDENT, FACULTY, ADMIN }

enum class AccountStatus { PENDING, ACTIVE, REJECTED }

enum class ReservationStatus { PENDING, APPROVED, REJECTED, CANCELLED, COMPLETED }

enum class RoomType(val label: String) {
    CLASSROOM("Classroom"),
    MEETING("Meeting room"),
    ACTIVITY("Activity space")
}

enum class SlotStatus { AVAILABLE, PENDING, TAKEN, BLOCKED }

data class User(
    val uid: String,
    val fullName: String,
    val email: String,
    val password: String, // Phase 1 mock only. Firebase Authentication replaces this in Phase 2.
    val role: Role,
    val idNumber: String,
    val organization: String,
    var accountStatus: AccountStatus,
    var verifiedBy: String = "",
    var adminRemarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Room(
    val roomId: String,
    var name: String,
    var building: String,
    var floor: String,
    var type: RoomType,
    var capacity: Int,
    val equipment: List<String>,
    var openTime: String = "07:00",
    var closeTime: String = "21:00",
    var bufferMinutes: Int = 20,
    var isActive: Boolean = true,
    var description: String = ""
)

data class Reservation(
    val reservationId: String,
    val roomId: String,
    val roomName: String,
    val requesterId: String,
    val requesterName: String,
    val organization: String,
    val date: String,        // yyyy-MM-dd
    val startTime: String,   // HH:mm
    val endTime: String,     // HH:mm
    val startMillis: Long,
    val endMillis: Long,
    val purpose: String,
    val equipmentNeeded: List<String>,
    val attendees: Int,
    var status: ReservationStatus,
    var adminRemarks: String = "",
    var reviewedBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

data class RoomBlock(
    val blockId: String,
    val roomId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val reason: String,
    val createdBy: String
)

data class AppNotification(
    val notificationId: String,
    val userId: String,
    val title: String,
    val message: String,
    val reservationId: String,
    var isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** One row of the day timeline on the Room Details screen. */
data class Slot(
    val startTime: String,
    val endTime: String,
    val status: SlotStatus,
    val detail: String
)
