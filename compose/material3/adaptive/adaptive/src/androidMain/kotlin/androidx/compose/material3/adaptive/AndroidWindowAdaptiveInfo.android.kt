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

package androidx.compose.material3.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.map

@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Moved to common source set, maintained for binary compatibility.",
)
@Composable
fun currentWindowAdaptiveInfo(): WindowAdaptiveInfo = currentWindowAdaptiveInfo(false)

/**
 * Returns and automatically update the current window size in [DpSize].
 *
 * @return an [DpSize] that represents the current window size.
 */
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Moved to common source set, maintained for binary compatibility.",
)
@JvmName("currentWindowDpSize")
@ExperimentalMaterial3AdaptiveApi
@Composable
fun currentWindowDpSizeDeprecated(): DpSize =
    with(LocalDensity.current) { currentWindowSize().toSize().toDpSize() }

/**
 * Returns and automatically update the current window size. It's a convenient function of getting
 * [androidx.compose.ui.platform.WindowInfo.containerSize] from [LocalWindowInfo].
 *
 * @return an [IntSize] that represents the current window size.
 */
@Deprecated(
    level = DeprecationLevel.HIDDEN,
    message = "Moved to common source set, maintained for binary compatibility.",
)
@JvmName("currentWindowSize")
@Composable
fun currentWindowSizeDeprecated(): IntSize = LocalWindowInfo.current.containerSize

/**
 * Collects the current window folding features from [WindowInfoTracker] in to a [State].
 *
 * @return a [State] of a [FoldingFeature] list.
 */
@Composable
fun collectFoldingFeaturesAsState(): State<List<FoldingFeature>> {
    val context = LocalContext.current
    return remember(context) {
            WindowInfoTracker.getOrCreate(context).windowLayoutInfo(context).map {
                it.displayFeatures.filterIsInstance<FoldingFeature>()
            }
        }
        .collectAsState(emptyList())
}
