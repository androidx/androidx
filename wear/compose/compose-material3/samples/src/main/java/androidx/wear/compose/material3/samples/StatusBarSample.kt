/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.suppressStatusBar

@Sampled
@Composable
fun ComponentSuppressStatusBarSample() {
    // Suppress the status bar to avoid overlapping with the full-screen circular progress
    // indicator.
    Box(
        modifier =
            Modifier.suppressStatusBar()
                .background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        CircularProgressIndicator(progress = { 0.75f }, startAngle = 120f, endAngle = 60f)
    }
}

@Sampled
@Composable
fun ScreenScaffoldSuppressStatusBarSample() {
    // Suppress the status bar for a screen and collapse its insets to 0.dp.
    ScreenScaffold(modifier = Modifier.suppressStatusBar()) { contentPadding ->
        Box(
            modifier =
                Modifier.background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
        ) {
            CircularProgressIndicator(progress = { 0.75f }, startAngle = 120f, endAngle = 60f)
        }
    }
}

@Sampled
@Composable
fun AppScaffoldSuppressStatusBarSample() {
    // Suppress the status bar for the entire application and collapse insets on all child screens.
    // Note: App-level suppression is not recommended for most apps because users will lose access
    // to the time and important status indicators on every screen.
    AppScaffold(modifier = Modifier.suppressStatusBar()) {
        ScreenScaffold { contentPadding ->
            Box(
                modifier =
                    Modifier.background(MaterialTheme.colorScheme.background)
                        .fillMaxSize()
                        .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("App-level suppression")
            }
        }
    }
}
