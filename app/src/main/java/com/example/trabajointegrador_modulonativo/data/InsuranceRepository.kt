package com.example.trabajointegrador_modulonativo.data

import android.system.Os.close
import android.util.Log
import com.example.trabajointegrador_modulonativo.FirebaseClient
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.model.Insurance
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await



class InsuranceRepository {

    private val insuranceCollection = FirebaseClient.db.collection("insurances")

    private val carCollection = FirebaseClient.db.collection("cars")

    suspend fun getInsuranceById(insuranceId : String): Insurance? {
        return try {
            val docRef = insuranceCollection.document(insuranceId)
            docRef.get().await().toObject(Insurance::class.java)
        } catch (e: Exception) {
            Log.e("InsuranceRepo", "Error al obtener seguro $insuranceId", e)
            null
        }
    }

    suspend fun addInsurance(insurance: Insurance, carId: String) {
        FirebaseClient.db.runTransaction { transaction ->

            val newInsuranceRef = insuranceCollection.document()
            transaction.set(newInsuranceRef, insurance)

            val carRef = carCollection.document(carId)
            transaction.update(carRef, "insuranceId", newInsuranceRef.id)
        }.await()
    }

    suspend fun updateInsurance(insurance: Insurance) {
        val docRef = insuranceCollection.document(insurance.id.toString())
        if(insurance.id.isNullOrBlank()) {
            Log.w("InsuranceRepository", "Error al actualizar, el id del seguro es nulo")
            return
        }
        docRef.set(insurance)
            .addOnSuccessListener {
                Log.d("TAG", "DocumentSnapshot successfully updated!")
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error updating document", e)
            }
    }

    suspend fun deleteInsurance(insuranceId: String, carId: String) {
        if(insuranceId.isBlank() || carId.isBlank()) {
            throw IllegalArgumentException("IDs de seguro o coche no válidos para eliminar.")
        }

        FirebaseClient.db.runTransaction { transaction ->
            transaction.delete(insuranceCollection.document(insuranceId))

            val carRef = carCollection.document(carId)
            transaction.update(carRef, "insuranceId", null)



            null
        }.await()
    }



}