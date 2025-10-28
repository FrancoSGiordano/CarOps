package com.example.trabajointegrador_modulonativo.data

import android.util.Log
import com.example.trabajointegrador_modulonativo.FirebaseClient
import com.example.trabajointegrador_modulonativo.model.Reminder
import com.example.trabajointegrador_modulonativo.model.ReminderState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReminderRepository {
    private val db = FirebaseClient.db
    private val remindersCol = db.collection("reminders")

    suspend fun addReminder(reminder: com.example.trabajointegrador_modulonativo.model.Reminder): String {
        // agrega el documento y devuelve el id generado por Firestore
        val docRef = remindersCol.add(reminder).await()
        return docRef.id
    }

    fun createReminder(reminder: Reminder){
        remindersCol.add(reminder)
            .addOnSuccessListener { documentReference ->
            Log.d("TAG", "DocumentSnapshot added with ID: ${documentReference.id}")
        }.addOnFailureListener { e ->
                Log.w("TAG", "Error adding document", e)
            }
    }

    suspend fun updateReminder(reminder: Reminder) {
        val id = reminder.id ?: return
        remindersCol.document(id).set(reminder).await()
    }

    suspend fun deleteReminder(id: String) {
        remindersCol.document(id).delete().await()
    }

    fun getRemindersStreamForCar(carId: String): Flow<List<Reminder>> = callbackFlow {
        val subscription = remindersCol
            .whereEqualTo("carId", carId)
            .orderBy("notifyAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val r = doc.toObject(Reminder::class.java)
                    r?.apply { id = doc.id }
                } ?: emptyList()
                trySend(items).isSuccess
            }
        awaitClose { subscription.remove() }
    }


    fun getRemindersStreamForUser(): Flow<List<Reminder>> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            trySend(emptyList()).isSuccess
            close()
            return@callbackFlow
        }
        val subscription = remindersCol
            .whereEqualTo("userId", uid)
            .orderBy("notifyAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val r = doc.toObject(Reminder::class.java)
                    r?.apply { id = doc.id }
                } ?: emptyList()
                trySend(items).isSuccess
            }
        awaitClose { subscription.remove() }
    }

    fun getPendingRemindersStreamForUser(): Flow<List<Reminder>> = callbackFlow {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            trySend(emptyList()).isSuccess
            close()
            return@callbackFlow
        }
        val subscription = remindersCol
            .whereEqualTo("userId", uid)
            .whereEqualTo("state", ReminderState.PENDIENTE.name)
            .orderBy("notifyAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    val r = doc.toObject(Reminder::class.java)
                    r?.apply { id = doc.id }
                } ?: emptyList()
                trySend(items).isSuccess
            }
        awaitClose { subscription.remove() }
    }

    suspend fun markAsRealizado(reminderId: String) {
        val updates = mapOf(
            "state" to ReminderState.REALIZADO.name,
            "notificationSent" to true,
            "done" to true
        )
        remindersCol.document(reminderId).update(updates).await()
    }

    suspend fun markDueRemindersAsPending() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val now = Timestamp.now()

        val q = remindersCol
            .whereEqualTo("userId", uid)
            .whereEqualTo("state", ReminderState.EN_ESPERA.name)
            .whereLessThanOrEqualTo("notifyAt", now)

        val snap = q.get().await()
        if (snap.isEmpty) return

        val batch = db.batch()
        for (doc in snap.documents) {
            batch.update(doc.reference, mapOf("state" to ReminderState.PENDIENTE.name))
        }
        batch.commit().await()
    }
}
