package androidx.ui.rendering.obj

import androidx.annotation.CallSuper
import androidx.ui.VoidCallback
import androidx.ui.assert

/// A reference to the semantics tree.
///
/// The framework maintains the semantics tree (used for accessibility and
/// indexing) only when there is at least one client holding an open
/// [SemanticsHandle].
///
/// The framework notifies the client that it has updated the semantics tree by
/// calling the [listener] callback. When the client no longer needs the
/// semantics tree, the client can call [dispose] on the [SemanticsHandle],
/// which stops these callbacks and closes the [SemanticsHandle]. When all the
/// outstanding [SemanticsHandle] objects are closed, the framework stops
/// updating the semantics tree.
///
/// To obtain a [SemanticsHandle], call [PipelineOwner.ensureSemantics] on the
/// [PipelineOwner] for the render tree from which you wish to read semantics.
/// You can obtain the [PipelineOwner] using the [RenderObject.owner] property.
class SemanticsHandle(
        private var owner: PipelineOwner?,
        /// The callback that will be notified when the semantics tree updates.
        private val listener: VoidCallback) {

    init {
        assert(owner != null)
        if (listener != null)
            owner!!.semanticsOwner.addListener(listener);
    }

    /// Closes the semantics handle and stops calling [listener] when the
    /// semantics updates.
    ///
    /// When all the outstanding [SemanticsHandle] objects for a given
    /// [PipelineOwner] are closed, the [PipelineOwner] will stop updating the
    /// semantics tree.
    @CallSuper
    fun dispose() {
        assert {
            if (owner == null) {
                throw IllegalStateException(
                        "SemanticsHandle has already been disposed.\n" +
                        "Each SemanticsHandle should be disposed exactly once."
                );
            }
            return@assert true;
        };
        if (owner != null) {
            if (listener != null)
                owner?.semanticsOwner.removeListener(listener);
            owner?._didDisposeSemanticsHandle();
            owner = null;
        }
    }
}
