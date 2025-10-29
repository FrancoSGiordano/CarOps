package com.example.trabajointegrador_modulonativo.ui.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.trabajointegrador_modulonativo.data.ReminderRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.trabajointegrador_modulonativo.databinding.ModalCreateReminderBinding
import com.example.trabajointegrador_modulonativo.model.Reminder
import com.example.trabajointegrador_modulonativo.model.ReminderState
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.Date
import androidx.lifecycle.lifecycleScope
import com.example.trabajointegrador_modulonativo.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderFormModal:  BottomSheetDialogFragment() {

    private var _binding : ModalCreateReminderBinding? = null
    private val binding get() = _binding!!
    private val selectedCal: Calendar = Calendar.getInstance()
    var carId: String? = null

    private val repo = ReminderRepository()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                scheduleReminder()
            } else {
                Toast.makeText(requireContext(), "No se pueden mostrar notificaciones sin permiso", Toast.LENGTH_SHORT).show()
            }
        }

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

        binding.filterStartDateEditText.setOnClickListener { openMaterialDatePicker() }
        binding.filterStartTimeEditText.setOnClickListener { openMaterialTimePicker() }

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
                selectedCal.set(Calendar.YEAR, selYear)
                selectedCal.set(Calendar.MONTH, selMonth)
                selectedCal.set(Calendar.DAY_OF_MONTH, selDay)

                val formattedDateForDisplay =
                    String.format("%02d/%02d/%d", selDay, selMonth + 1, selYear)
                binding.filterStartDateEditText.setText(formattedDateForDisplay)

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
        if (binding.reminderTitleEditText.text?.toString()?.trim().isNullOrEmpty()) {
            binding.reminderTitle.error = "Ingrese un título"
            return
        }
        if (selectedCal.timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(requireContext(), "Seleccioná una fecha/hora futura", Toast.LENGTH_SHORT).show()
            return
        }

        checkPermissionsAndSchedule()
    }

    private fun checkPermissionsAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also { intent ->
                    Toast.makeText(requireContext(), "Se necesita permiso para programar recordatorios precisos.", Toast.LENGTH_LONG).show()
                    startActivity(intent)
                }
                dismiss()
                return
            }
        }
        askForNotificationPermission()
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                scheduleReminder()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleReminder()
        }
    }

    private fun scheduleReminder() {
        val title = binding.reminderTitleEditText.text?.toString()?.trim().orEmpty()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val ts = Timestamp(Date(selectedCal.timeInMillis))

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
                    repo.addReminder(newReminder)
                }

                newReminder.id = newId

                withContext(Dispatchers.Main) {
                    ReminderScheduler.schedule(requireContext(), newReminder)
                }

                Toast.makeText(requireContext(), "Recordatorio creado", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (e: Exception) {
                Log.e("ReminderFormModal", "Error al schedul<caret>ear reminder", e)

                Toast.makeText(requireContext(), "Error al crear: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
