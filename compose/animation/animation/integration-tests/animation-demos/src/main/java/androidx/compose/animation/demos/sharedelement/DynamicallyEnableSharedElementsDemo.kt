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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package androidx.compose.animation.demos.sharedelement

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.SpaceEvenly
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val pairs = listOf("A" to "B", "A" to "C", "B" to "C", "B" to "A", "C" to "A", "C" to "B")

@Composable
fun Selection(
    pair: Pair<String, String>,
    listOfEnabledStatePairs: MutableList<Pair<String, String>>,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = listOfEnabledStatePairs.contains(pair),
            onCheckedChange = {
                if (it && !listOfEnabledStatePairs.contains(pair)) {
                    listOfEnabledStatePairs.add(pair)
                }
                if (!it) {
                    listOfEnabledStatePairs.remove(pair)
                }
            },
        )
        Text("${pair.first} -> ${pair.second}")
    }
}

@Preview
// Enable shared element transition for A <-> B, A <-> C, but not B <-> C
@Composable
fun DynamicallyEnableSharedElementsDemo() {

    var targetState by remember { mutableStateOf("A") }
    val listOfEnabledStatePairs = remember { mutableStateListOf("A" to "B", "A" to "C") }
    var accountForAnimation by remember { mutableStateOf(true) }

    Column {
        Row(Modifier.background(color = Color.LightGray)) {
            Column(Modifier.weight(1f)) {
                pairs.take(3).forEach { Selection(it, listOfEnabledStatePairs) }
            }
            Column(Modifier.weight(1f)) {
                pairs.drop(3).forEach { Selection(it, listOfEnabledStatePairs) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(accountForAnimation, { accountForAnimation = it })
            Text("Account for ongoing animations")
        }
        SharedTransitionLayout {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val animatedContentTransition = updateTransition(targetState)

                fun config() =
                    if (accountForAnimation) {
                        SharedContentConfig {
                            listOfEnabledStatePairs.contains(
                                animatedContentTransition.currentState to
                                    animatedContentTransition.targetState
                            )
                        }
                    } else {
                        object : SharedTransitionScope.SharedContentConfig {
                            override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
                                get() =
                                    listOfEnabledStatePairs.contains(
                                        animatedContentTransition.currentState to
                                            animatedContentTransition.targetState
                                    )

                            override val shouldKeepEnabledForOngoingAnimation: Boolean
                                get() = false
                        }
                    }
                animatedContentTransition.AnimatedContent(
                    transitionSpec = { fadeIn() togetherWith fadeOut() using null }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (it) {
                            "A" ->
                                ScreenA(
                                    Modifier.sharedElement(
                                        rememberSharedContentState("square", config()),
                                        this@AnimatedContent,
                                    )
                                )
                            "B" ->
                                ScreenB(
                                    Modifier.sharedElement(
                                        rememberSharedContentState("square", config()),
                                        this@AnimatedContent,
                                    )
                                )
                            "C" ->
                                ScreenC(
                                    Modifier.sharedElement(
                                        rememberSharedContentState("square", config()),
                                        this@AnimatedContent,
                                    )
                                )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                    horizontalArrangement = SpaceEvenly,
                ) {
                    Button(onClick = { targetState = "A" }) { Text("To A") }
                    Button(onClick = { targetState = "B" }) { Text("To B") }
                    Button(onClick = { targetState = "C" }) { Text("To C") }
                }
            }
        }
    }
}

@Composable
fun ScreenA(modifier: Modifier = Modifier) {
    Box(modifier.size(200.dp).background(Color.Red), contentAlignment = Alignment.Center) {
        Text(text = "A", fontSize = 50.sp)
    }
}

@Composable
fun ScreenB(modifier: Modifier = Modifier) {
    Box(modifier.size(400.dp).background(Color.Yellow), contentAlignment = Alignment.Center) {
        Text(text = "B", fontSize = 50.sp)
    }
}

@Composable
fun ScreenC(modifier: Modifier = Modifier) {
    Box(modifier.size(100.dp, 300.dp).background(Color.Blue), contentAlignment = Alignment.Center) {
        Text(text = "C", fontSize = 50.sp)
    }
}
