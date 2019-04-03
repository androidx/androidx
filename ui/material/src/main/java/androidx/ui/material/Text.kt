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

import androidx.ui.core.Text
import androidx.ui.painting.Color
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import com.google.r4a.Composable
import com.google.r4a.ambient
import com.google.r4a.composer
import com.google.r4a.effectOf
import com.google.r4a.unaryPlus

/**
 * Version of [Text] which allows you to specify a text style based on the
 * styles from the [MaterialTypography].
 *
 * Example:
 *     <StyledText text="Hello" style={ h1 } />
 *
 * @param text The text to display.
 * @param style The text style selector from the [MaterialTypography].
 */
@Composable
fun StyledText(text: String, style: (MaterialTypography.() -> TextStyle)) {
    <Text text=TextSpan(text = text, style = +themeTextStyle(style)) />
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