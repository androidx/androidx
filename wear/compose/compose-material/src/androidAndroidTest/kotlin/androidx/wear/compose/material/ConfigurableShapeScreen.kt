/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Helper function which changes [Configuration.isScreenRound] result by changing
 * [Configuration.screenLayout] field and allows to mock square and round behaviour
 * @param isRound mocks shape either to round or not round (square) form
 */
@Composable
fun ConfiguredShapeScreen(
    isRound: Boolean,
    content: @Composable () -> Unit
) {
    val newConfiguration = Configuration(LocalConfiguration.current)
    newConfiguration.screenLayout = newConfiguration.screenLayout and
        Configuration.SCREENLAYOUT_ROUND_MASK.inv() or
        if (isRound) {
            Configuration.SCREENLAYOUT_ROUND_YES
        } else {
            Configuration.SCREENLAYOUT_ROUND_NO
        }

    CompositionLocalProvider(
        LocalConfiguration provides newConfiguration,
        content = content
    )
}
