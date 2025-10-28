package com.example.trabajointegrador_modulonativo


import android.os.Bundle

import androidx.fragment.app.Fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast


import com.example.trabajointegrador_modulonativo.databinding.FragmentCarDetailBinding

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.trabajointegrador_modulonativo.adapter.AdapterMode
import com.example.trabajointegrador_modulonativo.adapter.ExpenseAdapter
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModel
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch




/**
 * A fragment representing a single car detail screen.
 * This fragment is either contained in a [carListFragment]
 * in two-pane mode (on larger screen devices) or self-contained
 * on handsets.
 */
class carDetailFragment : Fragment() {

    private var _binding: FragmentCarDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CarViewModel by activityViewModels {
        CarViewModelFactory(CarRepository(), ExpenseRepository(), SessionProvider())
    }

    private lateinit var expenseAdapter: ExpenseAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentCarDetailBinding.inflate(inflater, container, false)
        val rootView = binding.root
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecylcerView()

        arguments?.getString(ARG_ITEM_ID)?.let { carId ->
            viewModel.loadCarDetails(carId)
            viewModel.getCarExpenses(carId)
        }
        observeViewModel()

        binding.editCarDataButton?.setOnClickListener {
            val carId = viewModel.selectedCar.value?.id
            if(carId != null) {
                val bundle = Bundle().apply {
                    putString("car_id", carId)
                }
                findNavController().navigate(R.id.action_detail_to_form, bundle)
            } else {
                Toast.makeText(context, "Cargando datos del vehículo...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.addExpensesButton?.setOnClickListener {
            val selectedCar = viewModel.selectedCar.value
            if(selectedCar != null) {
                val action = carDetailFragmentDirections.actionDetailToExpenseForm(selectedCar)
                findNavController().navigate(action)
            } else {
                Toast.makeText(context, "Cargando datos del vehículo...", Toast.LENGTH_SHORT).show()
            }

        }

        binding.viewParkingButton?.setOnClickListener {
            val carId = viewModel.selectedCar.value?.id
            if (carId != null) {
                val bundle = Bundle().apply { putString("car_id", carId) }
                findNavController().navigate(R.id.action_detail_to_parking, bundle)
            } else {
                Toast.makeText(context, "Cargando datos del vehículo...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.viewReminderButton?.setOnClickListener {
            val carId = viewModel.selectedCar.value?.id
            if (carId != null) {
                val bundle = Bundle().apply { putString("car_id", carId) }
                findNavController().navigate(R.id.action_list_to_reminders, bundle)
            } else {
                Toast.makeText(context, "Cargando datos del vehículo...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecylcerView() {
        expenseAdapter = ExpenseAdapter(AdapterMode.SIMPLE_EXPENSE)
        binding.expensesRecyclerView.apply {
            this?.layoutManager = LinearLayoutManager(requireContext())
            this?.adapter = expenseAdapter
        }

    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->

                    }
                }

                launch {
                    viewModel.selectedCar.collect { car ->
                        car?.let {bindCarData(it)}
                    }
                }

                launch {
                    viewModel.carExpenses.collect { expenses ->
                        expenseAdapter.updateExpenses(expenses)
                    }
                }

            }
        }
    }

    private fun bindCarData(car: Car) {
        binding.plateTextView?.text = "Patente: ${car.licensePlate}"
        binding.carBrandTextView?.text = "Marca: ${car.brand}"
        binding.carModelTextView?.text = "Modelo: ${car.model}"
        binding.carYearTextView?.text = "Año: ${car.year}"
        binding.lastModifiedTextView?.text = "Última actualizacion: ${car.lastUpdate}"
        binding.carEngineTextView?.text = "Motor: ${car.engine}"
        binding.carTransmissionTextView?.text = "Transmisión: ${car.transmission}"

        binding.carImageView?.let { imageView ->
            Glide.with(this)
                .load(car.imageUrl)
                .into(imageView)
        }

    }

    companion object {
        const val ARG_ITEM_ID = "item_id"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearSelectedCar()
        _binding = null
    }
}


