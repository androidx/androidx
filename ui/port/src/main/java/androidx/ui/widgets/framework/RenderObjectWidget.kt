package androidx.ui.widgets.framework

import androidx.ui.foundation.Key
import androidx.ui.rendering.obj.RenderObject

// / RenderObjectWidgets provide the configuration for [RenderObjectElement]s,
// / which wrap [RenderObject]s, which provide the actual rendering of the
// / application.
abstract class RenderObjectWidget(key: Key) : Widget(key) {

    // / RenderObjectWidgets always inflate to a [RenderObjectElement] subclass.
    abstract override fun createElement(): RenderObjectElement

    // / Creates an instance of the [RenderObject] class that this
    // / [RenderObjectWidget] represents, using the configuration described by this
    // / [RenderObjectWidget].
    // /
    // / This method should not do anything with the children of the render object.
    // / That should instead be handled by the method that overrides
    // / [RenderObjectElement.mount] in the object rendered by this object's
    // / [createElement] method. See, for example,
    // / [SingleChildRenderObjectElement.mount].
    internal abstract fun createRenderObject(context: BuildContext): RenderObject?

    // / Copies the configuration described by this [RenderObjectWidget] to the
    // / given [RenderObject], which will be of the same type as returned by this
    // / object's [createRenderObject].
    // /
    // / This method should not do anything to update the children of the render
    // / object. That should instead be handled by the method that overrides
    // / [RenderObjectElement.update] in the object rendered by this object's
    // / [createElement] method. See, for example,
    // / [SingleChildRenderObjectElement.update].
    // TODO(Migration/Filip): Removed covariant keyword for renderObject
    internal fun updateRenderObject(context: BuildContext, renderObject: RenderObject?) { }

    // / A render object previously associated with this widget has been removed
    // / from the tree. The given [RenderObject] will be of the same type as
    // / returned by this object's [createRenderObject].
    // TODO(Migration/Filip): Removed covariant keyword for renderObject
    internal fun didUnmountRenderObject(renderObject: RenderObject?) { }
}