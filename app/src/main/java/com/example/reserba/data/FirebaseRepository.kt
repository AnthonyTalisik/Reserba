package com.example.reserba.data

import android.util.Log
import com.example.reserba.model.AccountStatus
import com.example.reserba.model.Role
import com.example.reserba.model.User
import com.example.reserba.util.PasswordUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseRepository {

    private const val TAG = "FirebaseRepository"
    private const val COLLECTION_USERS = "users"

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    /**
     * Seeds initial user credentials and sample data into Firebase Cloud Firestore
     * if the database is currently empty.
     */
    suspend fun seedInitialCredentialsIfNeeded() {
        try {
            val snapshot = db.collection(COLLECTION_USERS).get().await()
            if (!snapshot.isEmpty) {
                // Database already populated, sync remote users to MockData
                syncUsersFromFirebase()
                return
            }

            Log.d(TAG, "Seeding current user credentials into Firebase database...")
            
            // Seed Demo Accounts into Firebase Database with Hashed Passwords
            val seedUsers = listOf(
                User("u_admin", "Room", "", "Administrator", "admin@reserba.edu", PasswordUtils.hash("admin123"), Role.ADMIN, "ADMIN-001", "Facilities Office", AccountStatus.ACTIVE),
                User("u_student", "Maria", "", "Santos", "student@school.edu", PasswordUtils.hash("123456"), Role.STUDENT, "24-0124", "CS Society", AccountStatus.ACTIVE),
                User("u_faculty", "Prof.", "", "Reyes", "faculty@school.edu", PasswordUtils.hash("123456"), Role.FACULTY, "F-1020", "Computer Studies Dept.", AccountStatus.ACTIVE),
                User("u_p1", "Juan", "D.", "Cruz", "juan@school.edu", PasswordUtils.hash("123456"), Role.STUDENT, "24-0311", "Student Council", AccountStatus.PENDING),
                User("u_p2", "Ana", "", "Lopez", "ana@school.edu", PasswordUtils.hash("123456"), Role.STUDENT, "23-1180", "BSIT 3", AccountStatus.PENDING)
            )

            for (u in seedUsers) {
                db.collection(COLLECTION_USERS).document(u.uid).set(u).await()
            }

            // Sync with local memory cache
            MockData.users.clear()
            MockData.users.addAll(seedUsers)

            Log.d(TAG, "Successfully seeded initial credentials into Firebase database.")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding Firebase database, falling back to local seed", e)
        }
    }

    /**
     * Authenticate user by checking credentials stored directly inside Firebase Firestore.
     */
    suspend fun login(email: String, password: String): LoginResult {
        return try {
            val trimmedEmail = email.trim()
            val querySnapshot = db.collection(COLLECTION_USERS)
                .whereEqualTo("email", trimmedEmail)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                // Fallback check against in-memory user list if Firebase query returned no result
                return MockData.login(email, password)
            }

            val userDoc = querySnapshot.documents.first()
            val user = userDoc.toObject(User::class.java)
                ?: return LoginResult(null, "Error parsing user credentials from database.")

            if (!PasswordUtils.verify(password, user.password)) {
                return LoginResult(null, "Incorrect password.")
            }

            when (user.accountStatus) {
                AccountStatus.PENDING ->
                    LoginResult(null, "Your account is still waiting for administrator verification.")
                AccountStatus.REJECTED -> {
                    val why = if (user.adminRemarks.isBlank()) "" else " Reason: ${user.adminRemarks}."
                    LoginResult(null, "Your registration was rejected.$why Please register again with corrected information.")
                }
                AccountStatus.ACTIVE -> {
                    MockData.currentUser = user
                    // Keep in-memory user cache updated
                    val idx = MockData.users.indexOfFirst { it.uid == user.uid }
                    if (idx >= 0) MockData.users[idx] = user else MockData.users.add(user)
                    LoginResult(user, null)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase login failed, trying local fallback", e)
            MockData.login(email, password)
        }
    }

    /**
     * Register a new user and save their hashed credentials into Firebase database with PENDING status.
     */
    suspend fun register(
        firstName: String, middleName: String, lastName: String,
        idNumber: String, organization: String, email: String,
        password: String, role: Role
    ): String? {
        if (role == Role.ADMIN) return "Administrator accounts cannot be self-registered."

        return try {
            val trimmedEmail = email.trim()
            val trimmedId = idNumber.trim()

            // Check if email exists in database
            val emailCheck = db.collection(COLLECTION_USERS)
                .whereEqualTo("email", trimmedEmail)
                .get()
                .await()

            val existingUsers = emailCheck.documents.mapNotNull { it.toObject(User::class.java) }
            val activeOrPendingEmail = existingUsers.any { it.accountStatus != AccountStatus.REJECTED }
            if (activeOrPendingEmail) return "That email is already registered."

            // Check if ID number exists in database
            val idCheck = db.collection(COLLECTION_USERS)
                .whereEqualTo("idNumber", trimmedId)
                .get()
                .await()
            val existingIdUsers = idCheck.documents.mapNotNull { it.toObject(User::class.java) }
            val activeOrPendingId = existingIdUsers.any { it.accountStatus != AccountStatus.REJECTED }
            if (activeOrPendingId) return "That ID number is already linked to another account."

            // Delete any rejected account with same email/id from database so user can re-register
            existingUsers.filter { it.accountStatus == AccountStatus.REJECTED }.forEach {
                db.collection(COLLECTION_USERS).document(it.uid).delete().await()
            }
            existingIdUsers.filter { it.accountStatus == AccountStatus.REJECTED }.forEach {
                db.collection(COLLECTION_USERS).document(it.uid).delete().await()
            }

            val newUid = MockData.nextId("u")
            val hashedPassword = PasswordUtils.hash(password)
            val user = User(
                uid = newUid,
                firstName = firstName.trim(),
                middleName = middleName.trim(),
                lastName = lastName.trim(),
                email = trimmedEmail,
                password = hashedPassword,
                role = role,
                idNumber = trimmedId,
                organization = organization.trim(),
                accountStatus = AccountStatus.PENDING
            )

            // Save user credentials into Firebase Database
            db.collection(COLLECTION_USERS).document(newUid).set(user).await()

            // Also keep local cache updated
            MockData.users.removeAll {
                it.accountStatus == AccountStatus.REJECTED &&
                        (it.email.equals(trimmedEmail, true) || it.idNumber.equals(trimmedId, true))
            }
            MockData.users.add(user)

            null
        } catch (e: Exception) {
            Log.w(TAG, "Firebase register failed, using local fallback", e)
            MockData.register(firstName, middleName, lastName, idNumber, organization, email, password, role)
        }
    }

    /**
     * Sync user list from Firebase database.
     */
    suspend fun syncUsersFromFirebase(): List<User> {
        return try {
            val snapshot = db.collection(COLLECTION_USERS).get().await()
            val fetchedUsers = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            if (fetchedUsers.isNotEmpty()) {
                MockData.users.clear()
                MockData.users.addAll(fetchedUsers)
            }
            fetchedUsers
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync users from Firebase", e)
            MockData.users
        }
    }

    /**
     * Update user account status in Firebase database.
     */
    suspend fun updateUserStatus(uid: String, status: AccountStatus, remarks: String = "", verifiedBy: String = "") {
        try {
            db.collection(COLLECTION_USERS).document(uid)
                .update(
                    mapOf(
                        "accountStatus" to status.name,
                        "adminRemarks" to remarks,
                        "verifiedBy" to verifiedBy
                    )
                ).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update user status in Firebase", e)
        }
    }
}
