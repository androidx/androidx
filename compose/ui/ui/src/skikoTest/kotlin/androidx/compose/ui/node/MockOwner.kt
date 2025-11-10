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

package androidx.compose.ui.node

import androidx.collection.IntObjectMap
import androidx.collection.intObjectMapOf
import androidx.compose.runtime.retain.RetainedValuesStore
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.draganddrop.DragAndDropManager
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.ReusableGraphicsLayerScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.pointer.PointerIconService
import androidx.compose.ui.modifier.ModifierLocalManager
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.PlatformTextInputSessionScope
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.invertTo
import androidx.compose.ui.semantics.EmptySemanticsModifier
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.spatial.RectManager
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.viewinterop.InteropView
import kotlin.coroutines.CoroutineContext

// A copy of src/androidHostTest/kotlin/androidx/compose/ui/node/LayoutNodeTest.kt
internal class MockOwner(
    private val position: IntOffset = IntOffset.Zero,
    override val root: LayoutNode = LayoutNode(),
    override val semanticsOwner: SemanticsOwner =
        SemanticsOwner(root, EmptySemanticsModifier(), intObjectMapOf()),
) : Owner {
    val onRequestMeasureParams = mutableListOf<LayoutNode>()
    val onAttachParams = mutableListOf<LayoutNode>()
    val onDetachParams = mutableListOf<LayoutNode>()
    var layoutChangeCount = 0

    override val rootForTest: RootForTest
        get() = TODO("Not yet implemented")

    override val layoutNodes: IntObjectMap<LayoutNode>
        get() = TODO("Not yet implemented")

    override val hapticFeedBack: HapticFeedback
        get() = TODO("Not yet implemented")

    override val inputModeManager: InputModeManager
        get() = TODO("Not yet implemented")

    @Suppress("DEPRECATION")
    override val clipboardManager: androidx.compose.ui.platform.ClipboardManager
        get() = TODO("Not yet implemented")

    override val clipboard: Clipboard
        get() = TODO("Not yet implemented")

    override val accessibilityManager: AccessibilityManager
        get() = TODO("Not yet implemented")

    override val graphicsContext: GraphicsContext
        get() = TODO("Not yet implemented")

    override val textToolbar: TextToolbar
        get() = TODO("Not yet implemented")

    @Suppress("DEPRECATION")
    override val autofillTree: androidx.compose.ui.autofill.AutofillTree
        get() = TODO("Not yet implemented")

    @Suppress("DEPRECATION")
    override val autofill: androidx.compose.ui.autofill.Autofill?
        get() = TODO("Not yet implemented")

    override val autofillManager: AutofillManager
        get() = TODO("Not yet implemented")

    override val density: Density
        get() = Density(1f)

    override val textInputService: TextInputService
        get() = TODO("Not yet implemented")

    override val softwareKeyboardController: SoftwareKeyboardController
        get() = TODO("Not yet implemented")

    override val pointerIconService: PointerIconService
        get() = TODO("Not yet implemented")

    override val focusOwner: FocusOwner
        get() = TODO("Not yet implemented")

    override val retainedValuesStore: RetainedValuesStore
        get() = TODO("Not yet implemented")

    override val windowInfo: WindowInfo
        get() = TODO("Not yet implemented")

    override val rectManager: RectManager = RectManager()

    @Deprecated(
        "fontLoader is deprecated, use fontFamilyResolver",
        replaceWith = ReplaceWith("fontFamilyResolver"),
    )
    @Suppress("DEPRECATION")
    override val fontLoader: Font.ResourceLoader
        get() = TODO("Not yet implemented")

    override val fontFamilyResolver: FontFamily.Resolver
        get() = TODO("Not yet implemented")

    override val layoutDirection: LayoutDirection
        get() = LayoutDirection.Ltr

    @InternalCoreApi override var showLayoutBounds: Boolean = false
    override val snapshotObserver = OwnerSnapshotObserver { it.invoke() }
    override val modifierLocalManager: ModifierLocalManager = ModifierLocalManager(this)
    override val coroutineContext: CoroutineContext get() = throw IllegalStateException()
    override val dragAndDropManager: DragAndDropManager
        get() = TODO("Not yet implemented")

    override fun onRequestMeasure(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean,
        scheduleMeasureAndLayout: Boolean,
    ) {
        onRequestMeasureParams += layoutNode
        if (affectsLookahead) {
            layoutNode.markLookaheadMeasurePending()
        }
        layoutNode.markMeasurePending()
    }

    override fun onRequestRelayout(
        layoutNode: LayoutNode,
        affectsLookahead: Boolean,
        forceRequest: Boolean,
    ) {
        if (affectsLookahead) {
            layoutNode.markLookaheadLayoutPending()
        }
        layoutNode.markLayoutPending()
    }

    override fun requestOnPositionedCallback(layoutNode: LayoutNode) {}

    override fun onPreAttach(node: LayoutNode) {
        onAttachParams += node
    }

    override fun onPostAttach(node: LayoutNode) {}

    override fun onDetach(node: LayoutNode) {
        onDetachParams += node
    }

    override fun calculatePositionInWindow(localPosition: Offset): Offset =
        localPosition + position.toOffset()

    override fun calculateLocalPosition(positionInWindow: Offset): Offset =
        positionInWindow - position.toOffset()

    override fun requestAutofill(node: LayoutNode) {
        TODO("Not yet implemented")
    }

    override fun measureAndLayout(sendPointerUpdate: Boolean) {}

    override fun measureAndLayout(layoutNode: LayoutNode, constraints: Constraints) {}

    override fun forceMeasureTheSubtree(layoutNode: LayoutNode, affectsLookahead: Boolean) {}

    override fun registerOnEndApplyChangesListener(listener: () -> Unit) {
        listener()
    }

    override fun onEndApplyChanges() {}

    override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        TODO("Not yet implemented")
    }

    override suspend fun textInputSession(
        session: suspend PlatformTextInputSessionScope.() -> Nothing
    ): Nothing {
        TODO("Not yet implemented")
    }

    override fun screenToLocal(positionOnScreen: Offset): Offset {
        TODO("Not yet implemented")
    }

    override fun localToScreen(localPosition: Offset): Offset {
        TODO("Not yet implemented")
    }

    val invalidatedLayers = mutableListOf<OwnedLayer>()

    override fun createLayer(
        drawBlock: (Canvas, GraphicsLayer?) -> Unit,
        invalidateParentLayer: () -> Unit,
        explicitLayer: GraphicsLayer?,
    ): OwnedLayer {
        val transform = Matrix()
        val inverseTransform = Matrix()
        return object : OwnedLayer {
            override fun updateLayerProperties(scope: ReusableGraphicsLayerScope) {
                transform.reset()
                // This is not expected to be 100% accurate
                transform.scale(scope.scaleX, scope.scaleY)
                transform.rotateZ(scope.rotationZ)
                transform.translate(scope.translationX, scope.translationY)
                transform.invertTo(inverseTransform)
            }

            override fun isInLayer(position: Offset) = true

            override fun move(position: IntOffset) {}

            override fun resize(size: IntSize) {}

            override fun drawLayer(canvas: Canvas, parentLayer: GraphicsLayer?) {
                drawBlock(canvas, null)
            }

            override fun updateDisplayList() {}

            override fun invalidate() {
                invalidatedLayers.add(this)
            }

            override fun destroy() {}

            override fun mapBounds(rect: MutableRect, inverse: Boolean) {}

            override fun reuseLayer(
                drawBlock: (Canvas, GraphicsLayer?) -> Unit,
                invalidateParentLayer: () -> Unit,
            ) {}

            override fun transform(matrix: Matrix) {
                matrix.timesAssign(transform)
            }

            override val underlyingMatrix: Matrix
                get() = transform

            override var frameRate: Float
                get() = 0f
                set(_) {}

            override var isFrameRateFromParent: Boolean
                get() = false
                set(_) {}

            override fun inverseTransform(matrix: Matrix) {
                matrix.timesAssign(inverseTransform)
            }

            override fun mapOffset(point: Offset, inverse: Boolean) = point
        }
    }

    override fun onSemanticsChange() {}

    override fun onLayoutChange(layoutNode: LayoutNode) {
        layoutChangeCount++
    }

    override fun onLayoutNodeDeactivated(layoutNode: LayoutNode) {}

    @InternalComposeUiApi override fun onInteropViewLayoutChange(view: InteropView) {}

    override var measureIteration: Long = 0
    override val viewConfiguration: ViewConfiguration
        get() = TODO("Not yet implemented")

    override val sharedDrawScope = LayoutNodeDrawScope()
}
