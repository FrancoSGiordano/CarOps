package com.example.trabajointegrador_modulonativo.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.trabajointegrador_modulonativo.databinding.ActivityRegisterBinding
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.auth

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
            showLogin()
        }

        binding.registerButton.setOnClickListener {
            if (!validateForm()) {
                return@setOnClickListener
            }
            register()

        }
    }

    private fun register() {
        val email = binding.registerEmailEditText.text.toString()
        val password = binding.registerPasswordEditText.text.toString()

        binding.errorTextView.visibility = View.GONE

        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val user = Firebase.auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                showVerificationAlert()
                            } else {
                                showError("No se pudo enviar el correo de verificación. Inténtalo de nuevo más tarde.")
                            }
                        }
                } else {
                    var errorMessage: String

                    try {
                        throw it.exception!!
                    } catch (e: FirebaseAuthUserCollisionException) {
                        errorMessage = "Ya existe una cuenta asociada a este correo electrónico."
                    } catch (e: Exception) {
                        errorMessage = e.localizedMessage ?: "Ocurrió un error inesperado."
                        e.printStackTrace()
                    }
                    showError(errorMessage)
                }
            }
    }

    private fun showError(message: String) {
        binding.errorTextView.text = message
        binding.errorTextView.visibility = View.VISIBLE
    }

    private fun validateForm(): Boolean {
        var isValid = true

        binding.registerEmailLayout.error = null
        binding.registerPasswordLayout.error = null
        binding.registerConfirmPasswordLayout.error = null

        val email = binding.registerEmailEditText.text.toString()
        val password = binding.registerPasswordEditText.text.toString()
        val confirmPassword = binding.registerConfirmPasswordEditText.text.toString()

        if(email.isEmpty()){
            binding.registerEmailLayout.error = "El correo electrónico es obligatorio"
            isValid = false
        } else if(!isValidEmail(email)) {
            binding.registerEmailLayout.error = "El correo electrónico no es válido"
            isValid = false
        }

        if(password.isEmpty()){
            binding.registerPasswordLayout.error = "La contraseña es obligatoria"
            isValid = false
        } else if(!isValidPassword(password)){ // Cambiado a 'else if'
            binding.registerPasswordLayout.error = "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número"
            isValid = false
        }

        if(confirmPassword.isEmpty()){
            binding.registerConfirmPasswordLayout.error = "La confirmación de contraseña es obligatoria"
            isValid = false
        } else if(confirmPassword != password){
            binding.registerConfirmPasswordLayout.error = "Las contraseñas no coinciden"
            isValid = false
        }

        return isValid
    }


    private fun showLogin() {
        finish()
    }

    private fun showVerificationAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¡Registro Exitoso!")
        builder.setMessage("Se ha enviado un correo de verificación a tu dirección de email. Por favor, revísalo para poder iniciar sesión.")
        // Al hacer clic en "Aceptar", llevamos al usuario de vuelta a la pantalla de Login
        builder.setPositiveButton("Aceptar") { _, _ ->
            showLogin()
        }
        builder.setCancelable(false) // Evita que el usuario cierre el diálogo sin presionar "Aceptar"
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