@file:JvmName("RoomDatabaseKt")

package androidx.room3

import androidx.room3.RoomDatabase

@Suppress("UNUSED_PARAMETER")
public suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R {
    TODO("")
}