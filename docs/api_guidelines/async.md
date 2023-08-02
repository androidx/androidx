## Asynchronous work {#async}

### With return values {#async-return}

#### Kotlin

Traditionally, asynchronous work on Android that results in an output value
would use a callback; however, better alternatives exist for libraries.

Kotlin libraries should consider
[coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) and
`suspend` functions for APIs according to the following rules, but please refer
to the guidance on [allowable dependencies](#dependencies-coroutines) before
adding a new dependency on coroutines.

Kotlin suspend fun vs blocking       | Behavior
------------------------------------ | --------------------------
blocking function with @WorkerThread | API is blocking
suspend                              | API is async (e.g. Future)

In general, do not introduce a suspend function entirely to switch threads for
blocking calls. To do so correctly requires that we allow the developer to
configure the Dispatcher. As there is already a coroutines-based API for
changing dispatchers (withContext) that the caller may use to switch threads, it
is unecessary API overhead to provide a duplicate mechanism. In addition, it
unecessary limits callers to coroutine contexts.

```kotlin
// DO expose blocking calls as blocking calls
@WorkerThread
fun blockingCall()

// DON'T wrap in suspend functions (only to switch threads)
suspend fun blockingCallWrappedInSuspend(
  dispatcher: CoroutineDispatcher = Dispatchers.Default
) = withContext(dispatcher) { /* ... */ }

// DO expose async calls as suspend funs
suspend fun asyncCall(): ReturnValue

// DON'T expose async calls as a callback-based API (for the main API)
fun asyncCall(executor: Executor, callback: (ReturnValue) -> Unit)
```

#### Java

Java libraries should prefer `ListenableFuture` and the
[`CallbackToFutureAdapter`](https://developer.android.com/reference/androidx/concurrent/futures/CallbackToFutureAdapter)
implementation provided by the `androidx.concurrent:concurrent-futures` library.
Functions and methods that return `ListenableFuture` should be suffixed by,
`Async` to reserve the shorter, unmodified name for a `suspend` method or
extension function in Kotlin that returns the value normally in accordance with
structured concurrency.

Libraries **must not** use `java.util.concurrent.CompletableFuture`, as it has a
large API surface that permits arbitrary mutation of the future's value and has
error-prone defaults.

See the [Dependencies](#dependencies) section for more information on using
Kotlin coroutines and Guava in your library.

### Cancellation

Libraries that expose APIs for performing asynchronous work should support
cancellation. There are *very few* cases where it is not feasible to support
cancellation.

Libraries that use `ListenableFuture` must be careful to follow the exact
specification of
[`Future.cancel(boolean mayInterruptIfRunning)`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html?is-external=true#cancel-boolean-)
behavior.

```java {.bad}
@Override
public boolean cancel(boolean mayInterruptIfRunning) {
    // Does not support cancellation.
    return false;
}
```

```java {.bad}
@Override
public boolean cancel(boolean mayInterruptIfRunning) {
    // Aggressively does not support cancellation.
    throw new UnsupportedOperationException();
}
```

```java {.good}
@Override
public boolean cancel(boolean mayInterruptIfRunning) {
    // Pseudocode that ignores threading but follows the spec.
    if (mCompleted
            || mCancelled
            || mRunning && !mayInterruptIfRunning) {
        return false;
    }
    mCancelled = true;
    return true;
}
```

### Avoid `synchronized` methods

Whenever multiple threads are interacting with shared (mutable) references those
reads and writes must be synchronized in some way. However synchronized blocks
make your code thread-safe at the expense of concurrent execution. Any time
execution enters a synchronized block or method any other thread trying to enter
a synchronized block on the same object has to wait; even if in practice the
operations are unrelated (e.g. they interact with different fields). This can
dramatically reduce the benefit of trying to write multi-threaded code in the
first place.

Locking with synchronized is a heavyweight form of ensuring ordering between
threads, and there are a number of common APIs and patterns that you can use
that are more lightweight, depending on your use case:

*   Compute a value once and make it available to all threads
*   Update Set and Map data structures across threads
*   Allow a group of threads to process a stream of data concurrently
*   Provide instances of a non-thread-safe type to multiple threads
*   Update a value from multiple threads atomically
*   Maintain granular control of your concurrency invariants
