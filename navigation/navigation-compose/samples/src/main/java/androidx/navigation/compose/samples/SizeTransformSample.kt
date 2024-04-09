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

import androidx.annotation.Sampled
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Sampled
@Composable
fun SizeTransformNav() {
    val navController = rememberNavController()
    Box {
        NavHost(navController, startDestination = "collapsed") {
            composable("collapsed",
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                sizeTransform = {
                    SizeTransform { initialSize, targetSize ->
                        keyframes {
                            durationMillis = 500
                            IntSize(initialSize.width,
                                (initialSize.height + targetSize.height) / 2) at 150
                        }
                    }
                }) {
                CollapsedScreen { navController.navigate("expanded") }
            }
            composable("expanded",
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                sizeTransform = {
                    SizeTransform { initialSize, targetSize ->
                        keyframes {
                            durationMillis = 500
                            IntSize(targetSize.width, initialSize.height + 400) at 150
                        }
                    }
                }) {
                ExpandedScreen { navController.popBackStack() }
            }
        }
    }
}

@Composable
fun CollapsedScreen(onNavigate: () -> Unit) {
    Box(Modifier.clickable { onNavigate() }.size(40.dp).background(Green))
}

@Composable
fun ExpandedScreen(onNavigate: () -> Unit) {
    Box(Modifier.clickable { onNavigate() }.size(500.dp).background(Blue))
}

private val Blue = Color(0xFF2196F3)
private val Green = Color(0xFF4CAF50)
