package com.example.trabajointegrador_modulonativo


import android.content.Intent
import android.net.Uri
import android.os.Bundle

import androidx.fragment.app.Fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.ui.unit.Velocity


import com.example.trabajointegrador_modulonativo.databinding.FragmentCarDetailBinding

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.trabajointegrador_modulonativo.adapter.AdapterMode
import com.example.trabajointegrador_modulonativo.adapter.ExpenseAdapter
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.InsuranceRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.model.Insurance
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModel
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModelFactory
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
        CarViewModelFactory(CarRepository(), ExpenseRepository(), SessionProvider(),
            InsuranceRepository())
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

        binding.btnAddInsurance?.setOnClickListener {
            val carId = viewModel.selectedCar.value?.id
            val insurance = viewModel.selectedCar.value?.insurance
            if(carId != null) {
                val action = carDetailFragmentDirections.actionDetailToInsuranceForm(carId, insurance)
                findNavController().navigate(action)
            } else {
                Toast.makeText(context, "No se puedo obtener el ID del vehículo", Toast.LENGTH_SHORT).show()
            }

        }

        binding.editInsuranceDataButton?.setOnClickListener {
            val carId = viewModel.selectedCar.value?.id
            val insurance = viewModel.selectedCar.value?.insurance
            if(carId != null) {
                val action = carDetailFragmentDirections.actionDetailToInsuranceForm(carId, insurance)
                findNavController().navigate(action)
            } else {
                Toast.makeText(context, "No se puedo obtener el ID del vehículo", Toast.LENGTH_SHORT).show()
            }
        }

        binding.showCirculationCardButton?.setOnClickListener {
            showInsurancePolicy(viewModel.selectedCar.value?.insurance?.policyFileUrl)
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

        binding.showExpensesButton?.setOnClickListener {
            val carId = viewModel.selectedCar.value?.id
            if(carId != null) {
                val action = carDetailFragmentDirections.actionDetailToInsuranceList(carId)
                findNavController().navigate(action)
            } else {
                Toast.makeText(context, "No se puedo obtener el ID del vehículo", Toast.LENGTH_SHORT).show()
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
                        car?.let {
                            bindInsuranceData(it.insurance)
                            bindCarData(it)}
                    }
                }

                launch {
                    viewModel.carExpenses.collect { expenses ->
                        expenseAdapter.updateExpenses(expenses)

                        if(expenses.isEmpty()) {
                            binding.noExpensesContainer?.visibility = View.VISIBLE
                            binding.expensesRecyclerView?.visibility = View.GONE
                        } else {
                            binding.expensesRecyclerView?.visibility = View.VISIBLE
                            binding.noExpensesContainer?.visibility = View.GONE
                        }
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
        binding.lastModifiedTextView?.text = "Última Actualización: ${car.lastUpdate}"
        binding.carEngineTextView?.text = "Motor: ${car.engine}"
        binding.carTransmissionTextView?.text = "Transmisión: ${car.transmission}"

        binding.carImageView?.let { imageView ->

            val requestOptions = RequestOptions()
                .transform(CenterCrop(), RoundedCorners(24))

            val imageUrl = car.imageUrl
            if (imageUrl != null) {
                Glide.with(this)
                    .load(imageUrl)
                    .apply(requestOptions)
                    .placeholder(R.drawable.generic_car_icon)
                    .error(R.drawable.generic_car_icon)
                    .into(imageView)
            } else {
                Glide.with(this)
                    .load(R.drawable.generic_car_icon)
                    .transform(RoundedCorners(16))
                    .into(imageView)
            }
        }


    }

    private fun bindInsuranceData(insurance: Insurance?){
        if (insurance != null) {
            binding.insuranceDetailsContent?.visibility = View.VISIBLE
            binding.insuranceButtonsContainer?.visibility = View.VISIBLE
            binding.addInsuranceContainer?.visibility = View.GONE


            binding.lastModifiedInsuranceTextView?.text = "Última actualización: ${insurance.lastUpdate}"
            binding.insuranceNameTextView?.text = "Aseguradora: ${insurance.insuranceName}"
            binding.policyNumberTextView?.text = "N° de póliza: ${insurance.policyNumber}"

            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            binding.expirationDateTextView?.text = "Expiración: ${insurance.expirationDate?.let { sdf.format(it) }?: "N/A"}"
            binding.coverageTypeTextView?.text = "Cobertura: ${insurance.coverage}"

            binding.engineNumberTextView?.text = "N° de motor: ${insurance.engineNumber}"
            binding.chassisNumberTextView?.text = "N° de chasis: ${insurance.chassisNumber}"
            binding.ownerNameTextView?.text = "Titular de la póliza: ${insurance.policyHolderName}"

        } else {
            binding.insuranceDetailsContent?.visibility = View.GONE
            binding.insuranceButtonsContainer?.visibility = View.GONE
            binding.addInsuranceContainer?.visibility = View.VISIBLE
        }


    }


    private fun showInsurancePolicy(policyFileUrl: String?) {
        if (!policyFileUrl.isNullOrBlank()) {
            val fileUrl = policyFileUrl
            val uri = Uri.parse(fileUrl)

            val extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

            val intent = Intent(Intent.ACTION_VIEW)

            if (mimeType != null) {
                intent.setDataAndType(uri, mimeType)
            } else {
                intent.setData(uri)
            }

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "No hay aplicación para abrir este archivo.", Toast.LENGTH_LONG).show()
            }

        } else {
            Toast.makeText(requireContext(), "No hay archivo de póliza adjunto.", Toast.LENGTH_SHORT).show()
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



