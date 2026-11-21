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
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP
import androidx.compose.remote.core.operations.CountingAction
import androidx.compose.remote.core.operations.FakeClock
import androidx.compose.remote.core.operations.PointerInputTestContext
import androidx.compose.remote.core.operations.layout.MultiClickModifierOperation2
import androidx.compose.remote.core.operations.layout.MultiClickModifierOperation2.CLICK_TYPE_DOUBLE
import androidx.compose.remote.core.operations.layout.MultiClickModifierOperation2.CLICK_TYPE_LONG
import androidx.compose.remote.core.operations.layout.MultiClickModifierOperation2.CLICK_TYPE_SINGLE
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

/**
 * End-to-end routing of the `combinedClickable` shape — a `SINGLE`, a `LONG` and a `DOUBLE`
 * [MultiClickModifierOperation2] stacked on one box in that order — through the real
 * [PointerInputRouter], checking that they coordinate through consumption.
 */
class PointerInputRouterMultiClickTest {

    private val clock = FakeClock()
    private val context = PointerInputTestContext(clock)
    private val router = PointerInputRouter()

    @Test
    fun `tap fires only the single click`() {
        // Arrange - Root(100x100) > Box(100x50) with [SINGLE, LONG, DOUBLE].
        val singleClick = CountingAction()
        val longClick = CountingAction()
        val doubleClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(
                            MultiClickModifierOperation2(CLICK_TYPE_SINGLE).apply {
                                mList.add(singleClick)
                            }
                        )
                        mList.add(
                            MultiClickModifierOperation2(CLICK_TYPE_LONG).apply {
                                mList.add(longClick)
                            }
                        )
                        mList.add(
                            MultiClickModifierOperation2(CLICK_TYPE_DOUBLE).apply {
                                mList.add(doubleClick)
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
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 25f))

        // Assert.
        assertThat(singleClick.count).isEqualTo(1)
        assertThat(longClick.count).isEqualTo(0)
        assertThat(doubleClick.count).isEqualTo(0)
    }

    @Test
    fun `double tap fires the single click once and the double click once`() {
        // Arrange - Root(100x100) > Box(100x50) with [SINGLE, LONG, DOUBLE].
        val singleClick = CountingAction()
        val longClick = CountingAction()
        val doubleClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(
                            MultiClickModifierOperation2(CLICK_TYPE_SINGLE).apply {
                                mList.add(singleClick)
                            }
                        )
                        mList.add(
                            MultiClickModifierOperation2(CLICK_TYPE_LONG).apply {
                                mList.add(longClick)
                            }
                        )
                        mList.add(
                            MultiClickModifierOperation2(CLICK_TYPE_DOUBLE).apply {
                                mList.add(doubleClick)
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
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 25f))
        clock.advance(context.doubleTapTimeoutMillis / 2)
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 25f))

        // Assert.
        assertThat(singleClick.count).isEqualTo(1)
        assertThat(longClick.count).isEqualTo(0)
        assertThat(doubleClick.count).isEqualTo(1)
    }

    @Test
    fun `two slow taps fire the single click twice`() {
        // Arrange - Root(100x100) > Box(100x50) with [SINGLE, LONG, DOUBLE].
        val singleClick = CountingAction()
        val longClick = CountingAction()
        val doubleClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(
                            MultiClickModifierOperation2(CLICK_TYPE_SINGLE).apply {
                                mList.add(singleClick)
                            }
                        )
                        mList.add(
                            MultiClickModifierOperation2(CLICK_TYPE_LONG).apply {
                                mList.add(longClick)
                            }
                        )
                        mList.add(
                            MultiClickModifierOperation2(CLICK_TYPE_DOUBLE).apply {
                                mList.add(doubleClick)
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
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 25f))
        clock.advance(context.doubleTapTimeoutMillis + 1)
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 25f))

        // Assert.
        assertThat(singleClick.count).isEqualTo(2)
        assertThat(doubleClick.count).isEqualTo(0)
    }

    @Test
    fun `without a double click modifier every tap is a single click`() {
        // Arrange - Root(100x100) > Box(100x50) with [SINGLE, LONG].
        val singleClick = CountingAction()
        val longClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 100f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(
                            MultiClickModifierOperation2(CLICK_TYPE_SINGLE).apply {
                                mList.add(singleClick)
                            }
                        )
                        mList.add(
                            MultiClickModifierOperation2(CLICK_TYPE_LONG).apply {
                                mList.add(longClick)
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
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 25f))
        clock.advance(context.doubleTapTimeoutMillis / 2)
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 25f))

        // Assert.
        assertThat(singleClick.count).isEqualTo(2)
    }
}
