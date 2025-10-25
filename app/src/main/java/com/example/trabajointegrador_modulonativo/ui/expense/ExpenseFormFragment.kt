package com.example.trabajointegrador_modulonativo.ui.expense

import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.app.DatePickerDialog
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.trabajointegrador_modulonativo.R
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.databinding.FragmentCreateExpenseBinding
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.model.Expense
import com.example.trabajointegrador_modulonativo.model.ExpenseType
import com.example.trabajointegrador_modulonativo.util.CurrencyTextWatcher
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModel
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModelFactory
import com.example.trabajointegrador_modulonativo.viewmodel.ExpenseViewModel
import com.example.trabajointegrador_modulonativo.viewmodel.ExpenseViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch


class ExpenseFormFragment : Fragment() {
    private var _binding: FragmentCreateExpenseBinding? = null
    private val binding get() = _binding!!
    private var selectedExpenseType: ExpenseType? = null

    private val args: ExpenseFormFragmentArgs by navArgs()



    private val expenseViewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(ExpenseRepository(), CarRepository(), SessionProvider())
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?) : View {
        _binding = FragmentCreateExpenseBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentCar = args.selectedCar

        if(currentCar.id == null){
            findNavController().popBackStack()
            return
        }


        binding.expenseAmountEditText.addTextChangedListener(CurrencyTextWatcher(binding.expenseAmountEditText))


        setupClickListeners()
        setup()
        observeExpenseTypes()
        expenseViewModel.loadExpenseTypes()
    }
    private fun observeExpenseTypes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                expenseViewModel.expenseTypes.collect { expenseTypes ->

                }
            }
        }
    }


    private fun setupClickListeners() {

        addTextWatcherToLayout(binding.expenseDescriptionLayout)
        addTextWatcherToLayout(binding.expenseAmountLayout)

        binding.expenseTypeEditText.setOnClickListener {

            binding.expenseTypeLayout.error = null

            val currentExpenseTypes = expenseViewModel.expenseTypes.value
            if (currentExpenseTypes.isNotEmpty()) {
                showExpenseTypeSelectorDialog(currentExpenseTypes)
            } else {
                Toast.makeText(requireContext(), "Cargando tipos de gasto...", Toast.LENGTH_SHORT).show()

            }
        }

        binding.expenseDateEditText.setOnClickListener {
            binding.expenseDateLayout.error = null
            showDatePickerDialog()
        }

    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
                binding.expenseDateEditText.setText(formattedDate)
            },
            year,
            month,
            day
        )

        datePickerDialog.show()
    }

    private fun setup(){
        binding.createExpenseButton.setOnClickListener {
            if (validateForm()) {
                saveExpense()
            }
        }
    }



    private fun showExpenseTypeSelectorDialog(expenseTypes: List<ExpenseType>) {
        val typeNames = expenseTypes.map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogTheme)
            .setTitle("Selecciona un tipo de gasto")
            .setItems(typeNames) { dialog, which ->

                val selectedType = expenseTypes[which]
                this.selectedExpenseType = selectedType

                binding.expenseTypeEditText.setText(selectedType.name)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }



    // En ExpenseFormFragment.kt

    private fun validateForm(): Boolean {
        var isValid = true

        binding.expenseTypeLayout.error = null
        binding.expenseDescriptionLayout.error = null
        binding.expenseAmountLayout.error = null
        binding.expenseDateLayout.error = null

        if (selectedExpenseType == null) {
            binding.expenseTypeLayout.error = "Debes seleccionar un tipo"
            isValid = false
        }

        if (binding.expenseDescriptionEditText.text.toString().isBlank()) {
            binding.expenseDescriptionLayout.error = "La descripción es obligatoria"
            isValid = false
        }

        val amountString = binding.expenseAmountEditText.text.toString()
        if (amountString.isBlank()) {
            binding.expenseAmountLayout.error = "El monto es obligatorio"
            isValid = false
        } else {
            val cleanString = amountString.replace("[^\\d]".toRegex(), "")
            val amount = if (cleanString.isNotEmpty()) cleanString.toDoubleOrNull()?.div(100.0) else null
            if (amount == null || amount <= 0.0) {
                binding.expenseAmountLayout.error = "El monto debe ser mayor que cero"
                isValid = false
            }
        }

        if (binding.expenseDateEditText.text.toString().isBlank()) {
            binding.expenseDateLayout.error = "Debes seleccionar una fecha"
            isValid = false
        }

        return isValid
    }


    private fun saveExpense() {
        val userId = Firebase.auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Error: Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentCar = args.selectedCar

        val amountString = binding.expenseAmountEditText.text.toString()

        val cleanString = amountString.replace("[^\\d]".toRegex(), "")

        val amount = cleanString.toDoubleOrNull()!!.div(100.0)

        val description = binding.expenseDescriptionEditText.text.toString()
        val date = binding.expenseDateEditText.text.toString()

        val newExpense = Expense(
            amount = amount,
            description = description.uppercase(),
            date = date,
            expenseTypeId = selectedExpenseType!!.id,
            userId = userId,
            carId = currentCar.id,
            carName = "${currentCar.brand} ${currentCar.model}"
        )

        expenseViewModel.createExpense(newExpense)

        Toast.makeText(requireContext(), "Gasto guardado con éxito", Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    private fun addTextWatcherToLayout(textInputLayout: TextInputLayout) {
        val editText = textInputLayout.editText

        editText?.addTextChangedListener(object: TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if(s?.isNotEmpty() == true) {
                    textInputLayout.error = null
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}


