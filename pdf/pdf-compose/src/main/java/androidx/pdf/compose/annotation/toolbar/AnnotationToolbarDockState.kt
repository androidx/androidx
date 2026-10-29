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
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.view.annotation.AnnotationToolbarView.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.view.annotation.AnnotationToolbarView.Companion.DOCK_STATE_END
import androidx.pdf.view.annotation.AnnotationToolbarView.Companion.DOCK_STATE_START

/**
 * Edge docking alignment for an [AnnotationToolbar].
 *
 * Positions the toolbar along a screen edge ([Start], [Bottom], or [End]). Can be set
 * programmatically via [AnnotationToolbarState.dockState] or updated interactively when dragged
 * inside a coordinator layout (such as `AnnotationToolbarCoordinator`).
 */
@OptIn(ExperimentalPdfApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class AnnotationToolbarDockState private constructor(internal val value: Int) {
    public companion object {
        /** Docks the toolbar vertically along the start edge (left in LTR, right in RTL). */
        public val Start: AnnotationToolbarDockState = AnnotationToolbarDockState(DOCK_STATE_START)

        /** Docks the toolbar horizontally along the bottom edge. */
        public val Bottom: AnnotationToolbarDockState =
            AnnotationToolbarDockState(DOCK_STATE_BOTTOM)

        /** Docks the toolbar vertically along the end edge (right in LTR, left in RTL). */
        public val End: AnnotationToolbarDockState = AnnotationToolbarDockState(DOCK_STATE_END)

        internal fun fromViewDockState(viewDockState: Int): AnnotationToolbarDockState =
            when (viewDockState) {
                DOCK_STATE_START -> Start
                DOCK_STATE_END -> End
                else -> Bottom
            }
    }

    internal fun toViewDockState(): Int = value
}
