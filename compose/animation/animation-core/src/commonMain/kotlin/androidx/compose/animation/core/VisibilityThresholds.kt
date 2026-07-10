/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.animation.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Default visibility threshold for [Dp]. Through our experiments with various threshold values,
 * this value is optimal for stopping the animation without incurring any visual discontinuities.
 */
private const val DpVisibilityThreshold = 0.4f

/**
 * Default visibility threshold for Px. Through our experiments with various threshold values, this
 * value is optimal for stopping the animation without incurring any visual discontinuities.
 * However, different screens have varying densities, so thresholds should be adjusted accordingly.
 */
private const val PxVisibilityThreshold = 1.0f

private val RectVisibilityThreshold =
    Rect(PxVisibilityThreshold, PxVisibilityThreshold, PxVisibilityThreshold, PxVisibilityThreshold)

/**
 * Visibility threshold for [IntOffset]. This defines the amount of value change that is considered
 * to be no longer visible. The animation system uses this to signal to some default [spring]
 * animations to stop when the value is close enough to the target.
 */
public val IntOffset.Companion.VisibilityThreshold: IntOffset
    get() = IntOffset(1, 1)

/**
 * Visibility threshold for [Offset]. This defines the amount of value change that is considered to
 * be no longer visible. The animation system uses this to signal to some default [spring]
 * animations to stop when the value is close enough to the target.
 */
public val Offset.Companion.VisibilityThreshold: Offset
    get() = Offset(PxVisibilityThreshold, PxVisibilityThreshold)

/**
 * Visibility threshold for [Int]. This defines the amount of value change that is considered to be
 * no longer visible. The animation system uses this to signal to some default [spring] animations
 * to stop when the value is close enough to the target.
 */
public val Int.Companion.VisibilityThreshold: Int
    get() = 1

/**
 * Visibility threshold for [Dp]. This defines the amount of value change that is considered to be
 * no longer visible. The animation system uses this to signal to some default [spring] animations
 * to stop when the value is close enough to the target.
 */
public val Dp.Companion.VisibilityThreshold: Dp
    get() = DpVisibilityThreshold.dp

/**
 * Visibility threshold for [DpOffset]. This defines the amount of value change that is considered
 * to be no longer visible. The animation system uses this to signal to some default [spring]
 * animations to stop when the value is close enough to the target.
 */
public val DpOffset.Companion.VisibilityThreshold: DpOffset
    get() = DpOffset(Dp.VisibilityThreshold, Dp.VisibilityThreshold)

/**
 * Visibility threshold for [Size]. This defines the amount of value change that is considered to be
 * no longer visible. The animation system uses this to signal to some default [spring] animations
 * to stop when the value is close enough to the target.
 */
public val Size.Companion.VisibilityThreshold: Size
    get() = Size(PxVisibilityThreshold, PxVisibilityThreshold)

/**
 * Visibility threshold for [IntSize]. This defines the amount of value change that is considered to
 * be no longer visible. The animation system uses this to signal to some default [spring]
 * animations to stop when the value is close enough to the target.
 */
public val IntSize.Companion.VisibilityThreshold: IntSize
    get() = IntSize(1, 1)

/**
 * Visibility threshold for [Rect]. This defines the amount of value change that is considered to be
 * no longer visible. The animation system uses this to signal to some default [spring] animations
 * to stop when the value is close enough to the target.
 */
public val Rect.Companion.VisibilityThreshold: Rect
    get() = RectVisibilityThreshold

// TODO: Add Dp.DefaultAnimation = spring<Dp>(visibilityThreshold = Dp.VisibilityThreshold)
// The floats coming out of this map are fed to APIs that expect objects (generics), so it's
// better to store them as boxed floats here instead of causing unboxing/boxing every time
// the values are read out and forwarded to other APIs
@Suppress("PrimitiveInCollection")
internal val VisibilityThresholdMap: Map<TwoWayConverter<*, *>, Float> =
    mapOf(
        Int.VectorConverter to 1f,
        IntSize.VectorConverter to 1f,
        IntOffset.VectorConverter to 1f,
        Float.VectorConverter to 0.01f,
        Rect.VectorConverter to PxVisibilityThreshold,
        Size.VectorConverter to PxVisibilityThreshold,
        Offset.VectorConverter to PxVisibilityThreshold,
        Dp.VectorConverter to DpVisibilityThreshold,
        DpOffset.VectorConverter to DpVisibilityThreshold,
    )
