package com.example.trabajointegrador_modulonativo.ui.expense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trabajointegrador_modulonativo.adapter.AdapterMode
import com.example.trabajointegrador_modulonativo.adapter.ExpenseAdapter
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.databinding.FragmentUserExpenseListBinding
import com.example.trabajointegrador_modulonativo.viewmodel.ExpenseViewModel
import com.example.trabajointegrador_modulonativo.viewmodel.ExpenseViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch


class ExpenseListFragment : Fragment() {

    private var _binding: FragmentUserExpenseListBinding? = null

    private val binding get() = _binding!!

    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory(ExpenseRepository(), CarRepository(), SessionProvider())
    }

    private lateinit var expenseAdapter: ExpenseAdapter


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserExpenseListBinding.inflate(inflater, container, false)
        val rootView = binding.root
        return rootView

    }

    override fun onViewCreated(view: View, saveInstanceState: Bundle?) {
        super.onViewCreated(view, saveInstanceState)

        setupRecyclerView()

        viewModel.getUserExpenses()

        observeViewModel()
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(AdapterMode.USER_EXPENSE)
        binding.userExpensesRecyclerView.apply {
            this.layoutManager = LinearLayoutManager(requireContext())
            this.adapter = expenseAdapter
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
                    viewModel.expenses.collect { expenses ->
                        expenseAdapter.updateExpenses(expenses)
                    }
                }
            }
        }

    }

}