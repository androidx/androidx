/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.platform

import androidx.xr.scenecore.Dimensions
import androidx.xr.scenecore.Session
import java.io.Closeable

/** Provides a [SessionCallbacks] for the current Activity. */
public fun interface SessionCallbackProvider {
    public operator fun get(session: Session): SessionCallbacks

    public companion object {
        public val default: SessionCallbackProvider =
            object : SessionCallbackProvider {
                val callbackMap = mutableMapOf<Session, SessionCallbacks>()

                override fun get(session: Session): SessionCallbacks =
                    callbackMap.computeIfAbsent(session, ::DefaultSessionCallbacks)
            }
    }
}

/** Class to store callback lambdas associated with the Session. */
@Suppress("SingularCallback")
public interface SessionCallbacks {
    /**
     * Register a [callback] to be triggered when the app goes into full space mode. It will be
     * called immediately upon registration if the system is currently in full space mode and its
     * state is known.
     *
     * @param callback the method that will be called when the application enters full space mode.
     * @return a closeable to unregister the callback
     */
    public fun onFullSpaceMode(callback: () -> Unit): Closeable

    /**
     * Register a [callback] to be triggered when the app goes into home space mode. It will be
     * called immediately upon registration if the system is currently in home space mode and its
     * state is known.
     *
     * @param callback the method that will be called when the application enters home space mode.
     *   It will be called with the current application bounds as its argument.
     * @return a closeable to unregister the callback
     */
    public fun onHomeSpaceMode(callback: (Dimensions) -> Unit): Closeable

    /**
     * Register a [callback] to be triggered when the app changes space mode. It will be called
     * immediately with the current state if that state is available.
     *
     * @param callback the method that will be called when the space mode changes. It will accept an
     *   enum [SpaceMode] value indicating the current mode.
     * @return a closeable to unregister the callback
     */
    public fun onSpaceModeChanged(callback: (spaceMode: SpaceMode) -> Unit): Closeable

    /**
     * Register a [callback] to be triggered when the bounds of the app change. It will be called
     * immediately with the current state if that state is available.
     *
     * @param callback the method that will be called when the application bounds change. The
     *   argument passed to the callback represents the current bounds of the application.
     * @return a closeable to unregister the callback
     */
    public fun onBoundsChanged(callback: (Dimensions) -> Unit): Closeable
}

/**
 * Represents immersion level capabilities of the current application environment.
 *
 * In XR environments, there are a few different modes that an application can run in:
 * - Home Space Mode - the application can run side-by-side with other applications for
 *   multitasking; however, spatialization (e.g. Orbiters, Panels, 3D objects, etc.) is not allowed.
 * - Full Space Mode - the application is given the entire space and has access to spatialization.
 *   It may render multiple panels and 3D objects.
 */
@JvmInline
public value class SpaceMode private constructor(public val value: Int) {
    public companion object {
        /**
         * Current space mode or capabilities unknown. Awaiting checks to complete to indicate the
         * proper space mode.
         */
        public val Unspecified: SpaceMode = SpaceMode(0)

        /**
         * Space mode is not applicable to the current system. This indicates that the current
         * system is not XR (e.g. Phone, Tablet, etc.)
         */
        public val NotApplicable: SpaceMode = SpaceMode(1)

        /**
         * The XR application is currently in Full Space Mode.
         *
         * In Full Space Mode, the application is given the entire space for its own content. It has
         * access to spatialization and may render multiple panels and 3D objects.
         */
        public val Full: SpaceMode = SpaceMode(2)

        /**
         * The XR application is currently in Home Space Mode.
         *
         * In Home Space Mode, the application does not have access to spatialization. It may not
         * render multiple panels, orbiters, or 3D objects. However, it may run side-by-side with
         * other applications for multitasking.
         */
        public val Home: SpaceMode = SpaceMode(3)
    }
}

private class DefaultSessionCallbacks(session: Session) : SessionCallbacks {
    private var currentDimensions: Dimensions? = null
    private val onBoundsChangeListeners: MutableList<(Dimensions) -> Unit> = mutableListOf()

    init {
        // TODO: b/370618645 - This uses a direct executor for the callbacks, to maintain consistent
        // behavior for clients using Closeables returned by other methods in this class. We should
        // reconsider this approach, see the linked bug for more details.
        session.activitySpace.addBoundsChangedListener({ runnable -> runnable.run() }) {
            onBoundsChangeListeners.forEach { callback -> callback.invoke(it) }
            currentDimensions = it
        }
    }

    override fun onFullSpaceMode(callback: () -> Unit): Closeable = add {
        if (it.isFullSpace) {
            callback()
        }
    }

    override fun onHomeSpaceMode(callback: (Dimensions) -> Unit): Closeable = add {
        if (!it.isFullSpace) {
            callback(it)
        }
    }

    override fun onSpaceModeChanged(callback: (SpaceMode) -> Unit): Closeable = add {
        // Invoke the callback if the current full space state is not equal to the next full
        // space state or if the current dimensions are the same object reference as the
        // next dimensions (initial call when this callback is registered)
        if (it.isFullSpace != currentDimensions?.isFullSpace || it === currentDimensions) {
            callback(if (it.isFullSpace) SpaceMode.Full else SpaceMode.Home)
        }
    }

    override fun onBoundsChanged(callback: (Dimensions) -> Unit): Closeable = add(callback)

    private fun add(callback: (nextDimensions: Dimensions) -> Unit): Closeable {
        if (currentDimensions != null) {
            callback(currentDimensions!!)
        }
        onBoundsChangeListeners.add(callback)
        return Closeable {

            // Tests break if this line stays uncommented
            onBoundsChangeListeners.remove(callback)
        }
    }
}

private val Dimensions.isFullSpace: Boolean
    get() =
        width == Float.POSITIVE_INFINITY &&
            height == Float.POSITIVE_INFINITY &&
            depth == Float.POSITIVE_INFINITY
