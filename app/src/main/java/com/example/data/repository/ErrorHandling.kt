package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

enum class OperationType {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    LIST
}

fun handleFirestoreError(exception: Exception, operation: OperationType, path: String) {
    if (exception is FirebaseFirestoreException) {
        when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                Log.e("FirestoreError", "PERMISSION_DENIED during $operation at $path: ${exception.message}")
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                Log.w("FirestoreError", "UNAVAILABLE during $operation at $path: ${exception.message}")
            FirebaseFirestoreException.Code.NOT_FOUND ->
                Log.e("FirestoreError", "NOT_FOUND during $operation at $path: ${exception.message}")
            else ->
                Log.e("FirestoreError", "${exception.code} during $operation at $path", exception)
        }
    } else {
        Log.e("FirestoreError", "Error during $operation at $path", exception)
    }
}
