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

package androidx.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.rotary.RotaryScrollEvent
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.RootNodeOwner
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.InteropView

/**
 * Constructs a single-layer [ComposeScene] using the specified parameters. It utilizes
 * [ComposeSceneContext.createLayer] to position a new [LayoutNode] tree.
 *
 * After [ComposeScene] will no longer needed, you should call [ComposeScene.close] method, so
 * all resources and subscriptions will be properly closed. Otherwise, there can be a memory leak.
 *
 * @param frameRecomposer The host-owned recomposer and frame clock that drives this scene. The
 * scene launches its content effects on [FrameRecomposer.compositionContext]'s effect context, so
 * effects and animations are performed by the host's [FrameRecomposer.performFrame].
 * @param density Initial density of the content which will be used to convert [Dp] units.
 * @param layoutDirection Initial layout direction of the content.
 * @param size The size of the [ComposeScene]. Default value is `null`, which means the size will be
 * determined by the content.
 * @param composeSceneContext The context to share resources between multiple scenes and provide
 * a way for platform interaction.
 * @param invalidateLayout The function to be called when the content requires another
 * measure/layout pass.
 * @param invalidateDraw The function to be called when the content requires another draw pass.
 * @return The created [ComposeScene].
 *
 * @see ComposeScene
 */
@InternalComposeUiApi
fun PlatformLayersComposeScene(
    frameRecomposer: FrameRecomposer,
    density: Density = Density(1f),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    size: IntSize? = null,
    composeSceneContext: ComposeSceneContext = ComposeSceneContext.Empty(),
    invalidateLayout: () -> Unit = {},
    invalidateDraw: () -> Unit = {},
): ComposeScene = PlatformLayersComposeSceneImpl(
    frameRecomposer = frameRecomposer,
    density = density,
    layoutDirection = layoutDirection,
    size = size,
    composeSceneContext = composeSceneContext,
    invalidateLayout = invalidateLayout,
    invalidateDraw = invalidateDraw,
)

private class PlatformLayersComposeSceneImpl(
    frameRecomposer: FrameRecomposer,
    density: Density,
    layoutDirection: LayoutDirection,
    size: IntSize?,
    override val composeSceneContext: ComposeSceneContext,
    invalidateLayout: () -> Unit,
    invalidateDraw: () -> Unit,
) : BaseComposeScene(
    frameRecomposer = frameRecomposer,
    invalidateLayout = invalidateLayout,
    invalidateDraw = invalidateDraw,
) {
    private val mainOwner: RootNodeOwner by lazy {
        RootNodeOwner(
            density = density,
            layoutDirection = layoutDirection,
            coroutineContext = frameRecomposer.compositionContext.effectCoroutineContext,
            size = size,
            platformContext = composeSceneContext.platformContext,
            inputHandler = inputHandler,
            invalidate = ::invokeInvalidationCallbacks,
            onChangedExecutor = frameRecomposer::runOnComposeThread,
        )
    }

    override var density: Density = density
        set(value) {
            check(!isClosed) { "density set after ComposeScene is closed" }
            field = value
            mainOwner.density = value
        }

    override var layoutDirection: LayoutDirection = layoutDirection
        set(value) {
            check(!isClosed) { "layoutDirection set after ComposeScene is closed" }
            field = value
            mainOwner.layoutDirection = value
        }

    override var size: IntSize? = size
        set(value) {
            check(!isClosed) { "size set after ComposeScene is closed" }
            check(value == null || (value.width >= 0f && value.height >= 0)) {
                "Size of ComposeScene cannot be negative"
            }
            field = value
            mainOwner.size = value
        }

    override val focusManager = ComposeSceneFocusManager(
        focusOwner = { mainOwner.focusOwner },
        measureAndLayout = ::doMeasureAndLayout
    )

    // TODO: Extract to interface and return `mainOwner.dragAndDropOwner.rootDragAndDropNode`
    override val rootDragAndDropNode = ComposeSceneDragAndDropNode { mainOwner.dragAndDropOwner }

    init {
        onOwnerAppended(mainOwner)
    }

    override fun close() {
        check(!isClosed) { "close called after ComposeScene is already closed" }
        onOwnerRemoved(mainOwner)
        mainOwner.dispose()
        super.close()
    }

    override fun measureContent(constraints: Constraints): IntSize {
        return mainOwner.measureContentWithConstraints(constraints)
    }

    override fun invalidatePositionInWindow() {
        check(!isClosed) { "invalidatePositionInWindow called after ComposeScene is closed" }
        mainOwner.invalidatePositionInWindow()
    }

    override fun invalidatePositionOnScreen() {
        check(!isClosed) { "invalidatePositionOnScreen called after ComposeScene is closed" }
        mainOwner.invalidatePositionOnScreen()
    }

    override val hasPendingMeasureOrLayout: Boolean
        get() = hasForcedLayout || mainOwner.hasPendingMeasureOrLayout

    override val hasPendingDraw: Boolean
        get() = hasForcedDraw || mainOwner.hasPendingDraw

    override fun createComposition(
        parentCompositionContext: CompositionContext,
        content: @Composable () -> Unit,
    ): Composition = mainOwner.setContent(
        parent = parentCompositionContext,
        getCompositionLocalContext = { compositionLocalContext },
        content = content,
    )

    override fun hitTestInteropView(position: Offset): InteropView? {
        return mainOwner.hitTestInteropView(position)
    }

    override fun processPointerInputEvent(event: PointerInputEvent) =
        mainOwner.onPointerInput(event)

    override fun processCancelPointerInput() {
        mainOwner.onCancelPointerInput()
    }

    override fun processKeyEvent(keyEvent: KeyEvent): Boolean =
        mainOwner.onKeyEvent(keyEvent)

    override fun processRotaryScrollEvent(event: RotaryScrollEvent): Boolean =
        mainOwner.onRotaryEvent(event)

    override fun doMeasureAndLayout() {
        mainOwner.measureAndLayout()
    }

    override fun doDraw(canvas: Canvas) {
        mainOwner.draw(canvas)
    }

    override var showLayoutBounds: Boolean
        get() = mainOwner.owner.showLayoutBounds
        set(value) {
            mainOwner.owner.showLayoutBounds = value
        }

    private fun onOwnerAppended(owner: RootNodeOwner) {
        semanticsOwnerListener?.onSemanticsOwnerAppended(owner.semanticsOwner)
    }

    private fun onOwnerRemoved(owner: RootNodeOwner) {
        semanticsOwnerListener?.onSemanticsOwnerRemoved(owner.semanticsOwner)
    }
}
