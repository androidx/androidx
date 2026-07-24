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

package androidx.compose.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates

/**
 * Modifier.onFocusedBoundsChanged has been deprecated, and its implementation has been removed.
 * This modifier now no-ops. To retrieve the position of a focused node, use
 * [androidx.compose.ui.focus.getFocusedRect] to query this information as needed.
 */
@Suppress("unused")
@Deprecated(
    message =
        "onFocusedBoundsChanged doesn't reliably observe focus bounds changes through layout " +
            "coordinate changes and focus changes. The existing best-effort " +
            "implementation has been removed, resulting in this becoming a no-op Modifier where " +
            "onPositioned will never be called. Use FocusTargetModifierNode.getFocusedRect() " +
            "instead to query this information on demand as needed.",
    level = DeprecationLevel.ERROR,
)
public fun Modifier.onFocusedBoundsChanged(onPositioned: (LayoutCoordinates?) -> Unit): Modifier =
    this
