package com.example.trabajointegrador_modulonativo.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.semantics.dismiss
import androidx.fragment.app.Fragment
import com.example.trabajointegrador_modulonativo.LanguageSelectActivity
import com.example.trabajointegrador_modulonativo.R
import com.example.trabajointegrador_modulonativo.auth.LoginActivity
import com.example.trabajointegrador_modulonativo.databinding.FragmentSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.changeLanguageOption.setOnClickListener {
            val intent = Intent(activity, LanguageSelectActivity::class.java).apply {
                putExtra("IS_CHANGING_LANGUAGE", true)
            }
            startActivity(intent)
        }

        binding.logoutOption.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }



    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogTheme)
            .setTitle(getString(R.string.cerrar_sesi_n))
            .setMessage(getString(R.string.seguro_cerrar_sesion))
            .setNegativeButton(getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.aceptar)) { dialog, _ ->
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(activity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                startActivity(intent)

                Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}