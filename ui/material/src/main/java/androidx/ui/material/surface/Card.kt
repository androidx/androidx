/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.surface

import androidx.ui.core.Dp
import androidx.ui.core.dp
import androidx.ui.material.Colors
import androidx.ui.material.MaterialColors
import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.material.borders.ShapeBorder
import androidx.ui.painting.Color
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

/** The helper object to understand when the parameter was explicitly provided */
internal val ColorNotProvided = Color(0)

/**
 * Cards are [Surface]s that display content and actions on a single topic.
 *
 * By default it uses the [MaterialColors.surface] as a background color.
 */
@Composable
fun Card(
    /**
     * Defines the surface's shape as well its shadow.
     *
     * A shadow is only displayed if the [elevation] is greater than
     * zero.
     */
    shape: ShapeBorder = RoundedRectangleBorder(),
    /**
     * The color to paint the [Card].
     *
     * By default it uses the [MaterialColors.surface] color.
     * To create a transparent surface you can use a [TransparentSurface].
     */
    color: Color? = ColorNotProvided,
    /**
     * The z-coordinate at which to place this surface. This controls the size
     * of the shadow below the surface.
     */
    elevation: Dp = 0.dp,
    @Children children: () -> Unit
) {
    if (color == ColorNotProvided) {
        <Colors.Consumer> themeColor ->
            <Surface color=themeColor.surface shape elevation children />
        </Colors.Consumer>
    } else {
        <Surface shape elevation color children />
    }
}
