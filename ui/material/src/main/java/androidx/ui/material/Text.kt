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

package androidx.ui.material

import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.Text
import androidx.ui.painting.Color
import androidx.ui.painting.TextStyle
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.effectOf
import com.google.r4a.unaryPlus

/**
 * Applies [MaterialTypography.h1] for children [Text] components.
 */
@Composable
fun H1TextStyle(@Children children: () -> Unit) {
    <CurrentTextStyleProvider value=+themeTextStyle { h1 }>
        <children />
    </CurrentTextStyleProvider>
}

/**
 * Applies [MaterialTypography.body1] for children [Text] components.
 */
@Composable
fun Body1TextStyle(@Children children: () -> Unit) {
    <CurrentTextStyleProvider value=+themeTextStyle { body1 }>
        <children />
    </CurrentTextStyleProvider>
}

/**
 * Applies [MaterialTypography.button] for children [Text] components.
 */
@Composable
fun ButtonTextStyle(@Children children: () -> Unit) {
    <CurrentTextStyleProvider value=+themeTextStyle { button }>
        <children />
    </CurrentTextStyleProvider>
}

/**
 * Applies color for children [Text] components.
 */
@Composable
fun TextColor(
    color: (MaterialColors.() -> Color),
    @Children children: () -> Unit
) {
    val value = TextStyle(color = (+ambient(Colors)).color())
    <CurrentTextStyleProvider value>
        <children />
    </CurrentTextStyleProvider>
}

/**
 * Applies color for children [Text] components.
 *
 * Tries to match the background color to correlated text color. For example,
 * on [MaterialColors.primary] background [MaterialColors.onPrimary] will be used.
 *
 * @see textColorForBackground
 */
@Composable
fun TextColorForBackground(
    background: Color,
    @Children children: () -> Unit
) {
    val value = TextStyle(color = +textColorForBackground(background))
    <CurrentTextStyleProvider value>
        <children />
    </CurrentTextStyleProvider>
}

/**
 * Tries to match the background color to correlated text color. For example,
 * on [MaterialColors.primary] background [MaterialColors.onPrimary] will be used.
 */
fun textColorForBackground(background: Color) = effectOf<Color?> {
    with(+ambient(Colors)) {
        when (background) {
            primary -> onPrimary
            secondary -> onSecondary
            background -> onBackground
            surface -> onSurface
            error -> onError
            else -> primary
        }
    }
}