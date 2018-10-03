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

package androidx.ui.animation

import androidx.annotation.CallSuper
import androidx.ui.VoidCallback
import androidx.ui.foundation.ObserverList
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.assertions.FlutterErrorDetails
import androidx.ui.runtimeType

// TODO(Migration:Andrey) Crane's combination of ListenerMixin, AnimationLocalListenersMixin and
// TODO(Migration:Andrey) AnimationLocalStatusListenersMixin. Kotlin is not as flexible as mixins
abstract class AnimationLocalListenersMixin<T> : Animation<T>() {

    // ListenerMixin part:

    protected abstract fun didRegisterListener()

    protected abstract fun didUnregisterListener()

    // AnimationLocalListenersMixin part:
    //
    // A mixin that implements the [addListener]/[removeListener] protocol and notifies
    // all the registered listeners when [notifyListeners] is called.

    private val listeners = ObserverList<VoidCallback>()

    /**
     * Calls the listener every time the value of the animation changes.
     *
     * Listeners can be removed with [removeListener].
     */
    override fun addListener(listener: VoidCallback) {
        didRegisterListener()
        listeners.add(listener)
    }

    /**
     * Stop calling the listener every time the value of the animation changes.
     *
     * Listeners can be added with [addListener].
     */
    override fun removeListener(listener: VoidCallback) {
        listeners.remove(listener)
        didUnregisterListener()
    }

    /**
     * Calls all the listeners.
     *
     * If listeners are added or removed during this function, the modifications
     * will not change which listeners are called during this iteration.
     */
    protected val notifyListeners = {
        val localListeners = listeners.toList()
        for (listener: VoidCallback in localListeners) {
            try {
                if (listeners.contains(listener)) {
                    listener()
                }
            } catch (exception: Exception) {
                FlutterError.reportError(
                    FlutterErrorDetails(
                        exception = exception,
                        library = "animation library",
                        context = "while notifying listeners for ${runtimeType()}",
                        informationCollector = { information ->
                            information.append("The ${runtimeType()} notifying listeners was:\n")
                            information.append("  $this")
                        }
                    ))
            }
        }
    }

    // AnimationLocalStatusListenersMixin part:
    //
    // A mixin that implements the addStatusListener/removeStatusListener protocol
    // and notifies all the registered listeners when notifyStatusListeners is
    // called.

    private val statusListeners = ObserverList<AnimationStatusListener>()

    /**
     * Calls listener every time the status of the animation changes.
     *
     * Listeners can be removed with [removeStatusListener].
     */
    override fun addStatusListener(listener: AnimationStatusListener) {
        didRegisterListener()
        statusListeners.add(listener)
    }

    /**
     * Stops calling the listener every time the status of the animation changes.
     *
     * Listeners can be added with [addStatusListener].
     */
    override fun removeStatusListener(listener: AnimationStatusListener) {
        statusListeners.remove(listener)
        didUnregisterListener()
    }

    /**
     * Calls all the status listeners.
     *
     * If listeners are added or removed during this function, the modifications
     * will not change which listeners are called during this iteration.
     */
    protected val notifyStatusListeners = { status: AnimationStatus ->
        val localListeners = statusListeners.toList()
        for (listener: AnimationStatusListener in localListeners) {
            try {
                if (statusListeners.contains(listener)) {
                    listener(status)
                }
            } catch (exception: Exception) {
                FlutterError.reportError(FlutterErrorDetails(
                    exception = exception,
                    library = "animation library",
                    context = "while notifying status listeners for ${runtimeType()}",
                    informationCollector = { information ->
                        information.append("The ${runtimeType()} notifying status listeners was:\n")
                        information.append("  $this")
                    }
                ))
            }
        }
    }
}

/** A mixin that helps listen to another object only when this object has registered listeners. */
abstract class AnimationLazyListenerMixin<T> : AnimationLocalListenersMixin<T>() {

    private var listenerCounter: Int = 0

    override fun didRegisterListener() {
        assert(listenerCounter >= 0)
        if (listenerCounter == 0)
            didStartListening()
        listenerCounter += 1
    }

    override fun didUnregisterListener() {
        assert(listenerCounter >= 1)
        listenerCounter -= 1
        if (listenerCounter == 0)
            didStopListening()
    }

    /** Called when the number of listeners changes from zero to one. */
    protected abstract fun didStartListening()

    /** Called when the number of listeners changes from one to zero. */
    protected abstract fun didStopListening()

    /** Whether there are any listeners. */
    protected val isListening: Boolean = listenerCounter > 0
}

/**
 * A mixin that replaces the didRegisterListener/didUnregisterListener contract
 * with a dispose contract.
 */
abstract class AnimationEagerListenerMixin<T> : AnimationLocalListenersMixin<T>() {

    override fun didRegisterListener() {}

    override fun didUnregisterListener() {}

    /**
     * Release the resources used by this object. The object is no longer usable
     * after this method is called.
     */
    @CallSuper
    open fun dispose() {
    }
}