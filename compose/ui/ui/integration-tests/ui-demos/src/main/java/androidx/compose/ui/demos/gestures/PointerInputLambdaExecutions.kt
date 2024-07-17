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

package androidx.compose.ui.demos.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Shows how underlying lambdas are executed. */
@Composable
fun PointerInputLambdaExecutions() {

    var topBoxText by remember { mutableStateOf("Click button to see details") }
    var firstPointerInputLambdaExecutionCount by remember { mutableIntStateOf(0) }
    var firstPointerInputPressCounter by remember { mutableIntStateOf(0) }
    var firstPointerInputMoveCounter by remember { mutableIntStateOf(0) }
    var firstPointerInputReleaseCounter by remember { mutableIntStateOf(0) }

    var bottomBoxText by remember { mutableStateOf("Click button to see details") }
    var secondPointerInputLambdaExecutionCount by remember { mutableIntStateOf(0) }
    var secondPointerInputPressCounter by remember { mutableIntStateOf(0) }
    var secondPointerInputMoveCounter by remember { mutableIntStateOf(0) }
    var secondPointerInputReleaseCounter by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(
            modifier = Modifier.fillMaxWidth().weight(1f),
            textAlign = TextAlign.Center,
            text = "See how underlying lambdas are executed"
        )

        Column(modifier = Modifier.fillMaxSize().background(Color.LightGray).padding(8.dp)) {
            Text(
                modifier =
                    Modifier.size(400.dp).background(Color.Green).weight(1f).pointerInput(Unit) {
                        firstPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        firstPointerInputPressCounter++
                                    }
                                    PointerEventType.Move -> {
                                        firstPointerInputMoveCounter++
                                    }
                                    PointerEventType.Release -> {
                                        firstPointerInputReleaseCounter++
                                    }
                                }
                                topBoxText =
                                    "Lambda Execution: $firstPointerInputLambdaExecutionCount,\n" +
                                        "Press: $firstPointerInputPressCounter,\n" +
                                        "Move: $firstPointerInputMoveCounter,\n" +
                                        "Release: $firstPointerInputReleaseCounter"
                            }
                        }
                    },
                textAlign = TextAlign.Center,
                text = topBoxText
            )

            Text(
                modifier =
                    Modifier.size(400.dp).background(Color.Red).weight(1f).pointerInput(Unit) {
                        secondPointerInputLambdaExecutionCount++
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        secondPointerInputPressCounter++
                                    }
                                    PointerEventType.Move -> {
                                        secondPointerInputMoveCounter++
                                    }
                                    PointerEventType.Release -> {
                                        secondPointerInputReleaseCounter++
                                    }
                                }
                                bottomBoxText =
                                    "Lambda Execution: $secondPointerInputLambdaExecutionCount,\n" +
                                        "Press: $secondPointerInputPressCounter,\n" +
                                        "Move: $secondPointerInputMoveCounter,\n" +
                                        "Release: $secondPointerInputReleaseCounter"
                            }
                        }
                    },
                textAlign = TextAlign.Center,
                text = bottomBoxText
            )
        }
    }
}

/**
 * Shows how underlying lambdas are executed but instead of passing the lambdas directly, we call a
 * separate external function to return the PointerInputEventHandler.
 */
@Composable
fun PointerInputLambdaExecutionsUsingExternalFunctions() {

    var topBoxText by remember { mutableStateOf("Click button to see details") }
    var firstPointerInputLambdaExecutionCount by remember { mutableIntStateOf(0) }
    var firstPointerInputPressCounter by remember { mutableIntStateOf(0) }
    var firstPointerInputMoveCounter by remember { mutableIntStateOf(0) }
    var firstPointerInputReleaseCounter by remember { mutableIntStateOf(0) }

    var bottomBoxText by remember { mutableStateOf("Click button to see details") }
    var secondPointerInputLambdaExecutionCount by remember { mutableIntStateOf(0) }
    var secondPointerInputPressCounter by remember { mutableIntStateOf(0) }
    var secondPointerInputMoveCounter by remember { mutableIntStateOf(0) }
    var secondPointerInputReleaseCounter by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(
            modifier = Modifier.fillMaxWidth().weight(1f),
            textAlign = TextAlign.Center,
            text = "See how underlying lambdas are executed"
        )

        Column(modifier = Modifier.fillMaxSize().background(Color.LightGray).padding(8.dp)) {
            Text(
                modifier =
                    Modifier.size(400.dp)
                        .background(Color.Green)
                        .weight(1f)
                        .pointerInput(
                            Unit,
                            createPointerInputEventHandlerReturnTypeInterface {
                                firstPointerInputLambdaExecutionCount++
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        when (event.type) {
                                            PointerEventType.Press -> {
                                                firstPointerInputPressCounter++
                                            }
                                            PointerEventType.Move -> {
                                                firstPointerInputMoveCounter++
                                            }
                                            PointerEventType.Release -> {
                                                firstPointerInputReleaseCounter++
                                            }
                                        }
                                        topBoxText =
                                            "Lambda Execution: $firstPointerInputLambdaExecutionCount,\n" +
                                                "Press: $firstPointerInputPressCounter,\n" +
                                                "Move: $firstPointerInputMoveCounter,\n" +
                                                "Release: $firstPointerInputReleaseCounter"
                                    }
                                }
                            }
                        ),
                textAlign = TextAlign.Center,
                text = topBoxText
            )

            Text(
                modifier =
                    Modifier.size(400.dp)
                        .background(Color.Red)
                        .weight(1f)
                        .pointerInput(
                            Unit,
                            createPointerInputEventHandlerReturnTypeInterface {
                                secondPointerInputLambdaExecutionCount++
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        when (event.type) {
                                            PointerEventType.Press -> {
                                                secondPointerInputPressCounter++
                                            }
                                            PointerEventType.Move -> {
                                                secondPointerInputMoveCounter++
                                            }
                                            PointerEventType.Release -> {
                                                secondPointerInputReleaseCounter++
                                            }
                                        }
                                        bottomBoxText =
                                            "Lambda Execution: $secondPointerInputLambdaExecutionCount,\n" +
                                                "Press: $secondPointerInputPressCounter,\n" +
                                                "Move: $secondPointerInputMoveCounter,\n" +
                                                "Release: $secondPointerInputReleaseCounter"
                                    }
                                }
                            }
                        ),
                textAlign = TextAlign.Center,
                text = bottomBoxText
            )
        }
    }
}

// The return type (PointerInputEventHandler) is actually an interface, so Kotlin will create class
// type based on calling location/order.
private fun createPointerInputEventHandlerReturnTypeInterface(
    lambda: PointerInputEventHandler
): PointerInputEventHandler {
    return lambda
}
