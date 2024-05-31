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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.samples.R
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Preview
@Composable
fun NavigationWithSharedToolBarDemo() {
    val navController = rememberNavController()
    SharedTransitionLayout {
        NavHost(navController, startDestination = "first") {
            composable(
                "first",
                enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
            ) {
                Column {
                    TopAppBar(
                        title = { Text("Text") },
                        modifier =
                            Modifier.sharedElement(
                                rememberSharedContentState(key = "appBar"),
                                this@composable,
                            )
                    )
                    Text(
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent" +
                            " fringilla mollis efficitur. Maecenas sit amet urna eu urna blandit" +
                            " suscipit efficitur eget mauris. Nullam eget aliquet ligula. Nunc" +
                            "id euismod elit. Morbi aliquam enim eros, eget consequat" +
                            " dolor consequat id. Quisque elementum faucibus congue. Curabitur" +
                            " mollis aliquet turpis, ut pellentesque justo eleifend nec.\n",
                    )
                    Button(onClick = { navController.navigate("second") }) {
                        Text("Navigate to Cat")
                    }
                }
            }
            composable(
                "second",
                enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
            ) {
                Column {
                    TopAppBar(
                        title = { Text("Cat") },
                        modifier =
                            Modifier.sharedElement(
                                rememberSharedContentState(key = "appBar"),
                                this@composable,
                            )
                    )
                    Image(
                        painterResource(id = R.drawable.yt_profile),
                        contentDescription = "cute cat",
                        contentScale = ContentScale.FillHeight,
                        modifier = Modifier.clip(shape = RoundedCornerShape(20.dp))
                    )
                    Button(onClick = { navController.navigate("third") }) {
                        Text("Navigate to Empty Page")
                    }
                }
            }
            composable(
                "third",
                enterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text("Nothing to see here. Move on.")
                    Spacer(Modifier.size(200.dp))
                    Button(onClick = { navController.popBackStack("first", false) }) {
                        Text("Pop back to Text")
                    }
                }
            }
        }
    }
}
