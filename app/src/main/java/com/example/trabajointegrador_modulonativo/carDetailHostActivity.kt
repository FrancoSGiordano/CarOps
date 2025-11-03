package com.example.trabajointegrador_modulonativo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import android.app.Activity
import androidx.activity.enableEdgeToEdge


class carDetailHostActivity : AppCompatActivity() {
    private val TAG = "CarDetailHost"
    private val REQ_NOTIF = 1001

    private lateinit var appBarConfiguration: AppBarConfiguration
    private val requestNotifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // permiso concedido
            } else {
                // permiso denegado
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                        showOpenSettingsDialog()
                    } else {
                        showRationaleExplainingDialog()
                    }
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityCarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureNotificationPermission()

        setSupportActionBar(binding.carDetailToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_car_detail) as? NavHostFragment
        if (fragment == null) {
            Log.e(TAG, "NavHostFragment NO encontrado")
            finish()
            return
        }

        val navController = fragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
            }
        }
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

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.permiso_notificaciones))
                .setMessage(getString(R.string.necesita_permiso))
                .setPositiveButton(getString(R.string.permitir)) { _, _ ->
                    requestNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                .setNegativeButton(getString(R.string.ahora_no), null)
                .show()
        } else {
            requestNotifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun openRequestExactAlarms(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(intent)
            } catch (e: Exception) {
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                i.data = Uri.parse("package:${activity.packageName}")
                activity.startActivity(i)
            }
        }
    }

    private fun showRationaleExplainingDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.notificaciones))
            .setMessage(getString(R.string.aviso_rechazo_notificacion))
            .setPositiveButton(getString(R.string.abrir_ajustes)) { _, _ -> openAppNotificationSettings() }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun showOpenSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.habilitar_notificaciones))
            .setMessage(getString(R.string.rechazo_permanente))
            .setPositiveButton("Ir a ajustes") { _, _ -> openAppNotificationSettings() }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun openAppNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra("app_package", packageName)
            putExtra("app_uid", applicationInfo.uid)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            i.data = Uri.parse("package:$packageName")
            startActivity(i)
        }
    }
}
