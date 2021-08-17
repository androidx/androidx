package androidx.datastore.core

internal expect class AtomicCounter(initialValue: Int) {
    fun get(): Int
    fun getAndIncrement(): Int
    fun decrementAndGet(): Int
}

public expect open class IllegalStateException(message: String, cause: Throwable?) : RuntimeException {
    constructor(message: String)
}
