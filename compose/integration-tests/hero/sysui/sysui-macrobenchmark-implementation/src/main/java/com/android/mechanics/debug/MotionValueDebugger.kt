/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import com.android.mechanics.MotionValueState
import kotlinx.coroutines.DisposableHandle

/** Keeps track of MotionValues that are registered for debug-inspection. */
class MotionValueDebugController {
    private val observedMotionValues = mutableStateListOf<MotionValueState>()

    /**
     * Registers a [MotionValueState] to be debugged.
     *
     * Clients must call [DisposableHandle.dispose] when done.
     */
    fun register(motionValue: MotionValueState): DisposableHandle {
        observedMotionValues.add(motionValue)
        return DisposableHandle { observedMotionValues.remove(motionValue) }
    }

    /** The currently registered `MotionValues`. */
    val observed: List<MotionValueState>
        get() = observedMotionValues
}

/** Composition-local to provide a [MotionValueDebugController]. */
val LocalMotionValueDebugController = staticCompositionLocalOf<MotionValueDebugController?> { null }

/**
 * Provides a [MotionValueDebugController], to which [MotionValue]s within [content] can be
 * registered to.
 *
 * With [enableDebugger] set to `false` (or this composable not being in the composition in the
 * first place), downstream [debugMotionValue] and [DebugEffect] will be no-ops.
 */
@Composable
fun MotionValueDebuggerProvider(enableDebugger: Boolean = true, content: @Composable () -> Unit) {
    val debugger =
        remember(enableDebugger) { if (enableDebugger) MotionValueDebugController() else null }
    CompositionLocalProvider(LocalMotionValueDebugController provides debugger) { content() }
}

/** Registers the [motionValue] with the [LocalMotionValueDebugController], if available. */
fun Modifier.debugMotionValue(motionValue: MotionValueState): Modifier =
    this.then(DebugMotionValueElement(motionValue))

/** Registers the [motionValue] with the [LocalMotionValueDebugController], if available. */
@Composable
fun DebugEffect(motionValue: MotionValueState) {
    val debugger = LocalMotionValueDebugController.current
    if (debugger != null) {
        DisposableEffect(debugger, motionValue) {
            val handle = debugger.register(motionValue)
            onDispose { handle.dispose() }
        }
    }
}

/**
 * [DelegatableNode] to register the [motionValue] with the [LocalMotionValueDebugController], if
 * available.
 */
class DebugMotionValueNode(motionValue: MotionValueState) :
    Modifier.Node(), DelegatableNode, CompositionLocalConsumerModifierNode, ObserverModifierNode {
    private var debugger: MotionValueDebugController? = null

    internal var registration: DisposableHandle? = null

    override fun onAttach() {
        onObservedReadsChanged()
    }

    override fun onDetach() {
        debugger = null
        registration?.dispose()
        registration = null
    }

    override fun onObservedReadsChanged() {
        registration?.dispose()
        observeReads { debugger = currentValueOf(LocalMotionValueDebugController) }
        registration = debugger?.register(motionValue)
    }

    var motionValue = motionValue
        set(value) {
            registration = debugger?.register(value)
            field = value
        }
}

private data class DebugMotionValueElement(val motionValue: MotionValueState) :
    ModifierNodeElement<DebugMotionValueNode>() {
    override fun create(): DebugMotionValueNode = DebugMotionValueNode(motionValue)

    override fun InspectorInfo.inspectableProperties() {
        // Intentionally empty
    }

    override fun update(node: DebugMotionValueNode) {
        node.motionValue = motionValue
    }
}
