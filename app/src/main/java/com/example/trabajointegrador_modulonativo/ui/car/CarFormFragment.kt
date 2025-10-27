package com.example.trabajointegrador_modulonativo.ui.car

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.toUpperCase
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.databinding.FragmentCreateCarBinding
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.ui.expense.ExpenseFormFragment
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModel
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModelFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.jar.Manifest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CarFormFragment : Fragment()  {

    private var _binding: FragmentCreateCarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CarViewModel by activityViewModels{
        CarViewModelFactory(CarRepository(), ExpenseRepository(), SessionProvider())
    }

    private val carId: String? by lazy {
        arguments?.getString(ARG_CAR_ID)
    }

    private var selectedImageUri: Uri? = null
    private var tempImageUri: Uri? = null
    private var currentCar: Car? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivCarPhoto.setImageURI(it)
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempImageUri?.let {
                selectedImageUri = it
                binding.ivCarPhoto.setImageURI(it)
            }
        } else {

            tempImageUri = null
        }
    }


    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
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
                currentCar = it
                binding.carBrandEditText.setText(it.brand)
                binding.carModelEditText.setText(it.model)
                binding.carYearEditText.setText(it.year?.toString() ?: "")
                binding.carPlateEditText.setText(it.licensePlate)
                binding.carEngineEditText.setText(it.engine)
                binding.carTransmissionEditText.setText(it.transmission)

                // Mostrar imagen si existe (usa Glide o similar)
                it.imageUrl?.let { url ->
                    // si no tenés Glide importado, agregalo a dependencias
                    try {
                        com.bumptech.glide.Glide.with(this@CarFormFragment)
                            .load(url)
                            .centerCrop()
                            .into(binding.ivCarPhoto)
                    } catch (e: Exception) {
                        // fallback si no se puede cargar
                    }
                }
            }
        }

        binding.createCarButton.setOnClickListener {
            if(!validateForm()) return@setOnClickListener

            // Hacemos todo en coroutine porque podemos necesitar subir la imagen
            viewLifecycleOwner.lifecycleScope.launch {
                binding.createCarButton.isEnabled = false
                binding.createCarButton.alpha = 0.6f
                try {
                    // Si hay nueva imagen -> subir y obtener URL, si no -> conservar la anterior
                    val imageUrl = selectedImageUri?.let { uri ->
                        uploadImageAndGetUrl(uri) // suspend function
                    } ?: currentCar?.imageUrl

                    val updatedCar = Car(
                        id = id,
                        brand = binding.carBrandEditText.text.toString().trim().uppercase(Locale.getDefault()),
                        model = binding.carModelEditText.text.toString().trim().uppercase(Locale.getDefault()),
                        year = binding.carYearEditText.text.toString().toIntOrNull(),
                        licensePlate = binding.carPlateEditText.text.toString().trim().uppercase(Locale.getDefault()),
                        engine = binding.carEngineEditText.text.toString().trim().uppercase(Locale.getDefault()),
                        transmission = binding.carTransmissionEditText.text.toString().trim().uppercase(Locale.getDefault()),
                        imageUrl = imageUrl,
                        lastUpdate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()).toString()
                    )

                    viewModel.updateCar(updatedCar)
                    Toast.makeText(context, "Vehículo actualizado", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al actualizar vehículo: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    binding.createCarButton.isEnabled = true
                    binding.createCarButton.alpha = 1f
                }
            }
        }
    }


    private fun validateForm() : Boolean {
        var isValid = true

        binding.carBrandLayout.error = null
        binding.carModelLayout.error = null
        binding.carYearLayout.error = null
        binding.carPlateLayout.error = null
        binding.carEngineLayout.error = null
        binding.carTransmissionLayout.error = null

        if(binding.carBrandEditText.text.toString().trim().isEmpty()){
            binding.carBrandLayout.error = "La marca es obligatoria"
            isValid = false
        }

        if(binding.carModelEditText.text.toString().trim().isEmpty()){
            binding.carModelLayout.error = "El modelo es obligatorio"
            isValid = false
        }

        if(binding.carYearEditText.text.toString().trim().isEmpty()){
            binding.carYearLayout.error = "El año es obligatorio"
            isValid = false
        } else {
            val year = binding.carYearEditText.text.toString().toIntOrNull()
            if (year == null || year < 1917 || year > java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) {
                binding.carYearLayout.error = "El año ingresao es invalido"
                isValid = false
            }
        }

        val plate = binding.carPlateEditText.text.toString().trim()
        if(plate.isEmpty()){
            binding.carPlateLayout.error = "La patente es obligatoria"
            isValid = false
        } else {
            if(!validatePlate(plate)){
                binding.carPlateLayout.error = "La patente ingresada es invalida"
                isValid = false
            }
        }

        if(binding.carEngineEditText.text.toString().trim().isEmpty()){
            binding.carEngineLayout.error = "El motor es obligatorio"
            isValid = false
        }

        if(binding.carTransmissionEditText.text.toString().trim().isEmpty()){
            binding.carTransmissionLayout.error = "La transmisión es obligatoria"
            isValid = false
        }

        return isValid
    }

     fun validatePlate(plate: String): Boolean {

        val plateRegex = Regex("^[A-Z]{3}\\d{3}$|^[A-Z]{2}\\d{3}[A-Z]{2}$", RegexOption.IGNORE_CASE)

        return plateRegex.matches(plate)
    }

    private fun setupCreateMode() {
        binding.createVehicleTitle.text = "Agregar Vehículo"
        binding.createCarButton.text = "Agregar"
        binding.createCarButton.setOnClickListener {
            if(!validateForm()){
                return@setOnClickListener
            }
            val brand = binding.carBrandEditText.text.toString().trim().uppercase()
            val model = binding.carModelEditText.text.toString().trim().uppercase()
            val yearStr = binding.carYearEditText.text.toString().trim().uppercase()
            val plate = binding.carPlateEditText.text.toString().trim().uppercase()
            val year = yearStr.toIntOrNull()
            val engine = binding.carEngineEditText.text.toString().trim().uppercase()
            val transmission = binding.carTransmissionEditText.text.toString().trim().uppercase()

            binding.createCarButton.isEnabled = false
            binding.createCarButton.alpha = 0.6f

            lifecycleScope.launch {
                try {
                    val imageUrl = selectedImageUri?.let {uploadImageAndGetUrl(it)}

                    val newCar = Car(
                        brand = brand,
                        model = model,
                        year = year,
                        licensePlate = plate,
                        engine = engine,
                        transmission = transmission,
                        lastUpdate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()).toString(),
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
                        pickImageLauncher.launch("image/*")
                    }
                    1 -> {
                        requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
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
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
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