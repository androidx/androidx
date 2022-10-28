@file:JvmName("RoomDatabaseKt")

package androidx.room

import androidx.room.RoomDatabase

@Suppress("UNUSED_PARAMETER")
public suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R {
    TODO("")
}