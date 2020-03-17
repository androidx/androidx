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

package androidx.ui.material.studies.rally

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Clickable
import androidx.ui.graphics.Color
import androidx.ui.graphics.vector.VectorAsset
import androidx.ui.graphics.vector.drawVector
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutWidth
import androidx.ui.material.ripple.Ripple
import androidx.ui.unit.Dp
import androidx.ui.unit.dp

/**
 * A button made with a 24x24dp icon
 */
@Composable
fun IconButton(
    vectorImage: VectorAsset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.None
) {
    Box(modifier = modifier) {
        Ripple(bounded = false) {
            Clickable(onClick) {
                Icon(vectorImage)
            }
        }
    }
}

/**
 * 24x24dp icon from a resource id.
 */
@Composable
fun Icon(
    vectorImage: VectorAsset,
    tintColor: Color = Color.White,
    modifier: Modifier = Modifier.None,
    size: Dp = 24.dp
) {
    Box(
        modifier = LayoutHeight(size) + LayoutWidth(size) + modifier +
                drawVector(
                    vectorImage = vectorImage,
                    tintColor = tintColor
                )
    )
}