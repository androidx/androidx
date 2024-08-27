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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
@Suppress("DEPRECATION") // WindowSizeClass#compute is deprecated
actual fun currentWindowAdaptiveInfo(): WindowAdaptiveInfo {
    val windowSize = with(LocalDensity.current) { currentWindowSize().toSize().toDpSize() }
    return WindowAdaptiveInfo(
        WindowSizeClass.compute(windowSize.width.value, windowSize.height.value),
        calculatePosture(collectFoldingFeaturesAsState().value)
    )
}

/**
 * Returns and automatically update the current window size from [WindowMetricsCalculator].
 *
 * @return an [IntSize] that represents the current window size.
 */
@Composable
fun currentWindowSize(): IntSize {
    // Observe view configuration changes and recalculate the size class on each change. We can't
    // use Activity#onConfigurationChanged as this will sometimes fail to be called on different
    // API levels, hence why this function needs to be @Composable so we can observe the
    // ComposeView's configuration changes.
    LocalConfiguration.current
    val windowBounds =
        WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(LocalContext.current)
            .bounds
    return IntSize(windowBounds.width(), windowBounds.height())
}

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
