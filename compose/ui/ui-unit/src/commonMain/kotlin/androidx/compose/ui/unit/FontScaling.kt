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

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.internal.JvmDefaultWithCompatibility

/**
 * Converts [TextUnit] to [Dp] and vice-versa.
 *
 * If you are implementing this interface yourself on Android, please check the docs for important
 * optimization tips about caching.
 */
@Immutable
@JvmDefaultWithCompatibility
public expect interface FontScaling {
    /** Current user preference for the scaling factor for fonts. */
    @Stable public val fontScale: Float

    /** Convert [Dp] to Sp. Sp is used for font size, etc. */
    @Stable public open fun Dp.toSp(): TextUnit

    /**
     * Convert Sp to [Dp].
     *
     * @throws IllegalStateException if TextUnit other than SP unit is specified.
     */
    @Stable public open fun TextUnit.toDp(): Dp
}
