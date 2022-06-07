/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.datastore.core

/**
 * StorageConnection provides a way to read and write a particular type <T> of data.
 * StorageConnections are created from [Storage] objects.
 */
interface StorageConnection<T> : Closeable {

    /**
     * Creates a read transaction to allow storage reads.
     *
     * @param lockType Controls the kind of locking performed within the transaction.
     * @param block The block of code that is performed within this transaction.
     *
     * @throws IOException when there is an unrecoverable exception in reading.
     */
    suspend fun <R> readTransaction(
        lockType: ReadLockType = ReadLockType.NO_LOCK,
        block: suspend ReadScope<T>.() -> R
    ): R

    /**
     * Creates an write transaction that guaranteed to only have one single writer, insuring also
     * that any reads within this scope have the most current data.
     *
     * @throws IOException when there is an unrecoverable exception in writing.
     */
    suspend fun writeTransaction(block: suspend WriteScope<T>.() -> Unit)

    /** Specifies the locking type for this read transaction. */
    enum class ReadLockType {
        /**
         * Does not attempt to do a read lock in this transaction. All reads will be whatever
         * value has been saved and fsync'd to disk at the time of the read.
         */
        NO_LOCK,

        /**
         * Tries to get a read lock during the scope of the transaction. If it is successful,
         * [ReadScope.lockAcquired] will be true.  A successful lock guarantees no writers are
         * interleaved during the scope of this transaction, otherwise it behaves like NO_LOCK.
         */
        TRY_LOCK,

        /**
         * A lock is acquired before proceeding with the transaction.  This guarantees no writers
         * are interleaved during the scope of this transaction.
         */
        LOCK
    }
}

/**
 * The scope used for a read transaction.
 */
interface ReadScope<T> : Closeable {

    /**
     * Read the data <T> from the underlying storage.
     */
    suspend fun readData(): T

    /**
     * True, if a lock was acquired for this transaction.
     */
    val lockAcquired: Boolean
}

/**
 * The scope used for a write transaction.
 */
interface WriteScope<T> : ReadScope<T> {

    /**
     * Writes the data <T> to the underlying storage.
     */
    suspend fun writeData(value: T)
}

suspend fun <T> StorageConnection<T>.readData(): T = readTransaction { readData() }
suspend fun <T> StorageConnection<T>.writeData(value: T) = writeTransaction { writeData(value) }
