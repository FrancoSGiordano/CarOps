package com.example.trabajointegrador_modulonativo.data

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.vector.path
import androidx.work.await
import com.example.trabajointegrador_modulonativo.FirebaseClient
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.notifications.ReminderScheduler
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class CarRepository {

    private val db = FirebaseClient.db
    private val carCollection = db.collection("cars")
    private val insuranceCollection = db.collection("insurances")
    private val expenseCollection = db.collection("expenses")
    private val reminderCollection = db.collection("reminders")

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

    suspend fun saveParking(
        carId: String,
        lat: Double,
        lng: Double,

    ) {
        if (carId.isBlank()) {
            Log.w("CarRepository", "saveParkingLocation: carId vacío")
            return
        }

        val docRef = carCollection.document(carId)
        val updates = mutableMapOf<String, Any>(
            "parked" to true,
            "parkedLat" to lat,
            "parkedLng" to lng,
            "parkedDate" to FieldValue.serverTimestamp()
        )


        docRef.update(updates)
            .addOnSuccessListener { Log.d("CarRepository", "Parking location saved for $carId") }
            .addOnFailureListener { e -> Log.w("CarRepository", "Error saving parking location", e) }
    }



    suspend fun deleteCar(car: Car,  context: Context) {
        val carId = car.id
        if (carId.isNullOrBlank()) {
            Log.w("CarRepository", "ID del coche es nulo o vacío, no se puede eliminar.")
            return
        }

        try {
            // 1. Si tiene insuranceId, borrar la carpeta asociada en Storage
            if (!car.insuranceId.isNullOrBlank()) {
                val insuranceId = car.insuranceId
                insuranceCollection.document(insuranceId).delete().await()
                deleteInsurancePolicyFolder(car.userId, carId)
            }
            deleteExpensesForCar(carId)

            if (!car.imageUrl.isNullOrBlank()) {
                deleteImageFromUrl(car.imageUrl)
            }
            deleteRemindersForCar(carId, context)
            // 2. Eliminar el documento del coche en Firestore
            carCollection.document(carId).delete().await()
            Log.d("CarRepository", "Coche con ID: $carId eliminado de Firestore.")

        } catch (e: Exception) {
            Log.w("CarRepository", "Error al eliminar el coche con ID: $carId", e)

        }
    }

    private suspend fun deleteInsurancePolicyFolder(userId: String, carId: String) {
        val folderPath = "policies/$userId/$carId"
        val folderRef = FirebaseStorage.getInstance().reference.child(folderPath)

        try {
            val items = folderRef.listAll().await()
            items.items.forEach { item ->
                item.delete().await()
                Log.d("CarRepository", "Archivo de póliza eliminado: ${item.path}")
            }

            Log.d("CarRepository", "Carpeta de póliza '$folderPath' eliminada de Storage.")
        } catch (e: Exception) {
            Log.w(
                "CarRepository",
                "Error al eliminar la carpeta de póliza '$folderPath' en Storage",
                e
            )
        }

    }

    private suspend fun deleteExpensesForCar(carId: String) {
        val query = expenseCollection.whereEqualTo("carId", carId)
        try {
            val snapshot = query.get().await()
            val batch = db.batch()
            for (document in snapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()
            Log.d("CarRepository", "Eliminados ${snapshot.size()} gastos para el coche con ID: $carId")
        } catch (e: Exception) {
            Log.w("CarRepository", "Error al eliminar los gastos para el coche con ID: $carId", e)
        }
    }

    private suspend fun deleteImageFromUrl(imageUrl: String) {
        try {
            val imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
            Log.d("CarRepository", "Imagen eliminada de Storage: ${imageRef.path}")
        } catch (e: Exception) {
            Log.w("CarRepository", "Error al eliminar la imagen desde la URL: $imageUrl", e)
        }
    }

    private suspend fun deleteRemindersForCar(carId: String, context: Context) {
        val query = reminderCollection.whereEqualTo("carId", carId)
        try {
            val snapshot = query.get().await()
            if (snapshot.isEmpty) {
                Log.d("CarRepository", "No se encontraron recordatorios para el coche: $carId")
                return
            }

            val batch = db.batch()
            for (document in snapshot.documents) {
                // 1. Cancelar la alarma en el dispositivo
                ReminderScheduler.cancel(context, document.id)

                // 2. Añadir la eliminación del documento al batch de Firestore
                batch.delete(document.reference)
            }

            // 3. Ejecutar la eliminación en Firestore
            batch.commit().await()

            Log.d("CarRepository", "Eliminados ${snapshot.size()} recordatorios y sus alarmas para el coche: $carId")
        } catch (e: Exception) {
            Log.w("CarRepository", "Error al eliminar los recordatorios para el coche: $carId", e)
        }
    }
}




