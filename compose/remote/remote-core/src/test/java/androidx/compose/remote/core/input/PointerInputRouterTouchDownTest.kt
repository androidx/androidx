/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core.input

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_CONSUMED
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_DISPATCHED
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP
import androidx.compose.remote.core.operations.CountingAction
import androidx.compose.remote.core.operations.PointerInputTestContext
import androidx.compose.remote.core.operations.layout.ClickModifierOperation2
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation2
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

/** End-to-end routing of [TouchDownModifierOperation2] through the real [PointerInputRouter]. */
class PointerInputRouterTouchDownTest {

    private val context = PointerInputTestContext()
    private val router = PointerInputRouter()

    @Test
    fun `standalone touch down runs on down, is dispatched but not consumed`() {
        // Arrange - Root(100x300) > outer Box(100x100, [touchDown]) > inner Box(50x50).
        val touchDown = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(TouchDownModifierOperation2().apply { mList.add(touchDown) })
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 50f, 50f, 0, 0),
                                )
                            }
                        )
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }

        // Act.
        val down =
            router.routeEvent(
                context,
                document,
                pointerEvent(POINTER_SUBTYPE_DOWN, x = 25f, y = 25f),
            )
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 25f, y = 25f))

        // Assert.
        assertThat(touchDown.count).isEqualTo(1)
        assertThat(down and POINTER_STATUS_DISPATCHED).isEqualTo(POINTER_STATUS_DISPATCHED)
        assertThat(down and POINTER_STATUS_CONSUMED).isEqualTo(0)
    }

    @Test
    fun `touch down next to a click on the same component both run`() {
        // Arrange - Root(100x300) > outer Box(100x100, [touchDown, click]) > inner Box(50x50).
        val touchDown = CountingAction()
        val outerClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(TouchDownModifierOperation2().apply { mList.add(touchDown) })
                        mList.add(ClickModifierOperation2().apply { mList.add(outerClick) })
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 50f, 50f, 0, 0),
                                )
                            }
                        )
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }

        // Act.
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 25f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 25f, y = 25f))

        // Assert.
        assertThat(touchDown.count).isEqualTo(1)
        assertThat(outerClick.count).isEqualTo(1)
    }

    @Test
    fun `touch down on a parent runs when a clickable child is pressed`() {
        // Arrange - Root(100x300) > outer Box(100x100, [touchDown]) > inner Box(50x50, [click]).
        val touchDown = CountingAction()
        val innerClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(TouchDownModifierOperation2().apply { mList.add(touchDown) })
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 50f, 50f, 0, 0).apply {
                                        mList.add(
                                            ClickModifierOperation2().apply {
                                                mList.add(innerClick)
                                            }
                                        )
                                    },
                                )
                            }
                        )
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }

        // Act.
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 25f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 25f, y = 25f))

        // Assert.
        assertThat(touchDown.count).isEqualTo(1)
        assertThat(innerClick.count).isEqualTo(1)
    }

    @Test
    fun `touch down does not run for a down outside the component`() {
        // Arrange - Root(100x300) > outer Box(100x100, [touchDown]) > inner Box(50x50).
        val touchDown = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(TouchDownModifierOperation2().apply { mList.add(touchDown) })
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 50f, 50f, 0, 0),
                                )
                            }
                        )
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }

        // Act - The pointer is below the outer box.
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 250f))

        // Assert.
        assertThat(touchDown.count).isEqualTo(0)
    }
}
