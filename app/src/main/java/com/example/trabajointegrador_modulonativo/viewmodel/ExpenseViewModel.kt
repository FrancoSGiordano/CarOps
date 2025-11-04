
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.model.Expense
import com.example.trabajointegrador_modulonativo.model.ExpenseType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

data class FilterState(
    val carId: String? = null,
    val expenseTypeId: Int? = null,
    val startDate: Date? = null,
    val endDate: Date? = null,
    val id: String = UUID.randomUUID().toString()
)

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


    val expenseTypes: StateFlow<List<ExpenseType>> = flow {
        emit(expenseRepository.getExpenseTypes())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val cars: StateFlow<List<Car>> = carRepository.getCarsStream(userId ?: "")
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _filterEvents = MutableSharedFlow<FilterState>(replay = 1)

    val appliedFilters: Flow<FilterState> = _filterEvents.asSharedFlow()


    @OptIn(ExperimentalCoroutinesApi::class)
    private val expensesStream: Flow<List<Expense>> = _filterEvents
        .onStart { emit(FilterState()) }
        .flatMapLatest { filterState ->
            _isLoading.value = true
            if (userId == null) {
                flowOf(emptyList())
            } else {
                expenseRepository.getExpensesForUserStream(
                    userId,
                    filterState.carId,
                    filterState.expenseTypeId,
                    filterState.startDate,
                    filterState.endDate
                )
            }
        }.catch {
                e -> _error.value = "Error al cargar gastos: ${e.message}"
                _isLoading.value = false
                emit(emptyList())

        }


    val expenses: StateFlow<List<Expense>> = combine(cars, expensesStream) { carList, expenseList ->

        if (carList.isNotEmpty()) {
            val carNameMap = carList
                .filter { !it.id.isNullOrBlank() }
                .associateBy({ it.id!! }, { "${it.brand} ${it.model}" })

            expenseList.forEach { expense ->
                expense.carName = carNameMap[expense.carId] ?: "Vehículo no encontrado"
            }
        }
        _isLoading.value = false
        expenseList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )



    fun applyFilters(carId: String?, expenseTypeId: Int?, startDate: Date?, endDate: Date?) {
        _filterEvents.tryEmit(
            FilterState(
                carId = carId,
                expenseTypeId = expenseTypeId,
                startDate = startDate,
                endDate = endDate,
                id = UUID.randomUUID().toString()
            )
        )


    }


    fun createExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.createExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.updateExpense(expense)
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expenseId)

        }
    }

}


