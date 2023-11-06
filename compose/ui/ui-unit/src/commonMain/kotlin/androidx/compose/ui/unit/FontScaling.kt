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

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.internal.JvmDefaultWithCompatibility

/**
 * Flag indicating if [Density] will use non-linear font scaling for Compose.
 *
 * Set this flag to true to keep the old linear font scaling behavior. Note that this only affects
 * Compose. Views always use non-linear font scaling in Android 14 and later.
 *
 * <b>This flag will be removed in Compose 1.6.0-beta01.</b> If you encounter any issues with the
 * new behavior, please file an issue at: issuetracker.google.com/issues/new?component=779818
 */
// TODO(b/300538470): Remove flag before beta
@Suppress("GetterSetterNames", "OPT_IN_MARKER_ON_WRONG_TARGET")
@get:Suppress("GetterSetterNames")
@set:ExperimentalComposeUiApi
@get:ExperimentalComposeUiApi
@ExperimentalComposeUiApi
var DisableNonLinearFontScalingInCompose by mutableStateOf(false)

/**
 * Converts [TextUnit] to [Dp] and vice-versa.
 *
 * If you are implementing this interface yourself on Android, please check the docs for important
 * optimization tips about caching.
 */
@Immutable
@JvmDefaultWithCompatibility
expect interface FontScaling {
    /**
     * Current user preference for the scaling factor for fonts.
     */
    @Stable
    val fontScale: Float

    /**
     * Convert [Dp] to Sp. Sp is used for font size, etc.
     */
    @Stable
    open fun Dp.toSp(): TextUnit

    /**
     * Convert Sp to [Dp].
     * @throws IllegalStateException if TextUnit other than SP unit is specified.
     */
    @Stable
    open fun TextUnit.toDp(): Dp
}

/**
 * Converts [TextUnit] to [Dp] and vice-versa, using a linear conversion.
 *
 * This will be the default for most platforms except Android.
 */
@Immutable
@JvmDefaultWithCompatibility
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface FontScalingLinear {
    /**
     * Current user preference for the scaling factor for fonts.
     */
    @Stable
    val fontScale: Float

    /**
     * Convert [Dp] to Sp. Sp is used for font size, etc.
     */
    @Stable
    fun Dp.toSp(): TextUnit = (value / fontScale).sp

    /**
     * Convert Sp to [Dp].
     * @throws IllegalStateException if TextUnit other than SP unit is specified.
     */
    @Stable
    fun TextUnit.toDp(): Dp {
        check(type == TextUnitType.Sp) { "Only Sp can convert to Px" }
        return Dp(value * fontScale)
    }
}
