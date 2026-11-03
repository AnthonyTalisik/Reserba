package com.example.reserba.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reserba.adapter.NotificationAdapter
import com.example.reserba.data.MockData
import com.example.reserba.databinding.FragmentNotificationsBinding

/** Notifications (FR-12): list in-app updates and mark them as read. */
class NotificationsFragment : Fragment() {

    private var _b: FragmentNotificationsBinding? = null
    private val b get() = _b!!

    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentNotificationsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = NotificationAdapter { n ->
            n.isRead = true
            refresh()
        }
        b.rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        b.rvNotifications.adapter = adapter

        b.btnMarkAll.setOnClickListener {
            val uid = MockData.currentUser?.uid ?: return@setOnClickListener
            MockData.notificationsFor(uid).forEach { it.isRead = true }
            refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        if (_b == null) return
        val uid = MockData.currentUser?.uid ?: return
        val list = MockData.notificationsFor(uid)
        adapter.submit(list)
        b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
