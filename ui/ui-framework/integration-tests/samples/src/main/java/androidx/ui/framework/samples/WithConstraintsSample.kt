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

package androidx.ui.framework.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.DensityAmbient
import androidx.ui.core.WithConstraints
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.dp

@Sampled
@Composable
fun WithConstraintsSample() {
    WithConstraints { constraints ->
        val rectangleHeight = 100.dp
        val threshold = with(DensityAmbient.current) { (rectangleHeight * 2).toIntPx() }
        if (constraints.maxHeight < threshold) {
            Box(LayoutSize(50.dp, rectangleHeight), backgroundColor = Color.Blue)
        } else {
            Column {
                Box(LayoutSize(50.dp, rectangleHeight), backgroundColor = Color.Blue)
                Box(LayoutSize(50.dp, rectangleHeight), backgroundColor = Color.Gray)
            }
        }
    }
}
