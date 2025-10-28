package com.example.trabajointegrador_modulonativo.ui.reminder

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trabajointegrador_modulonativo.adapter.ReminderAdapter
import com.example.trabajointegrador_modulonativo.data.ReminderRepository
import com.example.trabajointegrador_modulonativo.databinding.FragmentCarReminderListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest

class ReminderListFragment : Fragment() {

    private var _binding: FragmentCarReminderListBinding? = null
    private val binding get() = _binding!!

    private val repo = ReminderRepository()
    private var carIdFromArgs: String? = null
    private lateinit var adapter: ReminderAdapter

    companion object {
        private const val TAG = "ReminderListFragment"
        const val ARG_CAR_ID = "car_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        carIdFromArgs = arguments?.getString(ARG_CAR_ID)
        Log.d(TAG, "onCreate: carIdFromArgs=$carIdFromArgs")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCarReminderListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // proteger por si el layout cambió y binding no tiene esas vistas
        try {
            adapter = ReminderAdapter(
                onMarkDone = { reminder -> markAsRealizado(reminder) },
                onDelete = { reminder -> deleteReminder(reminder) },
                onEdit = { /* editar futuro */ }
            )

            // RecyclerView
            binding.userExpensesRecyclerView.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = this@ReminderListFragment.adapter
            }

            // FAB abrir modal
            binding.addReminderButton.setOnClickListener {
                val modal = ReminderFormModal()
                carIdFromArgs?.let { modal.arguments = Bundle().apply { putString("carId", it) } }
                modal.show(parentFragmentManager, "CreateReminderModalTag")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            Toast.makeText(requireContext(), "Error inicializando la vista: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        // collect safely
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    if (carIdFromArgs != null) {
                        repo.getRemindersStreamForCar(carIdFromArgs!!).collectLatest { list ->
                            adapter.submitList(list)

                        }
                    } else {
                        repo.getPendingRemindersStreamForUser().collectLatest { list ->
                            adapter.submitList(list)

                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting reminders: ${e.message}", e)
                    // mostrar mensaje al usuario (no crashear)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error cargando recordatorios: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun markAsRealizado(reminder: com.example.trabajointegrador_modulonativo.model.Reminder) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repo.markAsRealizado(reminder.id ?: return@withContext)
                }
                Toast.makeText(requireContext(), "Marcado como realizado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "markAsRealizado error: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al marcar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteReminder(reminder: com.example.trabajointegrador_modulonativo.model.Reminder) {
        val id = reminder.id ?: return
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { repo.deleteReminder(id) }
                Toast.makeText(requireContext(), "Eliminado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "deleteReminder error: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
