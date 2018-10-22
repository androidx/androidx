/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.foundation

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.window.Window
import androidx.ui.foundation.LayoutNode.Companion.measure
import androidx.ui.painting.Canvas
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.widgets.binding.WidgetsFlutterBinding
import androidx.ui.widgets.framework.Element
import androidx.ui.widgets.framework.RenderObjectElement
import androidx.ui.widgets.framework.Widget
import androidx.ui.widgets.view.ViewHost

/**
 * [ComponentNode.ownerData] under [AndroidOwner] control.
 */
private class AndroidData(val view: ViewGroup) {
    val drawNodes = mutableSetOf<DrawNode>()
}

/**
 * An Android View-based owner. Only [LayoutNode]s and [DrawNode]s get unique
 * [ComponentNode.ownerData]. Other nodes share their owner data with their parent.
 *
 * This is currently single threaded and runs on the UI thread.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
private class AndroidOwner(val containingView: ContainingView) : Owner {
    val nodesRequiringLayout = sortedSetOf<LayoutNode>(DepthFirstComparison())
    val adjustedLayouts = mutableSetOf<LayoutNode>()

    val root = LayoutNode { constraints, _ ->
        assert(layoutChildren.size <= 1)
        val child = layoutChildren.values.firstOrNull()
        if (child == null) {
            constraints as BoxConstraints
            Size(constraints.minWidth, constraints.minHeight)
        } else {
            System.out.println("Measuring $child")
            measure(child, constraints, true)
            System.out.println("Child's size is ${child.width} x ${child.height}")
            Size(child.width.toDouble(), child.height.toDouble())
        }
    }

    var layoutRequested = false

    /**
     * Convenience property to avoid all that casting.
     */
    private var ComponentNode.androidData: AndroidData
        get() = ownerData as AndroidData
        set(value) {
            ownerData = value
        }

    private val LayoutNode.isRelayoutBoundary: Boolean
        get() = (!parentUsesSize && !sizedByParent)

    init {
        if (containingView.isAttachedToWindow) {
            root.attach(this)
        }
        root.sizedByParent = false
    }

    override fun onInvalidate(drawNode: DrawNode) {
        callOnUiThread {
            drawNode.androidData.view.invalidate()
        }
    }

    override fun onRequestLayout(layoutNode: LayoutNode) {
        if (!layoutRequested) {
            layoutRequested = true
            callOnUiThread {
                containingView.requestLayout()
            }
        }
        nodesRequiringLayout.add(layoutNode)
        // find the layout boundary:
        var parent = layoutNode.parentLayoutNode
        var node = layoutNode
        while (!node.isRelayoutBoundary && parent != null) {
            node = parent
            nodesRequiringLayout.add(node)
            node.needsLayout = true
            parent = node.parentLayoutNode
        }
    }

    override fun onSizeChange(layoutNode: LayoutNode) {
        adjustedLayouts.add(layoutNode)
    }

    override fun onPositionChange(layoutNode: LayoutNode) {
        adjustedLayouts.add(layoutNode)
    }

    override fun onAttach(node: ComponentNode) {
        if (node.ownerData != null) throw IllegalStateException()
        assert(node.ownerData == null)

        val parent = node.parent
        if (node !is LayoutNode) {
            val androidData = parent!!.androidData
            node.androidData = androidData
            if (node is DrawNode) {
                androidData.drawNodes.add(node)
                androidData.view.setWillNotDraw(false)
            }
            return
        }

        // LayoutNode and DrawNode get their own Views
        val container = if (parent == null) {
            // This must be the root node
            containingView
        } else {
            parent.androidData.view
        }

        val nodeView = NodeView(container, node)
        val data = AndroidData(nodeView)
        node.androidData = data

        // node position or size may be changed while the node was detached
        adjustedLayouts.add(node)
    }

    override fun onDetach(node: ComponentNode) {
        if (node is DrawNode) {
            val androidData = node.androidData
            androidData.drawNodes.remove(node)
            if (androidData.drawNodes.isEmpty()) {
                androidData.view.setWillNotDraw(true)
            }
        } else if (node is LayoutNode) {
            val view = node.androidData.view
            (view.parent as ViewGroup).removeView(view)
        }
        node.ownerData = null
    }

    /**
     * Does layout for all nodes that have pending layout.
     * This currently only works on the UI thread. At the end of this, all layouts will be
     * resized and positioned. [adjustedLayouts] will contain all [LayoutNode]s that have
     * changed size or position.
     */
    fun reconcileLayout() {
        nodesRequiringLayout.forEach { layout ->
            System.out.println("reconcile $layout")
            measure(layout, layout.constraints, layout.parentUsesSize)
        }
        nodesRequiringLayout.clear()
    }

    /**
     * Calls [android.view.View.measure] on each view underlying [LayoutNode]s that have
     * been resized or repositioned.
     */
    fun measureAdjustedLayouts() {
        adjustedLayouts.forEach { layout ->
            val width = View.MeasureSpec.makeMeasureSpec(layout.width, View.MeasureSpec.EXACTLY)
            val height = View.MeasureSpec.makeMeasureSpec(layout.height, View.MeasureSpec.EXACTLY)
            layout.androidData.view.measure(width, height)
        }
    }

    /**
     * Calls [android.view.View.layout] on each view underlying [LayoutNode]s that have
     * been resized or repositioned.
     */
    fun layoutAdjustedLayouts() {
        adjustedLayouts.forEach { layout ->
            val left = layout.x
            val top = layout.y
            val right = left + layout.width
            val bottom = top + layout.height
            layout.androidData.view.layout(left, top, right, bottom)
        }
        adjustedLayouts.clear()
        layoutRequested = false
    }

    private fun callOnUiThread(block: () -> Unit) {
        val handler = containingView.handler
        if (Looper.myLooper() === handler.looper) {
            block()
        } else {
            handler.postAtFrontOfQueue(block)
        }
    }
}

/**
 * The containing [View] for components. There currently is no public way to establish
 * a component, but the `internal` `root` property can be used to add nodes.
 */
class ContainingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private val owner = AndroidOwner(this)

    // `internal` so it can be used for test access
    internal val root = owner.root

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val targetWidth = convertMeasureSpec(widthMeasureSpec)
        val targetHeight = convertMeasureSpec(heightMeasureSpec)

        val constraints = BoxConstraints(
            targetWidth.min, targetWidth.max,
            targetHeight.min, targetHeight.max
        )
        System.out.println("Measuring root $root")
        // Layout with new constraints
        LayoutNode.measure(root, constraints, false)

        System.out.println("Root now ${root.width}. Measuring the rest")
        // force reconciliation immediately
        owner.reconcileLayout()

        setMeasuredDimension(root.width, root.height)
        owner.measureAdjustedLayouts()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        root.attach(owner)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        root.detach()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        owner.layoutAdjustedLayouts()
    }

    private fun convertMeasureSpec(measureSpec: Int): ConstraintRange {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec).toDouble()
        return when (mode) {
            MeasureSpec.EXACTLY -> ConstraintRange(size, size)
            MeasureSpec.UNSPECIFIED -> ConstraintRange(0.0, Double.POSITIVE_INFINITY)
            MeasureSpec.AT_MOST -> ConstraintRange(0.0, size)
            else -> throw IllegalStateException()
        }
    }
}

private class ConstraintRange(val min: Double, val max: Double)

/**
 * Defines a View used to keep RenderNode information on LayoutNodes and DrawNodes.
 * This will be replaced with using RenderNodes instead.
 */
private class NodeView(container: ViewGroup, val node: ComponentNode) :
    ViewGroup(container.context) {
    init {
        container.addView(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        assert(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY)
        assert(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        // Walk children to look for DrawNodes
        val wrappedCanvas = Canvas(canvas)
        callDrawOnChildren(node, wrappedCanvas)
    }

    /**
     * Override dispatchDraw so that we can change the order of drawing to intersperse
     * [DrawNode]s and [LayoutNode]s.
     */
    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        if (willNotDraw()) {
            val wrappedCanvas = Canvas(canvas)
            callDrawOnChildren(node, wrappedCanvas)
        }
    }

    private fun callDrawOnChildren(node: ComponentNode, canvas: Canvas) {
        val drawingTime = drawingTime
        node.visitChildren { child ->
            if (child is DrawNode) {
                child.onPaint(canvas)
                child.needsPaint = false
            } else if (child is LayoutNode) {
                val view = (child.ownerData as AndroidData).view
                drawChild(canvas.toFrameworkCanvas(), view, drawingTime)
            } else {
                callDrawOnChildren(child, canvas)
            }
        }
    }
}

/**
 * Used for sorting ComponentNodes, initially by depth, then by order in the hierarchy.
 * The root node is first. in a [LayoutNode], the first child is ordered before the second
 * child.
 */
private class DepthFirstComparison : Comparator<ComponentNode> {
    override fun compare(o1: ComponentNode, o2: ComponentNode): Int {
        if (o1 === o2) {
            return 0
        }
        val depthDiff = o1.depth - o2.depth
        if (depthDiff != 0) {
            return depthDiff
        }
        assert(o1.owner === o2.owner)
        assert(o1.owner != null)
        var o1Child: ComponentNode = o1
        var thatChild: ComponentNode = o2
        var o1Parent = o1.parent!!
        var thatParent = o2.parent!!
        while (o1Parent !== thatParent) {
            o1Child = o1Parent
            thatChild = thatParent
            o1Parent = o1Parent.parent!!
            thatParent = thatParent.parent!!
        }

        o1Parent as LayoutNode
        val firstChild = o1Parent.children.first { child ->
            child === o1Child || child === thatChild
        }
        return if (firstChild === o1Child) -1 else 1
    }
}

/**
 * Creates a ContainingView to display provided Widget
 */
fun ContainingView(context: Context, widget: Widget): ContainingView {
    val containingView = ContainingView(context)
    val viewHost = ViewHost(containingView, Key.createKey("viewHost"), widget)

    val bindings = WidgetsFlutterBinding.create(Window())
    bindings.renderingDrawFrameEnabled = false
    bindings.attachRootWidget(viewHost)

    val rootNode = containingView.root
    val rootWidgetElement = bindings.renderViewElement!!
    val nodesGenerator = ElementNodesGenerator(rootNode, rootWidgetElement)
    nodesGenerator.regenerate()

    bindings.setOnRebuildHappenedCallback {
        nodesGenerator.regenerate()
    }

    return containingView
}

private class ElementNodesGenerator(
    private val rootNode: LayoutNode,
    private val rootWidgetElement: Element
) {

    fun regenerate() {
        rootNode.removeChildren()
        parentNode = rootNode
        rootWidgetElement.visitChildren(::onElementVisited)
    }

    private var parentNode: LayoutNode = rootNode

    /**
     * We traverse through Widget's hierarchy and convert every RenderObject into ComponentNodes
     * and transform them into nodes hierarchy.
     *
     * One RenderObject can be representing or a pair of LayoutNode and DrawNode
     * or just one LayoutNode for Layout widgets.
     *
     * GestureNode is not yet supported.
     */
    private fun onElementVisited(element: Element) {
        if (element is RenderObjectElement) {
            element.renderObject.also { renderObject ->
                renderObject.inComponentsMode = true
                val newNode = renderObject.layoutNode
                parentNode.add(newNode)
                parentNode = newNode
                renderObject.drawNode?.also { drawNode ->
                    newNode.add(drawNode)
                }
            }
        }
        element.visitChildren(::onElementVisited)
    }
}
