package com.example.trabajointegrador_modulonativo.ui.expense

import ExpenseViewModel
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.trabajointegrador_modulonativo.R
import com.example.trabajointegrador_modulonativo.adapter.AdapterMode
import com.example.trabajointegrador_modulonativo.adapter.ExpenseAdapter
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.databinding.FragmentUserExpenseListBinding
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.model.Expense
import com.example.trabajointegrador_modulonativo.viewmodel.ExpenseViewModelFactory
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.exp


class ExpenseListFragment : Fragment() {

    private var _binding: FragmentUserExpenseListBinding? = null

    private val binding get() = _binding!!

    private val viewModel: ExpenseViewModel by activityViewModels {
        ExpenseViewModelFactory(ExpenseRepository(), CarRepository(), SessionProvider())
    }

    private lateinit var expenseAdapter: ExpenseAdapter

    private val args: ExpenseListFragmentArgs by navArgs()

    private val createPdfLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let{
            generatePdf(it)
        } ?: Toast.makeText(requireContext(), getString(R.string.error_PDF), Toast.LENGTH_SHORT).show()
    }


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
        observeViewModel()

        val carIdFromArgs = args.carId

        if(!carIdFromArgs.isNullOrBlank()){
            viewModel.applyFilters(carIdFromArgs, null, null, null)
        } else {
            viewModel.applyFilters(null, null, null, null)
        }

        binding.filterExpensesButton.setOnClickListener {
            FilterExpenseModalFragment().show(parentFragmentManager, "FilterExpenseModalTag")
        }

        binding.pdfExpensesButton.setOnClickListener {
            if (viewModel.expenses.value.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.no_gastos_exportar), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fileName = "Informe_Gastos_${System.currentTimeMillis()}.pdf"
            createPdfLauncher.launch(fileName)
        }
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(AdapterMode.USER_EXPENSE,
            onEditClick = { expense ->
                handleEditExpense(expense)
            },
            onDeleteClick = { expense ->
                handleDeleteExpense(expense)
            })
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

    private fun handleEditExpense(expense: Expense) {

        val carId = expense.carId
        val carName = expense.carName
        if(carId.isNullOrBlank() || carName.isNullOrBlank()){
            Toast.makeText(requireContext(), "Error: El gasto no tiene información del vehículo.", Toast.LENGTH_SHORT).show()
            return
        }

        val partialCar = Car(
            id = expense.carId,
            brand = carName.split(" - ").getOrElse(0) { "" },
            model = carName.split(" - ").getOrElse(1) { "" }

        )

        val action = ExpenseListFragmentDirections.actionExpenseListToExpenseForm(partialCar, expense)

        findNavController().navigate(action)
    }

    private fun handleDeleteExpense(expense: Expense) {

        val expenseId = expense.id

        if(expenseId.isNullOrBlank()){
            Toast.makeText(requireContext(), "Error: El gasto no tiene un ID válido.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar el gasto '${expense.description}'?")
            .setPositiveButton("Eliminar") { _, _ ->

                viewModel.deleteExpense(expenseId)
                Toast.makeText(requireContext(), "Gasto eliminado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }



    private fun generatePdf(uri: Uri) {
        val allExpenses = viewModel.expenses.value // Obtenemos la lista FILTRADA actual
        if (allExpenses.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_gastos_informe), Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Agrupar los gastos por 'carId'. El resultado es un Map<String, List<Expense>>
        val expensesGroupedByCar = allExpenses.groupBy { it.carId }

        // --- Configuración del Documento y Estilos ---
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Tamaño A4 (aprox)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Estilos de texto
        val titlePaint = Paint().apply {
            textAlign = Paint.Align.CENTER
            textSize = 20f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val carTitlePaint = Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val tableHeaderPaint = Paint().apply {
            textSize = 10f
            isFakeBoldText = true
            color = Color.DKGRAY
        }
        val textPaint = Paint().apply {
            textSize = 10f
            color = Color.BLACK
        }
        val totalPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = Color.BLACK
        }

        // --- Definición de la estructura de la tabla ---
        val leftMargin = 40f
        val rightMargin = 40f
        val tableWidth = pageInfo.pageWidth - leftMargin - rightMargin
        val columnWidths = floatArrayOf(0.20f, 0.40f, 0.40f) // 3 columnas: Fecha, Tipo/Descripción, Monto
        val columns = listOf(getString(R.string.fecha), getString(R.string.detalle), getString(R.string.monto))
        var yPosition = 80f // Posición Y inicial

        // --- Dibujar el Título Principal ---
        canvas.drawText(getString(R.string.informe_gastos), pageInfo.pageWidth / 2f, yPosition, titlePaint)
        yPosition += 40f // Espacio después del título

        // --- Procesar cada grupo de gastos (por coche) ---
        var grandTotal = 0.0
        for ((carId, expensesForCar) in expensesGroupedByCar) {
            // Dibujar el nombre del coche como título de la sección
            val carName = expensesForCar.firstOrNull()?.carName ?: getString(R.string.vehiculo_desconocido)
            canvas.drawText(carName, leftMargin, yPosition, carTitlePaint)
            yPosition += 25f

            // Dibujar la cabecera de la tabla
            var xPosition = leftMargin
            for (i in columns.indices) {
                canvas.drawText(columns[i], xPosition, yPosition, tableHeaderPaint)
                xPosition += tableWidth * columnWidths[i]
            }
            yPosition += 5f
            canvas.drawLine(leftMargin, yPosition, pageInfo.pageWidth - rightMargin, yPosition, tableHeaderPaint) // Línea divisoria
            yPosition += 15f

            // Dibujar cada gasto en una línea
            var carSubtotal = 0.0
            for (expense in expensesForCar) {
                val dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expense.date!!)
                val expenseTypeName = viewModel.expenseTypes.value.find { it.id == expense.expenseTypeId }?.name ?: "N/A"
                val detailString = "$expenseTypeName: ${expense.description}"
                val amountString = "$${"%.2f".format(expense.amount)}"

                // Dibujar en columnas
                xPosition = leftMargin
                // Columna 1: Fecha
                canvas.drawText(dateString, xPosition, yPosition, textPaint)
                xPosition += tableWidth * columnWidths[0]
                // Columna 2: Detalle
                canvas.drawText(detailString, xPosition, yPosition, textPaint)
                xPosition += tableWidth * columnWidths[1]
                // Columna 3: Monto
                canvas.drawText(amountString, xPosition, yPosition, textPaint)

                yPosition += 15f // Espacio para la siguiente línea
                carSubtotal += expense.amount!!
            }

            // Subtotal por coche (opcional)
            yPosition += 10f
            val formattedSubtotal = "$${"%.2f".format(carSubtotal)}"
            val subtotalString = getString(R.string.subtotal, carName, formattedSubtotal)

            totalPaint.textAlign = Paint.Align.RIGHT // Alineamos el subtotal a la derecha
            canvas.drawText(subtotalString, pageInfo.pageWidth - rightMargin, yPosition, totalPaint)
            totalPaint.textAlign = Paint.Align.LEFT // Restauramos la alineación
            yPosition += 35f // Espacio grande antes del siguiente coche

            grandTotal += carSubtotal
        }

        // --- Dibujar el Total General al Final ---
        yPosition += 20f
        canvas.drawLine(leftMargin, yPosition, pageInfo.pageWidth - rightMargin, yPosition, titlePaint) // Línea final
        yPosition += 30f
        val formattedTotal = "$${"%.2f".format(grandTotal)}"
        val grandTotalString = getString(R.string.pdf_general_total, formattedTotal)
        totalPaint.textSize = 18f
        totalPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(grandTotalString, pageInfo.pageWidth / 2f, yPosition, totalPaint)

        pdfDocument.finishPage(page)

        // --- Guardado del PDF ---
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
                Toast.makeText(requireContext(), getString(R.string.PDF_generado), Toast.LENGTH_LONG).show()
                openPdf(uri) // Abrir el PDF después de guardarlo
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), getString(R.string.error_guardar_pdf), Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }



    private fun openPdf(pdfUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.no_aplicacion_pdf), Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}




