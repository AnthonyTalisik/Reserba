package com.example.reserba.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.reserba.data.MockData
import com.example.reserba.databinding.FragmentProfileBinding
import com.example.reserba.model.Role

class ProfileFragment : Fragment() {

    private var _b: FragmentProfileBinding? = null
    private val b get() = _b!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentProfileBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = MockData.currentUser ?: return

        b.tvProfileName.text = user.fullName
        b.tvProfileRole.text = when (user.role) {
            Role.STUDENT -> "Student / Organization Representative"
            Role.FACULTY -> "Faculty / Staff"
            Role.ADMIN -> "Room Administrator"
        }
        b.tvProfileDetail.text = "Email: ${user.email}\nID number: ${user.idNumber}\nOrganization / department: ${user.organization}"

        b.btnLogout.setOnClickListener {
            MockData.logout()
            startActivity(
                Intent(requireContext(), LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
