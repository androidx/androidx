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

package androidx.compose.ui.node

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.DrawerValue
import androidx.compose.material.ModalDrawer
import androidx.compose.material.rememberDrawerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SharePointerInputWithSiblingTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun Drawer_drawerContentSharePointerInput_cantClickContent() {
        var box1Clicked = false
        var box2Clicked = false

        rule.setContent {
            val drawerState = rememberDrawerState(DrawerValue.Open)

            ModalDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Box(
                        Modifier.fillMaxSize().testTag("box1").testPointerInput(
                            sharePointerInputWithSibling = true
                        ) {
                            box1Clicked = true
                        }
                    )
                },
                content = {
                    Box(
                        Modifier.fillMaxSize().testTag("box2").testPointerInput {
                            box2Clicked = true
                        }
                    )
                }
            )
        }

        rule.onNodeWithTag("box1").performClick()
        assertThat(box1Clicked).isTrue()
        assertThat(box2Clicked).isFalse()
    }

    @Test
    fun stackedBox_doSharePointer() {
        var box1Clicked = false
        var box2Clicked = false

        rule.setContent {
            Box(Modifier.size(50.dp)) {
                Box(Modifier.fillMaxSize().testTag("box1").testPointerInput { box1Clicked = true })

                Box(
                    Modifier.fillMaxSize().testTag("box2").testPointerInput(
                        sharePointerInputWithSibling = true
                    ) {
                        box2Clicked = true
                    }
                )
            }
        }

        rule.onNodeWithTag("box2").performClick()
        assertThat(box1Clicked).isTrue()
        assertThat(box2Clicked).isTrue()
    }

    @Test
    fun stackedBox_parentDisallowShare_doSharePointerWithSibling() {
        var box1Clicked = false
        var box2Clicked = false

        rule.setContent {
            Box(Modifier.size(50.dp).testPointerInput(sharePointerInputWithSibling = false)) {
                Box(Modifier.fillMaxSize().testTag("box1").testPointerInput { box1Clicked = true })

                Box(
                    Modifier.fillMaxSize().testTag("box2").testPointerInput(
                        sharePointerInputWithSibling = true
                    ) {
                        box2Clicked = true
                    }
                )
            }
        }

        rule.onNodeWithTag("box2").performClick()
        assertThat(box1Clicked).isTrue()
        assertThat(box2Clicked).isTrue()
    }

    @Test
    fun stackedBox_parentDisallowShare_notSharePointerWithCousin() {
        var box1Clicked = false
        var box2Clicked = false

        rule.setContent {
            Box(Modifier.size(50.dp)) {
                Box(Modifier.fillMaxSize().testTag("box1").testPointerInput { box1Clicked = true })

                Box(Modifier.fillMaxSize().testPointerInput(sharePointerInputWithSibling = false)) {
                    Box(
                        Modifier.fillMaxSize().testTag("box2").testPointerInput(
                            sharePointerInputWithSibling = true
                        ) {
                            box2Clicked = true
                        }
                    )
                }
            }
        }

        rule.onNodeWithTag("box2").performClick()
        assertThat(box1Clicked).isFalse()
        assertThat(box2Clicked).isTrue()
    }

    @Test
    fun stackedBox_doSharePointerWithSiblings_untilFirstBoxDisallowShare() {
        var box1Clicked = false
        var box2Clicked = false
        var box3Clicked = false

        rule.setContent {
            Box(Modifier.size(50.dp)) {
                Box(Modifier.fillMaxSize().testTag("box1").testPointerInput { box1Clicked = true })

                Box(
                    Modifier.fillMaxSize().testTag("box2").testPointerInput(
                        sharePointerInputWithSibling = false
                    ) {
                        box2Clicked = true
                    }
                )

                Box(
                    Modifier.fillMaxSize().testTag("box3").testPointerInput(
                        sharePointerInputWithSibling = true
                    ) {
                        box3Clicked = true
                    }
                )
            }
        }

        rule.onNodeWithTag("box3").performClick()
        assertThat(box1Clicked).isFalse()
        assertThat(box2Clicked).isTrue()
        assertThat(box3Clicked).isTrue()
    }
}

private fun Modifier.testPointerInput(
    sharePointerInputWithSibling: Boolean = false,
    onPointerEvent: () -> Unit = {}
): Modifier = this.then(TestPointerInputElement(sharePointerInputWithSibling, onPointerEvent))

private data class TestPointerInputElement(
    val sharePointerInputWithSibling: Boolean,
    val onPointerEvent: () -> Unit
) : ModifierNodeElement<TestPointerInputNode>() {
    override fun create(): TestPointerInputNode {
        return TestPointerInputNode(sharePointerInputWithSibling, onPointerEvent)
    }

    override fun update(node: TestPointerInputNode) {
        node.sharePointerInputWithSibling = sharePointerInputWithSibling
        node.onPointerEvent = onPointerEvent
    }
}

private class TestPointerInputNode(
    var sharePointerInputWithSibling: Boolean,
    var onPointerEvent: () -> Unit
) : Modifier.Node(), PointerInputModifierNode {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        onPointerEvent.invoke()
    }

    override fun onCancelPointerInput() {}

    override fun sharePointerInputWithSiblings(): Boolean {
        return sharePointerInputWithSibling
    }
}
