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

package androidx.wear.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * Creates a lambda that always animates to the latest value. This can be useful for avoiding
 * snapping behavior when the value changes, and ensuring the component always animates when the
 * given progress value changes.
 *
 * This method has no side-effects causing recomposition when the value changes, but the underlying
 * value should always come from a State instance, otherwise it will not get animated.
 *
 * @param progress Value to animate to
 * @param animationSpec Animation specification to use
 */
@Composable
internal fun rememberAnimatedStateOf(
    progress: () -> Float,
    animationSpec: AnimationSpec<Float>
): Animatable<Float, AnimationVector1D> {
    val animatable = remember { Animatable(progress()) }
    LaunchedEffect(Unit) {
        snapshotFlow(progress).collectLatest { animatable.animateTo(it, animationSpec) }
    }
    return animatable
}
