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
import androidx.lifecycle.lifecycleScope
import com.example.trabajointegrador_modulonativo.data.ReminderRepository
import com.example.trabajointegrador_modulonativo.databinding.ModalCreateReminderBinding
import com.example.trabajointegrador_modulonativo.model.Reminder
import com.example.trabajointegrador_modulonativo.notifications.ReminderScheduler
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class ReminderFormModal : BottomSheetDialogFragment() {

    private var _binding: ModalCreateReminderBinding? = null
    private val binding get() = _binding!!
    private val selectedCal: Calendar = Calendar.getInstance()
    private var carId: String? = null
    private var reminderId: String? = null
    private var isEditMode = false
    private var currentReminder: Reminder? = null

    private val repo = ReminderRepository()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            saveReminder()
        } else {
            Toast.makeText(requireContext(), "No se pueden mostrar notificaciones sin permiso", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            carId = it.getString("carId")
            reminderId = it.getString("reminderId")
            isEditMode = reminderId != null
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

        if (isEditMode) {
            loadReminderData()
            binding.btnApplyFiltersButton.text = "Actualizar"
        } else {
            binding.btnApplyFiltersButton.text = "Crear"
        }

        binding.filterStartDateEditText.setOnClickListener { openMaterialDatePicker() }
        binding.filterStartTimeEditText.setOnClickListener { openMaterialTimePicker() }
        binding.btnApplyFiltersButton.setOnClickListener { onSaveClicked() }
        binding.btnDone.setOnClickListener { onDoneClicked() }
        binding.btnDelete.setOnClickListener { onDeleteClicked() }
    }

    private fun loadReminderData() {
        reminderId?.let {
            lifecycleScope.launch {
                currentReminder = withContext(Dispatchers.IO) { repo.getReminderById(it) }
                if (currentReminder != null) {
                    binding.reminderTitleEditText.setText(currentReminder!!.title)
                    currentReminder!!.notifyAt?.toDate()?.let {
                        date -> selectedCal.time = date
                        updateDateField()
                        updateTimeField()
                    }
                    if (currentReminder!!.pending) {
                        binding.btnDone.visibility = View.VISIBLE
                    } else if (isEditMode) {
                        binding.btnDelete.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(context, "Error al cargar el recordatorio", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }

    private fun openMaterialDatePicker() {
        val year = selectedCal.get(Calendar.YEAR)
        val month = selectedCal.get(Calendar.MONTH)
        val day = selectedCal.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selYear, selMonth, selDay ->
                selectedCal.set(selYear, selMonth, selDay)
                updateDateField()
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun openMaterialTimePicker() {
        val is24Hour = android.text.format.DateFormat.is24HourFormat(requireContext())
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(if (is24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(selectedCal.get(Calendar.HOUR_OF_DAY))
            .setMinute(selectedCal.get(Calendar.MINUTE))
            .build()

        timePicker.addOnPositiveButtonClickListener {
            selectedCal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            selectedCal.set(Calendar.MINUTE, timePicker.minute)
            updateTimeField()
        }
        timePicker.show(parentFragmentManager, "timePicker")
    }

    private fun updateDateField() {
        val formattedDate = String.format("%02d/%02d/%d", selectedCal.get(Calendar.DAY_OF_MONTH), selectedCal.get(Calendar.MONTH) + 1, selectedCal.get(Calendar.YEAR))
        binding.filterStartDateEditText.setText(formattedDate)
    }

    private fun updateTimeField() {
        val formattedTime = android.text.format.DateFormat.getTimeFormat(requireContext()).format(selectedCal.time)
        binding.filterStartTimeEditText.setText(formattedTime)
    }

    private fun onSaveClicked() {
        if (binding.reminderTitleEditText.text.toString().trim().isEmpty()) {
            binding.reminderTitle.error = "Ingrese un título"
            return
        }
        if (selectedCal.timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(requireContext(), "Seleccioná una fecha/hora futura", Toast.LENGTH_SHORT).show()
            return
        }
        checkPermissionsAndSchedule()
    }

    private fun onDoneClicked(){
        currentReminder?.id?.let {
            lifecycleScope.launch{
                withContext(Dispatchers.IO) { repo.deleteReminder(it) }
                ReminderScheduler.cancel(requireContext(), it)
                Toast.makeText(context, "Recordatorio eliminado", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

    private fun onDeleteClicked(){
        currentReminder?.id?.let {
            lifecycleScope.launch{
                withContext(Dispatchers.IO) { repo.deleteReminder(it) }
                ReminderScheduler.cancel(requireContext(), it)
                Toast.makeText(context, "Recordatorio eliminado", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
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
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                saveReminder()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            saveReminder()
        }
    }

    private fun saveReminder() {
        val title = binding.reminderTitleEditText.text.toString().trim()
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }

        val ts = Timestamp(Date(selectedCal.timeInMillis))

        val reminder = Reminder(
            id = reminderId,
            userId = uid,
            carId = carId,
            title = title,
            notifyAt = ts,
            pending = false,
            notificationSent = false,
            createdAt = if (isEditMode) currentReminder?.createdAt else Timestamp.now() // Keep original creation date on edit
        )

        lifecycleScope.launch {
            try {
                if (isEditMode) {
                    reminder.id?.let { ReminderScheduler.cancel(requireContext(), it) }
                    withContext(Dispatchers.IO) { repo.updateReminder(reminder) }
                } else {
                    val newId = withContext(Dispatchers.IO) { repo.addReminder(reminder) }
                    reminder.id = newId
                }

                withContext(Dispatchers.Main) {
                    ReminderScheduler.schedule(requireContext(), reminder)
                }

                Toast.makeText(requireContext(), if (isEditMode) "Recordatorio actualizado" else "Recordatorio creado", Toast.LENGTH_SHORT).show()
                dismiss()
            } catch (e: Exception) {
                Log.e("ReminderFormModal", "Error al guardar reminder", e)
                Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
