package com.example.trabajointegrador_modulonativo.ui.expense

import ExpenseViewModel
import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.databinding.ModalFilterExpensesBinding
import com.example.trabajointegrador_modulonativo.viewmodel.ExpenseViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.text.format
import java.util.Locale

class FilterExpenseModalFragment : BottomSheetDialogFragment() {

    private var _binding : ModalFilterExpensesBinding? = null
    private val binding get() = _binding!!

    private var selectedCarId: String? = null
    private var selectedExpenseTypeId: String? = null

    private var selectedStartDate: Date? = null
    private var selectedEndDate: Date? = null

    private val viewModel : ExpenseViewModel by activityViewModels {
        ExpenseViewModelFactory(ExpenseRepository(), CarRepository(), SessionProvider())
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ModalFilterExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupDatePickers()
        setupActionButtons()


    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.appliedFilters.firstOrNull()?.let { lastFilters ->

                        selectedCarId = lastFilters.carId
                        selectedExpenseTypeId = lastFilters.expenseTypeId
                        val car = viewModel.cars.value.find {it.id == lastFilters.carId}
                        val type = viewModel.expenseTypes.value.find { it.id == lastFilters.expenseTypeId}

                        if(car != null) {
                            binding.filterCarAutoComplete.setText(car.toString(), false)

                        }

                        if(type != null) {
                            binding.filterExpenseTypeAutoComplete.setText(type.name, false)

                        }

                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        binding.filterStartDateEditText.setText(lastFilters.startDate?.let { sdf.format(it) }?:"")
                        binding.filterEndDateEditText.setText(lastFilters.endDate?.let { sdf.format(it)}?: "")

                        selectedStartDate = lastFilters.startDate
                        selectedEndDate = lastFilters.endDate
                    }
                }


                launch{
                    viewModel.cars.collect { cars ->
                        val carFilterList = cars.mapNotNull { car ->
                            car.id?.let { id -> Pair(id, "${car.brand} ${car.model}") }
                        }
                        val carNames = carFilterList.map { it.second }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, carNames)
                        binding.filterCarAutoComplete.setAdapter(adapter)

                        binding.filterCarAutoComplete.setOnItemClickListener { _, _, position, _ ->
                            selectedCarId = carFilterList[position].first
                        }
                    }
                }

                launch {
                    viewModel.expenseTypes.collect { types ->
                        val typeFilterList = types.map { type ->
                            Pair(type.id, type.name)
                        }
                        val typeNames = typeFilterList.map { it.second }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, typeNames)
                        binding.filterExpenseTypeAutoComplete.setAdapter(adapter)

                        binding.filterExpenseTypeAutoComplete.setOnItemClickListener { _, _, position, _ ->
                            selectedExpenseTypeId = typeFilterList[position].first
                        }
                    }
                }
            }

        }
    }


    private fun setupDatePickers() {
        binding.filterStartDateEditText.setOnClickListener {
            showDatePickerDialog(binding.filterStartDateEditText) { date ->
                selectedStartDate = date
            }
        }

        binding.filterEndDateEditText.setOnClickListener {
            showDatePickerDialog(binding.filterEndDateEditText) { date ->
                selectedEndDate = date
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

    private fun setupActionButtons() {
        binding.btnApplyFiltersButton.setOnClickListener {

            viewModel.applyFilters(
                carId = selectedCarId,
                expenseTypeId = selectedExpenseTypeId,
                startDate = selectedStartDate,
                endDate = selectedEndDate,
            )
            dismiss()
        }

        binding.btnClearFiltersButton.setOnClickListener {
            selectedCarId = null
            selectedExpenseTypeId = null
            selectedStartDate = null
            selectedEndDate = null

            binding.filterCarAutoComplete.setText("", false)
            binding.filterExpenseTypeAutoComplete.setText("", false)
            binding.filterStartDateEditText.setText("")
            binding.filterEndDateEditText.setText("")

            viewModel.applyFilters(
                carId = null,
                expenseTypeId = null,
                startDate = null,
                endDate = null
            )
            dismiss()
        }


    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}









