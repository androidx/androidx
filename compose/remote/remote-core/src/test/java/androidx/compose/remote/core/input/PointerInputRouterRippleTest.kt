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
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_MOVE
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP
import androidx.compose.remote.core.operations.CountingAction
import androidx.compose.remote.core.operations.PointerInputTestContext
import androidx.compose.remote.core.operations.layout.ClickModifierOperation2
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.modifiers.RippleModifierOperation2
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

/** End-to-end routing of [RippleModifierOperation2] through the real [PointerInputRouter]. */
class PointerInputRouterRippleTest {

    private val context = PointerInputTestContext()
    private val router = PointerInputRouter()

    @Test
    fun `standalone ripple follows the press and never consumes`() {
        // Arrange - Root(100x100) > outer Box(100x100, [ripple]) > inner Box(50x50).
        val ripple = RippleModifierOperation2()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(ripple)
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
        val pressedAfterDown = ripple.isRippleActive && ripple.isPointerDown
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 25f, y = 25f))

        // Assert.
        assertThat(down and POINTER_STATUS_DISPATCHED).isEqualTo(POINTER_STATUS_DISPATCHED)
        assertThat(down and POINTER_STATUS_CONSUMED).isEqualTo(0)
        assertThat(pressedAfterDown).isTrue()
        // Released: still fading, but no longer pressed.
        assertThat(ripple.isRippleActive).isTrue()
        assertThat(ripple.isPointerDown).isFalse()
    }

    @Test
    fun `ripple and click on the same component both fire`() {
        // Arrange - Root(100x100) > outer Box(100x100, [ripple, click]) > inner Box(50x50).
        val ripple = RippleModifierOperation2()
        val outerClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(ripple)
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
        val pressedAfterDown = ripple.isPointerDown
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 25f, y = 25f))

        // Assert - The click modifier consumed the down; the ripple still follows the press.
        assertThat(pressedAfterDown).isTrue()
        assertThat(outerClick.count).isEqualTo(1)
        assertThat(ripple.isRippleActive).isTrue()
        assertThat(ripple.isPointerDown).isFalse()
    }

    @Test
    fun `pressing a clickable child also ripples the parent`() {
        // Arrange - Root(100x100) > outer Box(100x100, [ripple]) > inner Box(50x50, [click]).
        val ripple = RippleModifierOperation2()
        val innerClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(ripple)
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
        val pressedAfterDown = ripple.isPointerDown
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 25f, y = 25f))

        // Assert.
        assertThat(pressedAfterDown).isTrue()
        assertThat(innerClick.count).isEqualTo(1)
        assertThat(ripple.isPointerDown).isFalse()
    }

    @Test
    fun `nested ripples both fire`() {
        // Arrange - Root(100x100) > outer Box(100x100, [ripple]) > inner Box(50x50, [ripple]).
        val ripple = RippleModifierOperation2()
        val innerRipple = RippleModifierOperation2()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(ripple)
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 50f, 50f, 0, 0).apply {
                                        mList.add(innerRipple)
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

        // Assert.
        assertThat(ripple.isPointerDown).isTrue()
        assertThat(innerRipple.isPointerDown).isTrue()
    }

    @Test
    fun `pressing the parent outside the child ripples the parent`() {
        // Arrange - Root(100x100) > outer Box(100x100, [ripple]) > inner Box(50x50, [click]).
        val ripple = RippleModifierOperation2()
        val innerClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(ripple)
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

        // Act - The pointer is outside the inner box.
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 90f, y = 90f))

        // Assert.
        assertThat(ripple.isRippleActive).isTrue()
    }

    @Test
    fun `move outside the component releases the press`() {
        // Arrange - Root(100x100) > outer Box(100x100, [ripple]) > inner Box(50x50).
        val ripple = RippleModifierOperation2()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 100f, 0, 0).apply {
                        mList.add(ripple)
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
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_MOVE, x = 500f, y = 500f))

        // Assert.
        assertThat(ripple.isPointerDown).isFalse()
    }
}
