package com.example.reserba.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.reserba.R
import com.example.reserba.data.MockData
import com.example.reserba.databinding.ActivityMainBinding
import com.example.reserba.model.Role

/**
 * Single host activity with a bottom navigation bar.
 * The menu and fragments depend on the logged-in role (requester vs administrator).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = MockData.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        val isAdmin = user.role == Role.ADMIN
        b.bottomNav.menu.clear()
        b.bottomNav.inflateMenu(if (isAdmin) R.menu.menu_admin else R.menu.menu_requester)

        b.bottomNav.setOnItemSelectedListener { item ->
            show(fragmentFor(item.itemId))
            true
        }

        if (savedInstanceState == null) {
            b.bottomNav.selectedItemId = if (isAdmin) R.id.nav_requests else R.id.nav_rooms
        }
    }

    private fun fragmentFor(menuId: Int): Fragment = when (menuId) {
        R.id.nav_rooms -> RoomsFragment()
        R.id.nav_bookings -> MyReservationsFragment()
        R.id.nav_requests -> AdminRequestsFragment()
        R.id.nav_manage_rooms -> ManageRoomsFragment()
        R.id.nav_verify -> VerifyAccountsFragment()
        R.id.nav_alerts -> NotificationsFragment()
        else -> ProfileFragment()
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
