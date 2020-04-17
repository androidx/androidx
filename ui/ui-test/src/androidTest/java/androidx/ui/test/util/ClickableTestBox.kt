/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test.util

import androidx.compose.Composable
import androidx.ui.core.DensityAmbient
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.size
import androidx.ui.semantics.Semantics
import androidx.ui.semantics.testTag
import androidx.ui.unit.Px

@Composable
fun ClickableTestBox(
    width: Px,
    height: Px,
    color: Color,
    tag: String,
    pointerInputModifier: PointerInputModifier
) {
    Semantics(container = true, properties = { testTag = tag }) {
        with(DensityAmbient.current) {
            Box(
                modifier = pointerInputModifier.size(width.toDp(), height.toDp()),
                backgroundColor = color
            )
        }
    }
}
