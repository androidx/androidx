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

package androidx.compose.foundation.text.input.internal

import android.os.Build
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnection.CURSOR_UPDATE_FILTER_CHARACTER_BOUNDS
import android.view.inputmethod.InputConnection.CURSOR_UPDATE_FILTER_EDITOR_BOUNDS
import android.view.inputmethod.InputConnection.CURSOR_UPDATE_FILTER_INSERTION_MARKER
import android.view.inputmethod.InputConnection.CURSOR_UPDATE_FILTER_VISIBLE_LINE_BOUNDS
import androidx.compose.foundation.text.selection.visibleBounds
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.setFrom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

internal class CursorAnchorInfoController(
    private val textFieldState: TransformedTextFieldState,
    private val textLayoutState: TextLayoutState,
    private val composeImm: ComposeInputMethodManager,
    private val monitorScope: CoroutineScope,
) {
    private var monitorEnabled = false
    private var hasPendingImmediateRequest = false
    private var monitorJob: Job? = null

    private var includeInsertionMarker = false
    private var includeCharacterBounds = false
    private var includeEditorBounds = false
    private var includeLineBounds = false

    private val builder = CursorAnchorInfo.Builder()
    private val matrix = Matrix()
    private val androidMatrix = android.graphics.Matrix()

    /** Requests [CursorAnchorInfo] updates to be provided to the [ComposeInputMethodManager]. */
    fun requestUpdates(cursorUpdateMode: Int) {
        val immediate = cursorUpdateMode and InputConnection.CURSOR_UPDATE_IMMEDIATE != 0
        val monitor = cursorUpdateMode and InputConnection.CURSOR_UPDATE_MONITOR != 0

        // Before Android T, filter flags are not used, and insertion marker and character bounds
        // info are always included.
        var includeInsertionMarker = true
        var includeCharacterBounds = true
        var includeEditorBounds = false
        var includeLineBounds = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            includeInsertionMarker = cursorUpdateMode and CURSOR_UPDATE_FILTER_INSERTION_MARKER != 0
            includeCharacterBounds = cursorUpdateMode and CURSOR_UPDATE_FILTER_CHARACTER_BOUNDS != 0
            includeEditorBounds = cursorUpdateMode and CURSOR_UPDATE_FILTER_EDITOR_BOUNDS != 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                includeLineBounds =
                    cursorUpdateMode and CURSOR_UPDATE_FILTER_VISIBLE_LINE_BOUNDS != 0
            }
            // If no filter flags are used, then all info should be included.
            if (
                !includeInsertionMarker &&
                    !includeCharacterBounds &&
                    !includeEditorBounds &&
                    !includeLineBounds
            ) {
                includeInsertionMarker = true
                includeCharacterBounds = true
                includeEditorBounds = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    includeLineBounds = true
                }
            }
        }

        requestUpdates(
            immediate = immediate,
            monitor = monitor,
            includeInsertionMarker = includeInsertionMarker,
            includeCharacterBounds = includeCharacterBounds,
            includeEditorBounds = includeEditorBounds,
            includeLineBounds = includeLineBounds
        )
    }

    /**
     * Requests [CursorAnchorInfo] updates to be provided to the [ComposeInputMethodManager].
     *
     * Combinations of [immediate] and [monitor] are used to specify when to provide updates. If
     * these are both false, then no further updates will be provided.
     *
     * @param immediate whether to update with the current [CursorAnchorInfo] immediately, or as
     *   soon as available
     * @param monitor whether to provide [CursorAnchorInfo] updates for all future layout or
     *   position changes
     * @param includeInsertionMarker whether to include insertion marker (i.e. cursor) location
     *   information
     * @param includeCharacterBounds whether to include character bounds information for the
     *   composition range
     * @param includeEditorBounds whether to include editor bounds information
     * @param includeLineBounds whether to include line bounds information
     */
    private fun requestUpdates(
        immediate: Boolean,
        monitor: Boolean,
        includeInsertionMarker: Boolean,
        includeCharacterBounds: Boolean,
        includeEditorBounds: Boolean,
        includeLineBounds: Boolean
    ) {
        this.includeInsertionMarker = includeInsertionMarker
        this.includeCharacterBounds = includeCharacterBounds
        this.includeEditorBounds = includeEditorBounds
        this.includeLineBounds = includeLineBounds

        if (immediate) {
            hasPendingImmediateRequest = true
            calculateCursorAnchorInfo()?.let(composeImm::updateCursorAnchorInfo)
        }
        monitorEnabled = monitor
        startOrStopMonitoring()
    }

    /**
     * If [monitorEnabled] is rue, observes changes to [textLayoutState] and monitor state (from
     * [requestUpdates]) and sends updates to the [composeImm] as required until cancelled.
     * Otherwise, cancels any monitor [Job].
     */
    private fun startOrStopMonitoring() {
        if (monitorEnabled) {
            if (monitorJob?.isActive != true) {
                monitorJob =
                    monitorScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        // TODO (b/291327369) Confirm that we are sending updates at the right time.
                        snapshotFlow { calculateCursorAnchorInfo() }
                            .drop(1)
                            .filterNotNull()
                            .collect { composeImm.updateCursorAnchorInfo(it) }
                    }
            }
        } else {
            monitorJob?.cancel()
            monitorJob = null
        }
    }

    private fun calculateCursorAnchorInfo(): CursorAnchorInfo? {
        // State reads
        val textLayoutCoordinates =
            textLayoutState.textLayoutNodeCoordinates?.takeIf { it.isAttached } ?: return null
        val coreCoordinates =
            textLayoutState.coreNodeCoordinates?.takeIf { it.isAttached } ?: return null
        val decorationBoxCoordinates =
            textLayoutState.decoratorNodeCoordinates?.takeIf { it.isAttached } ?: return null
        val textLayoutResult = textLayoutState.layoutResult ?: return null
        val text = textFieldState.visualText

        // Updates matrix to transform text layout coordinates to screen coordinates.
        matrix.reset()
        textLayoutCoordinates.transformToScreen(matrix)
        androidMatrix.setFrom(matrix)

        val innerTextFieldBounds =
            coreCoordinates
                .visibleBounds()
                .translate(textLayoutCoordinates.localPositionOf(coreCoordinates, Offset.Zero))
        val decorationBoxBounds =
            decorationBoxCoordinates
                .visibleBounds()
                .translate(
                    textLayoutCoordinates.localPositionOf(decorationBoxCoordinates, Offset.Zero)
                )
        return builder.build(
            text,
            text.selection,
            text.composition,
            textLayoutResult,
            androidMatrix,
            innerTextFieldBounds,
            decorationBoxBounds,
            includeInsertionMarker,
            includeCharacterBounds,
            includeEditorBounds,
            includeLineBounds
        )
    }
}
