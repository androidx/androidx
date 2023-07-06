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

@file:OptIn(ExperimentalMotionApi::class)

package androidx.constraintlayout.compose.demos

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionScene

/**
 * A demo of using MotionLayout to create a complex animated emoji selector.
 */
@Preview
@Composable
fun ReactionSelectorDemo() {
    var selected by remember { mutableIntStateOf(3) }
    val transitionName = remember { mutableStateOf("transition1") }
    val emojis = remember { "😀 🙂 🤨 😐 😒 😬".split(' ') }
    val emojiNames = remember {
        listOf<String>(
            "Grinning Face",
            "Slightly Smiling Face",
            "Face with Raised Eyebrow",
            "Neutral Face",
            "Unamused Face",
            "Grimacing Face"
        )
    }

    val scene = MotionScene {
        val emojiIds = emojis.map { createRefFor(it) }.toTypedArray()
        val titleIds = emojiNames.map { createRefFor(it) }.toTypedArray()

        val start1 = constraintSet {
            createHorizontalChain(elements = emojiIds)
            emojiIds.forEach {
                constrain(it) {
                    top.linkTo(parent.top, 10.dp)
                }
            }
            titleIds.forEachIndexed { index, title ->
                constrain(title) {
                    top.linkTo(emojiIds[0].bottom, 10.dp)
                    start.linkTo(emojiIds[index].start)
                    end.linkTo(emojiIds[index].end)
                    bottom.linkTo(parent.bottom, 10.dp)
                    scaleX = 0.1f
                    alpha = 0f
                }
            }
        }
        val ends = titleIds.map {
            constraintSet(extendConstraintSet = start1) {
                constrain(it) {
                    scaleX = 1f
                    alpha = 1f
                }
            }
        }
        ends.mapIndexed { index, end ->
            transition(start1, end, "transition$index") {
            }
        }
    }
    val progress = remember { Animatable(0f) }
    val selectedFlow = snapshotFlow { selected }

    LaunchedEffect(Unit) {
        selectedFlow.collect {
            progress.snapTo(0f)
            transitionName.value = "transition$it"
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(800)
            )
        }
    }

    Column {
        MotionLayout(
            modifier = Modifier
                .background(Color(0xff334433))
                .fillMaxWidth(),
            motionScene = scene,
            transitionName = transitionName.value,
            progress = progress.value
        ) {
            emojis.forEachIndexed { index, icon ->
                Text(
                    text = icon,
                    modifier = Modifier
                        .layoutId(icon)
                        .clickable {
                            selected = index
                        }
                )
            }
            emojiNames.forEach { name ->
                Text(
                    text = name,
                    color = Color.White,
                    modifier = Modifier.layoutId(name)
                )
            }
        }
    }
}
