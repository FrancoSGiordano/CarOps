package com.example.trabajointegrador_modulonativo

import android.content.Intent
import android.os.Bundle
import android.os.Message
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.trabajointegrador_modulonativo.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            startCarHostActivity(currentUser.email ?: "", ProviderType.BASIC)
            finish()
            return
        }
        setup()

    }

    private fun setup() {

        title = "Iniciar Sesión"

        binding.registerTextView.setOnClickListener {
            val registerIntent = Intent(this, RegisterActivity::class.java)
            startActivity(registerIntent)
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if(email.isEmpty() || password.isEmpty()){
                binding.emailEditText.error = "Correo electrónico vacío"
                binding.passwordEditText.error = "Contraseña vacía"
                return@setOnClickListener
            }

            binding.emailEditText.error = null
            binding.passwordEditText.error = null

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        startCarHostActivity(it.result?.user?.email ?: "", ProviderType.BASIC)
                    } else {
                        showAlert("Error de autenticacion")
                    }
                }

        }
    }

    private fun showLogout(email: String, provider: ProviderType){
        val logoutIntent = Intent(this, LogoutActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(logoutIntent)
    }

    private fun startCarHostActivity(email: String, provider: ProviderType) {
        val intent = Intent(this, carDetailHostActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(intent)

    }
    private fun showAlert(message: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()

    }
}