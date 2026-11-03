package com.example.reserba.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.reserba.adapter.AccountAdapter
import com.example.reserba.data.MockData
import com.example.reserba.databinding.FragmentVerifyAccountsBinding

/** Verify Accounts (Admin): approve or reject new registrations after checking the ID (FR-11). */
class VerifyAccountsFragment : Fragment() {

    private var _b: FragmentVerifyAccountsBinding? = null
    private val b get() = _b!!

    private lateinit var adapter: AccountAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentVerifyAccountsBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val admin = MockData.currentUser ?: return

        adapter = AccountAdapter(
            onApprove = { u ->
                MockData.approveAccount(u.uid, admin)
                requireContext().toast("${u.fullName}'s account was approved.")
                refresh()
            },
            onReject = { u ->
                requireContext().promptRemarks("Reject account", "Reason (required)") { remarks ->
                    val error = MockData.rejectAccount(u.uid, remarks, admin)
                    requireContext().toast(error ?: "${u.fullName}'s account was rejected.")
                    refresh()
                }
            }
        )
        b.rvAccounts.layoutManager = LinearLayoutManager(requireContext())
        b.rvAccounts.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        if (_b == null) return
        val list = MockData.pendingAccounts()
        adapter.submit(list)
        b.tvInfo.text = "${list.size} new registration(s) to check"
        b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
