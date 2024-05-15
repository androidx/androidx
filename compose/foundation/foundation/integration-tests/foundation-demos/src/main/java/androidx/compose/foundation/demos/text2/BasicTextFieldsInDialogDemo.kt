/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.demos.text2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.integration.demos.common.ComposableDemo
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val dialogDemos = listOf(
    ComposableDemo("Full screen dialog, multiple fields") { onNavigateUp ->
        Dialog(onDismissRequest = onNavigateUp) {
            Surface {
                BasicTextFieldDemos()
            }
        }
    },
    ComposableDemo(
        "Small dialog, single field (platform default width, decor fits system windows)"
    ) { onNavigateUp ->
        Dialog(
            onDismissRequest = onNavigateUp,
            properties = DialogProperties(
                usePlatformDefaultWidth = true,
                decorFitsSystemWindows = true
            )
        ) { SingleTextFieldDialog() }
    },
    ComposableDemo(
        "Small dialog, single field (decor fits system windows)"
    ) { onNavigateUp ->
        Dialog(
            onDismissRequest = onNavigateUp,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true
            )
        ) { SingleTextFieldDialog() }
    },
    ComposableDemo(
        "Small dialog, single field (platform default width)"
    ) { onNavigateUp ->
        Dialog(
            onDismissRequest = onNavigateUp,
            properties = DialogProperties(
                usePlatformDefaultWidth = true,
                decorFitsSystemWindows = false
            )
        ) { SingleTextFieldDialog() }
    },
    ComposableDemo(
        "Small dialog, single field"
    ) { onNavigateUp ->
        Dialog(
            onDismissRequest = onNavigateUp,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) { SingleTextFieldDialog() }
    },
    ComposableDemo("Show keyboard automatically") { onNavigateUp ->
        Dialog(onDismissRequest = onNavigateUp) {
            AutoFocusTextFieldDialog()
        }
    }
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BasicTextFieldsInDialogDemo() {
    val listState = rememberLazyListState()
    val (currentDemoIndex, setDemoIndex) = rememberSaveable { mutableIntStateOf(-1) }

    if (currentDemoIndex == -1) {
        LazyColumn(state = listState) {
            itemsIndexed(dialogDemos) { index, demo ->
                ListItem(Modifier.clickable { setDemoIndex(index) }) {
                    Text(demo.title)
                }
            }
        }
    } else {
        val currentDemo = dialogDemos[currentDemoIndex]
        Text(
            currentDemo.title,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(),
            textAlign = TextAlign.Center
        )
        Layout(
            content = { currentDemo.content { setDemoIndex(-1) } }
        ) { measurables, _ ->
            check(measurables.isEmpty()) { "Dialog demo must only emit a Dialog composable." }
            layout(0, 0) {}
        }
    }
}

@Composable
private fun SingleTextFieldDialog() {
    val state = rememberTextFieldState()
    Surface {
        BasicTextField(state, decorator = materialTextFieldDecorator(state))
    }
}

@Composable
private fun AutoFocusTextFieldDialog() {
    val state = rememberTextFieldState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Surface {
        BasicTextField(
            state = state,
            modifier = Modifier.focusRequester(focusRequester),
            decorator = materialTextFieldDecorator(state)
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun materialTextFieldDecorator(
    state: TextFieldState,
    enabled: Boolean = true,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    interactionSource: InteractionSource = MutableInteractionSource()
) = TextFieldDecorator {
    TextFieldDefaults.TextFieldDecorationBox(
        value = state.text.toString(),
        innerTextField = it,
        enabled = enabled,
        singleLine = lineLimits == TextFieldLineLimits.SingleLine,
        visualTransformation = VisualTransformation.None,
        interactionSource = interactionSource
    )
}
