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

package androidx.compose.foundation.demos

import android.view.ViewTreeObserver
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** An xev-inspired input debugger that will display all input */
@Composable
fun InputDebuggerDemo() {
    var pointerEventInfoId by rememberSaveable { mutableIntStateOf(0) }
    val inputEventInfos = remember { mutableStateListOf<InputEventInfo>() }
    // TODO: Add UI to change pointer event pass
    var pointerEventPass: PointerEventPass by rememberSaveable {
        mutableStateOf(PointerEventPass.Main)
    }
    val focusRequester = remember { FocusRequester() }
    Column {
        Column {
            Text(
                "LocalWindowInfo.current.isWindowFocused: ${LocalWindowInfo.current.isWindowFocused}"
            )
            Text("LocalView.current.isInTouchMode: ${currentIsInTouchMode()}")
        }
        FlowRow {
            Button(onClick = { inputEventInfos.clear() }) { Text("Clear pointer events") }
            Button(onClick = { focusRequester.requestFocus() }) { Text("Focus input box") }
        }
        val interactionSource = remember { MutableInteractionSource() }

        val isFocused by interactionSource.collectIsFocusedAsState()

        LazyColumn(
            reverseLayout = true,
            modifier =
                Modifier.fillMaxSize()
                    .padding(16.dp)
                    .focusRequester(focusRequester)
                    .focusable(interactionSource = interactionSource)
                    .border(Dp.Hairline, if (isFocused) Color.Red else Color.Black)
                    .pointerInput(pointerEventPass) {
                        awaitPointerEventScope {
                            while (true) {
                                val pointerEvent = awaitPointerEvent(pass = pointerEventPass)
                                inputEventInfos.add(
                                    pointerEvent.toPointerEventInfo(pointerEventInfoId++)
                                )
                            }
                        }
                    }
                    .onKeyEvent { keyEvent ->
                        inputEventInfos.add(keyEvent.toKeyEventInfo(pointerEventInfoId++))
                        false
                    },
        ) {
            item(key = "KeepScrolledToBottom") {
                // Trick to keep scrolled to the bottom only if we are at the bottom
                // Having a 1px high item at the very start (therefore bottom with reverseLayout)
                // will be the current scroll position if and only if we are scrolled to the
                // very bottom
                Spacer(Modifier.height(with(LocalDensity.current) { 1f.toDp() }))
            }
            items(inputEventInfos.reversed(), key = InputEventInfo::id) { inputEventInfo ->
                Column {
                    Text(inputEventInfo.toString())
                    Divider(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun currentIsInTouchMode(): Boolean {
    val view = LocalView.current
    var isInTouchMode by remember { mutableStateOf(view.isInTouchMode) }
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnTouchModeChangeListener { isInTouchMode = it }
        view.viewTreeObserver.addOnTouchModeChangeListener(listener)
        onDispose { view.viewTreeObserver.removeOnTouchModeChangeListener(listener) }
    }
    return isInTouchMode
}

/** An intermediate data representation for an input event that we are interested in */
private sealed interface InputEventInfo {
    val id: Int

    data class PointerEventInfo(
        override val id: Int,
        val changesDescription: String,
        val motionEventDescription: String,
    ) : InputEventInfo

    data class KeyEventInfo(
        override val id: Int,
        val key: Key,
        val type: KeyEventType,
        val nativeKeyEventDescription: String,
    ) : InputEventInfo
}

private fun PointerEvent.toPointerEventInfo(id: Int): InputEventInfo.PointerEventInfo =
    InputEventInfo.PointerEventInfo(
        id = id,
        changesDescription = changes.toString(),
        motionEventDescription = motionEvent.toString(),
    )

private fun KeyEvent.toKeyEventInfo(id: Int): InputEventInfo.KeyEventInfo =
    InputEventInfo.KeyEventInfo(
        id = id,
        key = key,
        type = type,
        nativeKeyEventDescription = nativeKeyEvent.toString(),
    )
