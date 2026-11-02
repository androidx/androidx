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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.ComponentValue

/**
 * Identity token representing a specific layout component node in a RemoteCompose composition tree.
 *
 * Tracks which [ComponentValue] properties have been requested by expressions scoped to this
 * component so they can be emitted directly into the component's body during rendering.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComponentToken public constructor() {
    internal var requestedMask: Int = 0
        private set

    internal fun markRequested(propIndex: Int) {
        requestedMask = requestedMask or (1 shl propIndex)
    }

    internal fun isRequested(propIndex: Int): Boolean = (requestedMask and (1 shl propIndex)) != 0

    internal companion object {
        internal const val PROP_WIDTH: Int = 0
        internal const val PROP_HEIGHT: Int = 1
        internal const val PROP_X: Int = 2
        internal const val PROP_Y: Int = 3
        internal const val PROP_ROOT_X: Int = 4
        internal const val PROP_ROOT_Y: Int = 5
        internal const val PROP_COUNT: Int = 6

        internal fun toComponentValueType(propIndex: Int): Int =
            when (propIndex) {
                PROP_WIDTH -> ComponentValue.WIDTH
                PROP_HEIGHT -> ComponentValue.HEIGHT
                PROP_X -> ComponentValue.POS_X
                PROP_Y -> ComponentValue.POS_Y
                PROP_ROOT_X -> ComponentValue.POS_ROOT_X
                PROP_ROOT_Y -> ComponentValue.POS_ROOT_Y
                else -> throw IllegalArgumentException("Unknown property index: $propIndex")
            }
    }
}
