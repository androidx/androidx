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

package androidx.wear.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowInsets
import androidx.core.graphics.Insets as AndroidXInsets
import androidx.core.view.WindowInsetsCompat

internal const val STATUS_BAR_TOP_INSET_PX = 48

/**
 * Executes [content] with simulated system status bar insets.
 *
 * In tests running on headless emulators or test environments without window decor, platform insets
 * may default to 0px. This helper ensures a realistic, deterministic top status bar inset (default
 * [STATUS_BAR_TOP_INSET_PX] = 48px) is available to Compose window insets, allowing padding and
 * clearance assertions to distinguish between active and suppressed states.
 */
@Composable
internal fun WithMockStatusBarInsets(
    topInsetsPx: Int = STATUS_BAR_TOP_INSET_PX,
    content: @Composable () -> Unit,
) {
    val insets =
        WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.statusBars(),
                AndroidXInsets.of(0, topInsetsPx, 0, 0),
            )
            .setInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.statusBars(),
                AndroidXInsets.of(0, topInsetsPx, 0, 0),
            )
            .build()
    DeviceConfigurationOverride(DeviceConfigurationOverride.WindowInsets(insets)) {
        content()
    }
}
