package com.example.trabajointegrador_modulonativo.ui.car

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.text.toUpperCase
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CarFormFragment : Fragment()  {

    private var _binding: FragmentCreateCarBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CarViewModel by activityViewModels{
        CarViewModelFactory(CarRepository(), ExpenseRepository(), SessionProvider())
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
                binding.carBrandEditText.setText(it.brand)
                binding.carModelEditText.setText(it.model)
                binding.carYearEditText.setText(it.year.toString())
                binding.carPlateEditText.setText(it.licensePlate)
                binding.carEngineEditText.setText(it.engine)
                binding.carTransmissionEditText.setText(it.transmission)
            }
        }

        binding.createCarButton.setOnClickListener {
            if(!validateForm()){
                return@setOnClickListener
            }

            val updatedCar = Car(
                id = id,
                brand = binding.carBrandEditText.text.toString().uppercase(),
                model = binding.carModelEditText.text.toString().uppercase(),
                year = binding.carYearEditText.text.toString().toIntOrNull(),
                licensePlate = binding.carPlateEditText.text.toString().uppercase(),
                engine = binding.carEngineEditText.text.toString().uppercase(),
                transmission = binding.carTransmissionEditText.text.toString().uppercase(),
                lastUpdate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()).toString()
            )

            viewModel.updateCar(updatedCar)
            Toast.makeText(context, "Vehículo actualizado", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
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


            val newCar = Car(
                brand = brand,
                model = model,
                year = year,
                licensePlate = plate,
                engine = engine,
                transmission = transmission,
                lastUpdate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()).toString()
            )

            viewModel.addCar(newCar)
            Toast.makeText(context, "Vehiculo creado correctamente", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
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