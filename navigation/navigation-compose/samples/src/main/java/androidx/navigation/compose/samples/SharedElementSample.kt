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

package androidx.navigation.compose.samples

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

@Serializable object Select

@Serializable object SharedElement

@Serializable object TopAppBarShared

@Serializable object RedBox

@Serializable object BlueBox

@Serializable object First

@Serializable object Second

@Serializable object Third

@Composable
fun AnimatedNav() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Select) {
        composable<Select> {
            Column {
                Box(
                    Modifier.heightIn(min = 48.dp)
                        .fillMaxWidth()
                        .clickable(onClick = { navController.navigate(SharedElement) })
                        .padding(horizontal = 16.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("AnimationNav")
                }
                Box(
                    Modifier.heightIn(min = 48.dp)
                        .fillMaxWidth()
                        .clickable(onClick = { navController.navigate(TopAppBarShared) })
                        .padding(horizontal = 16.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text("Top Bar Shared Element")
                }
            }
        }
        composable<SharedElement> { SharedElementAnimationNav() }
        composable<TopAppBarShared> { TopAppBarElement() }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementAnimationNav() {
    val navController = rememberNavController()
    SharedTransitionLayout {
        val selectFirst = mutableStateOf(true)
        NavHost(navController, startDestination = RedBox) {
            composable<RedBox> {
                RedBox(this@SharedTransitionLayout, this, selectFirst) {
                    navController.navigate(BlueBox)
                }
            }
            composable<BlueBox> {
                BlueBox(this@SharedTransitionLayout, this, selectFirst) {
                    navController.popBackStack()
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RedBox(
    sharedScope: SharedTransitionScope,
    scope: AnimatedContentScope,
    selectFirst: MutableState<Boolean>,
    onNavigate: () -> Unit
) {
    with(sharedScope) {
        Box(
            Modifier.sharedBounds(
                    rememberSharedContentState("name"),
                    scope,
                    renderInOverlayDuringTransition = selectFirst.value
                )
                .clickable(
                    onClick = {
                        selectFirst.value = !selectFirst.value
                        onNavigate()
                    }
                )
                .background(Color.Red)
                .size(100.dp)
        ) {
            Text("start", color = Color.White)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BlueBox(
    sharedScope: SharedTransitionScope,
    scope: AnimatedContentScope,
    selectFirst: MutableState<Boolean>,
    onPopBack: () -> Unit
) {
    with(sharedScope) {
        Box(
            Modifier.offset(180.dp, 180.dp)
                .sharedBounds(
                    rememberSharedContentState("name"),
                    scope,
                    renderInOverlayDuringTransition = !selectFirst.value
                )
                .clickable(
                    onClick = {
                        selectFirst.value = !selectFirst.value
                        onPopBack()
                    }
                )
                .alpha(0.5f)
                .background(Color.Blue)
                .size(180.dp)
        ) {
            Text("finish", color = Color.White)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TopAppBarElement() {
    val navController = rememberNavController()
    SharedTransitionLayout {
        NavHost(navController, startDestination = First) {
            composable<First>(
                enterTransition = { slideIntoContainer(SlideDirection.Right) },
                exitTransition = { slideOutOfContainer(SlideDirection.Left) },
                popExitTransition = { slideOutOfContainer(SlideDirection.Right) }
            ) {
                Column {
                    TopAppBar(
                        title = { Text("first") },
                        modifier =
                            Modifier.sharedElement(
                                rememberSharedContentState("appBar"),
                                this@composable
                            )
                    )
                    Text("first", color = Color.White)
                    Button(onClick = { navController.navigate(Second) }) {
                        Text("Navigate to Second")
                    }
                }
            }
            composable<Second>(
                enterTransition = { slideIntoContainer(SlideDirection.Right) },
                exitTransition = { slideOutOfContainer(SlideDirection.Left) },
                popExitTransition = { slideOutOfContainer(SlideDirection.Right) }
            ) {
                Column {
                    TopAppBar(
                        title = { Text("second") },
                        modifier =
                            Modifier.sharedElement(
                                rememberSharedContentState("appBar"),
                                this@composable
                            )
                    )
                    Text("second", color = Color.White)
                    Button(onClick = { navController.navigate(Third) }) {
                        Text("Navigate to Third")
                    }
                }
            }
            composable<Third>(
                enterTransition = { slideIntoContainer(SlideDirection.Right) },
                exitTransition = { slideOutOfContainer(SlideDirection.Left) },
                popExitTransition = { slideOutOfContainer(SlideDirection.Right) }
            ) {
                Column {
                    Text("third", color = Color.White)
                    Button(onClick = { navController.popBackStack<First>(false) }) {
                        Text("Pop back to First")
                    }
                }
            }
        }
    }
}
