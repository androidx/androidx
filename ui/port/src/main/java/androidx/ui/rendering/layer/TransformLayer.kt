package androidx.ui.rendering.layer

import androidx.ui.compositing.SceneBuilder
import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.painting.matrixutils.TransformProperty
import androidx.ui.vectormath64.Matrix4

/**
 * A composited layer that applies a given transformation matrix to its
 * children.
 *
 * This class inherits from [OffsetLayer] to make it one of the layers that
 * can be used at the root of a [RenderObject] hierarchy.
 */
//
/**
 * Creates a transform layer.
 *
 * The [transform] and [offset] properties must be non-null before the
 * compositing phase of the pipeline.
 */
class TransformLayer(
    /**
     * The matrix to apply.
     *
     * The scene must be explicitly recomposited after this property is changed
     * (as described at [Layer]).
     *
     * This transform is applied before [offset], if both are set.
     *
     * The [transform] property must be non-null before the compositing phase of
     * the pipeline.
     */
    private val transform: Matrix4,
    offset: Offset = Offset.zero
) : OffsetLayer(offset) {

    private var _lastEffectiveTransform: Matrix4? = null

    override fun addToScene(builder: SceneBuilder, layerOffset: Offset) {
        _lastEffectiveTransform = transform
        val totalOffset = offset + layerOffset
        if (totalOffset != Offset.zero) {
            _lastEffectiveTransform!! *= Matrix4.translationValues(
                    totalOffset.dx,
                    totalOffset.dy,
                    0.0)
        }
        builder.pushTransform(_lastEffectiveTransform!!)
        addChildrenToScene(builder, Offset.zero)
        builder.pop()
    }

    override fun applyTransform(child: Layer, transform: Matrix4) {
        transform *= _lastEffectiveTransform!!
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(TransformProperty(name = "transform", value = transform))
    }
}