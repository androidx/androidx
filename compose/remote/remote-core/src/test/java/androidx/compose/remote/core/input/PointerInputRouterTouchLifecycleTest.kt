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
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_CANCEL
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_DOWN
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_MOVE
import androidx.compose.remote.core.input.PointerInputRouter.POINTER_SUBTYPE_UP
import androidx.compose.remote.core.operations.CountingAction
import androidx.compose.remote.core.operations.PointerInputTestContext
import androidx.compose.remote.core.operations.TouchExpressionOperation2
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.LayoutComponentContent
import androidx.compose.remote.core.operations.layout.RootLayoutComponent
import androidx.compose.remote.core.operations.layout.TouchCancelModifierOperation2
import androidx.compose.remote.core.operations.layout.TouchDownModifierOperation2
import androidx.compose.remote.core.operations.layout.TouchUpModifierOperation2
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation2
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

/**
 * The touch lifecycle trio (down / up / cancel) on a box inside a vertically scrollable column,
 * routed through the real [PointerInputRouter]: a tap ends with up, a drag that becomes a scroll
 * ends with cancel, and a press never gets both.
 */
class PointerInputRouterTouchLifecycleTest {

    private val context = PointerInputTestContext()
    private val router = PointerInputRouter()

    @Test
    fun `tap runs down then up`() {
        // Arrange - Root(100x300) > Column(100x300, vertical scroll) >
        // Box(100x50, [touchDown, touchUp, touchCancel]).
        val down = CountingAction()
        val up = CountingAction()
        val cancel = CountingAction()
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
                                        mList.add(
                                            TouchDownModifierOperation2().apply { mList.add(down) }
                                        )
                                        mList.add(
                                            TouchUpModifierOperation2().apply { mList.add(up) }
                                        )
                                        mList.add(
                                            TouchCancelModifierOperation2().apply {
                                                mList.add(cancel)
                                            }
                                        )
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
        // Within the touch slop.
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_MOVE, x = 50f, y = 22f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 22f))

        // Assert.
        assertThat(down.count).isEqualTo(1)
        assertThat(up.count).isEqualTo(1)
        assertThat(cancel.count).isEqualTo(0)
    }

    @Test
    fun `drag that becomes a scroll runs down then cancel, never up`() {
        // Arrange - Root(100x300) > Column(100x300, vertical scroll) >
        // Box(100x50, [touchDown, touchUp, touchCancel]).
        val down = CountingAction()
        val up = CountingAction()
        val cancel = CountingAction()
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
                                        mList.add(
                                            TouchDownModifierOperation2().apply { mList.add(down) }
                                        )
                                        mList.add(
                                            TouchUpModifierOperation2().apply { mList.add(up) }
                                        )
                                        mList.add(
                                            TouchCancelModifierOperation2().apply {
                                                mList.add(cancel)
                                            }
                                        )
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
        // Beyond the touch slop: the scroll takes over.
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_MOVE, x = 50f, y = 10f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_MOVE, x = 50f, y = 0f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 0f))

        // Assert.
        assertThat(scroll.isScrolling).isFalse() // released
        assertThat(down.count).isEqualTo(1)
        assertThat(cancel.count).isEqualTo(1)
        assertThat(up.count).isEqualTo(0)
    }

    @Test
    fun `platform cancel runs down then cancel`() {
        // Arrange - Root(100x300) > Column(100x300, vertical scroll) >
        // Box(100x50, [touchDown, touchUp, touchCancel]).
        val down = CountingAction()
        val up = CountingAction()
        val cancel = CountingAction()
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
                                        mList.add(
                                            TouchDownModifierOperation2().apply { mList.add(down) }
                                        )
                                        mList.add(
                                            TouchUpModifierOperation2().apply { mList.add(up) }
                                        )
                                        mList.add(
                                            TouchCancelModifierOperation2().apply {
                                                mList.add(cancel)
                                            }
                                        )
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
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_CANCEL, x = 50f, y = 25f))

        // Assert.
        assertThat(down.count).isEqualTo(1)
        assertThat(cancel.count).isEqualTo(1)
        assertThat(up.count).isEqualTo(0)
    }

    @Test
    fun `a second press after a cancelled one works normally`() {
        // Arrange - Root(100x300) > Column(100x300, vertical scroll) >
        // Box(100x50, [touchDown, touchUp, touchCancel]).
        val down = CountingAction()
        val up = CountingAction()
        val cancel = CountingAction()
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
                                        mList.add(
                                            TouchDownModifierOperation2().apply { mList.add(down) }
                                        )
                                        mList.add(
                                            TouchUpModifierOperation2().apply { mList.add(up) }
                                        )
                                        mList.add(
                                            TouchCancelModifierOperation2().apply {
                                                mList.add(cancel)
                                            }
                                        )
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
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_MOVE, x = 50f, y = 0f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 0f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_DOWN, x = 50f, y = 25f))
        router.routeEvent(context, document, pointerEvent(POINTER_SUBTYPE_UP, x = 50f, y = 25f))

        // Assert.
        assertThat(down.count).isEqualTo(2)
        assertThat(cancel.count).isEqualTo(1)
        assertThat(up.count).isEqualTo(1)
    }

    companion object {
        private const val POSITION_ID = 100
    }
}
