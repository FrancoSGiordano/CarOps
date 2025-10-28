package com.example.trabajointegrador_modulonativo.ui.reminder

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.trabajointegrador_modulonativo.data.ReminderRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.trabajointegrador_modulonativo.databinding.ModalCreateReminderBinding
import com.example.trabajointegrador_modulonativo.databinding.ModalFilterExpensesBinding
import com.example.trabajointegrador_modulonativo.model.Reminder
import com.example.trabajointegrador_modulonativo.model.ReminderState
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.Date
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderFormModal:  BottomSheetDialogFragment() {

    private var _binding : ModalCreateReminderBinding? = null
    private val binding get() = _binding!!
    private val selectedCal: Calendar = Calendar.getInstance()
    var carId: String? = null

    private val repo = ReminderRepository()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            carId = arguments?.getString("carId")
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ModalCreateReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Si venimos en modo edición, prefill

        // listeners para abrir pickers
        binding.filterStartDateEditText.setOnClickListener { openMaterialDatePicker() }
        binding.filterStartTimeEditText.setOnClickListener { openMaterialTimePicker() }

        // botón agregar / guardar
        binding.btnApplyFiltersButton.setOnClickListener {
            onCreateClicked()

        }


    }



    private fun openMaterialDatePicker() {
        val year = selectedCal.get(Calendar.YEAR)
        val month = selectedCal.get(Calendar.MONTH)
        val day = selectedCal.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selYear, selMonth, selDay ->
                // actualizamos solo Y/M/D, dejando hora/minutos como estaban
                selectedCal.set(Calendar.YEAR, selYear)
                selectedCal.set(Calendar.MONTH, selMonth)
                selectedCal.set(Calendar.DAY_OF_MONTH, selDay)

                // Formato igual que tu función: dd/MM/yyyy
                val formattedDateForDisplay =
                    String.format("%02d/%02d/%d", selDay, selMonth + 1, selYear)
                binding.filterStartDateEditText.setText(formattedDateForDisplay)

                // Actualizamos también la vista de hora por si hace falta
                updateDateField()
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }


    private fun openMaterialTimePicker() {
        val is24Hour = android.text.format.DateFormat.is24HourFormat(requireContext())
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(if (is24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(selectedCal.get(Calendar.HOUR_OF_DAY))
            .setMinute(selectedCal.get(Calendar.MINUTE))
            .setTitleText("Seleccioná la hora")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            selectedCal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            selectedCal.set(Calendar.MINUTE, timePicker.minute)
            selectedCal.set(Calendar.SECOND, 0)
            updateTimeField()

        }
        timePicker.show(parentFragmentManager, "MATERIAL_TIME_PICKER")
    }

    private fun updateDateField() {
        // mostramos dd/MM/yyyy exactamente como pediste
        val d = selectedCal.get(Calendar.DAY_OF_MONTH)
        val m = selectedCal.get(Calendar.MONTH) + 1
        val y = selectedCal.get(Calendar.YEAR)
        val formatted = String.format("%02d/%02d/%d", d, m, y)
        binding.filterStartDateEditText.setText(formatted)
    }

    private fun updateTimeField() {
        val timeStr = android.text.format.DateFormat.getTimeFormat(requireContext())
            .format(Date(selectedCal.timeInMillis))
        binding.filterStartTimeEditText.setText(timeStr)
    }



    private fun onCreateClicked() {
        val title = binding.reminderTitleEditText.text?.toString()?.trim().orEmpty()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }
        if (title.isEmpty()) {
            binding.reminderTitle.error = "Ingrese un título"
            return
        }

        val notifyMillis = selectedCal.timeInMillis
        if (notifyMillis <= System.currentTimeMillis()) {
            Toast.makeText(requireContext(), "Seleccioná una fecha/hora futura", Toast.LENGTH_SHORT).show()
            return
        }

        val ts = Timestamp(Date(notifyMillis))

        val newReminder = Reminder(
            id = null,
            userId = uid,
            carId = carId,
            title = title,
            notifyAt = ts,
            state = if (ts.toDate().time > System.currentTimeMillis()) ReminderState.EN_ESPERA.name else ReminderState.PENDIENTE.name,
            notificationSent = false,
            createdAt = null,
            done = false
        )

        lifecycleScope.launch {
            try {
                val newId = withContext(Dispatchers.IO) {
                    repo.addReminder(newReminder) // suspend
                }

                // setear el id en el objeto (útil para programar alarmas con id estable)
                newReminder.id = newId



                Toast.makeText(requireContext(), "Recordatorio creado", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al crear: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}