package com.example.trabajointegrador_modulonativo.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.trabajointegrador_modulonativo.R
import com.example.trabajointegrador_modulonativo.databinding.CarExpenseItemBinding
import com.example.trabajointegrador_modulonativo.databinding.UserExpenseItemBinding
import com.example.trabajointegrador_modulonativo.model.Expense
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.math.exp

enum class AdapterMode {
    SIMPLE_EXPENSE,
    USER_EXPENSE
}
class ExpenseAdapter(private val mode: AdapterMode) : RecyclerView.Adapter<RecyclerView.ViewHolder> (){


    private var expenses: List<Expense> = emptyList()

    fun setImage(expense: Expense): Int {
        val imageResource = when (expense.expenseTypeId) {
            0L -> R.drawable.ic_fuel
            1L -> R.drawable.ic_insurance
            2L -> R.drawable.ic_maintenance
            3L -> R.drawable.ic_parts
            4L -> R.drawable.ic_cleaning
            else -> R.drawable.ic_car
        }
        return imageResource
    }

    inner class ExpenseViewHolder(private val binding: CarExpenseItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: Expense) {

            binding.expenseImageView.setImageResource(setImage(expense))

            binding.expenseNameTextView.text = expense.description
            binding.expenseDateTextView.text = formatDateForDisplay(expense.date!!)

            val amountToFormat = expense.amount ?: 0.0

            val format = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
            binding.expenseAmountTextView.text = format.format(amountToFormat)

        }
    }

    inner class UserExpenseViewHolder(private val binding: UserExpenseItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: Expense) {
            binding.userExpenseImageView.setImageResource(setImage(expense))

            binding.userExpenseNameTextView.text = expense.description
            binding.userExpenseDateTextView.text = formatDateForDisplay(expense.date!!)
            binding.userCarNameTextView.text = expense.carName

            val amountToFormat = expense.amount ?: 0.0

            val format = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
            binding.userExpenseAmountTextView.text = format.format(amountToFormat)

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType){
            VIEW_TYPE_SIMPLE_EXPENSE -> {
                val binding = CarExpenseItemBinding.inflate(inflater, parent, false)
                ExpenseViewHolder(binding)
            }

            VIEW_TYPE_USER_EXPENSE -> {
                val binding = UserExpenseItemBinding.inflate(inflater, parent, false)
                UserExpenseViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val expense = expenses[position]
        when (holder) {
            is ExpenseViewHolder -> holder.bind(expense)
            is UserExpenseViewHolder -> holder.bind(expense)
        }
    }

    override fun getItemCount(): Int {
        return expenses.size
    }

    fun updateExpenses(newExpenses: List<Expense>) {
        this.expenses = newExpenses
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (mode) {
            AdapterMode.USER_EXPENSE -> VIEW_TYPE_USER_EXPENSE
            AdapterMode.SIMPLE_EXPENSE -> VIEW_TYPE_SIMPLE_EXPENSE
        }
    }

    private fun formatDateForDisplay(date: Date): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(date)
    }

    companion object {
        private const val VIEW_TYPE_SIMPLE_EXPENSE = 1
        private const val VIEW_TYPE_USER_EXPENSE = 2
    }


}