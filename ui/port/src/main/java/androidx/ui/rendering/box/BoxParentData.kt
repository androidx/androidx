package androidx.ui.rendering.box

import androidx.ui.engine.geometry.Offset
import androidx.ui.rendering.obj.ParentData

// / Parent data used by [RenderBox] and its subclasses.
class BoxParentData : ParentData() {
    // / The offset at which to paint the child in the parent's coordinate system.
    var offset = Offset.zero

    override fun toString() = "offset=$offset"
}