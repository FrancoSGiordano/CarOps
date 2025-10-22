package com.example.trabajointegrador_modulonativo.ui.createcar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.databinding.FragmentCreateCarBinding
import com.example.trabajointegrador_modulonativo.model.Car
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
        CarViewModelFactory(CarRepository())
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
                binding.brandEditText.setText(it.brand)
                binding.modelEditText.setText(it.model)
                binding.yearEditText.setText(it.year.toString())
                binding.plateEditText.setText(it.licensePlate)
            }
        }

        binding.createCarButton.setOnClickListener {
            val updatedCar = Car(
                id = id,
                brand = binding.brandEditText.text.toString(),
                model = binding.modelEditText.text.toString(),
                year = binding.yearEditText.text.toString().toIntOrNull(),
                licensePlate = binding.plateEditText.text.toString()
            )
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

            val newCar = Car(
                brand = brand,
                model = model,
                year = year,
                licensePlate = plate,
                lastUpdate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
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