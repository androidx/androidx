package androidx.ui.rendering.layer

import androidx.annotation.CallSuper
import androidx.ui.assert
import androidx.ui.compositing.SceneBuilder
import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.AbstractNode
import androidx.ui.foundation.diagnostics.DiagnosticLevel
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticableTree
import androidx.ui.foundation.diagnostics.DiagnosticsProperty

abstract class Layer : AbstractNode(), DiagnosticableTree {

    // / This layer's parent in the layer tree.
    // /
    // / The [parent] of the root node in the layer tree is null.
    // /
    // / Only subclasses of [ContainerLayer] can have children in the layer tree.
    // / All other layer classes are used for leaves in the layer tree.
    val parentContainer get() = super.parent as ContainerLayer?

    // / This layer's next sibling in the parent layer's child list.
    internal var nextSibling: Layer? = null

    // / This layer's previous sibling in the parent layer's child list.
    internal var previousSibling: Layer? = null

    // / Removes this layer from its parent layer's child list.
    @CallSuper
    fun remove() {
        parentContainer?._removeChild(this)
    }

    // / Replaces this layer with the given layer in the parent layer's child list.
    fun replaceWith(newLayer: Layer) {
        assert(parentContainer != null)
        val parent = parentContainer!!
        assert(attached == parent.attached)
        assert(newLayer.parentContainer == null)
        assert(newLayer.nextSibling == null)
        assert(newLayer.previousSibling == null)
        assert(!newLayer.attached)
        newLayer.nextSibling = nextSibling
        nextSibling?.previousSibling = newLayer
        newLayer.previousSibling = previousSibling
        previousSibling?.nextSibling = newLayer
        assert {
            var node: Layer = this
            while (node.parentContainer != null)
                node = node.parentContainer!!
            assert(node != newLayer) // indicates we are about to create a cycle
            true
        }
        parentContainer?.adoptChild(newLayer)
        assert(newLayer.attached == parent.attached)
        if (parent.firstChild == this)
            parent.firstChild = newLayer
        if (parent.lastChild == this)
            parent.lastChild = newLayer
        nextSibling = null
        previousSibling = null
        parent.dropChild(this)
        assert(!attached)
    }

    // / Override this method to upload this layer to the engine.
    // /
    // / The `layerOffset` is the accumulated offset of this layer's parent from the
    // / origin of the builder's coordinate system.
    abstract fun addToScene(builder: SceneBuilder, layerOffset: Offset)

    // / The object responsible for creating this layer.
    // /
    // / Defaults to the value of [RenderObject.debugCreator] for the render object
    // / that created this layer. Used in debug messages.
    var debugCreator: Any? = null

    override fun toString() = toStringDiagnostic()

    override fun toStringShort() = super.toStringShort() + (if (owner == null) " DETACHED" else "")

    @CallSuper
    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty(name = "owner", value = owner,
                level = (if (parent != null) DiagnosticLevel.hidden else DiagnosticLevel.info),
                defaultValue = null))
        properties.add(
                DiagnosticsProperty(name = "creator", value = debugCreator, defaultValue = null,
                        level = DiagnosticLevel.debug))
    }
}