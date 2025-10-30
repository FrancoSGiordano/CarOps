package com.example.trabajointegrador_modulonativo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trabajointegrador_modulonativo.data.InsuranceRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.model.Insurance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch



class InsuranceViewModel (
    private val insuranceRepository: InsuranceRepository,
    val sessionProvider: SessionProvider
) : ViewModel() {

    private val userId: String? = sessionProvider.getUserId()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()



    suspend fun addInsurance(insurance: Insurance, carId: String) {
        if(userId == null) {
            _error.value = "Error: Usuario no autenticado"
            return
        }
        insurance.userId = userId
        viewModelScope.launch {
            insuranceRepository.addInsurance(insurance, carId)
        }

    }

    fun updateInsurance(insurance: Insurance) {
        if(userId == null) {
            _error.value = "Error: Usuario no autenticado"
            return
        }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                insuranceRepository.updateInsurance(insurance)
            } catch (e: Exception) {
                _error.value = "Error al actualizar seguro: ${e.message}"
            } finally {
                _isLoading.value = false
            }

        }
    }

    fun deleteInsurance(insuranceId: String, carId: String) {
        if(userId == null) {
            _error.value = "Error: Usuario no autenticado"
            return
        }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                insuranceRepository.deleteInsurance(insuranceId, carId)
            } catch (e: Exception) {
                _error.value = "Error al eliminar seguro: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }

    }




}