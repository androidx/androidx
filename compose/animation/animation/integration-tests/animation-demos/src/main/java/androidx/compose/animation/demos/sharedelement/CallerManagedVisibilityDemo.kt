/*
 * Copyright 2024 The Android Open Source Project
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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package androidx.compose.animation.demos.sharedelement

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun SharedElementWithCallerManagedVisibility() {
    var selectFirst by remember { mutableStateOf(true) }
    val key = remember { Any() }
    SharedTransitionLayout(
        Modifier.fillMaxSize().padding(10.dp).clickable { selectFirst = !selectFirst }
    ) {
        Box(
            Modifier.sharedElementWithCallerManagedVisibility(
                    rememberSharedContentState(key = key),
                    !selectFirst,
                    boundsTransform = boundsTransform
                )
                .background(Color.Red)
                .size(100.dp)
        ) {
            Text(if (!selectFirst) "false" else "true", color = Color.White)
        }
        // TODO: Check isTransitionActive is false in the end. Why are the shared bounds not
        // two separate entities when transition is finished?
        Box(
            Modifier.offset(180.dp, 180.dp)
                .sharedElementWithCallerManagedVisibility(
                    rememberSharedContentState(
                        key = key,
                    ),
                    selectFirst,
                    boundsTransform = boundsTransform
                )
                .alpha(0.5f)
                .background(Color.Blue)
                .size(180.dp)
        ) {
            Text(if (selectFirst) "false" else "true", color = Color.White)
        }
    }
}

private val boundsTransform = BoundsTransform { initial, target ->
    // Move vertically first then horizontally
    keyframes {
        durationMillis = 500
        initial at 0
        Rect(initial.left, target.top, initial.left + target.width, target.bottom) at 300
    }
}
