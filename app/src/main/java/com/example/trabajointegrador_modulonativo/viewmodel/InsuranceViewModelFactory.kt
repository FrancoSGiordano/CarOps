package com.example.trabajointegrador_modulonativo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.InsuranceRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider

class InsuranceViewModelFactory(
    private val sessionProvider: SessionProvider,
    private val insuranceRepository: InsuranceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(InsuranceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InsuranceViewModel(insuranceRepository, sessionProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}