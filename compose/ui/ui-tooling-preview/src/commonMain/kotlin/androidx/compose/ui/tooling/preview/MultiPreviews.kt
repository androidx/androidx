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

package androidx.compose.ui.tooling.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices.DESKTOP
import androidx.compose.ui.tooling.preview.Devices.FOLDABLE
import androidx.compose.ui.tooling.preview.Devices.PHONE
import androidx.compose.ui.tooling.preview.Devices.TABLET
import androidx.compose.ui.tooling.preview.UiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.UiModes.UI_MODE_TYPE_NORMAL
import androidx.compose.ui.tooling.preview.Wallpapers.BLUE_DOMINATED_EXAMPLE
import androidx.compose.ui.tooling.preview.Wallpapers.GREEN_DOMINATED_EXAMPLE
import androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE
import androidx.compose.ui.tooling.preview.Wallpapers.YELLOW_DOMINATED_EXAMPLE

/**
 * A MultiPreview annotation for displaying a @[Composable] method using the screen sizes of five
 * different reference devices.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(name = "Phone", device = PHONE, showSystemUi = true)
@Preview(
    name = "Phone - Landscape",
    device = "spec:width=411dp,height=891dp,orientation=landscape,dpi=420",
    showSystemUi = true,
)
@Preview(name = "Unfolded Foldable", device = FOLDABLE, showSystemUi = true)
@Preview(
    name = "Tablet",
    device = "spec:width=1280dp,height=800dp,dpi=240,orientation=portrait",
    showSystemUi = true,
)
@Preview(name = "Tablet - Landscape", device = TABLET, showSystemUi = true)
@Preview(name = "Desktop", device = DESKTOP, showSystemUi = true)
annotation class PreviewScreenSizes

/**
 * A MultiPreview annotation for desplaying a @[Composable] method using seven standard font sizes.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(name = "85%", fontScale = 0.85f)
@Preview(name = "100%", fontScale = 1.0f)
@Preview(name = "115%", fontScale = 1.15f)
@Preview(name = "130%", fontScale = 1.3f)
@Preview(name = "150%", fontScale = 1.5f)
@Preview(name = "180%", fontScale = 1.8f)
@Preview(name = "200%", fontScale = 2f)
annotation class PreviewFontScale

/**
 * A MultiPreview annotation for desplaying a @[Composable] method using light and dark themes.
 *
 * Note that the app theme should support dark and light modes for these previews to be different.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
annotation class PreviewLightDark

/**
 * A MultiPreview annotation for desplaying a @[Composable] method using four different wallpaper
 * colors.
 *
 * Note that the app should use a dynamic theme for these previews to be different.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(name = "Red", wallpaper = RED_DOMINATED_EXAMPLE)
@Preview(name = "Blue", wallpaper = BLUE_DOMINATED_EXAMPLE)
@Preview(name = "Green", wallpaper = GREEN_DOMINATED_EXAMPLE)
@Preview(name = "Yellow", wallpaper = YELLOW_DOMINATED_EXAMPLE)
annotation class PreviewDynamicColors
