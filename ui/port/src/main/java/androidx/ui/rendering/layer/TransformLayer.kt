package androidx.ui.rendering.layer

import androidx.ui.compositing.SceneBuilder
import androidx.ui.engine.geometry.Offset
import androidx.ui.vectormath64.Matrix4

// / A composited layer that applies a given transformation matrix to its
// / children.
// /
// / This class inherits from [OffsetLayer] to make it one of the layers that
// / can be used at the root of a [RenderObject] hierarchy.
//
// / Creates a transform layer.
// /
// / The [transform] and [offset] properties must be non-null before the
// / compositing phase of the pipeline.
class TransformLayer(
        // / The matrix to apply.
        // /
        // / The scene must be explicitly recomposited after this property is changed
        // / (as described at [Layer]).
        // /
        // / This transform is applied before [offset], if both are set.
        // /
        // / The [transform] property must be non-null before the compositing phase of
        // / the pipeline.
    private val transform: Matrix4,
    offset: Offset = Offset.zero
) : OffsetLayer(offset) {

    var _lastEffectiveTransform: Matrix4? = null

    override fun addToScene(builder: SceneBuilder, layerOffset: Offset) {
        // TODO(Migration/andrey): needs Matrix4: translationValues, multiply; SceneBuilder: pushTransform, pop
        TODO()
//        _lastEffectiveTransform = transform;
//        val totalOffset = offset + layerOffset;
//        if (totalOffset != Offset.zero) {
//            _lastEffectiveTransform = Matrix4.translationValues(totalOffset.dx, totalOffset.dy, 0.0)
//            ..multiply(_lastEffectiveTransform);
//        }
//        builder.pushTransform(_lastEffectiveTransform.storage);
//        addChildrenToScene(builder, Offset.zero);
//        builder.pop();
    }

    override fun applyTransform(child: Layer, transform: Matrix4) {
        assert(child != null)
        assert(transform != null)
        transform.multiply(_lastEffectiveTransform)
    }

    // TODO(Migration/andrey): Layer class should implement DiagnosticableTreeMixin first
//    @override
//    void debugFillProperties(DiagnosticPropertiesBuilder properties) {
//        super.debugFillProperties(properties);
//        properties.add(new TransformProperty('transform', transform));
//    }
}