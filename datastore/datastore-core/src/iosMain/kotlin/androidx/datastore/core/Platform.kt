package androidx.datastore.core

import kotlin.native.concurrent.AtomicInt

internal actual class AtomicCounter actual constructor(initialValue: Int) {

    var atomicInt = AtomicInt(initialValue)

    actual fun get(): Int {
        return atomicInt.value
    }
    actual fun getAndIncrement(): Int {
        var oldValue: Int
        do {
            oldValue = atomicInt.value
            val newValue = oldValue + 1
        } while (!atomicInt.compareAndSet(oldValue, newValue))
        return oldValue
    }
    actual fun decrementAndGet(): Int {
        return atomicInt.addAndGet(-1)
    }
}

actual open class IllegalStateException actual constructor(message: String, cause: Throwable?) : RuntimeException(message, cause) {
    actual constructor(message: String) : this(message, null)
}
