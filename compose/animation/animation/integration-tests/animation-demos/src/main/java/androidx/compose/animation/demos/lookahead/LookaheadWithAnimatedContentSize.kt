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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.demos.gesture.pastelColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun LookaheadWithAnimatedContentSize() {
    val expanded by
        produceState(initialValue = true) {
            while (true) {
                delay(3000)
                value = !value
            }
        }
    LookaheadScope {
        Column {
            Column(
                Modifier.then(if (expanded) Modifier.fillMaxWidth() else Modifier)
                    .animateContentSize()
                    .zIndex(2f)
            ) {
                Box(Modifier.fillMaxWidth().height(100.dp).background(pastelColors[0]))
                if (expanded) {
                    Box(Modifier.fillMaxWidth().height(200.dp).background(Color.White))
                }
            }
            Box(
                Modifier.animateBounds(this@LookaheadScope)
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(pastelColors[1])
            )
        }
    }
}
