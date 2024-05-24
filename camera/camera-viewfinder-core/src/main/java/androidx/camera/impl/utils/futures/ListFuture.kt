/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.impl.utils.futures

import androidx.camera.impl.utils.executor.ViewfinderExecutors
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.util.Preconditions
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * The Class is based on the ListFuture in Guava and to use the CallbackToFutureAdapter instead of
 * the AbstractFuture.
 *
 * Class that implements [Futures.allAsList] and [Futures.successfulAsList]. The idea is to create a
 * (null-filled) List and register a listener with each component future to fill out the value in
 * the List when that future completes.
 *
 * @param futures all the futures to build the list from
 * @param allMustSucceed whether a single failure or cancellation should propagate to this future
 * @param listenerExecutor used to run listeners on all the passed in futures.
 */
internal class ListFuture<V>(
    private var futures: List<ListenableFuture<out V>>,
    private val allMustSucceed: Boolean,
    listenerExecutor: Executor
) : ListenableFuture<List<V?>?> {
    private var futuresInternal: List<ListenableFuture<out V>>?
    private var values: MutableList<V?>?
    private val remaining: AtomicInteger
    private val result: ListenableFuture<List<V?>?>
    var resultNotifier: CallbackToFutureAdapter.Completer<List<V?>?>? = null

    init {
        futuresInternal = futures
        values = ArrayList(futures.size)
        remaining = AtomicInteger(futures.size)
        result =
            CallbackToFutureAdapter.getFuture(
                object : CallbackToFutureAdapter.Resolver<List<V?>?> {
                    override fun attachCompleter(
                        completer: CallbackToFutureAdapter.Completer<List<V?>?>
                    ): Any {
                        Preconditions.checkState(
                            resultNotifier == null,
                            "The result can only set once!"
                        )
                        resultNotifier = completer
                        return "ListFuture[$this]"
                    }
                }
            )
        init(listenerExecutor)
    }

    private fun init(listenerExecutor: Executor) {
        // First, schedule cleanup to execute when the Future is done.
        addListener(
            { // By now the values array has either been set as the Future's value,
                // or (in case of failure) is no longer useful.
                values = null

                // Let go of the memory held by other futuresInternal
                futuresInternal = null
            },
            ViewfinderExecutors.directExecutor()
        )

        // Now begin the "real" initialization.

        // Corner case: List is empty.
        if (futuresInternal!!.isEmpty()) {
            resultNotifier!!.set(ArrayList(values!!))
            return
        }

        // Populate the results list with null initially.
        for (i in futuresInternal!!.indices) {
            values!!.add(null)
        }

        // Register a listener on each Future in the list to update
        // the state of this future.
        // Note that if all the futuresInternal on the list are done prior to completing
        // this loop, the last call to addListener() will callback to
        // setOneValue(), transitively call our cleanup listener, and set
        // futuresInternal to null.
        // We store a reference to futuresInternal to avoid the NPE.
        val localFutures = futuresInternal
        for (i in localFutures!!.indices) {
            val listenable = localFutures[i]
            listenable.addListener({ setOneValue(i, listenable) }, listenerExecutor)
        }
    }

    /** Sets the value at the given index to that of the given future. */
    private fun setOneValue(index: Int, future: Future<out V>) {
        var localValues = values
        if (isDone || localValues == null) {
            // Some other future failed or has been cancelled, causing this one to
            // also be cancelled or have an exception set. This should only happen
            // if mAllMustSucceed is true.
            Preconditions.checkState(
                this.allMustSucceed,
                "Future was done before all dependencies completed"
            )
            return
        }
        try {
            Preconditions.checkState(
                future.isDone,
                "Tried to set value from future which is not done"
            )
            localValues[index] = Futures.getUninterruptibly(future)
        } catch (e: CancellationException) {
            if (this.allMustSucceed) {
                // Set ourselves as cancelled. Let the input futures keep running
                // as some of them may be used elsewhere.
                // (Currently we don't override interruptTask, so
                // mayInterruptIfRunning==false isn't technically necessary.)
                cancel(false)
            }
        } catch (e: ExecutionException) {
            if (this.allMustSucceed) {
                // As soon as the first one fails, throw the exception up.
                // The result of all other inputs is then ignored.
                resultNotifier!!.setException(e.cause!!)
            }
        } catch (e: RuntimeException) {
            if (this.allMustSucceed) {
                resultNotifier!!.setException(e)
            }
        } catch (e: Error) {
            // Propagate errors up ASAP - our superclass will rethrow the error
            resultNotifier!!.setException(e)
        } finally {
            val newRemaining = remaining.decrementAndGet()
            Preconditions.checkState(newRemaining >= 0, "Less than 0 remaining futures")
            if (newRemaining == 0) {
                localValues = values
                if (localValues != null) {
                    resultNotifier!!.set(ArrayList(localValues))
                } else {
                    Preconditions.checkState(isDone)
                }
            }
        }
    }

    override fun addListener(listener: Runnable, executor: Executor) {
        result.addListener(listener, executor)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (futuresInternal != null) {
            for (f in futuresInternal!!) {
                f.cancel(mayInterruptIfRunning)
            }
        }
        return result.cancel(mayInterruptIfRunning)
    }

    override fun isCancelled(): Boolean {
        return result.isCancelled
    }

    override fun isDone(): Boolean {
        return result.isDone
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): List<V?>? {
        callAllGets()

        // This may still block in spite of the calls above, as the listeners may
        // be scheduled for execution in other threads.
        return result.get()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun get(timeout: Long, unit: TimeUnit): List<V?>? {
        return result[timeout, unit]
    }

    /**
     * Calls the get method of all dependency futures to work around a bug in some ListenableFutures
     * where the listeners aren't called until get() is called.
     */
    @Throws(InterruptedException::class)
    private fun callAllGets() {
        val oldFutures = futuresInternal
        if (oldFutures != null && !isDone) {
            for (future in oldFutures) {
                // We wait for a little while for the future, but if it's not done,
                // we check that no other futures caused a cancellation or failure.
                // This can introduce a delay of up to 10ms in reporting an exception.
                while (!future.isDone) {
                    try {
                        future.get()
                    } catch (e: Error) {
                        throw e
                    } catch (e: InterruptedException) {
                        throw e
                    } catch (e: Throwable) {
                        // ExecutionException / CancellationException / RuntimeException
                        if (this.allMustSucceed) {
                            return
                        } else {
                            continue
                        }
                    }
                }
            }
        }
    }
}
