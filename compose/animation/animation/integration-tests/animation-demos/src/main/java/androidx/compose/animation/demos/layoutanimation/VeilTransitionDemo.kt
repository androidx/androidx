/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.animation.demos.layoutanimation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.unveilIn
import androidx.compose.animation.veilOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VeilTransitionDemo() {
    var page by remember { mutableIntStateOf(0) }
    val veilColor = Color.Black.copy(alpha = 0.8f)
    Column(
        Modifier.fillMaxSize().padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AnimatedContent(
            targetState = page,
            modifier = Modifier.fillMaxSize().weight(1f),
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } togetherWith
                            slideOutHorizontally { -it / 2 } + veilOut(targetColor = veilColor))
                        .apply { targetContentZIndex = 1f }
                } else {
                    slideInHorizontally { -it / 2 } +
                        unveilIn(initialColor = veilColor) togetherWith slideOutHorizontally { it }
                }
            },
        ) { targetPage ->
            if (targetPage == 0) {
                Page1()
            } else {
                Page2()
            }
        }
        Button(onClick = { page = 1 - page }) { Text(if (page == 0) "Next" else "Back") }
    }
}

@Composable
fun Page1() {
    Column(Modifier.fillMaxSize().background(Color.White).padding(20.dp)) {
        Text(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed non risus. Suspendisse lectus tortor, dignissim sit amet, " +
                "adipiscing nec, ultricies sed, dolor. Cras elementum ultrices " +
                "diam. Maecenas ligula massa, varius a, semper congue, euismod " +
                "non, mi. Proin porttitor, orci nec nonummy molestie, enim est " +
                "eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. " +
                "Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim."
        )
    }
}

@Composable
fun Page2() {
    Column(Modifier.fillMaxSize().background(Color.White).padding(20.dp)) {
        Text(
            "Pellentesque vel lacus. Mauris nibh felis, adipiscing varius, " +
                "adipiscing in, lacinia vel, tellus. Suspendisse ac urna. " +
                "Etiam pellentesque mauris ut lectus. Nunc tellus ante, mattis " +
                "eget, gravida vitae, ultricies ac, leo. Integer leo psum, " +
                "tellus sit amet, tincidunt eu, luctus ut, magna. " +
                "Vivamus a ligula. Fusce mauris. Nullam at enim."
        )
    }
}
