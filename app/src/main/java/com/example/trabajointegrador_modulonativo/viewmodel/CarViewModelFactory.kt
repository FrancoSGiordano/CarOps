package com.example.trabajointegrador_modulonativo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.InsuranceRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider

class CarViewModelFactory(
    private val carRepository: CarRepository,
    private val expenseRepository: ExpenseRepository,
    private val sessionProvider: SessionProvider,
    private val insuranceRepository: InsuranceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(CarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CarViewModel(carRepository, expenseRepository, sessionProvider, insuranceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
