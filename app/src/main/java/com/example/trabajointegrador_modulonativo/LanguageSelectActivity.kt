package com.example.trabajointegrador_modulonativo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.trabajointegrador_modulonativo.databinding.ActivityLanguageSelectionBinding
import com.example.trabajointegrador_modulonativo.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth


object LanguageManager {
    fun setAppLanguage(languageCode: String) {

        val localeList = LocaleListCompat.forLanguageTags(languageCode)

        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
// Data class simple para definir idiomas desde código
data class LanguageItem(val key: String, val title: String)

class LanguageSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageSelectionBinding

    private var isChangingLanguage = false

    private val PREFS_NAME = "app_prefs"
    private val KEY_SELECTED_LANG = "selected_language_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        isChangingLanguage = intent.getBooleanExtra("IS_CHANGING_LANGUAGE", false)

        if(isChangingLanguage){
            setupLanguageSelection()
        } else {
            checkCurrentUser()
        }





    }


    private fun checkCurrentUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            startCarHostActivity()
            finish()
        } else {
            setupLanguageSelection()
        }
    }

    private fun setupLanguageSelection(){
        val container = findViewById<LinearLayout>(R.id.languageListContainer)
        val acceptBtn = findViewById<View>(R.id.acceptButton)

        if (isChangingLanguage) {
            (acceptBtn as? TextView)?.text = getString(R.string.volver)
        }

        val languages = listOf(
            LanguageItem("es", getString(R.string.espanol)),
            LanguageItem("en", getString(R.string.ingles)),
            LanguageItem("pt", getString(R.string.portugues)),
        )

        setLanguages(container, languages)

        acceptBtn.setOnClickListener {
            if(isChangingLanguage){
                finish()
            } else {
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val selectedKey = prefs.getString(KEY_SELECTED_LANG, null)
                if (selectedKey != null) {
                    Toast.makeText(this, "Guardando idioma...", Toast.LENGTH_SHORT).show()
                    startLoginActivity()
                    finish()
                } else {
                    Toast.makeText(this, "No seleccionaste ningún idioma.", Toast.LENGTH_SHORT).show()
                }
            }

        }




    }
    private fun setLanguages(container: LinearLayout, languages: List<LanguageItem>) {
        val inflater = LayoutInflater.from(this)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedKey = prefs.getString(KEY_SELECTED_LANG, null)

        val radios = mutableListOf<RadioButton>()

        container.removeAllViews()
        languages.forEach { lang ->
            val row = inflater.inflate(R.layout.item_language, container, false)
            val title = row.findViewById<TextView>(R.id.itemTitle)
            val radio = row.findViewById<RadioButton>(R.id.itemRadio)

            title.text = lang.title

            // Tag con la key para identificar
            row.tag = lang.key
            radio.tag = lang.key
            radio.id = View.generateViewId()

            // si la guardada coincide con la key, márcalo
            if (lang.key == savedKey) radio.isChecked = true

            radios.add(radio)
            container.addView(row)

            // Función auxiliar para aplicar el idioma y guardar
            fun applyLang(key: String) {
                // guardar selección
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SELECTED_LANG, key)
                    .apply()

                LanguageManager.setAppLanguage(key)
            }

            // cuando tocan la fila, actualizamos la selección y aplicamos idioma
            row.setOnClickListener {
                radios.forEach { it.isChecked = false }
                radio.isChecked = true
                applyLang(lang.key)
                radio.post { radio.jumpDrawablesToCurrentState() }

            }

            // si el usuario toca directamente el radio, hacemos lo mismo
            radio.setOnClickListener {
                radios.forEach { it.isChecked = false }
                radio.isChecked = true
                applyLang(lang.key)
                radio.post { radio.jumpDrawablesToCurrentState() }
            }
        }
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java).apply {
        }
        startActivity(intent)

    }

    private fun startCarHostActivity() {
        val intent = Intent(this, carDetailHostActivity::class.java).apply {
        }
        startActivity(intent)

    }

}
