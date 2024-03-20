/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.animation.demos.lookahead

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.ExperimentalAnimatableApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.samples.LookaheadLayoutCoordinatesSample
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun LookaheadSamplesDemo() {
    Column {
        ApproachLayoutSample0()
        LookaheadLayoutCoordinatesSample()
    }
}

@OptIn(ExperimentalAnimatableApi::class, ExperimentalSharedTransitionApi::class)
@Composable
public fun ApproachLayoutSample0() {
    var fullWidth by remember { mutableStateOf(false) }
    LookaheadScope {
        Row(
            (if (fullWidth) Modifier.fillMaxWidth() else Modifier.width(100.dp))
                .height(200.dp)
                // Use the custom modifier created above to animate the constraints passed
                // to the child, and therefore resize children in an animation.
                .animateBounds(this@LookaheadScope)
                .clickable { fullWidth = !fullWidth }
        ) {
            Box(
                Modifier.weight(1f).fillMaxHeight().background(Color(0xffff6f69)),
            )
            Box(Modifier.weight(2f).fillMaxHeight().background(Color(0xffffcc5c)))
        }
    }
}
