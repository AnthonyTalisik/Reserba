Reserba

Reserba is an Android app (Kotlin) for reserving campus rooms without double bookings. Students, organizations, faculty and staff can check which rooms are free and request a reservation from their phones. The room administrator reviews and decides on requests in one place.

The problem

Room schedules are managed by paper forms, messages and logbooks. The same room can be promised to two groups at the same time, nobody has a live view of free rooms, and requesters wait without knowing the status of their request.

What the app does
Find a room: browse and search by name or building, and filter by type, capacity and "free now".
Check availability: see a day timeline with Available, Pending, Taken and Blocked slots.
Request a reservation: pick date, start and end time (30-minute steps), attendees, purpose and equipment.
Automatic conflict check: a request that overlaps an approved booking, or the 20-minute cleaning gap after it, is blocked. The app shows the clash, the earliest free start, and other suitable rooms you can switch to with one tap.
Administrator review: approve, or reject with required remarks. Approval re-checks the slot, and overlapping pending requests are rejected automatically.
Track and cancel: My Reservations (Pending / Approved / History) with cancellation before the start time.
In-app notifications for approvals, rejections and cancellations.
Account verification: new accounts stay Pending until the administrator approves them.
Room management: the administrator adds rooms, deactivates them and blocks dates.

Roles
Role	Can do
-Student / Organization Representative	Browse rooms, request, track and cancel own reservations, get notifications
-Faculty / Staff	Same as student
-Room Administrator	Verify accounts, approve or reject requests, cancel with a reason, manage rooms and blocked dates

Tech
Kotlin, XML layouts, ViewBinding, Material Components, RecyclerView, Fragments with bottom navigation
Data passed between screens with Intent extras
Mock data in memory (MockData.kt), reset every time the app restarts
minSdk 24, compileSdk 34, Android Gradle Plugin 8.2.2, Kotlin 1.9.22, JDK 17
How to run
Requirements
Android Studio (Hedgehog or newer recommended)
JDK 17 (the JDK bundled with Android Studio works)
An emulator or a physical Android phone (Android 7.0 / API 24 or higher)

Steps
Clone the repository:
   git clone https://github.com/AnthonyTalisik/Reserba.git
In Android Studio choose File > Open and select the Reserba folder.
Wait for Gradle Sync to finish. Android Studio will download the dependencies, so you need an internet connection the first time.
Pick a device in the toolbar:
Emulator (tested on a Pixel 4 virtual device): open Device Manager, create a Pixel 4 device with a recent system image (API 34 recommended), and start it.
Physical phone: enable Developer options and USB debugging, then plug it in.
Press Run (the green play button, or Shift + F10).
Troubleshooting
Gradle sync fails: open File > Settings > Build > Gradle and set Gradle JDK to 17.
SDK not found: install Android SDK Platform 34 from Tools > SDK Manager.
Emulator is slow or won't start: enable virtualization (Intel VT-x / AMD-V) in the BIOS and install the emulator acceleration drivers from SDK Manager.
Still broken after pulling changes: run Build > Clean Project, then Build > Rebuild Project.
Demo accounts
Role	Email	Password
Student / Org Rep	student@school.edu	123456
Faculty / Staff	faculty@school.edu	123456
Room Administrator	admin@reserba.edu	admin123
Pending account (cannot log in until verified)	juan@school.edu	123456
Quick tour

Try a booking conflict (student):

Log in as the student, open Room 301 - CCS Lab, then tap Request this room.
Choose a date 2 days from today, 12:00 PM to 1:30 PM. This clashes with an approved 11:00 AM to 1:00 PM booking.
The app shows the conflict, the earliest free start (1:30 PM, because of the 20-minute gap) and other rooms you can switch to.
Book 2 or more days ahead, because requests must be made at least 24 hours in advance.

Approve a request (administrator):

Log in as the admin. The Requests tab shows two overlapping pending requests for Room 301, flagged in amber.
Open one with Review and tap Approve. The other is rejected automatically with "Slot no longer available".
Open the Verify tab to approve or reject new accounts. Register a new user first to see it appear.
Business rules
Rule	Value
Gap between reservations	20 minutes (per room)
Operating hours	7:00 AM to 9:00 PM (default)
Advance booking	At least 24 hours, at most 30 days ahead
Maximum length	4 hours per reservation
Attendees	1 up to the room's capacity
Pending requests per user	At most 3
Rejection	Remarks required
Cancellation	Own reservations only, before the start time. Admin needs a reason.
ID number	One account per ID number

The overlap check is: newStart < existingEnd + buffer && newEnd + buffer > existingStart.

Project structure
app/src/main/
├── java/com/example/reserba/
│   ├── model/Models.kt        Data classes and enums
│   ├── util/TimeUtils.kt      Date and time helpers, overlap check
│   ├── data/MockData.kt       Mock data and business rules
│   ├── adapter/Adapters.kt    RecyclerView adapters
│   └── ui/                    Activities and fragments
└── res/
    ├── layout/                XML layouts
    ├── drawable/              Backgrounds and icons
    ├── menu/                  Bottom navigation menus
    └── values/                Colors, themes, styles
Scope and limitations

Not included: recurring or multi-day bookings, payments, push, email or SMS notifications, calendar or timetable integration, QR check-in, multiple campuses, iOS or web versions, and automatic approval rules. Account verification is done manually by the administrator.


