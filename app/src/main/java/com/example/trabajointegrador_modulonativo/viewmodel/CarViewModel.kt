package com.example.trabajointegrador_modulonativo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.model.Car
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CarViewModel (
    private val repository: CarRepository
) : ViewModel() {

    private val _carsState = MutableStateFlow<List<Car>>(emptyList())
    val carsState : StateFlow<List<Car>> = _carsState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedCar = MutableStateFlow<Car?>(null)
    val selectedCar: StateFlow<Car?> = _selectedCar.asStateFlow()

    fun loadCarDetails(carId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getCarById(carId)
                .catch { e ->
                    _error.value = "Error al cargar los detalles: ${e.message}"
                    _isLoading.value = false
                }
                .collect { car ->
                    _selectedCar.value = car
                    _isLoading.value = false
                    if (car != null) {
                        _error.value = null
                        return@collect
                    }
                    _error.value = "Vehículo no encontrado"
                }

        }
    }

    fun updateCar(car: Car) {
        viewModelScope.launch {
            repository.updateCar(car)
        }
    }

    fun clearSelectedCar() {
        _selectedCar.value = null
    }

    init {
        fetchCars()
    }

    private fun fetchCars() {
       viewModelScope.launch {
           _isLoading.value = true
           repository.getCarsStream()
               .catch { e ->
                   _error.value = e.message
                   _isLoading.value = false

               }
               .collect { carList ->
                   _carsState.value = carList
                   _isLoading.value = false
                   _error.value = null
               }
       }
    }

    fun addCar(car : Car) {
        viewModelScope.launch {
            repository.addCar(car)
        }
    }




}