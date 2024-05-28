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

package androidx.glance.session

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceComposable
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException

/**
 * [Session] is implemented by Glance surfaces in order to provide content for the composition and
 * process the results of recomposition.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class Session(val key: String) {
    // _isOpen/isOpen is used to check whether this Session's event channel is still open and
    // accepting events (close has not been called). It may be checked or set from different
    // threads, so we use an AtomicBoolean so that the value is updated atomically.
    private val _isOpen = AtomicBoolean(true)
    internal val isOpen: Boolean
        get() = _isOpen.get()

    private val eventChannel = Channel<Any>(Channel.UNLIMITED)

    /**
     * Create the [EmittableWithChildren] that will be used as the [androidx.glance.Applier] root.
     */
    abstract fun createRootEmittable(): EmittableWithChildren

    /** Provide the Glance composable to be run in the [androidx.compose.runtime.Composition]. */
    abstract fun provideGlance(context: Context): @Composable @GlanceComposable () -> Unit

    /**
     * Process the Emittable tree that results from the running [provideGlance].
     *
     * This will also be called for the results of future recompositions.
     *
     * @return true if the tree has been processed and the session is ready to handle events.
     */
    abstract suspend fun processEmittableTree(
        context: Context,
        root: EmittableWithChildren
    ): Boolean

    /** Process an event that was sent to this session. */
    abstract suspend fun processEvent(context: Context, event: Any)

    /**
     * Enqueues an [event] to be processed by the session.
     *
     * These requests may be processed by calling [receiveEvents]. Session implementations should
     * wrap sendEvent with public methods to send the event types that their Session supports.
     */
    protected suspend fun sendEvent(event: Any) {
        eventChannel.send(event)
    }

    /**
     * Process incoming events, additionally running [block] for each event that is received.
     *
     * This function suspends until [close] is called.
     */
    suspend fun receiveEvents(context: Context, block: (Any) -> Unit) {
        try {
            for (event in eventChannel) {
                block(event)
                processEvent(context, event)
            }
        } catch (_: ClosedReceiveChannelException) {}
    }

    /**
     * Close the session. Any events sent before [close] will be processed unless the Worker for
     * this session is cancelled.
     */
    fun close() {
        eventChannel.close()
        _isOpen.set(false)
        onClosed()
    }

    /**
     * Called after the session is closed. Can be used by implementers to clean up any resources.
     */
    open fun onClosed() {}

    /**
     * Called when there is an error in the composition. The session will be closed immediately
     * after this.
     */
    open suspend fun onCompositionError(context: Context, throwable: Throwable) {
        Log.e("GlanceSession", "Error running composition", throwable)
    }
}
