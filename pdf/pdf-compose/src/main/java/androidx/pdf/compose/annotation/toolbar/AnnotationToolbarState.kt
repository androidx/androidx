/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.compose.annotation.toolbar

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.view.annotation.AnnotationToolbarView

/**
 * A state object that can be hoisted to control and observe an [AnnotationToolbar].
 *
 * Use [rememberAnnotationToolbarState] to create and remember an instance.
 */
@OptIn(ExperimentalPdfApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public class AnnotationToolbarState {
    private var _toolbarView by mutableStateOf<AnnotationToolbarView?>(null)

    internal var toolbarView: AnnotationToolbarView?
        get() = _toolbarView
        set(value) {
            if (_toolbarView === value) return
            // Push buffered state to the newly attached view before updating the reference.
            value?.let { view -> syncStateToView(view) }
            _toolbarView = value
        }

    /**
     * `null` until set by the caller or adopted from the attached [AnnotationToolbarView], whose
     * default depends on the form factor (end edge on tablets, bottom edge on phones).
     */
    internal val _dockState = mutableStateOf<AnnotationToolbarDockState?>(null)
    internal val _isConfigPopupVisible = mutableStateOf(false)
    internal val _isExpanded = mutableStateOf(true)

    // Buffers the value when detached; delegates directly to the view when attached.
    /** Controls the enabled state of the built-in undo button. */
    @get:JvmName("canUndo")
    public var canUndo: Boolean = false
        get() = _toolbarView?.canUndo ?: field
        set(value) {
            field = value
            _toolbarView?.canUndo = value
        }

    // Buffers the value when detached; delegates directly to the view when attached.
    /** Controls the enabled state of the built-in redo button. */
    @get:JvmName("canRedo")
    public var canRedo: Boolean = false
        get() = _toolbarView?.canRedo ?: field
        set(value) {
            field = value
            _toolbarView?.canRedo = value
        }

    /**
     * Controls whether the toolbar is expanded to show interactive tools or collapsed to a single
     * tool size.
     */
    public var isExpanded: Boolean
        get() = _isExpanded.value
        set(value) {
            _isExpanded.value = value
            if (value) {
                _toolbarView?.expandToolbar()
            } else {
                _toolbarView?.collapseToolbar()
            }
        }

    /**
     * Returns `true` if any of the configuration popups (such as the color palette or brush size
     * slider) are currently visible.
     */
    public val isConfigPopupVisible: Boolean
        get() = _isConfigPopupVisible.value

    /**
     * The current edge dock state of the toolbar ([AnnotationToolbarDockState.Start],
     * [AnnotationToolbarDockState.Bottom], or [AnnotationToolbarDockState.End]).
     */
    public var dockState: AnnotationToolbarDockState
        get() = _dockState.value ?: AnnotationToolbarDockState.Bottom
        set(value) {
            _dockState.value = value
            _toolbarView?.dockState = value.toViewDockState()
        }

    /** Dismisses any currently visible popups (such as the color palette or brush size slider). */
    public fun dismissPopups() {
        _isConfigPopupVisible.value = false
        _toolbarView?.dismissPopups()
    }

    /** Clears any tool selection on the toolbar. */
    public fun clearToolSelection() {
        _toolbarView?.clearToolSelection()
    }

    /** Resets the toolbar to its initial default configuration and tool selection state. */
    public fun reset() {
        canUndo = false
        canRedo = false
        _isExpanded.value = true
        _isConfigPopupVisible.value = false
        _toolbarView?.reset()
        _dockState.value = _toolbarView?.let {
            AnnotationToolbarDockState.fromViewDockState(it.dockState)
        }
    }

    private fun syncStateToView(view: AnnotationToolbarView) {
        view.canUndo = canUndo
        view.canRedo = canRedo
        val dockState = _dockState.value
        if (dockState == null) {
            // Adopt the view's form-factor default (End on tablets, Bottom on phones) if no
            // explicit dock state was set or restored.
            _dockState.value = AnnotationToolbarDockState.fromViewDockState(view.dockState)
        } else {
            // Apply the explicitly set or restored dock state over the view's default.
            view.dockState = dockState.toViewDockState()
        }
        // Only toggle expansion when differing to avoid re-triggering view bounds transitions.
        if (view.isExpanded != _isExpanded.value) {
            if (_isExpanded.value) {
                view.expandToolbar()
            } else {
                view.collapseToolbar()
            }
        }
        _isConfigPopupVisible.value = view.isConfigPopupVisible
    }

    internal companion object {
        private const val KEY_CAN_UNDO = "canUndo"
        private const val KEY_CAN_REDO = "canRedo"
        private const val KEY_DOCK_STATE = "dockState"
        private const val KEY_IS_EXPANDED = "isExpanded"

        /** The default [Saver] implementation for [AnnotationToolbarState]. */
        internal val Saver: Saver<AnnotationToolbarState, *> =
            mapSaver(
                save = { state ->
                    mapOf(
                        KEY_CAN_UNDO to state.canUndo,
                        KEY_CAN_REDO to state.canRedo,
                        KEY_DOCK_STATE to state._dockState.value?.toViewDockState(),
                        KEY_IS_EXPANDED to state.isExpanded,
                    )
                },
                restore = { map ->
                    AnnotationToolbarState().apply {
                        canUndo = map[KEY_CAN_UNDO] as? Boolean ?: false
                        canRedo = map[KEY_CAN_REDO] as? Boolean ?: false
                        (map[KEY_DOCK_STATE] as? Int)?.let {
                            dockState = AnnotationToolbarDockState.fromViewDockState(it)
                        }
                        isExpanded = map[KEY_IS_EXPANDED] as? Boolean ?: true
                    }
                },
            )
    }
}

/**
 * Creates and remembers an [AnnotationToolbarState] instance across recompositions and
 * configuration changes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberAnnotationToolbarState(): AnnotationToolbarState {
    return rememberSaveable(saver = AnnotationToolbarState.Saver) { AnnotationToolbarState() }
}
