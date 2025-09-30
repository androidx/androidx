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

package androidx.compose.remote.core

import androidx.compose.remote.core.layout.ApplyTouchDown
import androidx.compose.remote.core.layout.CaptureComponentTree
import androidx.compose.remote.core.layout.Color
import androidx.compose.remote.core.layout.LayoutTestPlayer
import androidx.compose.remote.core.layout.TestComponentVisibility
import androidx.compose.remote.core.layout.TestOperation
import androidx.compose.remote.core.layout.TestParameters
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.actions.ValueIntegerChange
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class LayoutTest : LayoutTestPlayer() {
    val GENERATE_GOLD_FILES: Boolean = false
    var platform: Platform = Platform.None

    @Rule @JvmField var name = TestName()

    private fun checkLayout(w: Int, h: Int, description: String, ops: ArrayList<TestOperation?>) {
        if (ops.size == 0) {
            return
        }
        if (ops[0] !is TestLayout) {
            return
        }
        val function = (ops[0] as TestLayout).layout
        val testParameters = TestParameters(name.getMethodName(), GENERATE_GOLD_FILES)
        val writer =
            RemoteComposeContext(w, h, description, platform, { root { function.invoke(this) } })
                .writer
        play(writer, ops, testParameters)
    }

    data class TestLayout(var layout: RemoteComposeContext.() -> Unit) : TestOperation() {
        override fun apply(
            context: RemoteContext,
            document: CoreDocument,
            testParameters: TestParameters,
            commands: List<Map<String?, Any?>?>?,
        ): Boolean {
            // Nothing here
            return false
        }
    }

    @Test
    fun testTouchDownVisibilityChange() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    val visibilityId = writer.addInteger(Component.Visibility.GONE)
                    row(
                        Modifier.componentId(1).fillMaxHeight().background(Color.YELLOW).padding(8),
                        horizontal = RowLayout.CENTER,
                    ) {
                        box(
                            Modifier.componentId(2)
                                .size(200)
                                .visibility(visibilityId.toInt())
                                .background(Color.GREEN)
                        )
                        box(
                            Modifier.componentId(3)
                                .size(100)
                                .background(Color.RED)
                                .onTouchDown(
                                    ValueIntegerChange(
                                        visibilityId.toInt(),
                                        Component.Visibility.VISIBLE,
                                    )
                                )
                        )
                    }
                },
                TestComponentVisibility(2, Component.Visibility.GONE),
                ApplyTouchDown(50f, 50f),
                TestComponentVisibility(2, Component.Visibility.VISIBLE),
                CaptureComponentTree(),
            )
        checkLayout(1000, 1000, "Layout", ops)
    }
}
