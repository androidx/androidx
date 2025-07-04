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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo

/**
 * ResizeEvent for XR Runtime Platform.
 *
 * @param resizeState the current state of the resize action.
 * @param newSize proposed (width, height, depth) size in meters. The resize event listener must use
 *   this proposed size to resize the content.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ResizeEvent(@ResizeState public val resizeState: Int, public val newSize: Dimensions) {

    /** States of the Resize action. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public annotation class ResizeState {
        public companion object {
            public const val RESIZE_STATE_UNKNOWN: Int = 0
            public const val RESIZE_STATE_START: Int = 1
            public const val RESIZE_STATE_ONGOING: Int = 2
            public const val RESIZE_STATE_END: Int = 3
        }
    }

    public companion object {
        public const val RESIZE_STATE_UNKNOWN: Int = 0
        public const val RESIZE_STATE_START: Int = 1
        public const val RESIZE_STATE_ONGOING: Int = 2
        public const val RESIZE_STATE_END: Int = 3
    }
}
