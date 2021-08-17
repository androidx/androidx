package androidx.datastore.core

import java.util.concurrent.atomic.AtomicInteger

internal actual class AtomicCounter actual constructor(initialValue: Int) {
    val counter = AtomicInteger(initialValue)
    actual fun get(): Int = counter.get()
    actual fun getAndIncrement(): Int = counter.getAndIncrement()
    actual fun decrementAndGet(): Int = counter.decrementAndGet()
}

public actual typealias IllegalStateException = java.lang.IllegalStateException
