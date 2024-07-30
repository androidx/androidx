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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect

/**
 * Creates a new instance of [androidx.core.graphics.Insets] with the same bounds specified in the
 * given [IntRect].
 */
fun IntRect.toAndroidXInsets(): androidx.core.graphics.Insets =
    androidx.core.graphics.Insets.of(left, top, right, bottom)

/**
 * Creates a new instance of [IntRect] with the same bounds specified in the given
 * [androidx.core.graphics.Insets].
 */
fun androidx.core.graphics.Insets.toComposeIntRect(): IntRect = IntRect(left, top, right, bottom)

/**
 * Converts the [DpRect] to [androidx.core.graphics.Insets] by using the [LocalDensity] and rounding
 * to the nearest pixel values in each dimension.
 */
@Composable
fun DpRect.roundToAndroidXInsets(): androidx.core.graphics.Insets =
    with(LocalDensity.current) { toRect() }.roundToIntRect().toAndroidXInsets()
