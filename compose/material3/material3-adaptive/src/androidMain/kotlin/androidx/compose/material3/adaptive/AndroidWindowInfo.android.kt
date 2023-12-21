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

import android.app.Activity
import android.content.Context
import androidx.annotation.UiContext
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.flow.map

/**
 * Calculates and returns [WindowAdaptiveInfo] of the provided context. It's a convenient function
 * that uses the Material default [WindowSizeClass.calculateFromSize] and [calculatePosture]
 * functions to retrieve [WindowSizeClass] and [Posture].
 *
 * @param context Optional [UiContext] of the window, defaulted to [LocalContext]'s current value
 * @return [WindowAdaptiveInfo] of the provided context
 */
@ExperimentalMaterial3AdaptiveApi
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun calculateWindowAdaptiveInfo(
    @UiContext context: Context = LocalContext.current
): WindowAdaptiveInfo {
    return WindowAdaptiveInfo(
        WindowSizeClass.calculateFromSize(
            windowSizeAsState(context).value.toSize(),
            LocalDensity.current
        ),
        calculatePosture(foldingFeaturesAsState(context).value)
    )
}

/**
 * Collects the current window size from [WindowMetricsCalculator] in to a [State].
 *
 * @param context Optional [UiContext] of the window, defaulted to [LocalContext]'s current value.
 * @return a [State] of [IntSize] that represents the current window size.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun windowSizeAsState(@UiContext context: Context = LocalContext.current): State<IntSize> {
    val size = remember {
        mutableStateOf(IntSize(0, 0))
    }

    // Observe view configuration changes and recalculate the size class on each change. We can't
    // use Activity#onConfigurationChanged as this will sometimes fail to be called on different
    // API levels, hence why this function needs to be @Composable so we can observe the
    // ComposeView's configuration changes.
    size.value = remember(context, LocalConfiguration.current) {
        val windowBounds =
            WindowMetricsCalculator
                .getOrCreate()
                .computeCurrentWindowMetrics(context)
                .bounds
        IntSize(windowBounds.width(), windowBounds.height())
    }

    return size
}

/**
 * Collects the current window folding features from [WindowInfoTracker] in to a [State].
 *
 * @param context Optional [UiContext] of the window, defaulted to [LocalContext]'s current value.
 * @return a [State] of a [FoldingFeature] list.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun foldingFeaturesAsState(
    @UiContext context: Context = LocalContext.current
): State<List<FoldingFeature>> {
    return remember(context) {
        if (context is Activity) {
            // TODO(b/284347941) remove the instance check after the test bug is fixed.
            WindowInfoTracker
                .getOrCreate(context)
                .windowLayoutInfo(context)
        } else {
            WindowInfoTracker
                .getOrCreate(context)
                .windowLayoutInfo(context)
        }.map { it.displayFeatures.filterIsInstance<FoldingFeature>() }
    }.collectAsState(emptyList())
}
