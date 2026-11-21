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
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_CONSUMED
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_STATUS_DISPATCHED
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_MOVE
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP
import androidx.compose.remote.core.operations.PointerInputTestContext
import androidx.compose.remote.core.operations.TouchExpressionOperation2
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.ClickModifierOperation2
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation2
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

/**
 * End-to-end routing of a clickable box inside a vertically scrollable column through the real
 * [PointerInputRouter], exercising the main-pass scroll detection and the final-pass click
 * cancellation together.
 */
class PointerInputRouterScrollTest {

    private val context = PointerInputTestContext()
    private val router = PointerInputRouter()

    @Test
    fun `tap on a button inside a scroll column clicks`() {
        // Arrange - Root(100x300) > Column(100x300, vertical scroll) > Box(100x50, clickable).
        val touchExpression =
            TouchExpressionOperation2(
                /* id = */ POSITION_ID,
                /* exp = */ floatArrayOf(
                    RemoteContext.FLOAT_TOUCH_POS_Y,
                    -1f,
                    AnimatedFloatExpression.MUL,
                ),
                /* defValue = */ 0f,
                /* min = */ 0f,
                /* max = */ 1000f,
                /* touchEffects = */ 0,
                /* velocityId = */ 0f,
                /* stopMode = */ TouchExpressionOperation2.STOP_GENTLY,
                /* stopSpec = */ null,
                /* easingSpec = */ null,
            )
        val scroll =
            ScrollModifierOperation2(0, Utils.asNan(POSITION_ID), 1000f, 0f).apply {
                list.add(touchExpression)
            }
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    ColumnLayout(null, 1, 0, 0f, 0f, 100f, 300f, 0, 0, 0f).apply {
                        mList.add(scroll)
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                                        mList.add(ClickModifierOperation2())
                                    },
                                )
                            }
                        )
                        scroll.setVerticalScrollDimension(300f, 1000f)
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        touchExpression.updateVariables(context)
        touchExpression.apply(context)

        // Act.
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 25f))

        // Assert.
        assertThat(context.hapticEffects).isEqualTo(1)
        assertThat(scroll.isScrolling).isFalse()
    }

    @Test
    fun `drag within touch slop still clicks and does not scroll`() {
        // Arrange - Root(100x300) > Column(100x300, vertical scroll) > Box(100x50, clickable).
        val touchExpression =
            TouchExpressionOperation2(
                /* id = */ POSITION_ID,
                /* exp = */ floatArrayOf(
                    RemoteContext.FLOAT_TOUCH_POS_Y,
                    -1f,
                    AnimatedFloatExpression.MUL,
                ),
                /* defValue = */ 0f,
                /* min = */ 0f,
                /* max = */ 1000f,
                /* touchEffects = */ 0,
                /* velocityId = */ 0f,
                /* stopMode = */ TouchExpressionOperation2.STOP_GENTLY,
                /* stopSpec = */ null,
                /* easingSpec = */ null,
            )
        val scroll =
            ScrollModifierOperation2(0, Utils.asNan(POSITION_ID), 1000f, 0f).apply {
                list.add(touchExpression)
            }
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    ColumnLayout(null, 1, 0, 0f, 0f, 100f, 300f, 0, 0, 0f).apply {
                        mList.add(scroll)
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                                        mList.add(ClickModifierOperation2())
                                    },
                                )
                            }
                        )
                        scroll.setVerticalScrollDimension(300f, 1000f)
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        touchExpression.updateVariables(context)
        touchExpression.apply(context)

        // Act.
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_MOVE, x = 50f, y = 20f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 20f))

        // Assert.
        assertThat(context.hapticEffects).isEqualTo(1)
        assertThat(context.getFloat(POSITION_ID)).isEqualTo(0f)
    }

    @Test
    fun `drag beyond touch slop scrolls and cancels the click`() {
        // Arrange - Root(100x300) > Column(100x300, vertical scroll) > Box(100x50, clickable).
        val touchExpression =
            TouchExpressionOperation2(
                /* id = */ POSITION_ID,
                /* exp = */ floatArrayOf(
                    RemoteContext.FLOAT_TOUCH_POS_Y,
                    -1f,
                    AnimatedFloatExpression.MUL,
                ),
                /* defValue = */ 0f,
                /* min = */ 0f,
                /* max = */ 1000f,
                /* touchEffects = */ 0,
                /* velocityId = */ 0f,
                /* stopMode = */ TouchExpressionOperation2.STOP_GENTLY,
                /* stopSpec = */ null,
                /* easingSpec = */ null,
            )
        val scroll =
            ScrollModifierOperation2(0, Utils.asNan(POSITION_ID), 1000f, 0f).apply {
                list.add(touchExpression)
            }
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    ColumnLayout(null, 1, 0, 0f, 0f, 100f, 300f, 0, 0, 0f).apply {
                        mList.add(scroll)
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 50f, 0, 0).apply {
                                        mList.add(ClickModifierOperation2())
                                    },
                                )
                            }
                        )
                        scroll.setVerticalScrollDimension(300f, 1000f)
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        touchExpression.updateVariables(context)
        touchExpression.apply(context)

        // Act.
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        // 15px is more than the touch slop.
        val slopMove =
            router.routeEvent(
                context,
                document,
                pointerEvent(POINTER_SUBTYPE_MOVE, x = 50f, y = 10f),
            )
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_MOVE, x = 50f, y = 0f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 0f))

        // Assert.
        assertThat(slopMove and POINTER_STATUS_CONSUMED).isEqualTo(POINTER_STATUS_CONSUMED)
        assertThat(context.hapticEffects).isEqualTo(0)
        // Re-anchored at y=10, then dragged up by 10px: content scrolled down by 10px.
        assertThat(context.getFloat(POSITION_ID)).isEqualTo(10f)
    }

    @Test
    fun `down over a non clickable scroll area is dispatched but not consumed`() {
        // Arrange - Root(100x300) > Column(100x300, vertical scroll) > Box(100x50).
        val touchExpression =
            TouchExpressionOperation2(
                /* id = */ POSITION_ID,
                /* exp = */ floatArrayOf(
                    RemoteContext.FLOAT_TOUCH_POS_Y,
                    -1f,
                    AnimatedFloatExpression.MUL,
                ),
                /* defValue = */ 0f,
                /* min = */ 0f,
                /* max = */ 1000f,
                /* touchEffects = */ 0,
                /* velocityId = */ 0f,
                /* stopMode = */ TouchExpressionOperation2.STOP_GENTLY,
                /* stopSpec = */ null,
                /* easingSpec = */ null,
            )
        val scroll =
            ScrollModifierOperation2(0, Utils.asNan(POSITION_ID), 1000f, 0f).apply {
                list.add(touchExpression)
            }
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    ColumnLayout(null, 1, 0, 0f, 0f, 100f, 300f, 0, 0, 0f).apply {
                        mList.add(scroll)
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 50f, 0, 0),
                                )
                            }
                        )
                        scroll.setVerticalScrollDimension(300f, 1000f)
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        touchExpression.updateVariables(context)
        touchExpression.apply(context)

        // Act.
        val status =
            router.routeEvent(
                context,
                document,
                pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f),
            )

        // Assert.
        assertThat(status and POINTER_STATUS_CONSUMED).isEqualTo(0)
        assertThat(status and POINTER_STATUS_DISPATCHED).isEqualTo(POINTER_STATUS_DISPATCHED)
    }

    @Test
    fun `scroll works without a clickable child`() {
        // Arrange - Root(100x300) > Column(100x300, vertical scroll) > Box(100x50).
        val touchExpression =
            TouchExpressionOperation2(
                /* id = */ POSITION_ID,
                /* exp = */ floatArrayOf(
                    RemoteContext.FLOAT_TOUCH_POS_Y,
                    -1f,
                    AnimatedFloatExpression.MUL,
                ),
                /* defValue = */ 0f,
                /* min = */ 0f,
                /* max = */ 1000f,
                /* touchEffects = */ 0,
                /* velocityId = */ 0f,
                /* stopMode = */ TouchExpressionOperation2.STOP_GENTLY,
                /* stopSpec = */ null,
                /* easingSpec = */ null,
            )
        val scroll =
            ScrollModifierOperation2(0, Utils.asNan(POSITION_ID), 1000f, 0f).apply {
                list.add(touchExpression)
            }
        val root =
            RootLayoutComponent(0, 0f, 0f, 100f, 300f, null, 0).apply {
                addChild(
                    context,
                    ColumnLayout(null, 1, 0, 0f, 0f, 100f, 300f, 0, 0, 0f).apply {
                        mList.add(scroll)
                        mList.add(
                            LayoutComponentContent(-1).apply {
                                addChild(
                                    context,
                                    BoxLayout(null, 2, 0, 0f, 0f, 100f, 50f, 0, 0),
                                )
                            }
                        )
                        scroll.setVerticalScrollDimension(300f, 1000f)
                    },
                )
            }
        val document =
            object : CoreDocument() {
                override fun getRootLayoutComponent(): RootLayoutComponent = root
            }
        touchExpression.updateVariables(context)
        touchExpression.apply(context)

        // Act.
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_MOVE, x = 50f, y = 10f))
        val status =
            router.routeEvent(
                context,
                document,
                pointerEvent(POINTER_SUBTYPE_MOVE, x = 50f, y = 0f),
            )

        // Assert.
        assertThat(status and POINTER_STATUS_CONSUMED).isEqualTo(POINTER_STATUS_CONSUMED)
        assertThat(context.getFloat(POSITION_ID)).isEqualTo(10f)
    }

    companion object {
        private const val POSITION_ID = 100
    }
}
