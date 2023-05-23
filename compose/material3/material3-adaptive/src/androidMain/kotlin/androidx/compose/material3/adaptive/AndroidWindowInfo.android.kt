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

import android.content.Context
import androidx.annotation.UiContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.window.layout.WindowMetricsCalculator

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
