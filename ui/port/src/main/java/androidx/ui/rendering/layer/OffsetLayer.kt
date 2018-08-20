package androidx.ui.rendering.layer

import androidx.ui.compositing.SceneBuilder
import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty

// / A layer that is displayed at an offset from its parent layer.
// /
// / Offset layers are key to efficient repainting because they are created by
// / repaint boundaries in the [RenderObject] tree (see
// / [RenderObject.isRepaintBoundary]). When a render object that is a repaint
// / boundary is asked to paint at given offset in a [PaintingContext], the
// / render object first checks whether it needs to repaint itself. If not, it
// / reuses its existing [OffsetLayer] (and its entire subtree) by mutating its
// / [offset] property, cutting off the paint walk.
// /
// / Creates an offset layer.
// /
// / By default, [offset] is zero. It must be non-null before the compositing
// / phase of the pipeline.
open class OffsetLayer(
        // / Offset from parent in the parent's coordinate system.
        // /
        // / The scene must be explicitly recomposited after this property is changed
        // / (as described at [Layer]).
        // /
        // / The [offset] property must be non-null before the compositing phase of the
        // / pipeline.
    var offset: Offset = Offset.zero
) : ContainerLayer() {

    override fun addToScene(builder: SceneBuilder, layerOffset: Offset) {
        addChildrenToScene(builder, offset + layerOffset)
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create(name = "offset", value = offset))
    }

    // TODO(Migration/andrey): Requires Image class
//    /// Capture an image of the current state of this layer and its children.
//    ///
//    /// The returned [ui.Image] has uncompressed raw RGBA bytes, will be offset
//    /// by the top-left corner of [bounds], and have dimensions equal to the size
//    /// of [bounds] multiplied by [pixelRatio].
//    ///
//    /// The [pixelRatio] describes the scale between the logical pixels and the
//    /// size of the output image. It is independent of the
//    /// [window.devicePixelRatio] for the device, so specifying 1.0 (the default)
//    /// will give you a 1:1 mapping between logical pixels and the output pixels
//    /// in the image.
//    ///
//    /// See also:
//    ///
//    ///  * [RenderRepaintBoundary.toImage] for a similar API at the render object level.
//    ///  * [dart:ui.Scene.toImage] for more information about the image returned.
//    Future<ui.Image> toImage(Rect bounds, {double pixelRatio: 1.0}) async {
//        assert(bounds != null);
//        assert(pixelRatio != null);
//        final ui.SceneBuilder builder = new ui.SceneBuilder();
//        final Matrix4 transform = new Matrix4.translationValues(bounds.left - offset.dx, bounds.top - offset.dy, 0.0);
//        transform.scale(pixelRatio, pixelRatio);
//        builder.pushTransform(transform.storage);
//        addToScene(builder, Offset.zero);
//        final ui.Scene scene = builder.build();
//        try {
//            // Size is rounded up to the next pixel to make sure we don't clip off
//            // anything.
//            return await scene.toImage(
//                    (pixelRatio * bounds.width).ceil(),
//            (pixelRatio * bounds.height).ceil(),
//            );
//        } finally {
//            scene.dispose();
//        }
//    }
}