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
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

/**
 * Hit testing must skip components that are not drawn. Reproduces the StateLayout tab bug: a
 * non-selected pane is marked GONE but keeps its last layout bounds, and being the last child it
 * used to win the hit test over the visible pane underneath.
 */
class HitPathTrackerVisibilityTest {

    private val context = PointerInputTestContext()
    private val router = PointerInputRouter()

    @Test
    fun `gone component on top is not hit`() {
        // Arrange - Root(100x300) > [bottom Box(100x50, clickable), top Box(100x50, clickable)].
        // The two boxes overlap.
        val bottomClick = CountingAction()
        val topClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(ClickModifierOperation2().apply { mList.add(bottomClick) })
                    },
                )
                addChild(
                    context,
                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(ClickModifierOperation2().apply { mList.add(topClick) })
                        mVisibility = Component.Visibility.GONE
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
        assertThat(bottomClick.count).isEqualTo(1)
        assertThat(topClick.count).isEqualTo(0)
    }

    @Test
    fun `invisible component on top is not hit`() {
        // Arrange - Root(100x300) > [bottom Box(100x50, clickable), top Box(100x50, clickable)].
        // The two boxes overlap.
        val bottomClick = CountingAction()
        val topClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(ClickModifierOperation2().apply { mList.add(bottomClick) })
                    },
                )
                addChild(
                    context,
                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(ClickModifierOperation2().apply { mList.add(topClick) })
                        mVisibility = Component.Visibility.INVISIBLE
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
        assertThat(bottomClick.count).isEqualTo(1)
        assertThat(topClick.count).isEqualTo(0)
    }

    @Test
    fun `visible component on top is hit`() {
        // Arrange - Root(100x300) > [bottom Box(100x50, clickable), top Box(100x50, clickable)].
        // The two boxes overlap.
        val bottomClick = CountingAction()
        val topClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(ClickModifierOperation2().apply { mList.add(bottomClick) })
                    },
                )
                addChild(
                    context,
                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(ClickModifierOperation2().apply { mList.add(topClick) })
                        mVisibility = Component.Visibility.VISIBLE
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
        assertThat(topClick.count).isEqualTo(1)
        assertThat(bottomClick.count).isEqualTo(0)
    }

    @Test
    fun `pointer over nothing visible is neither dispatched nor consumed`() {
        // Arrange - Root(100x300) > [bottom Box(100x50, clickable), top Box(100x50, clickable)].
        // The two boxes overlap.
        val bottomClick = CountingAction()
        val topClick = CountingAction()
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    BoxLayout(null, 1, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(ClickModifierOperation2().apply { mList.add(bottomClick) })
                    },
                )
                addChild(
                    context,
                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                        mList.add(ClickModifierOperation2().apply { mList.add(topClick) })
                        mVisibility = Component.Visibility.GONE
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }

        // Act - The pointer is below both boxes.
        val status =
            router.routeEvent(
                context,
                document,
                pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 200f),
            )

        // Assert.
        assertThat(status and (POINTER_STATUS_DISPATCHED or POINTER_STATUS_CONSUMED)).isEqualTo(0)
    }
}
