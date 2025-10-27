package com.example.trabajointegrador_modulonativo.data

import android.system.Os.close
import android.util.Log
import com.example.trabajointegrador_modulonativo.FirebaseClient
import com.example.trabajointegrador_modulonativo.model.Expense
import com.example.trabajointegrador_modulonativo.model.ExpenseType
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date


class ExpenseRepository {

    private val db = FirebaseClient.db

    private val expenseCollection = db.collection("expenses")
    private val expenseTypeCollection = db.collection("expenseTypes")


    fun createExpense(expense: Expense) {
        expenseCollection.add(expense)
            .addOnSuccessListener { documentReference ->
                Log.d("TAG", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error adding document", e)
            }
    }



    fun getExpensesForUserStream(userId: String, carId: String?, expenseTypeId: Int?, startDate: Date?, endDate: Date?): Flow<List<Expense>> = callbackFlow {

        var query: Query = expenseCollection
            .whereEqualTo("userId", userId)

        if(!carId.isNullOrBlank()) {
            query = query.whereEqualTo("carId", carId)
        }

        if(expenseTypeId != null) {
            query = query.whereEqualTo("expense_type_id", expenseTypeId)
        }

        if(startDate != null) {
            query = query.whereGreaterThanOrEqualTo("date", startDate)
        }

        if(endDate != null) {
            query = query.whereLessThanOrEqualTo("date", endDate)
        }

        val subscription = query
            .addSnapshotListener { snapshot, error ->
                if(error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val expenses = snapshot?.documents?.mapNotNull { document ->
                    try {
                        document.toObject<Expense>()?.apply {
                            id = document.id
                        }
                    } catch (e: Exception) {
                        Log.e("Repo", "Error al parsear gasto ${document.id}", e)
                        null
                    }
                } ?: emptyList()

                Log.e("FilterDebug", "Firestore devolvió ${expenses.size} documentos.")

                trySend(expenses)
            }

        awaitClose {
            subscription.remove()
        }
    }



    suspend fun getExpenseTypes(): List<ExpenseType> {
        return try {
            val snapshot = expenseTypeCollection.get().await()
            snapshot.toObjects(ExpenseType::class.java)
        } catch (e: Exception) {
            Log.e("Repo", "Error al obtener tipos de gasto", e)
            emptyList()
        }
    }



}

