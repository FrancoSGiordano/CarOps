package com.example.trabajointegrador_modulonativo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.trabajointegrador_modulonativo.databinding.ActivityLogoutBinding
import com.google.firebase.auth.FirebaseAuth

enum class ProviderType {
    BASIC,
}
class LogoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogoutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.extras

        val email = bundle?.getString("email")
        val provider =bundle?.getString("provider")

        setup(email ?: "", provider ?: "")
    }

    private fun setup(email: String, provider: String) {
        title = "Log Out"
        binding.emailTextView.text = email
        binding.providerTextView.text = provider

        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            onBackPressedDispatcher.onBackPressed()
        }
    }

}