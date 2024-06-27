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

package androidx.compose.ui.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ReturnFromAwaitPointerEventScopeDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ReturnFromAwaitPointerEventScopeDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(ReturnFromAwaitPointerEventScopeDetector.ExitAwaitPointerEventScope)

    private val ForEachGestureStub: TestFile =
        bytecodeStub(
            filename = "ForEachGesture.kt",
            filepath = "androidx/compose/foundation/gestures",
            checksum = 0x1be9b2ef,
            """
            package androidx.compose.foundation.gestures
            import androidx.compose.ui.input.pointer.PointerInputScope

            suspend fun PointerInputScope.forEachGesture(block: suspend PointerInputScope.() -> Unit) {
                block()
            }
            """,
            """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAA/2VMQQoCMRAbUQR7EOkDBMWThzl7F1dkL4J+oGzr7oDOlHYK
        Pt/KejOQEBISAJgCwKRyAT+Yg9k59knIv7GTV5Qc8CGFvVMSxj5kLSlku2ok
        nVw3nMegVXM0m79lISSORTEKsYZk17eSY2BP3F/H6PLtG3pWW0+WZqb10c7v
        VVvdwh4+IfeLY6cAAAA=
        """,
            """
        androidx/compose/foundation/gestures/ForEachGestureKt.class:
        H4sIAAAAAAAA/7VUzU8bRxT/zdr4C0jMBlIgjUOK2/ARsoaGNq2jqBEx7aqO
        QTGkqjhU4/XiDLZnrN1Zi944Veq/0UPPUU9tDxXKsX9U1Te2IYBpkCL1sLO/
        9zHv/d57M/P3P3/+BeAhigzrXNYDJeqHjqfaHRX6zr6KZJ1roaTT8EMdBX7o
        bKqgxL1XX/flb3USjCF7wLvcaXHZcLZqB75H2hjDtf1zvgw/L5SHckTCEbIT
        aaejhNR+4Gz3/65RVj3V8YvlptItIZ2DbtvZj6RnCBGRAVo7tXsqUJEWklhu
        KEkg6nEvLpYv0isyNvW/cHm88l5RV64o4fGpw64UuvikuDxc0pOr+jAU5LK+
        IF9WQcM58HUt4IJK41IqzftlVqJWi9daPrnNv8tNaeNJXrl3dyuJDENCyK5q
        0ul4tDDMZ1hzCekxjGF8FKO4xrA43ANqdkAUhRc67ik0BzfLcLPh642tF1u7
        O26l9EN1t7pdqjwrPWOYXLg0kY0bGUxgkmH0TC+TuMmQcivVnaeVjRLD+LlG
        j2EaM2l8gFkKm9evRJi/eDHW3+vUMIzUWsprMsxddUWIb95EbvlGPrPhv+4M
        w8SJy3Nfc3oFOOmsdjdGzwUzS9osYGBNAywyHgqDCoTqqwz7x0e5zPFRxspa
        GWva6sHpPrSyA+H4aLZIeNYqsCWrYK3dy8Zm51PMjtsk2Rk71UOsMGIn7Pg0
        KyQK8Te/JKxU8ps3P31lUDZlsq0xQ8Q+IXy27NyJsnSofZq8kifWnR97LZw4
        /6A9aGqG+Iaq01TGq5p7zee8s2MOPcP1MvWpErVrfjDQ2GXl8dZLHggjD5Tp
        qmhI3h/srRcRNbXtu7IrQkHmp2/vCd21i9ZtHvC2T1M+55apqijw/E1hos8M
        9rwciodVWIijP5UZjCBB0hpJHmIwk5laXrr/B67H8P3vmPoV8dff/YZbr8kQ
        w6e0JsC2k7TlIeEMhTKaSSSxTmiuHwAf4nYvwRRyuENpDJrDXfL+zBwC8v68
        Hwkp+j+i70aMhHSP09vVwhe9dRVf0n+TtB8R3/k9xFzkXXzs4hPcc7GARRdL
        WN4DC3EfK3tIhxgJ8SDE7RC5EE6Iuz2xECLxLyV8urHMBgAA
        """
        )

    private val stubs =
        arrayOf(
            Stubs.Composable,
            Stubs.Modifier,
            UiStubs.Density,
            UiStubs.PointerInputScope,
            UiStubs.PointerEvent,
            ForEachGestureStub,
            UiStubs.Alignment,
            CoroutineStubs.coroutineContextTestFile,
            CoroutineStubs.coroutineScopeTestFile
        )

    @Test
    fun awaitPointerEventScope_standalone_shouldNotWarn() {
        expectClean(
            """
                package test
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {

                        }
                    }
                }
            """
        )
    }

    // Current way to create a handler for the pointer input type
    @Test
    fun awaitPointerEventScope_assignedFromContainingPointerInputEventHandler_shouldNotWarn() {
        expectClean(
            """
                package test
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.input.pointer.PointerInputEventHandler

                @Composable
                fun SomeFunction() {
                        val blockNew = PointerInputEventHandler {
                            awaitPointerEventScope { }
                        }
                }
            """
        )
    }

    // Current way to create a handler for the pointer input type
    @Test
    fun awaitPointerEventScope_assignedFromContainingPointerInputEventHandlerAfter_shouldNotWarn() {
        expectClean(
            """
                package test
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.input.pointer.PointerInputEventHandler

                @Composable
                fun SomeFunction() {
                        val blockNew = PointerInputEventHandler {
                            awaitPointerEventScope { }
                            val something = "hello"
                        }
                }
            """
        )
    }

    // Current way to create a handler for the pointer input type with variables above
    // awaitPointerEventScope.
    @Test
    fun awaitPointerEventScope_assignedFromContainingPointerInputEventHandlerVars_shouldNotWarn() {
        expectClean(
            """
                package test
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.input.pointer.PointerInputEventHandler

                @Composable
                fun SomeFunction() {
                        val blockNew = PointerInputEventHandler {
                            var variable1 = 0
                            var variable2 = "hello"
                            var variable3 = 0.0
                            var variable4 = "hello2"
                            awaitPointerEventScope { }
                        }
                }
            """
        )
    }

    @Test
    fun awaitPointerEventScope_assignedFromContainingLambdaMethod_shouldNotWarn() {
        expectClean(
            """
                package test
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.input.pointer.PointerInputScope

                @Composable
                fun SomeFunction() {
                        val block: suspend PointerInputScope.() -> Unit = {
                            awaitPointerEventScope { }
                        }
                }
            """
        )
    }

    @Test
    fun awaitPointerEventScope_inCoroutineScopeStandaloneExtensionFunction_shouldNotWarn() {
        expectClean(
            """
                package test
                import androidx.compose.ui.input.pointer.PointerInputScope
                import kotlinx.coroutines.coroutineScope

                suspend fun PointerInputScope.detectMoves() = coroutineScope {
                    awaitPointerEventScope { }
                }
            """
        )
    }

    // Pointer input handler implicitly using PointerInputEventHandler for pointer input handler.
    @Test
    fun awaitPointerEventScope_standaloneExtensionFunction_shouldNotWarn() {
        expectClean(
            """
                package test
                import android.view.MotionEvent
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                fun Modifier.motionEventSpy(watcher: (motionEvent: MotionEvent) -> Unit): Modifier =
                    this.pointerInput(watcher) {
                        interceptOutOfBoundsChildEvents = true
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.motionEvent?.let(watcher)
                            }
                        }
                    }
            """
        )
    }

    @Test
    fun awaitPointerEventScope_insideForEach_shouldNotWarn() {
        expectClean(
            """
                package test
                import androidx.compose.foundation.gestures.forEachGesture
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    Modifier.pointerInput(Unit) {
                        forEachGesture {
                            awaitPointerEventScope {

                            }
                        }
                    }
                }
            """
        )
    }

    @Test
    fun awaitPointerEventScope_otherMethodName_shouldNotWarn() {
        expectClean(
            """
                package test

                fun <T> awaitPointerEventScope(block: () -> T): T {
                    return block()
                }
                fun TestComposable() {
                   val result = awaitPointerEventScope {
                        "Result"
                    }
                    println(result)
                }
            """
        )
    }

    @Test
    fun awaitPointerEventScope_assignedToVariable_shouldWarn() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    Modifier.pointerInput(Unit) {
                        while (true) {
                            val assigned = awaitPointerEventScope {

                            }
                        }
                    }
                }
            """
                ),
                *stubs,
            )
            .run()
            .expect(
                """
src/test/test.kt:12: $WarningMessage
                            val assigned = awaitPointerEventScope {
                                           ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                    """
                    .trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_returnedFromMethod_shouldWarn() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.input.pointer.PointerInputScope

                private suspend fun PointerInputScope.doSomethingInInputScope(): Any {
                    return awaitPointerEventScope {

                    }
                }
            """
                ),
                *stubs,
            )
            .run()
            .expect(
                """
src/test/test.kt:7: $WarningMessage
                    return awaitPointerEventScope {
                           ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
                    """
                    .trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_assignedFromLambdaMethod_shouldWarn() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.input.pointer.PointerInputScope

                private suspend fun PointerInputScope.doSomethingInInputScope(): Boolean {
                    val result = run {
                        awaitPointerEventScope {
                            true
                        }
                    }

                    return result
                }

                private suspend fun PointerInputScope.doSomethingInInputScope(nullable: String?): Boolean {
                    val result = nullable?.let {
                        awaitPointerEventScope {
                            true
                        }
                    } ?: false

                    return result
                }
            """
                ),
                *stubs,
            )
            .run()
            .expect(
                """
src/test/test.kt:8: $WarningMessage
                        awaitPointerEventScope {
                        ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:18: $WarningMessage
                        awaitPointerEventScope {
                        ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
                    """
                    .trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_returnedFromLambdaMethod_shouldWarn() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.input.pointer.PointerInputScope

                private suspend fun PointerInputScope.doSomethingInInputScope(): Boolean {
                    return run {
                        awaitPointerEventScope {
                            true
                        }
                    }
                }

                private suspend fun PointerInputScope.doSomethingInInputScope(nullable: String?): Boolean {
                    return nullable?.let {
                        awaitPointerEventScope {
                            true
                        }
                    } ?: false
                }
            """
                ),
                *stubs,
            )
            .skipTestModes(TestMode.BODY_REMOVAL)
            .run()
            .expect(
                """
src/test/test.kt:8: $WarningMessage
                        awaitPointerEventScope {
                        ~~~~~~~~~~~~~~~~~~~~~~
src/test/test.kt:16: $WarningMessage
                        awaitPointerEventScope {
                        ~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
                    """
                    .trimIndent()
            )
    }

    @Test
    fun awaitPointerEventScope_notAssignedToVariable_shouldNotWarn() {
        expectClean(
            """
                package test

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.input.pointer.pointerInput

                @Composable
                fun TestComposable() {
                    Modifier.pointerInput(Unit) {
                        while (true) {
                            awaitPointerEventScope {

                            }
                        }
                    }
                }
            """
        )
    }

    private fun expectClean(source: String) {
        lint().files(kotlin(source), *stubs).run().expectClean()
    }

    private val WarningMessage: String =
        "Warning: ${ReturnFromAwaitPointerEventScopeDetector.ErrorMessage} " +
            "[${ReturnFromAwaitPointerEventScopeDetector.IssueId}]"
}
