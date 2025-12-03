package com.example.trabajointegrador_modulonativo.ui.parking

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.trabajointegrador_modulonativo.R
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.InsuranceRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.databinding.FragmentParkingBinding
import com.example.trabajointegrador_modulonativo.ui.car.CarFormFragment.Companion.ARG_CAR_ID
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModel
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModelFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import android.graphics.Color
@OptIn(ExperimentalCoroutinesApi::class)
class ParkingFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentParkingBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null

    // Fused location provider
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(requireActivity()) }
    private val viewModel: CarViewModel by activityViewModels {
        CarViewModelFactory(CarRepository(), ExpenseRepository(), SessionProvider(),
            InsuranceRepository()
        )
    }

    private var lastLocation: Location? = null

    private val selectedCarIdFromArgs: String? by lazy {
        arguments?.getString(ARG_CAR_ID)
    }

    private var pendingParkedLatLng: LatLng? = null
    private var pendingParkedAddress: String? = null

    private var pendingParkedAt: com.google.firebase.Timestamp? = null

    private val currentLocationMarkerIcon by lazy {
        val hsv = FloatArray(3)
        Color.colorToHSV(Color.parseColor("#0067FF"), hsv) // Blue
        BitmapDescriptorFactory.defaultMarker(hsv[0])
    }
    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                enableLocationAndMoveToCurrent()
            } else {
                Toast.makeText(requireContext(), getString(R.string.ubicacion_denegado), Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentParkingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveParking.isEnabled = false
        binding.btnSaveParking.alpha = 0.6f

        var mapFragment = childFragmentManager.findFragmentById(binding.mapContainer.id) as? SupportMapFragment
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
            childFragmentManager.beginTransaction()
                .replace(binding.mapContainer.id, mapFragment)
                .commitNowAllowingStateLoss()
        }

        mapFragment.getMapAsync(this)


        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.selectedCar.collect { car ->
                if (car != null && car.parked == true && car.parkedLat != null && car.parkedLng != null) {
                    val lat = car.parkedLat!!
                    val lng = car.parkedLng!!
                    val ts = car.parkedDate
                    pendingParkedAt = ts

                    Log.d("ParkingFragment", "selectedCar tiene ubicación guardada: $lat,$lng")
                    pendingParkedLatLng = LatLng(lat, lng)
                    applyParkedIfAvailable()
                } else {

                    if (pendingParkedLatLng == null) {
                        Log.d("ParkingFragment", "selectedCar no tiene parked -> pedimos ubicación actual")
                        checkLocationPermissionAndEnable()
                    } else {
                        Log.d("ParkingFragment", "pendingParkedLatLng ya presente, no pedir ubicación actual")
                    }
                }
            }
        }

        binding.btnGetLocation.setOnClickListener {
            pendingParkedLatLng = null
            pendingParkedAddress = null
            checkLocationPermissionAndEnable()
        }

        binding.btnSaveParking.setOnClickListener {
            val loc = lastLocation
            val selectedCarId = selectedCarIdFromArgs
            if (selectedCarId.isNullOrBlank()) {
                Toast.makeText(requireContext(), getString(R.string.no_vehiculo_seleccionado), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (loc == null) {
                Toast.makeText(requireContext(), getString(R.string.ubicacion_no_disponible), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveParking(
                carId = selectedCarId,
                lat = loc.latitude,
                lng = loc.longitude,

            )

            Toast.makeText(requireContext(), getString(R.string.ubicacion_guardada), Toast.LENGTH_SHORT).show()

            googleMap?.let { g ->
                val pos = LatLng(loc.latitude, loc.longitude)
                g.clear()
                g.addMarker(MarkerOptions().position(pos).title(getString(R.string.estacionado)))
                g.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 17f))
            }
        }

        binding.helpButton.setOnClickListener {
            binding.helpText.visibility = if (binding.helpText.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            try {
                googleMap?.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                Log.w("ParkingFragment", "No se pudo activar myLocation", e)
            }
        }
        if (pendingParkedLatLng != null) {
            Log.d("ParkingFragment", "onMapReady -> aplicando parked pendiente")
            showParkedLocationOnMapIfReady()
        } else {
            Log.d("ParkingFragment", "onMapReady -> no hay parked pendiente -> pedir ubicación actual")
            checkLocationPermissionAndEnable()
        }
    }

    private fun applyParkedIfAvailable() {
        if (pendingParkedLatLng != null) {
            showParkedLocationOnMapIfReady()
        }
    }

    private fun showParkedLocationOnMapIfReady() {
        val pending = pendingParkedLatLng ?: return
        val g = googleMap ?: return

        g.clear()

        g.addMarker(
            MarkerOptions()
                .position(pending)
                .title(getString(R.string.estacionado)).icon(currentLocationMarkerIcon)
                .snippet(pendingParkedAddress)
        )
        g.animateCamera(CameraUpdateFactory.newLatLngZoom(pending, 17f))
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        val formatted = pendingParkedAt?.toDate()?.let { sdf.format(it) } ?: "--"

        binding.tvStatus.text = getString(R.string.actualizacion_ubicacion, formatted)


        pendingParkedLatLng = null
        pendingParkedAddress = null
        pendingParkedAt = null

    }

    private fun checkLocationPermissionAndEnable() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> {
                enableLocationAndMoveToCurrent()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @Suppress("MissingPermission")
    private fun enableLocationAndMoveToCurrent() {
        try {
            googleMap?.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            Log.w("ParkingFragment", "enableLocationAndMoveToCurrent: sin permiso", e)
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d("ParkingFragment", "lastLocation success: $location")
                    lastLocation = location
                    // si hay parked pendiente NO queremos centrar en ubicación actual
                    if (pendingParkedLatLng == null) {
                        moveCameraToLocation(location)
                        enableSaveButton(true)
                    } else {
                        Log.d("ParkingFragment", "Hay parked pendiente: no centramos en ubicación actual")
                    }
                } else {
                    Log.d("ParkingFragment", "lastLocation null -> solicitando getCurrentLocation")
                    fusedLocationClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).addOnSuccessListener { loc ->
                        if (loc != null) {
                            Log.d("ParkingFragment", "getCurrentLocation success: $loc")
                            lastLocation = loc
                            if (pendingParkedLatLng == null) {
                                moveCameraToLocation(loc)
                                enableSaveButton(true)
                            } else {
                                Log.d("ParkingFragment", "Hay parked pendiente: no centramos en ubicación actual")
                            }
                        } else {
                            Log.w("ParkingFragment", "getCurrentLocation devolvió null")
                            Toast.makeText(requireContext(), getString(R.string.obtener_ubicacion_fallido), Toast.LENGTH_LONG).show()
                            enableSaveButton(false)
                        }
                    }.addOnFailureListener {
                        Log.w("ParkingFragment", "Error getCurrentLocation: ${it.message}", it)
                        Toast.makeText(requireContext(), getString(R.string.error_ubicacion, it.message), Toast.LENGTH_SHORT).show()
                        enableSaveButton(false)
                    }
                }
            }
            .addOnFailureListener {
                Log.w("ParkingFragment", "Error lastLocation: ${it.message}", it)
                Toast.makeText(requireContext(), getString(R.string.error_ubicacion, it.message), Toast.LENGTH_SHORT).show()
                enableSaveButton(false)
            }
    }

    private fun moveCameraToLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        googleMap?.clear()
        googleMap?.addMarker(MarkerOptions().position(latLng).title(getString(R.string.tu_ubicacion)))


        val zoomLevel = 17f
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel))
    }



    private fun enableSaveButton(enabled: Boolean) {
        binding.btnSaveParking.isEnabled = enabled
        binding.btnSaveParking.alpha = if (enabled) 1.0f else 0.6f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
