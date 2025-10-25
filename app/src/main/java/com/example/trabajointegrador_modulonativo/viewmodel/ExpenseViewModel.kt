package com.example.trabajointegrador_modulonativo.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.model.Expense
import com.example.trabajointegrador_modulonativo.model.ExpenseType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ExpenseViewModel (
    private val expenseRepository: ExpenseRepository,
    private val carRepository: CarRepository,
    private val sessionProvider: SessionProvider
): ViewModel() {

    private val userId: String? = sessionProvider.getUserId()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _expenseTypes = MutableStateFlow<List<ExpenseType>>(emptyList())
    val expenseTypes: StateFlow<List<ExpenseType>> = _expenseTypes.asStateFlow()


    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    fun loadExpenseTypes() {
        viewModelScope.launch {
            try {
                _expenseTypes.value = expenseRepository.getExpenseTypes()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun getUserExpenses() {
        if(userId == null) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            expenseRepository.getExpensesForUserStream(userId, null)
                .catch { e ->
                    _error.value = "Error al cargar los gastos: ${e.message}"
                    _isLoading.value = false
                }
                .collect { expenses ->
                    _expenses.value = expenses
                    _isLoading.value = false
                }
        }

    }


    fun createExpense(expense: Expense){
        viewModelScope.launch {
            expenseRepository.createExpense(expense)
        }
    }
}