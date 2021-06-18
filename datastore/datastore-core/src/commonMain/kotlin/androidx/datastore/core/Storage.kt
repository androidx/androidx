package androidx.datastore.core

interface Storage<T> {
    suspend fun readData(): T
    suspend fun writeData(newData: T)

    // TODO(lukhnos): Conform to Kotlin's Completable/Closeable (?) interface
    fun complete();
}