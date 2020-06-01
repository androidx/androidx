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
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.size
import androidx.ui.test.util.ClickableTestBox.defaultColor
import androidx.ui.test.util.ClickableTestBox.defaultSize
import androidx.ui.test.util.ClickableTestBox.defaultTag

object ClickableTestBox {
    const val defaultSize = 100.0f
    val defaultColor = Color.Yellow
    const val defaultTag = "ClickableTestBox"
}

@Composable
fun ClickableTestBox(
    modifier: Modifier = Modifier,
    width: Float = defaultSize,
    height: Float = defaultSize,
    color: Color = defaultColor,
    tag: String = defaultTag
) {
    with(DensityAmbient.current) {
        Box(
            modifier = modifier.testTag(tag).size(width.toDp(), height.toDp()),
            backgroundColor = color
        )
    }
}
