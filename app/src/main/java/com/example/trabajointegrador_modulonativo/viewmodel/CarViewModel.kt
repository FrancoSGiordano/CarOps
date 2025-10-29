package com.example.trabajointegrador_modulonativo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.InsuranceRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.model.Expense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CarViewModel (
    private val carRepository: CarRepository,
    private val expenseRepository: ExpenseRepository,
    private val sessionProvider: SessionProvider,
    private val insuranceRepository: InsuranceRepository?
) : ViewModel() {

    private val userId: String? = sessionProvider.getUserId()

    private val _carsState = MutableStateFlow<List<Car>>(emptyList())
    val carsState : StateFlow<List<Car>> = _carsState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedCar = MutableStateFlow<Car?>(null)
    val selectedCar: StateFlow<Car?> = _selectedCar.asStateFlow()

    private val _carExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val carExpenses: StateFlow<List<Expense>> = _carExpenses.asStateFlow()



    fun getCarExpenses(carId: String) {

        if(userId == null){
            _error.value = "Error: Usuario no autenticado"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            expenseRepository.getExpensesForUserStream(userId, carId, null, null, null)
                .catch { e ->
                    _error.value = "Error al cargar los gastos: ${e.message}"
                    _isLoading.value = false
                }
                .collect { expenses ->
                    _carExpenses.value = expenses
                    _isLoading.value = false
                    _error.value = null
                }
        }
    }

    fun loadCarDetails(carId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            carRepository.getCarById(carId)
                .catch { e ->
                    _error.value = "Error al cargar los detalles: ${e.message}"
                    _isLoading.value = false
                }
                .collect { car ->

                    _isLoading.value = false
                    if (car != null) {

                        if (car.insuranceId != null) {
                            val insurance = insuranceRepository?.getInsuranceById(car.insuranceId)
                            car.insurance = insurance
                        }

                        _error.value = null
                        _selectedCar.value = car
                        return@collect
                    } else {
                        _error.value = "Vehículo no encontrado"
                    }

                }

        }
    }


    fun updateCar(car: Car) {
        if(userId == null) {
            _error.value = "Error: Usuario no autenticado"
            return
        }
        car.userId = this.userId
        viewModelScope.launch {
            carRepository.updateCar(car)
        }
    }

    fun clearSelectedCar() {
        _selectedCar.value = null
    }


    fun getCars() {

        if(userId == null) {
            _error.value = "Error: Usuario no autenticado"
            return
        }

       viewModelScope.launch {
           _isLoading.value = true
           carRepository.getCarsStream(userId)
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
        if(userId == null) {
            _error.value = "Error: Usuario no autenticado"
            return
        }
        car.userId = this.userId
        viewModelScope.launch {
            carRepository.addCar(car)
        }
    }

    fun saveParking(carId: String, lat: Double, lng: Double) {
        if (carId.isBlank()) {
            _error.value = "Error: id de vehículo inválido"
            return
        }
        viewModelScope.launch {
            try {
                carRepository.saveParking(carId, lat, lng)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No se pudo guardar la ubicación: ${e.message}"
            }
        }
    }

    fun clearParking(carId: String) {
        if (carId.isBlank()) {
            _error.value = "Error: id de vehículo inválido"
            return
        }
        viewModelScope.launch {
            try {
                carRepository.clearParking(carId)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "No se pudo borrar la ubicación: ${e.message}"
            }
        }
    }
}