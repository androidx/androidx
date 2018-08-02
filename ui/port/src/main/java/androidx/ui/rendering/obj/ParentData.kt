package androidx.ui.rendering.obj

import androidx.annotation.CallSuper

/// Base class for data associated with a [RenderObject] by its parent.
///
/// Some render objects wish to store data on their children, such as their
/// input parameters to the parent's layout algorithm or their position relative
/// to other children.
class ParentData {
    /// Called when the RenderObject is removed from the tree.
    @CallSuper
    protected fun detach() { }

    override fun toString() = "<none>"
}