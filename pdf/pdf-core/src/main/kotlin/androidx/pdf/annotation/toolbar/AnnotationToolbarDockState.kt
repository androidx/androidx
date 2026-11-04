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

package androidx.pdf.annotation.toolbar

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.pdf.ExperimentalPdfApi

/**
 * Defines edge docking states for an annotation toolbar ([DOCK_STATE_START], [DOCK_STATE_BOTTOM],
 * or [DOCK_STATE_END]).
 */
@ExperimentalPdfApi
public object AnnotationToolbarDockState {
    /**
     * Represents a state where the toolbar is docked vertically along the start (left in LTR, right
     * in RTL) edge.
     */
    public const val DOCK_STATE_START: Int = 0

    /** Represents a state where the toolbar is docked horizontally along the bottom edge. */
    public const val DOCK_STATE_BOTTOM: Int = 1

    /**
     * Represents a state where the toolbar is docked vertically along the end (right in LTR, left
     * in RTL) edge.
     */
    public const val DOCK_STATE_END: Int = 2

    /** Denotes the docking edge of the toolbar within its parent container. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(DOCK_STATE_START, DOCK_STATE_BOTTOM, DOCK_STATE_END)
    public annotation class DockState
}
