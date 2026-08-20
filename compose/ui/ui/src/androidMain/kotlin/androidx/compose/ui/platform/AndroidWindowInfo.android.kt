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

package androidx.compose.ui.platform

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.WindowInfoImpl.Companion.GlobalKeyboardModifiers
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toSize
import androidx.window.layout.WindowMetricsCalculator
import java.util.function.Consumer

/**
 * Whether cross-window blur is currently enabled by the system.
 *
 * Cross-window blur might not be supported by some devices due to GPU limitations. It can also be
 * disabled at runtime (e.g., during battery saving mode, when multimedia tunneling is used, or when
 * minimal post-processing is requested). In these situations, no blur is computed or drawn. Apps
 * should check this flag to fall back to alternative styling (such as adjusting scrim opacity or
 * changing themes) when blurs are disabled.
 *
 * This affects both window blur behind (see
 * [android.view.WindowManager.LayoutParams.setBlurBehindRadius]) and window background blur (see
 * [android.view.Window.setBackgroundBlurRadius]).
 *
 * On Android, this property queries [android.view.WindowManager.isCrossWindowBlurEnabled] on API
 * 31+; on older API levels it always returns `false`.
 */
public val WindowInfo.isCrossWindowBlurEnabled: Boolean
    get() = (this as? LazyWindowInfo)?.isCrossWindowBlurEnabled ?: false

/**
 * WindowInfo that only calculates [containerSize] if the property has been read, to avoid expensive
 * size calculation when no one is reading the value.
 */
internal class LazyWindowInfo(private val context: Context) : WindowInfo {
    private var onInitializeContainerSize: (() -> DerivedSize)? = null
    private var _containerSize: MutableState<DerivedSize>? = null

    override var isWindowFocused: Boolean by mutableStateOf(false)

    override var keyboardModifiers: PointerKeyboardModifiers
        get() = GlobalKeyboardModifiers.value
        set(value) {
            GlobalKeyboardModifiers.value = value
        }

    private val crossWindowBlurObserver = CrossWindowBlur(context)

    val isCrossWindowBlurEnabled: Boolean
        get() = crossWindowBlurObserver.isCrossWindowBlurEnabled

    fun observeCrossWindowBlurState() {
        crossWindowBlurObserver.onAttached()
    }

    fun stopObservingCrossWindowBlurState() {
        crossWindowBlurObserver.onDetached()
    }

    inline fun updateContainerSizeIfObserved(calculateContainerSize: () -> DerivedSize) {
        _containerSize?.let { it.value = calculateContainerSize() }
    }

    fun setOnInitializeContainerSize(onInitializeContainerSize: (() -> DerivedSize)?) {
        // If we have already initialized, no need to set a listener here
        if (_containerSize == null) {
            this.onInitializeContainerSize = onInitializeContainerSize
        }
    }

    override val containerSize: IntSize
        get() {
            if (_containerSize == null) {
                val initialSize = onInitializeContainerSize?.invoke() ?: DerivedSize.Zero
                _containerSize = mutableStateOf(initialSize)
                onInitializeContainerSize = null
            }
            return _containerSize!!.value.pxSize
        }

    override val containerDpSize: DpSize
        get() {
            if (_containerSize == null) {
                val initialSize = onInitializeContainerSize?.invoke() ?: DerivedSize.Zero
                _containerSize = mutableStateOf(initialSize)
                onInitializeContainerSize = null
            }
            return _containerSize!!.value.dpSize
        }
}

internal fun calculateWindowSize(view: View): DerivedSize {
    val context = view.context
    val unwrapped = tryUnwrapContext(context)
    return if (unwrapped != null) {
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(unwrapped)
        DerivedSize.fromPxSize(
            IntSize(metrics.bounds.width(), metrics.bounds.height()),
            Density(unwrapped),
        )
    } else {
        // Fallback behavior for views created with an unsupported context, try to get some value
        // instead of crashing
        val configuration = context.resources.configuration
        val density = Density(context)
        DerivedSize.fromDpSize(
            dpSize = DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp),
            density = density,
        )
    }
}

internal class DerivedSize(val pxSize: IntSize, val dpSize: DpSize) {
    companion object {
        val Zero = DerivedSize(IntSize.Zero, DpSize.Zero)

        fun fromPxSize(pxSize: IntSize, density: Density) =
            DerivedSize(pxSize, with(density) { pxSize.toSize().toDpSize() })

        fun fromDpSize(dpSize: DpSize, density: Density) =
            DerivedSize(with(density) { dpSize.toSize().toIntSize() }, dpSize)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DerivedSize) return false

        if (pxSize != other.pxSize) return false
        if (dpSize != other.dpSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pxSize.hashCode()
        result = 31 * result + dpSize.hashCode()
        return result
    }
}

/**
 * Return the base context from a context wrapper, or null if a supported context could not be
 * found. Forked from androidx.window.layout.util.ContextCompatHelper#unwrapContext to work around
 * b/449386176 and b/449389108
 */
private fun tryUnwrapContext(context: Context): Context? {
    var iterator = context

    while (iterator is ContextWrapper) {
        if (iterator is Activity) {
            // Activities are always ContextWrappers
            return iterator
        } else if (iterator is InputMethodService) {
            // InputMethodService are always ContextWrappers
            return iterator
        } else if (iterator is Application) {
            // Applications are always ContextWrappers
            return iterator
        } else if (iterator.baseContext == null) {
            return null
        }

        iterator = iterator.baseContext
    }

    return null
}

/**
 * Observes system-wide cross-window blur state.
 *
 * On API 31+, it registers a listener with the system [WindowManager] (using
 * [WindowManager.addCrossWindowBlurEnabledListener]) to receive updates whenever the system-wide
 * blur state changes (e.g., when the user enters Battery Saver).
 *
 * It is instantiated per [ComposeViewContext] (via [LazyWindowInfo]) to tie the listener's
 * lifecycle to the active views. This ensures we unregister the listener when all views are
 * detached, preventing memory leaks of the [Context] used to retrieve the [WindowManager].
 *
 * Note: On API levels below 31, blurs are always reported as disabled, and no listener is
 * registered.
 */
internal class CrossWindowBlur(private val context: Context) {
    // Tracks whether the hosting view is attached to a window. To prevent memory leaks,
    // we only register the WindowManager listener while the view is active (attached).
    private var isAttached = false
    // Tracks whether the blur property has been read in composition.
    // Used for laziness: we defer registering the system listener until the app actually
    // queries this state, saving resources for screens that don't use blurs.
    private var isObserved = false

    private var listenerRegistration: Api31Impl.Registration? = null

    /** Shows whether cross-window blurs are currently enabled by the system. */
    private var _isEnabled by mutableStateOf(false)

    /**
     * The system-wide cross-window blur state.
     *
     * Reading this property in composition subscribes the Composable to changes and lazily
     * registers the system listener on the first read.
     */
    val isCrossWindowBlurEnabled: Boolean
        get() {
            if (!isObserved) {
                isObserved = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isAttached) {
                    _isEnabled = Api31Impl.isCrossWindowBlurEnabled(context)
                }
                if (isAttached) {
                    registerSystemListener()
                }
            }
            return _isEnabled
        }

    /**
     * Called when the associated view context becomes active (attached to a window).
     *
     * If a Composable already read the blur property while the context was inactive, this will now
     * register the WindowManager listener.
     */
    fun onAttached() {
        isAttached = true
        if (isObserved) {
            registerSystemListener()
        }
    }

    /** Unregisters the listener on view detachment to prevent memory leaks. */
    fun onDetached() {
        isAttached = false
        unregisterSystemListener()
    }

    private fun registerSystemListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && listenerRegistration == null) {
            listenerRegistration =
                Api31Impl.registerListener(context) { enabled -> _isEnabled = enabled }
        }
    }

    private fun unregisterSystemListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listenerRegistration?.unregister()
        }
        listenerRegistration = null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private object Api31Impl {
        /**
         * Stores the [WindowManager] and [Consumer] callback instance to allow unregistering the
         * listener.
         */
        class Registration(private val wm: WindowManager, private val consumer: Consumer<Boolean>) {
            fun unregister() {
                wm.removeCrossWindowBlurEnabledListener(consumer)
            }
        }

        /** Queries the system-wide cross-window blur state. */
        @DoNotInline
        fun isCrossWindowBlurEnabled(context: Context): Boolean {
            val wm = context.getSystemService(WindowManager::class.java) ?: return false
            return wm.isCrossWindowBlurEnabled
        }

        /** Registers the blur listener using the provided [Context]. */
        @DoNotInline
        fun registerListener(context: Context, onBlurChanged: (Boolean) -> Unit): Registration? {
            val wm = context.getSystemService(WindowManager::class.java) ?: return null
            onBlurChanged(wm.isCrossWindowBlurEnabled)

            val consumer = Consumer(onBlurChanged)
            wm.addCrossWindowBlurEnabledListener(context.mainExecutor, consumer)
            return Registration(wm, consumer)
        }
    }
}
