package com.example.trabajointegrador_modulonativo.ui.insurance

import android.app.DatePickerDialog
import android.content.Intent
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.trabajointegrador_modulonativo.data.InsuranceRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.databinding.FragmentAddInsuranceBinding
import com.example.trabajointegrador_modulonativo.model.Insurance
import com.example.trabajointegrador_modulonativo.viewmodel.InsuranceViewModel
import com.example.trabajointegrador_modulonativo.viewmodel.InsuranceViewModelFactory
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class InsuranceFormFragment : Fragment() {

    private var _binding : FragmentAddInsuranceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InsuranceViewModel by activityViewModels {
        InsuranceViewModelFactory(SessionProvider(), InsuranceRepository())
    }

    private val args: InsuranceFormFragmentArgs by navArgs()

    private var userId: String? = null
    private var carId: String? = null
    private var insurance: Insurance? = null
    private var currentInsurance: Insurance? = null
    private var selectedDate: Date? = null

    private var selectedFileUri: Uri? = null
    private var currentPolicyFileUrl: String? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            binding.fileNameTextView.text = getFileName(it)
            binding.fileNameTextView.visibility = View.VISIBLE
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "archivo"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if(nameIndex != -1){
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?) : View {
        _binding = FragmentAddInsuranceBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = viewModel.sessionProvider.getUserId()
        carId = args.carId
        insurance = args.insurance

        setupClickListeners()


        if(insurance != null){
            setupEditMode(insurance!!)
        } else {
            setupCreateMode()
        }

    }



    private fun setupCreateMode() {
        binding.addInsuranceTitle.text = "Añadir seguro"
        binding.saveInsuranceFab.visibility = View.VISIBLE
        binding.btnDeleteInsurance.visibility = View.GONE

        binding.saveInsuranceFab.setOnClickListener {
            handleSaveInsurance(null)
        }

    }

    private fun setupEditMode(insurance: Insurance){
        binding.addInsuranceTitle.text = "Editar seguro"
        binding.saveInsuranceFab.visibility = View.VISIBLE
        binding.btnDeleteInsurance.visibility = View.VISIBLE


        binding.insuranceNameEditText.setText(insurance.insuranceName)
        binding.insurancePolicyNumberEditText.setText(insurance.policyNumber)
        binding.insuranceCoverageEditText.setText(insurance.coverage)
        binding.engineNumberEditText.setText(insurance.engineNumber)
        binding.chassisNumberEditText.setText(insurance.chassisNumber)
        binding.ownerNameEditText.setText(insurance.policyHolderName)

        insurance.policyFileUrl?.let { url ->
            currentPolicyFileUrl = url
            binding.fileNameTextView.text = "Póliza actual (Click para ver)"
            binding.fileNameTextView.visibility = View.VISIBLE
        } ?: run {
            binding.fileNameTextView.text = ""
            binding.fileNameTextView.visibility = View.GONE
        }

        insurance.expirationDate?.let { dateFromDb ->

            selectedDate = dateFromDb

            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.insuranceExpirationDateEditText.setText(sdf.format(dateFromDb))
        }

        binding.saveInsuranceFab.setOnClickListener {
            handleSaveInsurance(insurance.id)
        }

        binding.btnDeleteInsurance.setOnClickListener {
            viewModel.deleteInsurance(insurance.id!!, carId!!)
            findNavController().popBackStack()
        }

    }

    private fun handleSaveInsurance(id: String?) {
        if (!validateForm()) {
            return
        }

        val insuranceName = binding.insuranceNameEditText.text.toString().trim().uppercase()
        val insurancePolicyNumber = binding.insurancePolicyNumberEditText.text.toString().trim().uppercase()
        val expirationDateString = binding.insuranceExpirationDateEditText.text.toString().trim()
        val insuranceCoverage = binding.insuranceCoverageEditText.text.toString().trim().uppercase()
        val engineNumber = binding.engineNumberEditText.text.toString().trim().uppercase()
        val chassisNumber = binding.chassisNumberEditText.text.toString().trim().uppercase()
        val ownerName = binding.ownerNameEditText.text.toString().trim().uppercase()
        val expirationDateObject = formateDate(expirationDateString)

        binding.saveInsuranceFab.isEnabled = false
        binding.saveInsuranceFab.alpha = 0.6f

        binding.btnDeleteInsurance.isEnabled = false
        binding.btnDeleteInsurance.alpha = 0.6f

        lifecycleScope.launch {
            try {

                val finalPolicyFileUrl: String? = when {
                    selectedFileUri != null -> {
                        uploadFileToStorage(selectedFileUri!!, carId!!, userId!!)
                    }
                    id != null && currentPolicyFileUrl != null -> {

                        currentPolicyFileUrl
                    }
                    else -> {
                        null
                    }
                }


                val insuranceToSave = Insurance(
                    id = id,
                    insuranceName = insuranceName,
                    policyNumber = insurancePolicyNumber,
                    expirationDate = expirationDateObject,
                    coverage = insuranceCoverage,
                    engineNumber = engineNumber,
                    chassisNumber = chassisNumber,
                    policyHolderName = ownerName,
                    carId = carId,
                    userId = userId,
                    policyFileUrl = finalPolicyFileUrl,
                    lastUpdate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()).toString()
                )


                if (id == null) {
                    viewModel.addInsurance(insuranceToSave, carId!!)
                    Toast.makeText(requireContext(), "Seguro añadido con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateInsurance(insuranceToSave)
                    Toast.makeText(requireContext(), "Seguro actualizado con éxito", Toast.LENGTH_SHORT).show()
                }
                findNavController().popBackStack()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al guardar/actualizar el seguro: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.saveInsuranceFab.isEnabled = true
                binding.saveInsuranceFab.alpha = 1f

                binding.btnDeleteInsurance.isEnabled = true
                binding.btnDeleteInsurance.alpha = 1f
            }
        }
    }

    private suspend fun uploadFileToStorage(fileUri: Uri, carId: String, userId: String): String? {
        return try {
            val fileName = getFileName(fileUri)
            val storagePath = "policies/$userId/$carId/${UUID.randomUUID()}_$fileName"
            val storageRef = Firebase.storage.reference.child(storagePath)

            storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            downloadUrl
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al subir el archivo: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun setupClickListeners() {
        binding.insuranceExpirationDateEditText.setOnClickListener {
            binding.insuranceExpirationDateLayout.error = null
            showDatePickerDialog(binding.insuranceExpirationDateEditText) { selectedDate ->
                this.selectedDate = selectedDate
            }
        }

        binding.btnSelectFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }

        binding.fileNameTextView.setOnClickListener {
            currentPolicyFileUrl?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
    }


    private fun showDatePickerDialog(targetEditText: TextInputEditText, onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->

                val selectedCalendar = Calendar.getInstance().apply { set(year, month, day) }

                val dateObject = selectedCalendar.time

                val formattedDateForDisplay = String.format("%02d/%02d/%d", day, month + 1, year)
                targetEditText.setText(formattedDateForDisplay)

                onDateSelected(dateObject)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }


    private fun formateDate(date: String): Date? {
        if(date.isBlank()) {
            return null
        }

        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            format.parse(date)
        } catch (e: Exception) {
            null
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        binding.insuranceNameLayout.error = null
        binding.insurancePolicyNumberLayout.error = null
        binding.insuranceExpirationDateLayout.error = null
        binding.insuranceCoverageLayout.error = null
        binding.engineNumberLayout.error = null
        binding.chassisNumberLayout.error = null
        binding.ownerNameLayout.error = null

        if(binding.insuranceNameEditText.text.toString().trim().isEmpty()){
            binding.insuranceNameLayout.error = "El nombre de la aseguradora es obligatorio"
            isValid = false
        }

        if(binding.insurancePolicyNumberEditText.text.toString().trim().isEmpty()){
            binding.insurancePolicyNumberLayout.error = "El numero de poliza es obligatorio"
            isValid = false
        }

        if(binding.insuranceExpirationDateEditText.text.toString().trim().isEmpty()){
            binding.insuranceExpirationDateLayout.error = "La fecha de expiración es obligatoria"
            isValid = false
        } else {

        }

       if(binding.insuranceCoverageEditText.toString().trim().isEmpty()){
           binding.insuranceCoverageLayout.error = "El tipo de cobertura es obligatorio"
           isValid = false
       }

        if(binding.engineNumberEditText.text.toString().trim().isEmpty()){
            binding.engineNumberLayout.error = "El numero de motor es obligatorio"
            isValid = false
        }

        if(binding.chassisNumberEditText.text.toString().trim().isEmpty()){
            binding.chassisNumberEditText.error = "El numero de chasis es obligatorio"
            isValid = false
        }

        if(binding.ownerNameEditText.text.toString().trim().isEmpty()){

            binding.ownerNameLayout.error = "El titular es obligatorio"
            isValid = false
        }

        return isValid
    }
}