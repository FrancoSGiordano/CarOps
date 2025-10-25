package com.example.trabajointegrador_modulonativo.data

import android.util.Log
import com.example.trabajointegrador_modulonativo.FirebaseClient
import com.example.trabajointegrador_modulonativo.model.Car
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class CarRepository {

    private val db = FirebaseClient.db
    private val carCollection = db.collection("cars")
    fun getCarsStream(userId: String): Flow<List<Car>> = callbackFlow {

        val query: Query = carCollection.whereEqualTo("userId", userId)

        val subscription = query.addSnapshotListener { snapshot, error ->

            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val cars = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Car::class.java)?.apply {
                    id = doc.id
                }
            } ?: emptyList()

            trySend(cars)
        }
        awaitClose {
            subscription.remove()
        }
    }

    suspend fun addCar(car : Car) {
        carCollection.add(car)
            .addOnSuccessListener { documentReference ->
                Log.d("TAG", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error adding document", e)
            }

    }

    fun getCarById(carId: String): Flow<Car?> = callbackFlow {
        val docRef = carCollection.document(carId)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val car = snapshot.toObject(Car::class.java)
                car?.id = snapshot.id
                trySend(car)
            } else {
                trySend(null)
            }
        }
        awaitClose{
            subscription.remove()
        }
    }

    suspend fun updateCar(car: Car) {
        val docRef = carCollection.document(car.id.toString())
        if(car.id.isNullOrBlank()) {
            Log.w("CarRepository", "Error al actualizar, el id del coche es nulo")
            return
        }
        docRef.set(car)
            .addOnSuccessListener {
                Log.d("TAG", "DocumentSnapshot successfully updated!")
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error updating document", e)
            }

    }



}




