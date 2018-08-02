package androidx.ui.widgets.framework

/// The element at the root of the tree.
///
/// Only root elements may have their owner set explicitly. All other
/// elements inherit their owner from their parent.
abstract class RootRenderObjectElement(widget: RenderObjectWidget) : RenderObjectElement(widget) {

    /// Set the owner of the element. The owner will be propagated to all the
    /// descendants of this element.
    ///
    /// The owner manages the dirty elements list.
    ///
    /// The [WidgetsBinding] introduces the primary owner,
    /// [WidgetsBinding.buildOwner], and assigns it to the widget tree in the call
    /// to [runApp]. The binding is responsible for driving the build pipeline by
    /// calling the build owner's [BuildOwner.buildScope] method. See
    /// [WidgetsBinding.drawFrame].
    fun assignOwner(owner: BuildOwner) {
        this.owner = owner;
    }

    override fun mount(parent: Element, newSlot: Any) {
        // Root elements should never have parents.
        assert(parent == null);
        assert(newSlot == null);
        super.mount(parent, newSlot);
    }
}