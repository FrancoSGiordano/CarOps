package com.example.trabajointegrador_modulonativo.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.semantics.error
import com.example.trabajointegrador_modulonativo.R
import com.example.trabajointegrador_modulonativo.carDetailHostActivity
import com.example.trabajointegrador_modulonativo.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        checkCurrentUser()

    }

    private fun checkCurrentUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val refreshedUser = FirebaseAuth.getInstance().currentUser
                    if (refreshedUser != null && refreshedUser.isEmailVerified) {
                        startCarHostActivity()
                        finish()
                    } else {
                        setup()
                    }
                } else {
                    setup()
                }
            }
        } else {

            setup()
        }
    }

    override fun onResume() {
        super.onResume()
        resetErrors()
    }

    private fun setup() {


        binding.registerTextView.setOnClickListener {
            val registerIntent = Intent(this, RegisterActivity::class.java)
            startActivity(registerIntent)
        }

        binding.loginButton.setOnClickListener {
            if(!validateForm()) {
                return@setOnClickListener
            }
            login()
        }
    }

    private fun login() {
        val email = binding.loginEmailEditText.text.toString()
        val password = binding.loginPasswordEditText.text.toString()

        binding.errorTextView.visibility = View.GONE

        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null && user.isEmailVerified) {
                        startCarHostActivity()
                        finish()
                    } else {
                        showError(getString(R.string.verificar_correo))
                        FirebaseAuth.getInstance().signOut()
                    }

                } else {
                    var errorMessage: String

                    try {
                        throw it.exception!!
                    } catch (e: FirebaseAuthInvalidUserException) {
                        errorMessage = getString(R.string.credenciales_invalidas)
                    } catch (e: FirebaseAuthInvalidCredentialsException) {
                        errorMessage = getString(R.string.credenciales_invalidas)
                    } catch (e: Exception) {
                        errorMessage = e.localizedMessage ?: getString(R.string.error_inesperado)
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
    private fun resetErrors() {
        binding.loginEmailLayout.error = null
        binding.loginPasswordLayout.error = null
        binding.errorTextView.visibility = View.GONE
    }

    private fun validateForm(): Boolean {
        var isValid = true

        binding.loginEmailLayout.error = null
        binding.loginPasswordLayout.error = null

        val email = binding.loginEmailEditText.text.toString()
        val password = binding.loginPasswordEditText.text.toString()

        if (email.isEmpty()){
            binding.loginEmailLayout.error = getString(R.string.debe_ingresar_correo)
            isValid = false
        }

        if(password.isEmpty()){
            binding.loginPasswordLayout.error = getString(R.string.debe_ingresar_contrasena)
            isValid = false
        }

        return isValid

    }



    private fun startCarHostActivity() {
        val intent = Intent(this, carDetailHostActivity::class.java).apply {
        }
        startActivity(intent)

    }
    private fun showAlert(message: String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.error))
        builder.setMessage(message)
        builder.setPositiveButton(getString(R.string.aceptar), null)
        val dialog: AlertDialog = builder.create()
        dialog.show()

    }
}