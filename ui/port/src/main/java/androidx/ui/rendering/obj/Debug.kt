package androidx.ui.rendering.obj

// / Log the call stacks that mark render objects as needing layout.
// /
// / For sanity, this only logs the stack traces of cases where an object is
// / added to the list of nodes needing layout. This avoids printing multiple
// / redundant stack traces as a single [RenderObject.markNeedsLayout] call walks
// / up the tree.
var debugPrintMarkNeedsLayoutStacks: Boolean = false

// / Log the dirty render objects that are laid out each frame.
// /
// / Combined with [debugPrintBeginFrameBanner], this allows you to distinguish
// / layouts triggered by the initial mounting of a render tree (e.g. in a call
// / to [runApp]) from the regular layouts triggered by the pipeline.
// /
// / Combined with [debugPrintMarkNeedsLayoutStacks], this lets you watch a
// / render object's dirty/clean lifecycle.
// /
// / See also:
// /
// /  * [debugProfilePaintsEnabled], which does something similar for
// /    painting but using the timeline view.
// /
// /  * [debugPrintRebuildDirtyWidgets], which does something similar for widgets
// /    being rebuilt.
// /
// /  * The discussion at [RendererBinding.drawFrame].
var debugPrintLayouts: Boolean = false
