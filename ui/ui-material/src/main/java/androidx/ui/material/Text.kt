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

import androidx.ui.graphics.Color
import androidx.compose.ambient
import androidx.compose.effectOf

/**
 * Tries to match the background color to correlated text color. For example,
 * on [MaterialColors.primary] background [MaterialColors.onPrimary] will be used.
 */
fun textColorForBackground(background: Color) = effectOf<Color?> {
    with(+ambient(Colors)) {
        when (background) {
            primary -> onPrimary
            primaryVariant -> onPrimary
            secondary -> onSecondary
            secondaryVariant -> onSecondary
            this.background -> onBackground
            surface -> onSurface
            error -> onError
            else -> null
        }
    }
}