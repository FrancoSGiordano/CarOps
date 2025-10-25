package com.example.trabajointegrador_modulonativo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider

class ExpenseViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val carRepository: CarRepository,
    private val sessionProvider: SessionProvider
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(expenseRepository, carRepository, sessionProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}