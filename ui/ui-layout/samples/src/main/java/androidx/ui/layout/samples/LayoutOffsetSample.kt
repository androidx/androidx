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

package androidx.ui.layout.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.foundation.Text
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.offset
import androidx.ui.layout.offsetPx
import androidx.ui.layout.wrapContentSize
import androidx.ui.unit.dp

@Sampled
@Composable
fun LayoutOffsetModifier() {
    // This text will be offset (10.dp, 20.dp) from the center of the available space.
    Text(
        "Layout offset modifier sample",
        Modifier.fillMaxSize()
            .wrapContentSize(Alignment.Center)
            .offset(10.dp, 20.dp)
    )
}

@Sampled
@Composable
fun LayoutOffsetPxModifier() {
    // This text will be offset in steps of 10.dp from the top left of the available space.
    val offset = state { 0f }
    Text(
        "Layout offset modifier sample",
        Modifier
            .tapGestureFilter { offset.value += 10f }
            .offsetPx(offset, offset)
    )
}
