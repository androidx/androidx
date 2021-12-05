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

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

@Composable
internal actual fun isRoundDevice(): Boolean = LocalConfiguration.current.isScreenRound

@Composable
internal actual fun imageResource(image: ImageResources): Painter =
    painterResource(
        when (image) {
            ImageResources.CircularVignetteBottom -> R.drawable.circular_vignette_bottom
            ImageResources.CircularVignetteTop -> R.drawable.circular_vignette_top
            ImageResources.RectangularVignetteBottom -> R.drawable.rectangular_vignette_bottom
            ImageResources.RectangularVignetteTop -> R.drawable.rectangular_vignette_top
        }
    )

@Composable
internal actual fun is24HourFormat(): Boolean = DateFormat.is24HourFormat(LocalContext.current)

internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()