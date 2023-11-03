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

import android.provider.Settings
import android.text.format.DateFormat
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

internal enum class ImageResources {
    CircularVignetteBottom,
    CircularVignetteTop,
    RectangularVignetteBottom,
    RectangularVignetteTop,
}

@Composable
internal fun imageResource(image: ImageResources): Painter =
    painterResource(
        when (image) {
            ImageResources.CircularVignetteBottom -> R.drawable.circular_vignette_bottom
            ImageResources.CircularVignetteTop -> R.drawable.circular_vignette_top
            ImageResources.RectangularVignetteBottom -> R.drawable.rectangular_vignette_bottom
            ImageResources.RectangularVignetteTop -> R.drawable.rectangular_vignette_top
        }
    )

@Composable
internal fun is24HourFormat(): Boolean = DateFormat.is24HourFormat(LocalContext.current)

internal fun currentTimeMillis(): Long = System.currentTimeMillis()

@Composable
internal fun isLeftyModeEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.USER_ROTATION,
            Surface.ROTATION_0
        ) == Surface.ROTATION_180
    }
}

@Composable
internal fun screenHeightDp() = LocalContext.current.resources.configuration.screenHeightDp

@Composable
internal fun screenWidthDp() = LocalContext.current.resources.configuration.screenWidthDp
