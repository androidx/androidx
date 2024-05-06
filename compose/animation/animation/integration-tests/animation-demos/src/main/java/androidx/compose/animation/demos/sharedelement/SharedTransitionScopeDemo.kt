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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.ScaleToBounds
import androidx.compose.animation.samples.R
import androidx.compose.animation.samples.SharedElementInAnimatedContentSample
import androidx.compose.animation.samples.SharedElementWithFABInOverlaySample
import androidx.compose.animation.samples.SharedElementWithMovableContentSample
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun SharedElementDemos() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val list = listOf<Pair<String, @Composable () -> Unit>>(
        "AnimContent\n List To Details" to { ListToDetailsDemo() },
        "Nested" to { NestedSharedElementDemo() },
        "Expanded Card" to { SwitchBetweenCollapsedAndExpanded() },
        "Container Transform" to { ContainerTransformDemo() },
        "Shared Element\n Caller Managed Vis" to { SharedElementWithCallerManagedVisibility() },
        "FABInOverlay" to { SharedElementWithFABInOverlaySample() },
        "AnimatedContent" to { SharedElementInAnimatedContentSample() },
        "Text transform" to { TextSharedBoundsExperiments() },
        "Nav Shared Tool Bar" to { NavigationWithSharedToolBarDemo() },
    )

    Column {
        ScrollableTabRow(selectedTab) {
            list.forEachIndexed { index, (text, _) ->
                Tab(
                    index == selectedTab,
                    { selectedTab = index },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Text(text)
                }
            }
        }
        list[selectedTab].second.invoke()
    }
}

@Preview
@Composable
fun SharedElementWithMovableContent() {
    SharedElementWithMovableContentSample()
}

@Preview
@Composable
fun SharedElementInAnimatedVisibilityWithFABRenderedInOverlay() {
    SharedElementWithFABInOverlaySample()
}

@Preview
@Composable
fun SharedElementInAnimatedContent() {
    SharedElementInAnimatedContentSample()
}

@Preview
@Composable
fun ScaleContentTransition() {
    val someText =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent fringilla" +
            " mollis efficitur. Maecenas sit amet urna eu urna blandit suscipit efficitur" +
            " eget mauris. Nullam eget aliquet ligula. Nunc id euismod elit. Morbi aliquam" +
            " enim eros, eget consequat dolor consequat id. Quisque elementum faucibus" +
            " congue. Curabitur mollis aliquet turpis, ut pellentesque justo eleifend nec.\n" +
            "\n" +
            "Suspendisse ac consequat turpis, euismod lacinia quam. Nulla lacinia tellus" +
            " eu felis tristique ultricies. Vivamus et ultricies dolor. Orci varius" +
            " natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus." +
            " Ut gravida porttitor arcu elementum elementum. Phasellus ultrices vel turpis" +
            " volutpat mollis. Vivamus leo diam, placerat quis leo efficitur, ultrices" +
            " placerat ex. Nullam mollis et metus ac ultricies. Ut ligula metus, congue" +
            " gravida metus in, vestibulum posuere velit. Sed et ex nisl. Fusce tempor" +
            " odio eget sapien pellentesque, sed cursus velit fringilla. Nullam odio" +
            " ipsum, eleifend non consectetur vitae, congue id libero. Etiam tincidunt" +
            " mauris at urna dictum ornare.\n"

    var showText by remember { mutableStateOf(true) }
    val rememberSharedKey = remember { Any() }
    SharedTransitionLayout(Modifier.clickable { showText = !showText }) {
        AnimatedContent(
            targetState = showText,
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { showText ->
            if (showText) {
                Text(
                    text = someText,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .padding(20.dp)
                        .sharedBounds(
                            rememberSharedContentState(key = rememberSharedKey),
                            this,
                            EnterTransition.None,
                            ExitTransition.None,
                            resizeMode = ScaleToBounds(contentScale = ContentScale.Crop),
                        )
                )
            } else {
                Image(
                    painterResource(id = R.drawable.yt_profile),
                    contentDescription = "cute cat",
                    modifier = Modifier
                        .wrapContentSize()
                        .sharedBounds(
                            rememberSharedContentState(key = rememberSharedKey),
                            this,
                            EnterTransition.None,
                            ExitTransition.None,
                            resizeMode = ScaleToBounds(contentScale = ContentScale.Crop),
                        )
                        .requiredSize(200.dp)
                        .clip(shape = RoundedCornerShape(10))
                )
            }
        }
    }
}
