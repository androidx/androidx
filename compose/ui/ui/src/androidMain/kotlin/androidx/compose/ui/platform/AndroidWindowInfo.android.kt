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
import android.view.View
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

/**
 * WindowInfo that only calculates [containerSize] if the property has been read, to avoid expensive
 * size calculation when no one is reading the value.
 */
internal class LazyWindowInfo : WindowInfo {
    private var onInitializeContainerSize: (() -> DerivedSize)? = null
    private var _containerSize: MutableState<DerivedSize>? = null

    override var isWindowFocused: Boolean by mutableStateOf(false)

    override var keyboardModifiers: PointerKeyboardModifiers
        get() = GlobalKeyboardModifiers.value
        set(value) {
            GlobalKeyboardModifiers.value = value
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
