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
import com.example.trabajointegrador_modulonativo.R
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.InsuranceRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.databinding.FragmentCreateCarBinding
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.ui.expense.ExpenseFormFragment
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModel
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModelFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class CarFormFragment : Fragment()  {

    private var _binding: FragmentCreateCarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CarViewModel by activityViewModels{
        CarViewModelFactory(CarRepository(), ExpenseRepository(), SessionProvider(),
            InsuranceRepository())
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
            Toast.makeText(requireContext(), getString(R.string.permiso_camara_denegado), Toast.LENGTH_SHORT).show()
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
        binding.createVehicleTitle.text = getString(R.string.editar_vehiculo)
        binding.createCarButton.text = getString(R.string.guardar)

        viewLifecycleOwner.lifecycleScope.launch {
            val car = viewModel.selectedCar.first()
            car?.let {
                currentCar = it
                binding.carBrandEditText.setText(it.brand)
                binding.carModelEditText.setText(it.model)
                binding.carYearEditText.setText(it.year?.toString() ?: "")
                binding.carPlateEditText.setText(it.licensePlate)
                binding.carEngineEditText.setText(it.engine)
                binding.carTransmissionEditText.setText(it.transmission)


                it.imageUrl?.let { url ->
                    try {
                        com.bumptech.glide.Glide.with(this@CarFormFragment)
                            .load(url)
                            .centerCrop()
                            .into(binding.ivCarPhoto)
                    } catch (e: Exception) {
                    }
                }
            }
        }

        binding.createCarButton.setOnClickListener {
            if(!validateForm()) return@setOnClickListener

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
                    Toast.makeText(context, getString(R.string.vehiculo_actualizado), Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(context, getString(R.string.error_actualizar_vehiculo , e.message),  Toast.LENGTH_LONG).show()
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
            binding.carBrandLayout.error = getString(R.string.marca_obligatoria)
            isValid = false
        }

        if(binding.carModelEditText.text.toString().trim().isEmpty()){
            binding.carModelLayout.error = getString(R.string.modelo_obligatorio)
            isValid = false
        }

        if(binding.carYearEditText.text.toString().trim().isEmpty()){
            binding.carYearLayout.error = getString(R.string.anio_oblligatorio)
            isValid = false
        } else {
            val year = binding.carYearEditText.text.toString().toIntOrNull()
            if (year == null || year < 1917 || year > java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) {
                binding.carYearLayout.error = getString(R.string.anio_invalido)
                isValid = false
            }
        }

        val plate = binding.carPlateEditText.text.toString().trim()
        if(plate.isEmpty()){
            binding.carPlateLayout.error = getString(R.string.patente_obligatoria)
            isValid = false
        } else {
            if(!validatePlate(plate)){
                binding.carPlateLayout.error = getString(R.string.patente_invalida)
                isValid = false
            }
        }

        if(binding.carEngineEditText.text.toString().trim().isEmpty()){
            binding.carEngineLayout.error = getString(R.string.motor_obligatorio)
            isValid = false
        }

        if(binding.carTransmissionEditText.text.toString().trim().isEmpty()){
            binding.carTransmissionLayout.error = getString(R.string.transmision_obligatorio)
            isValid = false
        }

        return isValid
    }

     fun validatePlate(plate: String): Boolean {

        val plateRegex = Regex("^[A-Z]{3}\\d{3}$|^[A-Z]{2}\\d{3}[A-Z]{2}$", RegexOption.IGNORE_CASE)

        return plateRegex.matches(plate)
    }

    private fun setupCreateMode() {
        binding.createVehicleTitle.text = getString(R.string.agregar_vehiculo)
        binding.createCarButton.text = getString(R.string.agregar)
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
                    Toast.makeText(context, getString(R.string.vehiculo_creado_correctamente), Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } catch (e: Exception) {
                    Toast.makeText(context, getString(R.string.error_crear_vehiculo, e.message) , Toast.LENGTH_LONG).show()
                } finally {
                    binding.createCarButton.isEnabled = true
                    binding.createCarButton.alpha = 1f
                }
            }



        }
    }

    private fun showImageOptionsDialog() {
        val items = arrayOf(getString(R.string.galeria), getString(R.string.foto))
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.foto_vehiculo))
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