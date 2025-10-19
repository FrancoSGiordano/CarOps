package com.example.trabajointegrador_modulonativo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.trabajointegrador_modulonativo.databinding.ActivityCarDetailBinding
import android.util.Log
import androidx.appcompat.widget.Toolbar

class carDetailHostActivity : AppCompatActivity() {
    private val TAG = "CarDetailHost"
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // <-- Asegurate de tener un Toolbar en el layout con id = toolbar
        setSupportActionBar(binding.toolbar)

        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_car_detail) as? NavHostFragment
        if (fragment == null) {
            Log.e(TAG, "NavHostFragment NO encontrado")
            finish()
            return
        }

        val navController = fragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_car_detail)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}