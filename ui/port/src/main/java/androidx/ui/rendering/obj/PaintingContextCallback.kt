package androidx.ui.rendering.obj

import androidx.ui.engine.geometry.Offset

// / Signature for painting into a [PaintingContext].
// /
// / The `offset` argument is the offset from the origin of the coordinate system
// / of the [PaintingContext.canvas] to the coordinate system of the callee.
// /
// / Used by many of the methods of [PaintingContext].

typealias PaintingContextCallback = (PaintingContext, Offset) -> Unit