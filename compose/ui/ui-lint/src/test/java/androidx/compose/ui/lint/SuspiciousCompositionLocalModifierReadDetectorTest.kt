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

package androidx.compose.ui.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.ui.lint.SuspiciousCompositionLocalModifierReadDetector.Companion.SuspiciousCompositionLocalModifierRead
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SuspiciousCompositionLocalModifierReadDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = SuspiciousCompositionLocalModifierReadDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(SuspiciousCompositionLocalModifierRead)

    private val CompositionLocalConsumerModifierStub =
        kotlin(
            """
            package androidx.compose.ui.node

            import androidx.compose.runtime.CompositionLocal
            import java.lang.RuntimeException

            interface CompositionLocalConsumerModifierNode

            fun <T> CompositionLocalConsumerModifierNode.currentValueOf(
                local: CompositionLocal<T>
            ): T {
                throw RuntimeException("Not implemented in lint stubs.")
            }
        """
        )

    private val ModifierNodeStub =
        kotlin(
            """
        package androidx.compose.ui

        interface Modifier {
            class Node {
                open fun onAttach() {}
                open fun onDestroy() {}
            }
        }
        """
        )

    @Test
    fun testCompositionLocalReadInModifierAttachAndDetach() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
                import androidx.compose.ui.node.currentValueOf
                import androidx.compose.runtime.CompositionLocal
                import androidx.compose.runtime.compositionLocalOf
                import androidx.compose.runtime.staticCompositionLocalOf

                val staticLocalInt = staticCompositionLocalOf { 0 }
                val localInt = compositionLocalOf { 0 }

                class NodeUnderTest : Modifier.Node(), CompositionLocalConsumerModifierNode {
                    override fun onAttach() {
                        val readValue = currentValueOf(localInt)
                    }

                    override fun onDetach() {
                        val readValue = currentValueOf(staticLocalInt)
                    }
                }
            """
                ),
                Stubs.CompositionLocal,
                CompositionLocalConsumerModifierStub,
                ModifierNodeStub
            )
            .run()
            .expect(
                """
src/test/NodeUnderTest.kt:16: Error: Reading localInt in onAttach will only access the CompositionLocal's value when the modifier is attached. To be notified of the latest value of the CompositionLocal, read the value in one of the modifier's other callbacks. [SuspiciousCompositionLocalModifierRead]
                        val readValue = currentValueOf(localInt)
                                        ~~~~~~~~~~~~~~~~~~~~~~~~
src/test/NodeUnderTest.kt:20: Error: Reading staticLocalInt in onDetach will only access the CompositionLocal's value when the modifier is detached. To be notified of the latest value of the CompositionLocal, read the value in one of the modifier's other callbacks. [SuspiciousCompositionLocalModifierRead]
                        val readValue = currentValueOf(staticLocalInt)
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
                """
            )
    }

    @Test
    fun testCompositionLocalReadInAttachDetachLambdaNotReported() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
                import androidx.compose.ui.node.currentValueOf
                import androidx.compose.runtime.CompositionLocal
                import androidx.compose.runtime.compositionLocalOf
                import androidx.compose.runtime.staticCompositionLocalOf

                val staticLocalInt = staticCompositionLocalOf { 0 }
                val localInt = compositionLocalOf { 0 }

                class NodeUnderTest : Modifier.Node(), CompositionLocalConsumerModifierNode {
                    override fun onAttach() {
                        func { val readValue = currentValueOf(localInt) }
                    }

                    override fun onDetach() {
                        func { val readValue = currentValueOf(staticLocalInt) }
                    }

                    private inline fun func(block: () -> Unit) {
                        block()
                    }
                }
            """
                ),
                Stubs.CompositionLocal,
                CompositionLocalConsumerModifierStub,
                ModifierNodeStub
            )
            .run()
            .expectClean()
    }

    @Test
    fun testCompositionLocalReadInModifierInitializer() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
                import androidx.compose.ui.node.currentValueOf
                import androidx.compose.runtime.CompositionLocal
                import androidx.compose.runtime.compositionLocalOf
                import androidx.compose.runtime.staticCompositionLocalOf

                val staticLocalInt = staticCompositionLocalOf { 0 }
                val localInt = compositionLocalOf { 0 }

                class NodeUnderTest : Modifier.Node(), CompositionLocalConsumerModifierNode {
                    init {
                        val readValue = currentValueOf(localInt)
                        val readValue = currentValueOf(staticLocalInt)
                    }
                }
            """
                ),
                Stubs.CompositionLocal,
                CompositionLocalConsumerModifierStub,
                ModifierNodeStub
            )
            .run()
            .expect(
                """
src/test/NodeUnderTest.kt:16: Error: CompositionLocals cannot be read in modifiers before the node is attached. [SuspiciousCompositionLocalModifierRead]
                        val readValue = currentValueOf(localInt)
                                        ~~~~~~~~~~~~~~~~~~~~~~~~
src/test/NodeUnderTest.kt:17: Error: CompositionLocals cannot be read in modifiers before the node is attached. [SuspiciousCompositionLocalModifierRead]
                        val readValue = currentValueOf(staticLocalInt)
                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
                """
            )
    }

    @Test
    fun testCompositionLocalReadInModifierComputedProperty() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
                import androidx.compose.ui.node.currentValueOf
                import androidx.compose.runtime.CompositionLocal
                import androidx.compose.runtime.compositionLocalOf
                import androidx.compose.runtime.staticCompositionLocalOf

                val staticLocalInt = staticCompositionLocalOf { 0 }
                val localInt = compositionLocalOf { 0 }

                class NodeUnderTest : Modifier.Node(), CompositionLocalConsumerModifierNode {
                    val readValue: Int get() = currentValueOf(localInt)
                    val readValue: Int get() = currentValueOf(staticLocalInt)
                }
            """
                ),
                Stubs.CompositionLocal,
                CompositionLocalConsumerModifierStub,
                ModifierNodeStub
            )
            .run()
            .expectClean()
    }

    @Test
    fun testCompositionLocalReadInLazyPropertyDelegate() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
                import androidx.compose.ui.node.currentValueOf
                import androidx.compose.runtime.CompositionLocal
                import androidx.compose.runtime.compositionLocalOf
                import androidx.compose.runtime.staticCompositionLocalOf

                val staticLocalInt = staticCompositionLocalOf { 0 }
                val localInt = compositionLocalOf { 0 }

                class NodeUnderTest : Modifier.Node(), CompositionLocalConsumerModifierNode {
                    val readValue by lazy { currentValueOf(localInt) }
                    val staticReadValue by lazy { currentValueOf(staticLocalInt) }
                }
            """
                ),
                Stubs.CompositionLocal,
                CompositionLocalConsumerModifierStub,
                ModifierNodeStub
            )
            .run()
            .expect(
                """
src/test/NodeUnderTest.kt:15: Error: Reading localInt lazily will only access the CompositionLocal's value once. To be notified of the latest value of the CompositionLocal, read the value in one of the modifier's callbacks. [SuspiciousCompositionLocalModifierRead]
                    val readValue by lazy { currentValueOf(localInt) }
                                            ~~~~~~~~~~~~~~~~~~~~~~~~
src/test/NodeUnderTest.kt:16: Error: Reading staticLocalInt lazily will only access the CompositionLocal's value once. To be notified of the latest value of the CompositionLocal, read the value in one of the modifier's callbacks. [SuspiciousCompositionLocalModifierRead]
                    val staticReadValue by lazy { currentValueOf(staticLocalInt) }
                                                  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2 errors, 0 warnings
                """
            )
    }

    @Test
    fun testCompositionLocalReadInArbitraryFunction() {
        lint()
            .files(
                kotlin(
                    """
                package test

                import androidx.compose.ui.Modifier
                import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
                import androidx.compose.ui.node.currentValueOf
                import androidx.compose.runtime.CompositionLocal
                import androidx.compose.runtime.compositionLocalOf
                import androidx.compose.runtime.staticCompositionLocalOf

                val staticLocalInt = staticCompositionLocalOf { 0 }
                val localInt = compositionLocalOf { 0 }

                class NodeUnderTest : Modifier.Node(), CompositionLocalConsumerModifierNode {
                    fun onDoSomethingElse() {
                        val readValue = currentValueOf(localInt)
                        val readStaticValue = currentValueOf(staticLocalInt)
                    }
                }
            """
                ),
                Stubs.CompositionLocal,
                CompositionLocalConsumerModifierStub,
                ModifierNodeStub
            )
            .run()
            .expectClean()
    }
}
