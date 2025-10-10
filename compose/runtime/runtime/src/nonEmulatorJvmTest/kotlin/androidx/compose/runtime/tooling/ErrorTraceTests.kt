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

package androidx.compose.runtime.tooling

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mock.CompositionTestScope
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.fastForEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalComposeRuntimeApi::class)
class ErrorTraceTests {
    @Test
    fun setContent() =
        exceptionTest(listOf("<lambda>(ErrorTraceTests.kt:<unknown line>)"), groupKeyTrace(1)) {
            compose { throwTestException() }
        }

    @Test
    fun recompose() =
        exceptionTest(listOf("<lambda>(ErrorTraceTests.kt:<unknown line>)"), groupKeyTrace(1)) {
            var state by mutableStateOf(false)
            compose {
                if (state) {
                    throwTestException()
                }
            }

            state = true
            advance()
        }

    @Test
    fun setContentLinear() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:77)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "Linear(ErrorTraceComposables.kt:73)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(3),
        ) {
            compose { Linear { throwTestException() } }
        }

    @Test
    fun recomposeLinear() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:77)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "Linear(ErrorTraceComposables.kt:73)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(3),
        ) {
            var state by mutableStateOf(false)
            compose {
                Linear {
                    if (state) {
                        throwTestException()
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    fun setContentInlineLinear() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:87)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "InlineLinear(ErrorTraceComposables.kt:83)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(1), // All frames except from initial lambda are source markers
        ) {
            compose { InlineLinear { throwTestException() } }
        }

    @Test
    fun recomposeInlineLinear() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:87)",
                "ReusableComposeNode(Composables.kt:<line number>)",
                "InlineLinear(ErrorTraceComposables.kt:83)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(1),
        ) {
            var state by mutableStateOf(false)

            compose {
                InlineLinear {
                    if (state) {
                        throwTestException()
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    fun setContentAfterTextInLoopInlineWrapper() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "InlineWrapper(ErrorTraceComposables.kt:57)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(2),
        ) {
            compose {
                InlineWrapper {
                    repeat(5) { it ->
                        Text("test")
                        if (it > 3) {
                            throwTestException()
                        }
                    }
                }
            }
        }

    @Test
    fun recomposeAfterTextInLoopInlineWrapper() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "InlineWrapper(ErrorTraceComposables.kt:57)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(2),
        ) {
            var state by mutableStateOf(false)

            compose {
                InlineWrapper {
                    repeat(5) { it ->
                        Text("test")
                        if (it > 3 && state) {
                            throwTestException()
                        }
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    fun setContentAfterTextInLoop() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "Repeated(ErrorTraceComposables.kt:94)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(3),
        ) {
            compose {
                Repeated(List(10) { it }) {
                    Text("test")
                    throwTestException()
                }
            }
        }

    @Test
    fun recomposeAfterTextInLoop() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "Repeated(ErrorTraceComposables.kt:94)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(3),
        ) {
            var state by mutableStateOf(false)

            compose {
                Repeated(List(10) { it }) {
                    Text("test")
                    if (state) {
                        throwTestException()
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    fun setContentSubcomposition() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:66)",
                "Subcompose(ErrorTraceComposables.kt:62)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(4),
        ) {
            compose { Subcompose { throwTestException() } }
        }

    @Test
    fun setContentNestedSubcomposition() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:66)",
                "Subcompose(ErrorTraceComposables.kt:62)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "<lambda>(ErrorTraceComposables.kt:66)",
                "Subcompose(ErrorTraceComposables.kt:62)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(7),
        ) {
            compose { Subcompose { Subcompose { throwTestException() } } }
        }

    @Test
    fun recomposeSubcomposition() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceComposables.kt:66)",
                "Subcompose(ErrorTraceComposables.kt:62)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(4),
        ) {
            var state by mutableStateOf(false)

            compose {
                Subcompose {
                    if (state) {
                        throwTestException()
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    fun setContentDefaults() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "ComposableWithDefaults(ErrorTraceComposables.kt:109)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(3),
        ) {
            compose { ComposableWithDefaults { throwTestException() } }
        }

    @Test
    fun recomposeDefaults() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "ComposableWithDefaults(ErrorTraceComposables.kt:109)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(3),
        ) {
            var state by mutableStateOf(false)

            compose {
                ComposableWithDefaults {
                    if (state) {
                        throwTestException()
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    fun setContentRemember() =
        exceptionTest(
            listOf(
                "remember(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(1),
        ) {
            compose { remember { throwTestException() } }
        }

    @Test
    fun setContentRememberObserver() =
        exceptionTest(
            listOf(
                "remember(Effects.kt:<unknown line>)",
                "DisposableEffect(Effects.kt:<line number>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(1),
        ) {
            compose { DisposableEffect(Unit) { throwTestException() } }
        }

    @Test
    fun recomposeRememberObserver() =
        exceptionTest(
            listOf(
                "remember(Effects.kt:<unknown line>)",
                "DisposableEffect(Effects.kt:<line number>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(1),
        ) {
            var state by mutableStateOf(false)
            compose {
                DisposableEffect(state) {
                    if (state) {
                        throwTestException()
                    }
                    onDispose {}
                }
            }

            state = true
            advance()
        }

    @Test
    fun nodeReuse() =
        exceptionTest(
            listOf(
                "NodeWithCallbacks(ErrorTraceComposables.kt:121)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "ReusableContent(Composables.kt:<line number>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(2),
        ) {
            var state by mutableStateOf(false)
            compose {
                ReusableContent(state) { NodeWithCallbacks(onReuse = { throwTestException() }) }
            }

            state = true
            advance()
        }

    @Test
    fun nodeDeactivate() =
        exceptionTest(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:122)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "ReusableContentHost(Composables.kt:<line number>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(3),
        ) {
            var active by mutableStateOf(true)
            compose {
                ReusableContentHost(active) {
                    NodeWithCallbacks(onDeactivate = { throwTestException() })
                }
            }

            active = false
            advance()
        }

    @Test
    fun setContentNodeAttach() =
        exceptionTest(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:122)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "InlineWrapper(ErrorTraceComposables.kt:57)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(2),
        ) {
            compose { InlineWrapper { NodeWithCallbacks(onAttach = { throwTestException() }) } }
        }

    @Test
    fun recomposeNodeAttach() =
        exceptionTest(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:122)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "Wrapper(ErrorTraceComposables.kt:149)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(5),
        ) {
            var state by mutableStateOf(false)
            compose {
                Wrapper {
                    if (state) {
                        NodeWithCallbacks(onAttach = { throwTestException() })
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    // todo(b/409033128): Investigate why NodeWithCallbacks has an incorrect line
    fun recomposeNodeAttachInlineWrapper() =
        exceptionTest(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:122)",
                // (b/380272059): groupless source information is missing here after recomposition
                //                "<lambda>(ErrorTraceTests.kt:<line number>)",
                //                "InlineWrapper(ErrorTraceComposables.kt:148)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(3),
        ) {
            var state by mutableStateOf(false)
            compose {
                InlineWrapper {
                    if (state) {
                        NodeWithCallbacks(onAttach = { throwTestException() })
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    fun emptySourceInformation() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "InlineWrapper(ErrorTraceComposables.kt:57)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(4),
        ) {
            @Suppress("PrimitiveInCollection") val list = listOf(1, 2, 3)
            var content: (@Composable () -> Unit)? = null
            // some gymnastics to ensure that Kotlin generates a null check
            if (3 in list) {
                content = { throwTestException() }
            }

            compose { InlineWrapper { list.fastForEach { key(it) { content?.invoke() } } } }
        }

    @Test
    fun setContentNodeUpdate() =
        exceptionTest(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:122)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "Wrapper(ErrorTraceComposables.kt:149)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(4),
        ) {
            compose { Wrapper { NodeWithCallbacks(onUpdate = { throwTestException() }) } }
        }

    @Test
    fun recomposeUpdate() =
        exceptionTest(
            listOf(
                "ReusableComposeNode(Composables.kt:<unknown line>)",
                "NodeWithCallbacks(ErrorTraceComposables.kt:122)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "Wrapper(ErrorTraceComposables.kt:149)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(5),
        ) {
            var state by mutableStateOf(false)
            compose {
                Wrapper {
                    if (state) {
                        NodeWithCallbacks(
                            onUpdate =
                                if (state) {
                                    { throwTestException() }
                                } else {
                                    {}
                                }
                        )
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    fun setContentMovableContent() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(MovableContent.kt:<line number>)",
                "<lambda>(ComposerImpl.kt:<line number>)",
                "<lambda>(MovableContent.kt:<unknown line>)",
                "MovableWrapper(ErrorTraceComposables.kt:156)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(6),
        ) {
            compose { MovableWrapper { throwTestException() } }
        }

    @Test
    fun recomposeMovableContent() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(MovableContent.kt:<line number>)",
                "<lambda>(ComposerImpl.kt:<line number>)",
                "<lambda>(MovableContent.kt:<unknown line>)",
                "MovableWrapper(ErrorTraceComposables.kt:156)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(6),
        ) {
            var state by mutableStateOf(false)
            compose {
                MovableWrapper {
                    if (state) {
                        throwTestException()
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    fun moveMovableContentOf() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ComposerImpl.kt:<line number>)",
                "<lambda>(MovableContent.kt:<unknown line>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "WrappedMovableContent(ErrorTraceComposables.kt:166)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(7),
        ) {
            var state by mutableStateOf(false)
            compose {
                WrappedMovableContent(
                    content = {
                        if (it) {
                            throwTestException()
                        }
                    }
                ) { movableContent ->
                    if (!state) {
                        movableContent(false)
                    } else {
                        Wrapper { movableContent(true) }
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    fun moveMovableContentOfStateRead() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ComposerImpl.kt:<line number>)",
                "<lambda>(MovableContent.kt:<unknown line>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "WrappedMovableContent(ErrorTraceComposables.kt:166)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(7),
        ) {
            var state by mutableStateOf(false)
            compose {
                WrappedMovableContent(
                    content = {
                        if (state) {
                            throwTestException()
                        }
                    }
                ) { movableContent ->
                    if (!state) {
                        movableContent(state)
                    } else {
                        Wrapper { movableContent(state) }
                    }
                }
            }

            state = true
            advance()
        }

    @Test
    fun moveMovableContentOfReverse() =
        exceptionTest(
            listOf(
                "<lambda>(ErrorTraceTests.kt:<unknown line>)",
                "<lambda>(ComposerImpl.kt:<line number>)",
                "<lambda>(MovableContent.kt:<unknown line>)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "Wrapper(ErrorTraceComposables.kt:149)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
                "WrappedMovableContent(ErrorTraceComposables.kt:166)",
                "<lambda>(ErrorTraceTests.kt:<line number>)",
            ),
            groupKeyTrace(9),
        ) {
            var state by mutableStateOf(true)
            compose {
                WrappedMovableContent(
                    content = {
                        if (it) {
                            throwTestException()
                        }
                    }
                ) { movableContent ->
                    if (!state) {
                        movableContent(false)
                    } else {
                        Wrapper { movableContent(true) }
                    }
                }
            }

            state = false
            advance()
        }
}

private fun throwTestException(): Nothing = throw TestComposeException()

private class TestComposeException : Exception("Test exception")

private const val TestFile = "ErrorTraceComposables.kt"
private const val DebugKeepLineNumbers = false

private fun exceptionTest(
    sourceTrace: List<String>,
    groupKeyTrace: List<String>,
    block: suspend CompositionTestScope.() -> Unit,
) {
    Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.SourceInformation)
    assertTrace(sourceTrace) { compositionTest(block) }
    Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.GroupKeys)
    assertTrace(groupKeyTrace) { compositionTest(block) }
    Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.Auto)
}

private fun assertTrace(expected: List<String>, block: () -> Unit) {
    var exception: TestComposeException? = null
    try {
        block()
    } catch (e: TestComposeException) {
        exception = e
    }
    exception = exception ?: error("Composition exception was not caught or not thrown")

    val composeTrace =
        exception.suppressedExceptions.firstOrNull { it is DiagnosticComposeException }
    if (composeTrace == null) {
        throw exception
    }
    val message = composeTrace.stackTraceToString()

    val frameString =
        message
            .substringAfter("Composition stack when thrown:\n")
            .lines()
            .filter { it.isNotEmpty() }
            .map {
                val trace = it.removePrefix("\tat ")
                // Only keep the lines in the test file
                if (trace.contains(TestFile)) {
                    trace
                } else if (trace.startsWith("$\$compose")) {
                    // Group key stack traces
                    val groupKey = trace.substringBefore('(').takeLastWhile { it != '$' }
                    assertNotNull(groupKey.toIntOrNull(), "Invalid group key: $groupKey")
                    // Remove key values for test stability
                    if (!DebugKeepLineNumbers) {
                        trace.replace(groupKey, "<group-key>")
                    } else {
                        trace
                    }
                } else {
                    val line = trace.substringAfter(':').substringBefore(')')
                    if (line == "<unknown line>" || DebugKeepLineNumbers) {
                        trace
                    } else {
                        trace.replace(line, "<line number>")
                    }
                }
            }
            .joinToString(",\n") { "\"$it\"" }

    val expectedString = expected.joinToString(",\n") { "\"$it\"" }
    assertEquals(expectedString, frameString)
}

private fun groupKeyTrace(stackFrameCount: Int): List<String> =
    List(stackFrameCount) { "$\$compose.m$<group-key>(SourceFile:1)" }
