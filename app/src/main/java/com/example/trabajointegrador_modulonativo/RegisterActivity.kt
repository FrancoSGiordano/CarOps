package com.example.trabajointegrador_modulonativo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.trabajointegrador_modulonativo.databinding.ActivityRegisterBinding
import android.util.Patterns
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setup()
    }

    private fun setup() {
        title = "Registrarse"

        binding.loginTextView.setOnClickListener {
            finish()
        }

        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            if(email.isEmpty() || password.isEmpty()){
                binding.emailEditText.error = "Correo electrónico vacío"
                binding.passwordEditText.error = "Contraseña vacía"
                return@setOnClickListener
            }

            if(!isValidEmail(email)){
                binding.emailEditText.error = "Correo electrónico no válido"
                return@setOnClickListener
            }

            if(!isValidPassword(password)){
                binding.passwordEditText.error = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número"
                return@setOnClickListener
            }

            if(password != confirmPassword){
                binding.confirmPasswordEditText.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            binding.emailEditText.error = null
            binding.passwordEditText.error = null
            binding.confirmPasswordEditText.error = null

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        showLogin()
                    } else {
                        showAlert()
                    }
                }

        }
    }

    private fun showLogin() {
        val loginIntent = Intent(this, LogoutActivity::class.java)
        startActivity(loginIntent)
    }

    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage("Se ha producido un error registrando al usuario")
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun isValidEmail(email: String): Boolean{
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean{
        val passwordPattern = Regex(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@\$!%*?&.]{8,}$"
        )

        return passwordPattern.matches(password)
    }
}