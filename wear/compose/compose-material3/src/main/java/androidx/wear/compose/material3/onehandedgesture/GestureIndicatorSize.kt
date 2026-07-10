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

package androidx.wear.compose.material3.onehandedgesture

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Represents the size of the gesture indicator icon.
 *
 * Provides standard size definitions to ensure visual consistency across different component sizes
 * (e.g., standard vs. compact buttons).
 */
@JvmInline
public value class GestureIndicatorSize internal constructor(internal val size: Dp) {
    public companion object {
        /**
         * The recommended default size for indicators when used inside a content of size 48dp or
         * greater
         */
        public val Medium: GestureIndicatorSize = GestureIndicatorSize(36.dp)

        /** The recommended size for indicators when used inside a content of size less than 48dp */
        public val Small: GestureIndicatorSize = GestureIndicatorSize(28.dp)
    }
}
