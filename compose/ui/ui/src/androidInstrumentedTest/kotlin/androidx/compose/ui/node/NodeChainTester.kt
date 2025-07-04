/*
 * Copyright 2022 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.compose.ui.node

import androidx.collection.IntObjectMap
import androidx.collection.intObjectMapOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.Autofill
import androidx.compose.ui.autofill.AutofillManager
import androidx.compose.ui.autofill.AutofillTree
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.InspectorInfo
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
import com.google.common.base.Objects
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.Assert

internal fun chainTester() = NodeChainTester()

class DiffLog {
    private val oplog = mutableListOf<DiffOp>()

    fun op(op: DiffOp) = oplog.add(op)

    fun clear() = oplog.clear()

    fun assertElementDiff(expected: String) {
        Assert.assertEquals(expected, oplog.joinToString("\n") { it.elementDiffString() })
    }

    fun debug(): String = buildString {
        for (op in oplog) {
            appendLine(op.debug())
        }
    }
}

internal class NodeChainTester : NodeChain.Logger {
    private val layoutNode = LayoutNode()
    val chain = layoutNode.nodes.also { it.useLogger(this) }
    private val log = DiffLog()
    val nodes: List<Modifier.Node>
        get() {
            val result = mutableListOf<Modifier.Node>()
            chain.headToTailExclusive { result.add(it) }
            return result
        }

    val aggregateChildMasks: List<Int>
        get() = nodes.map { it.aggregateChildKindSet }

    fun attach(): NodeChainTester {
        check(!layoutNode.isAttached)
        layoutNode.attach(MockOwner())
        return this
    }

    fun detach(): NodeChainTester {
        check(layoutNode.isAttached)
        layoutNode.detach()
        return this
    }

    fun clearLog(): NodeChainTester {
        log.clear()
        return this
    }

    fun debug(): NodeChainTester {
        if (true) error(log.debug())
        return this
    }

    fun validateAttached(): NodeChainTester {
        chain.head.visitSubtree(Nodes.Any) { check(it.isAttached) }
        return this
    }

    fun withModifiers(vararg modifiers: Modifier): NodeChainTester {
        chain.updateFrom(modifierOf(*modifiers))
        return this
    }

    fun withModifierNodes(vararg nodes: Modifier.Node): NodeChainTester {
        chain.updateFrom(modifierOf(*nodes))
        return this
    }

    fun assertStringEquals(expected: String): NodeChainTester {
        Assert.assertEquals(expected, chain.toString())
        return this
    }

    fun assertElementDiff(expected: String): NodeChainTester {
        log.assertElementDiff(expected)
        return this
    }

    override fun linearDiffAborted(
        index: Int,
        prev: Modifier.Element,
        next: Modifier.Element,
        node: Modifier.Node,
    ) {
        // TODO
    }

    override fun nodeUpdated(
        oldIndex: Int,
        newIndex: Int,
        prev: Modifier.Element,
        next: Modifier.Element,
        node: Modifier.Node,
    ) {
        log.op(DiffOp.Same(oldIndex, newIndex, prev, next, node, true))
    }

    override fun nodeReused(
        oldIndex: Int,
        newIndex: Int,
        prev: Modifier.Element,
        next: Modifier.Element,
        node: Modifier.Node,
    ) {
        log.op(DiffOp.Same(oldIndex, newIndex, prev, next, node, false))
    }

    override fun nodeInserted(
        atIndex: Int,
        newIndex: Int,
        element: Modifier.Element,
        child: Modifier.Node,
        inserted: Modifier.Node,
    ) {
        log.op(DiffOp.Insert(atIndex, newIndex, element, child, inserted))
    }

    override fun nodeRemoved(oldIndex: Int, element: Modifier.Element, node: Modifier.Node) {
        log.op(DiffOp.Remove(oldIndex, element, node))
    }
}

sealed class DiffOp(
    private val element: Modifier.Element,
    private val opChar: String,
    val opString: String,
) {
    fun elementDiffString(): String {
        return "$opChar$element"
    }

    abstract fun debug(): String

    class Same(
        private val oldIndex: Int,
        private val newIndex: Int,
        private val beforeEl: Modifier.Element,
        private val afterEl: Modifier.Element,
        private val node: Modifier.Node,
        val updated: Boolean,
    ) : DiffOp(beforeEl, if (updated) "*" else " ", "Same") {
        override fun debug() =
            """
            <$opString>
                $beforeEl @ $oldIndex = $afterEl @ $newIndex
                node: $node
                updated? = $updated
            </$opString>
        """
                .trimIndent()
    }

    class Insert(
        private val oldIndex: Int,
        private val newIndex: Int,
        private val afterEl: Modifier.Element,
        val child: Modifier.Node,
        private val inserted: Modifier.Node,
    ) : DiffOp(afterEl, "+", "Insert") {
        override fun debug() =
            """
            <$opString>
                $afterEl @ $newIndex (inserted at $oldIndex)
                child = $child
                inserted = $inserted
            </$opString>
        """
                .trimIndent()
    }

    class Remove(
        private val oldIndex: Int,
        private val beforeEl: Modifier.Element,
        private val beforeEntity: Modifier.Node,
    ) : DiffOp(beforeEl, "-", "Remove") {
        override fun debug() =
            """
            <$opString>
                $beforeEl @ $oldIndex
                beforeEntity = $beforeEntity
            </$opString>
        """
                .trimIndent()
    }
}

fun modifierOf(vararg modifiers: Modifier): Modifier {
    var result: Modifier = Modifier
    for (m in modifiers) {
        result = result.then(m)
    }
    return result
}

fun modifierOf(vararg nodes: Modifier.Node): Modifier {
    var result: Modifier = Modifier
    for (n in nodes) {
        result = result.then(NodeModifierElementNode(n))
    }
    return result
}

internal open class NodeModifierElementNode(val node: Modifier.Node) :
    ModifierNodeElement<Modifier.Node>() {
    override fun create(): Modifier.Node = node

    override fun update(node: Modifier.Node) {}

    override fun hashCode(): Int = node.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other !is NodeModifierElementNode) return false
        return other.node === node
    }
}

fun reusableModifier(name: String): Modifier.Element =
    object : Modifier.Element {
        override fun toString(): String = name
    }

fun reusableModifiers(vararg names: String): List<Modifier.Element> {
    return names.map { reusableModifier(it) }
}

class A : Modifier.Node() {
    override fun toString(): String = "a"
}

fun modifierA(params: Any? = null): Modifier.Element {
    return object : TestElement<A>("a", params, A()) {}
}

class B : Modifier.Node() {
    override fun toString(): String = "b"
}

fun modifierB(params: Any? = null): Modifier.Element {
    return object : TestElement<B>("b", params, B()) {}
}

class C : Modifier.Node() {
    override fun toString(): String = "c"
}

fun modifierC(params: Any? = null): Modifier.Element {
    return object : TestElement<C>("c", params, C()) {}
}

fun modifierD(params: Any? = null): Modifier.Element {
    return object : TestElement<Modifier.Node>("d", params, object : Modifier.Node() {}) {}
}

fun managedModifier(name: String, params: Any? = null): ModifierNodeElement<*> =
    object : TestElement<Modifier.Node>(name, params, object : Modifier.Node() {}) {}

private abstract class TestElement<T : Modifier.Node>(
    val modifierName: String,
    val param: Any? = null,
    val node: T,
) : ModifierNodeElement<T>() {
    override fun create(): T = node

    override fun update(node: T) {}

    override fun InspectorInfo.inspectableProperties() {
        name = modifierName
    }

    override fun hashCode() = Objects.hashCode(modifierName, param)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is TestElement<*> &&
            javaClass == other.javaClass &&
            modifierName == other.modifierName &&
            param == other.param
    }

    override fun toString() = modifierName
}

fun entityModifiers(vararg names: String): List<ModifierNodeElement<*>> {
    return names.map { managedModifier(it, null) }
}

fun assertReused(before: Modifier.Element, after: Modifier.Element) {
    with(chainTester()) {
        withModifiers(before)
        val beforeEntity = chain.tail
        withModifiers(after)
        val afterEntity = chain.tail
        assert(beforeEntity === afterEntity)
    }
}

private class MockOwner(
    private val position: IntOffset = IntOffset.Zero,
    override val root: LayoutNode = LayoutNode(),
    override val coroutineContext: CoroutineContext =
        Executors.newFixedThreadPool(3).asCoroutineDispatcher(),
) : Owner {
    val onRequestMeasureParams = mutableListOf<LayoutNode>()
    val onAttachParams = mutableListOf<LayoutNode>()
    val onDetachParams = mutableListOf<LayoutNode>()
    var layoutChangeCount = 0
    var semanticsChanged: Boolean = false
    val invalidatedLayers = mutableListOf<OwnedLayer>()

    @InternalCoreApi override var showLayoutBounds: Boolean = false
    override val snapshotObserver = OwnerSnapshotObserver { it.invoke() }
    override val modifierLocalManager: ModifierLocalManager = ModifierLocalManager(this)
    override var measureIteration: Long = 0
    override val sharedDrawScope = LayoutNodeDrawScope()
    override val density: Density
        get() = Density(1f)

    override val layoutDirection: LayoutDirection
        get() = LayoutDirection.Ltr

    override val rectManager: RectManager = RectManager()

    override val semanticsOwner: SemanticsOwner =
        SemanticsOwner(root, EmptySemanticsModifier(), intObjectMapOf())

    override val viewConfiguration: ViewConfiguration
        get() = TODO("Not yet implemented")

    override val layoutNodes: IntObjectMap<LayoutNode>
        get() = TODO("Not yet implemented")

    override val rootForTest: RootForTest
        get() = TODO("Not yet implemented")

    override val hapticFeedBack: HapticFeedback
        get() = TODO("Not yet implemented")

    override val inputModeManager: InputModeManager
        get() = TODO("Not yet implemented")

    override val clipboardManager: ClipboardManager
        get() = TODO("Not yet implemented")

    override val clipboard: Clipboard
        get() = TODO("Not yet implemented")

    override val accessibilityManager: AccessibilityManager
        get() = TODO("Not yet implemented")

    override val graphicsContext: GraphicsContext
        get() = TODO("Not yet implemented")

    override val textToolbar: TextToolbar
        get() = TODO("Not yet implemented")

    override val textInputService: TextInputService
        get() = TODO("Not yet implemented")

    override val pointerIconService: PointerIconService
        get() = TODO("Not yet implemented")

    override val focusOwner: FocusOwner
        get() = TODO("Not yet implemented")

    override val windowInfo: WindowInfo
        get() = TODO("Not yet implemented")

    override val fontFamilyResolver: FontFamily.Resolver
        get() = TODO("Not yet implemented")

    override val dragAndDropManager: DragAndDropManager
        get() = TODO("Not yet implemented")

    override val autofillTree: AutofillTree
        get() = TODO("Not yet implemented")

    override val autofill: Autofill?
        get() = TODO("Not yet implemented")

    override val autofillManager: AutofillManager?
        get() = TODO("Not yet implemented")

    override val softwareKeyboardController: SoftwareKeyboardController
        get() = TODO("Not yet implemented")

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

    @Deprecated(
        "fontLoader is deprecated, use fontFamilyResolver",
        replaceWith = ReplaceWith("fontFamilyResolver"),
    )
    @Suppress("DEPRECATION")
    override val fontLoader: Font.ResourceLoader
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
        if (affectsLookahead) layoutNode.markLookaheadLayoutPending()
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

    override fun onSemanticsChange() {
        semanticsChanged = true
    }

    override fun onLayoutChange(layoutNode: LayoutNode) {
        layoutChangeCount++
    }

    override fun onLayoutNodeDeactivated(layoutNode: LayoutNode) {}

    @InternalComposeUiApi override fun onInteropViewLayoutChange(view: InteropView) {}

    override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
        TODO("Not yet implemented")
    }

    override fun createLayer(
        drawBlock: (Canvas, GraphicsLayer?) -> Unit,
        invalidateParentLayer: () -> Unit,
        explicitLayer: GraphicsLayer?,
    ): OwnedLayer {
        val transform = Matrix()
        val inverseTransform = Matrix()
        return object : OwnedLayer {
            override fun isInLayer(position: Offset) = true

            override fun move(position: IntOffset) {}

            override fun resize(size: IntSize) {}

            override fun drawLayer(canvas: Canvas, parentLayer: GraphicsLayer?) {
                drawBlock(canvas, parentLayer)
            }

            override fun updateDisplayList() {}

            override fun invalidate() {
                invalidatedLayers.add(this)
            }

            override fun destroy() {}

            override fun mapBounds(rect: MutableRect, inverse: Boolean) {}

            override fun transform(matrix: Matrix) {
                matrix.timesAssign(transform)
            }

            override val underlyingMatrix: Matrix
                get() = transform

            override var frameRate: Float = 0f

            override var isFrameRateFromParent = false

            override fun inverseTransform(matrix: Matrix) {
                matrix.timesAssign(inverseTransform)
            }

            override fun mapOffset(point: Offset, inverse: Boolean) = point

            override fun reuseLayer(
                drawBlock: (Canvas, GraphicsLayer?) -> Unit,
                invalidateParentLayer: () -> Unit,
            ) {}

            override fun updateLayerProperties(scope: ReusableGraphicsLayerScope) {
                transform.reset()
                // This is not expected to be 100% accurate
                transform.scale(scope.scaleX, scope.scaleY)
                transform.rotateZ(scope.rotationZ)
                transform.translate(scope.translationX, scope.translationY)
                transform.invertTo(inverseTransform)
            }
        }
    }
}
