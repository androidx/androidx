/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.tooling

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class CheckBoxState { Unselected, Selected }

@Preview("Single CheckBox")
@Composable
fun CheckBoxPreview() {
    CheckBox()
}

@Preview(name = "CheckBox + Scaffold")
@Composable
fun CheckBoxScaffoldPreview() {
    Scaffold {
        CheckBox()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Preview(name = "AnimatedContent")
@Composable
fun AnimatedContentPreview() {
    Row {
        var count by remember { mutableStateOf(0) }
        Button(onClick = { count++ }) {
            Text("Add")
        }
        AnimatedContent(targetState = count) { targetCount ->
            // Make sure to use `targetCount`, not `count`.
            Text(text = "Count: $targetCount")
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Preview(name = "AnimatedVisibility")
@Composable
fun AnimatedVisibilityPreview() {
    val editable by remember { mutableStateOf(true) }
    AnimatedVisibility(label = "My Animated Visibility", visible = editable) {
        Text(text = "Edit")
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Preview(name = "transition.AnimatedVisibility")
@Composable
fun TransitionAnimatedVisibilityPreview() {
    val editable by remember { mutableStateOf(CheckBoxState.Unselected) }
    val transition = updateTransition(targetState = editable, label = "transition.AV")
    transition.AnimatedVisibility(visible = { it == CheckBoxState.Selected }) {
        Text(text = "Edit")
    }
}

@Composable
private fun CheckBox() {
    val (selected, onSelected) = remember { mutableStateOf(false) }
    val transition = updateTransition(
        if (selected) CheckBoxState.Selected else CheckBoxState.Unselected,
        label = "checkBoxAnim"
    )

    val checkBoxCorner by transition.animateDp(
        transitionSpec = {
            tween(durationMillis = 1000, easing = LinearEasing)
        },
        label = "CheckBox Corner"
    ) {
        when (it) {
            CheckBoxState.Selected -> 28.dp
            CheckBoxState.Unselected -> 0.dp
        }
    }

    Surface(
        shape = MaterialTheme.shapes.large.copy(topStart = CornerSize(checkBoxCorner)),
        modifier = Modifier.toggleable(value = selected, onValueChange = onSelected)
    ) {
        Icon(imageVector = Icons.Filled.Done, contentDescription = null)
    }
}
