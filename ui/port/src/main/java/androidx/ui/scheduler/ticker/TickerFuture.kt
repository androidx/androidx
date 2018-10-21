/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.scheduler.ticker

import androidx.ui.VoidCallback
import androidx.ui.foundation.diagnostics.describeIdentity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/**
 * An object representing an ongoing [Ticker] sequence.
 *
 * The [Ticker.start] method returns a [TickerFuture]. The [TickerFuture] will
 * complete successfully if the [Ticker] is stopped using [Ticker.stop] with
 * the `canceled` argument set to false (the default).
 *
 * If the [Ticker] is disposed without being stopped, or if it is stopped with
 * `canceled` set to true, then this Future will never complete.
 *
 * This class works like a normal [Future], but has an additional property,
 * [orCancel], which returns a derivative [Future] that completes with an error
 * if the [Ticker] that returned the [TickerFuture] was stopped with `canceled`
 * set to true, or if it was disposed without being stopped.
 *
 * To run a callback when either this future resolves or when the ticker is
 * canceled, use [whenCompleteOrCancel].
 */
class TickerFuture internal constructor(
    private val primaryCompleter: CompletableDeferred<Unit> = CompletableDeferred()
) : Deferred<Unit> by primaryCompleter {

    companion object {

        /**
         * Creates a [TickerFuture] instance that represents an already-complete
         * [Ticker] sequence.
         *
         * This is useful for implementing objects that normally defer to a [Ticker]
         * but sometimes can skip the ticker because the animation is of zero
         * duration, but which still need to represent the completed animation in the
         * form of a [TickerFuture].
         */
        fun complete(): TickerFuture {
            return TickerFuture(CompletableDeferred(Unit))
        }
    }

    private var secondaryCompleter: CompletableDeferred<Unit>? = null
    private var completed: Boolean? =
        null // null means unresolved, true means complete, false means canceled

    internal fun complete() {
        assert(completed == null)
        completed = true
        primaryCompleter.complete(Unit)
        secondaryCompleter?.complete(Unit)
    }

    internal fun cancel(ticker: Ticker) {
        assert(completed == null)
        completed = false
        secondaryCompleter?.completeExceptionally(TickerCanceled(ticker))
    }

    /**
     * Calls `callback` either when this future resolves or when the ticker is
     * canceled.
     *
     * Calling this method registers an exception handler for the [orCancel]
     * future, so even if the [orCancel] property is accessed, canceling the
     * ticker will not cause an uncaught exception in the current zone.
     */
    fun whenCompleteOrCancel(callback: VoidCallback) {
        orCancel.invokeOnCompletion { callback() }
    }

    /**
     * A future that resolves when this future resolves or throws when the ticker
     * is canceled.
     *
     * If this property is never accessed, then canceling the ticker does not
     * throw any exceptions. Once this property is accessed, though, if the
     * corresponding ticker is canceled, then the [Future] returned by this
     * getter will complete with an error, and if that error is not caught, there
     * will be an uncaught exception in the current zone.
     */
    val orCancel: Deferred<Unit>
        get() = secondaryCompleter ?: CompletableDeferred<Unit>().apply {
            val valCompleted = completed
            if (valCompleted != null) {
                if (valCompleted) {
                    complete(Unit)
                } else {
                    completeExceptionally(TickerCanceled())
                }
            }
            secondaryCompleter = this
        }

    override fun toString() = "${describeIdentity(this)}(" +
            "${if (completed == null) "active" else if (completed!!) "complete" else "canceled"})"
}

/**
 * Exception thrown by [Ticker] objects on the [TickerFuture.orCancel] future
 * when the ticker is canceled.
 */
class TickerCanceled(
    /**
     * Reference to the [Ticker] object that was canceled.
     *
     * This may be null in the case that the [Future] created for
     * [TickerFuture.orCancel] was created after the ticker was canceled.
     */
    val ticker: Ticker? = null
) : Exception() {

    override fun toString(): String {
        if (ticker != null)
            return "This ticker was canceled: $ticker"
        return "The ticker was canceled before the \"orCancel\" property was first used."
    }
}