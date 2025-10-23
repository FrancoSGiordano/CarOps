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
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import kotlin.math.max

class carDetailHostActivity : AppCompatActivity() {
    private val TAG = "CarDetailHost"

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar: MaterialToolbar = findViewById(R.id.car_detail_toolbar)
        val root: View = findViewById(R.id.container)
        val navHost: View = findViewById(R.id.nav_host_fragment_car_detail)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Aplicar insets a las vistas relevantes
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            // pedimos insets por separado y calculamos el top que queramos usar
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutoutInsets = insets.getInsets(WindowInsetsCompat.Type.displayCutout())

            // usamos el top más alto (por notch o por status bar)
            val topInset = max(systemBarsInsets.top, displayCutoutInsets.top)

            // aplicamos padding superior al toolbar
            toolbar.updatePadding(top = topInset)

            // si usas un BottomBar o querés empujar contenido inferior:
            // val bottomInset = systemBarsInsets.bottom
            // someBottomView.updatePadding(bottom = bottomInset)

            WindowInsetsCompat.CONSUMED
        }
        // <-- Asegurate de tener un Toolbar en el layout con id = toolbar
        setSupportActionBar(binding.carDetailToolbar)

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}