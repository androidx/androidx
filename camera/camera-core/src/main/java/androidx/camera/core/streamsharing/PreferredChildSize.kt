/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core.streamsharing

import android.graphics.Rect
import android.util.Size

/** Data class representing the preferred size information for a child. */
internal data class PreferredChildSize(
    /** The cropping rectangle to apply before scaling. */
    val cropRectBeforeScaling: Rect,

    /** The size of the child after scaling. */
    val childSizeToScale: Size,

    /**
     * The original selected size from the child's preferred sizes before any scaling, cropping, or
     * rotating.
     */
    val originalSelectedChildSize: Size,
)
