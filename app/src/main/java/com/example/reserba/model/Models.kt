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
    val uid: String = "",
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val role: Role = Role.STUDENT,
    val idNumber: String = "",
    val organization: String = "",
    var accountStatus: AccountStatus = AccountStatus.PENDING,
    var verifiedBy: String = "",
    var adminRemarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = if (middleName.isBlank()) "$firstName $lastName".trim() else "$firstName $middleName $lastName".trim()
}

data class Room(
    val roomId: String = "",
    var name: String = "",
    var building: String = "",
    var floor: String = "",
    var type: RoomType = RoomType.CLASSROOM,
    var capacity: Int = 0,
    val equipment: List<String> = emptyList(),
    var openTime: String = "07:00",
    var closeTime: String = "21:00",
    var bufferMinutes: Int = 20,
    var isActive: Boolean = true,
    var description: String = ""
)

data class Reservation(
    val reservationId: String = "",
    val roomId: String = "",
    val roomName: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val organization: String = "",
    val date: String = "",        // yyyy-MM-dd
    val startTime: String = "",   // HH:mm
    val endTime: String = "",     // HH:mm
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val purpose: String = "",
    val equipmentNeeded: List<String> = emptyList(),
    val attendees: Int = 0,
    var status: ReservationStatus = ReservationStatus.PENDING,
    var adminRemarks: String = "",
    var reviewedBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)

data class RoomBlock(
    val blockId: String = "",
    val roomId: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val reason: String = "",
    val createdBy: String = ""
)

data class AppNotification(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val reservationId: String = "",
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
