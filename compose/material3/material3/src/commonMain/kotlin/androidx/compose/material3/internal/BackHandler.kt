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

package androidx.compose.material3.internal

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

// TODO(b/352352908): Remove once this API will be available in common
internal expect class BackEventCompat {
    val touchX: Float
    val touchY: Float
    val progress: Float
    val swipeEdge: Int

    companion object {
        val EDGE_LEFT: Int
        val EDGE_RIGHT: Int
    }
}

// TODO(b/352352908): Remove once this API will be available in common
@Composable internal expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)

// TODO(b/352352908): Remove once this API will be available in common
@Composable
internal expect fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit
)

private val PredictiveBackEasing: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

internal object PredictiveBack {
    internal fun transform(progress: Float) = PredictiveBackEasing.transform(progress)
}
