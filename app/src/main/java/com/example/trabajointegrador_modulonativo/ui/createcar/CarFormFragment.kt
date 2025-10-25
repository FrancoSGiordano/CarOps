package com.example.trabajointegrador_modulonativo.ui.createcar

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.databinding.FragmentCreateCarBinding
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModel
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.trabajointegrador_modulonativo.R
import com.google.firebase.FirebaseApp

class CarFormFragment : Fragment()  {

    private var _binding: FragmentCreateCarBinding? = null
    private val binding get() = _binding!!
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val viewModel: CarViewModel by activityViewModels{
        CarViewModelFactory(CarRepository())
    }

    // Uri seleccionada (galería o cámara)
    private var selectedImageUri: Uri? = null
    // Uri temporal para cámara
    private var tempImageUri: Uri? = null

    // Lanzador para elegir imagen desde galería (no necesita permisos)
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivCarPhoto.setImageURI(it)
        }
    }

    // Lanzador para tomar foto y guardar en URI temporal
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempImageUri?.let {
                selectedImageUri = it
                binding.ivCarPhoto.setImageURI(it)
            }
        } else {
            // eliminar archivo temporal si no se usó
            tempImageUri = null
        }
    }

    // Solicitador de permiso CAMERA (si no está concedido)
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val carId: String? by lazy {
        arguments?.getString(ARG_CAR_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?) : View {
        _binding = FragmentCreateCarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Listener del ImageView: abre diálogo de opciones
        binding.ivCarPhoto.setOnClickListener {
            showImageOptionsDialog()
        }

        if (carId == null) {
            setupCreateMode()
        } else {
            setupEditMode(carId!!)
        }
    }

    private fun setupEditMode(id: String) {
        binding.createVehicleTitle.text = "Editar Vehículo"
        binding.createCarButton.text = "Guardar Cambios"

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadCarDetails(id)
            val car = viewModel.selectedCar.first()
            car?.let {
                binding.brandEditText.setText(it.brand)
                binding.modelEditText.setText(it.model)
                binding.yearEditText.setText(it.year.toString())
                binding.plateEditText.setText(it.licensePlate)
                it.imageUrl?.let { url ->
                    // si querés usar Glide/Picasso mejor para cargar desde URL
                    // aquí uso setImageURI solo si ya hay cached Uri (si no, podés usar Glide)
                }
            }
        }

        binding.createCarButton.setOnClickListener {
            val updatedCar = Car(
                id = id,
                brand = binding.brandEditText.text.toString(),
                model = binding.modelEditText.text.toString(),
                year = binding.yearEditText.text.toString().toIntOrNull(),
                licensePlate = binding.plateEditText.text.toString(),
                imageUrl = selectedImageUri?.toString() // opcional: ideal subir y reemplazar
            )
            // Si querés soportar reemplazo de imagen en edición, deberías subir antes y pasar la URL real.
            viewModel.updateCar(updatedCar)
            Toast.makeText(context, "Vehículo actualizado", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun setupCreateMode() {
        binding.createVehicleTitle.text = "Agregar Vehículo"
        binding.createCarButton.text = "Agregar"
        binding.createCarButton.setOnClickListener {
            val brand = binding.brandEditText.text.toString().trim()
            val model = binding.modelEditText.text.toString().trim()
            val yearStr = binding.yearEditText.text.toString().trim()
            val plate = binding.plateEditText.text.toString().trim()

            if(brand.isEmpty() || model.isEmpty() || yearStr.isEmpty() || plate.isEmpty()) {
                Toast.makeText(context, "Por favor, ingrese todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val year = yearStr.toIntOrNull()
            if(year == null){
                Toast.makeText(context, "Por favor, ingrese un año válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Deshabilitar botón para evitar doble click mientras sube
            binding.createCarButton.isEnabled = false
            binding.createCarButton.alpha = 0.6f

            lifecycleScope.launch {
                try {
                    // si hay imagen seleccionada, subir y obtener URL
                    val imageUrl = selectedImageUri?.let { uploadImageAndGetUrl(it) }

                    val newCar = Car(
                        brand = brand,
                        model = model,
                        year = year,
                        licensePlate = plate,
                        lastUpdate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
                        ownerId = currentUser?.uid,
                        imageUrl = imageUrl
                    )

                    viewModel.addCar(newCar)
                    Toast.makeText(context, "Vehiculo creado correctamente", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al crear vehículo: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.createCarButton.isEnabled = true
                    binding.createCarButton.alpha = 1f
                }
            }
        }
    }

    private fun showImageOptionsDialog() {
        val items = arrayOf("Elegir de galería", "Tomar foto")
        AlertDialog.Builder(requireContext())
            .setTitle("Foto del vehículo")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        // galería
                        pickImageLauncher.launch("image/*")
                    }
                    1 -> {
                        // cámara: pedir permiso si es necesario
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }

                }
            }
            .show()
    }

    private fun launchCamera() {
        val file = createTempImageFile()
        tempImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        takePictureLauncher.launch(tempImageUri)
    }

    private fun createTempImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "CAR_${timeStamp}_"
        val storageDir = requireContext().cacheDir
        return File.createTempFile(fileName, ".jpg", storageDir).apply { deleteOnExit() }
    }

    private suspend fun uploadImageAndGetUrl(uri: Uri): String? =
        suspendCancellableCoroutine { cont ->
            val uid = currentUser?.uid ?: ""
            val path = "cars/$uid/${System.currentTimeMillis()}.jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child(path)
            Log.d("DBG", "storageBucket = ${FirebaseApp.getInstance().options.storageBucket}")
            val user = FirebaseAuth.getInstance().currentUser
            Log.d("DBG", "currentUser = $user, uid=${user?.uid}")
            val uploadTask = storageRef.putFile(uri)
            uploadTask
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            if (!cont.isCompleted) cont.resume(downloadUri.toString())
                        }
                        .addOnFailureListener { e ->
                            if (!cont.isCompleted) cont.resumeWithException(e)
                        }
                }
                .addOnFailureListener { e ->
                    if (!cont.isCompleted) cont.resumeWithException(e)
                }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_CAR_ID = "car_id"

        fun newInstance(carId: String? = null): CarFormFragment {
            val fragment = CarFormFragment()
            val args = Bundle()
            carId?.let {
                args.putString(ARG_CAR_ID, it)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
