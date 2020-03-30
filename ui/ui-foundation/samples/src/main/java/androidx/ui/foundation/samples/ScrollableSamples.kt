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

package androidx.ui.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.Text
import androidx.ui.foundation.gestures.DragDirection
import androidx.ui.foundation.gestures.ScrollableState
import androidx.ui.foundation.gestures.scrollable
import androidx.ui.graphics.Color
import androidx.ui.layout.preferredSize
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import kotlin.math.roundToInt

@Sampled
@Composable
fun ScrollableSample() {
    // actual composable state
    val offset = state { 0f }
    // state for Scrollable, describes how to consume scrolling delta and update offset
    Box(
        Modifier
            .preferredSize(200.dp)
            .scrollable(
                dragDirection = DragDirection.Vertical,
                scrollableState = ScrollableState { delta ->
                    offset.value = offset.value + delta
                    delta
                }
            ),
        backgroundColor = Color.LightGray,
        gravity = ContentGravity.Center
    ) {
        Text(offset.value.roundToInt().toString(), style = TextStyle(fontSize = 50.sp))
    }
}