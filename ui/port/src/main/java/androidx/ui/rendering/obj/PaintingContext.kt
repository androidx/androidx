package androidx.ui.rendering.obj

import androidx.ui.assert
import androidx.ui.developer.timeline.Timeline
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.foundation.timelineWhitelistArguments
import androidx.ui.painting.Canvas
import androidx.ui.painting.Paint
import androidx.ui.painting.PictureRecorder
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.debugProfilePaintsEnabled
import androidx.ui.rendering.layer.ContainerLayer
import androidx.ui.rendering.layer.Layer
import androidx.ui.rendering.layer.OffsetLayer
import androidx.ui.rendering.layer.PictureLayer
import androidx.ui.runtimeType

/**
 * A place to paint.
 *
 * Rather than holding a canvas directly, [RenderObject]s paint using a painting
 * context. The painting context has a [Canvas], which receives the
 * individual draw operations, and also has functions for painting child
 * render objects.
 *
 * When painting a child render object, the canvas held by the painting context
 * can change because the draw operations issued before and after painting the
 * child might be recorded in separate compositing layers. For this reason, do
 * not hold a reference to the canvas across operations that might paint
 * child render objects.
 *
 * New [PaintingContext] objects are created automatically when using
 * [PaintingContext.repaintCompositedChild] and [pushLayer].
 */
class PaintingContext(
    private val _containerLayer: ContainerLayer,
    /**
     * An estimate of the bounds within which the painting context's [canvas]
     * will record painting commands. This can be useful for debugging.
     *
     * The canvas will allow painting outside these bounds.
     *
     * The [estimatedBounds] rectangle is in the [canvas] coordinate system.
     */
    private val estimatedBounds: Rect?
) {

    companion object {
        // Repaint the given render object.
        /**
         *
         * The render object must be attached to a [PipelineOwner], must have a
         * composited layer, and must be in need of painting. The render object's
         * layer, if any, is re-used, along with any layers in the subtree that don't
         * need to be repainted.
         *
         * See also:
         *
         *  * [RenderObject.isRepaintBoundary], which determines if a [RenderObject]
         *    has a composited layer.
         */
        fun repaintCompositedChild(child: RenderObject, debugAlsoPaintedParent: Boolean = false) {
            assert(child.isRepaintBoundary)
            assert(child._needsPaint)
            assert {
                // register the call for RepaintBoundary metrics
                child.debugRegisterRepaintBoundaryPaint(
                        includedParent = debugAlsoPaintedParent,
                        includedChild = true
                )
                true
            }
            if (child._layer == null) {
                assert(debugAlsoPaintedParent)
                child._layer = OffsetLayer()
            } else {
                assert(debugAlsoPaintedParent || child._layer!!.attached)
                child._layer!!.removeAllChildren()
            }
            assert {
                child._layer!!.debugCreator = child.debugCreator ?: child.runtimeType()
                true
            }
            val childContext = PaintingContext(child._layer!!, child.paintBounds)
            child.paintWithContext(childContext, Offset.zero)
            childContext._stopRecordingIfNeeded()
        }

        internal val _defaultPaint = Paint()
    }

    /**
     * Paint a child [RenderObject].
     *
     * If the child has its own composited layer, the child will be composited
     * into the layer subtree associated with this painting context. Otherwise,
     * the child will be painted into the current PictureLayer for this context.
     */
    fun paintChild(child: RenderBox, offset: Offset) {
        assert {
            if (debugProfilePaintsEnabled) {
                Timeline.startSync("${child.runtimeType()}", timelineWhitelistArguments)
            }
            true
        }

        if (child.isRepaintBoundary) {
            _stopRecordingIfNeeded()
            _compositeChild(child, offset)
        } else {
            child.paintWithContext(this, offset)
        }

        assert {
            if (debugProfilePaintsEnabled) {
                Timeline.finishSync()
            }
            true
        }
    }

    private fun _compositeChild(child: RenderObject, offset: Offset) {
        assert(!_isRecording)
        assert(child.isRepaintBoundary)
        assert(_canvas == null || _canvas!!.getSaveCount() == 1)

        // Create a layer for our child, and paint the child into it.
        if (child._needsPaint) {
            repaintCompositedChild(child, debugAlsoPaintedParent = true)
        } else {
            assert(child._layer != null)
            assert {
                // register the call for RepaintBoundary metrics
                child.debugRegisterRepaintBoundaryPaint(
                        includedParent = true,
                        includedChild = false
                )
                child._layer!!.debugCreator = child.debugCreator ?: child
                true
            }
        }
        child._layer!!.offset = offset
        _appendLayer(child._layer!!)
    }

    private fun _appendLayer(layer: Layer) {
        assert(!_isRecording)
        layer.remove()
        _containerLayer.append(layer)
    }

    private val _isRecording: Boolean
        get() {
            val hasCanvas: Boolean = _canvas != null
            assert {
                if (hasCanvas) {
                    assert(_currentLayer != null)
                    assert(_recorder != null)
                    assert(_canvas != null)
                } else {
                    assert(_currentLayer == null)
                    assert(_recorder == null)
                    assert(_canvas == null)
                }
                true
            }
            return hasCanvas
        }

//     Recording state
    private var _currentLayer: PictureLayer? = null
    private var _recorder: PictureRecorder? = null

    private var _canvas: Canvas? = null

    /**
     * The canvas on which to paint.
     *
     * The current canvas can change whenever you paint a child using this
     * context, which means it's fragile to hold a reference to the canvas
     * returned by this getter.
     */
    val canvas: Canvas
        get() {
            if (_canvas == null)
                _startRecording()
            return _canvas!!
        }

    private fun _startRecording() {
        assert(!_isRecording)
        _currentLayer = PictureLayer(estimatedBounds)
        _recorder = PictureRecorder()
        _canvas = Canvas(_recorder!!)
        _containerLayer.append(_currentLayer!!)
    }

    internal fun _stopRecordingIfNeeded() {
        if (!_isRecording)
            return
        assert {
            // TODO(Migration/andrey): do we need debug drawing?
//            if (debugRepaintRainbowEnabled) {
//                final Paint paint = new Paint()
//                ..style = PaintingStyle.stroke
//                ..strokeWidth = 6.0
//                ..color = debugCurrentRepaintColor.toColor();
//                canvas.drawRect(estimatedBounds.deflate(3.0), paint);
//            }
//            if (debugPaintLayerBordersEnabled) {
//                final Paint paint = new Paint()
//                ..style = PaintingStyle.stroke
//                ..strokeWidth = 1.0
//                ..color = const Color(0xFFFF9800);
//                canvas.drawRect(estimatedBounds, paint);
//            }
            true
        }
        assert(_currentLayer != null)
        assert(_recorder != null)
        _currentLayer!!.picture = _recorder!!.endRecording()
        _currentLayer = null
        _recorder = null
        _canvas = null
    }

    /**
     * Hints that the painting in the current layer is complex and would benefit
     * from caching.
     *
     * If this hint is not set, the compositor will apply its own heuristics to
     * decide whether the current layer is complex enough to benefit from
     * caching.
     */
    fun setIsComplexHint() {
        _currentLayer?.isComplexHint = true
    }

    /**
     * Hints that the painting in the current layer is likely to change next frame.
     *
     * This hint tells the compositor not to cache the current layer because the
     * cache will not be used in the future. If this hint is not set, the
     * compositor will apply its own heuristics to decide whether the current
     * layer is likely to be reused in the future.
     */
    fun setWillChangeHint() {
        _currentLayer?.willChangeHint = true
    }

    /**
     * Adds a composited leaf layer to the recording.
     *
     * After calling this function, the [canvas] property will change to refer to
     * a new [Canvas] that draws on top of the given layer.
     *
     * A [RenderObject] that uses this function is very likely to require its
     * [RenderObject.alwaysNeedsCompositing] property to return true. That informs
     * ancestor render objects that this render object will include a composited
     * layer, which, for example, causes them to use composited clips.
     *
     * See also:
     *
     *  * [pushLayer], for adding a layer and using its canvas to paint with that
     *    layer.
     */
    fun addLayer(layer: Layer) {
        _stopRecordingIfNeeded()
        _appendLayer(layer)
    }

    /**
     * Appends the given layer to the recording, and calls the `painter` callback
     * with that layer, providing the `childPaintBounds` as the estimated paint
     * bounds of the child. The `childPaintBounds` can be used for debugging but
     * have no effect on painting.
     *
     * The given layer must be an unattached orphan. (Providing a newly created
     * object, rather than reusing an existing layer, satisfies that
     * requirement.)
     *
     * The `offset` is the offset to pass to the `painter`.
     *
     * If the `childPaintBounds` are not specified then the current layer's paint
     * bounds are used. This is appropriate if the child layer does not apply any
     * transformation or clipping to its contents. The `childPaintBounds`, if
     * specified, must be in the coordinate system of the new layer, and should
     * not go outside the current layer's paint bounds.
     *
     * See also:
     *
     *  * [addLayer], for pushing a leaf layer whose canvas is not used.
     */
    // TODO(Migration/andrey): changed childLayer to ContainerLayer from Layer, as it casted later
    fun pushLayer(
        childLayer: ContainerLayer,
        painter: PaintingContextCallback,
        offset: Offset,
        childPaintBounds: Rect? = null
    ) {
        assert(!childLayer.attached)
        assert(childLayer.parent == null)
        assert(painter != null)
        _stopRecordingIfNeeded()
        _appendLayer(childLayer)
        val childContext = PaintingContext(childLayer, childPaintBounds ?: estimatedBounds)
        painter(childContext, offset)
        childContext._stopRecordingIfNeeded()
    }

    // TODO(Migration/andrey): needs ClipRectLayer and clipRect support on canvas
//    /// Clip further painting using a rectangle.
//    ///
//    /// * `needsCompositing` is whether the child needs compositing. Typically
//    ///   matches the value of [RenderObject.needsCompositing] for the caller.
//    /// * `offset` is the offset from the origin of the canvas' coordinate system
//    ///   to the origin of the caller's coordinate system.
//    /// * `clipRect` is rectangle (in the caller's coordinate system) to use to
//    ///   clip the painting done by [painter].
//    /// * `painter` is a callback that will paint with the [clipRect] applied. This
//    ///   function calls the [painter] synchronously.
//    void pushClipRect(bool needsCompositing, Offset offset, Rect clipRect, PaintingContextCallback painter) {
//        final Rect offsetClipRect = clipRect.shift(offset);
//        if (needsCompositing) {
//            pushLayer(new ClipRectLayer(clipRect: offsetClipRect), painter, offset, childPaintBounds: offsetClipRect);
//        } else {
//            canvas
//            ..save()
//            ..clipRect(offsetClipRect);
//            painter(this, offset);
//            canvas
//            ..restore();
//        }
//    }

    // TODO(Migration/andrey): needs ClipRRectLayer and clipRRect support on canvas
//    /// Clip further painting using a rounded rectangle.
//    ///
//    /// * `needsCompositing` is whether the child needs compositing. Typically
//    ///   matches the value of [RenderObject.needsCompositing] for the caller.
//    /// * `offset` is the offset from the origin of the canvas' coordinate system
//    ///   to the origin of the caller's coordinate system.
//    /// * `bounds` is the region of the canvas (in the caller's coordinate system)
//    ///   into which `painter` will paint in.
//    /// * `clipRRect` is the rounded-rectangle (in the caller's coordinate system)
//    ///   to use to clip the painting done by `painter`.
//    /// * `painter` is a callback that will paint with the `clipRRect` applied. This
//    ///   function calls the `painter` synchronously.
//    void pushClipRRect(bool needsCompositing, Offset offset, Rect bounds, RRect clipRRect, PaintingContextCallback painter) {
//        final Rect offsetBounds = bounds.shift(offset);
//        final RRect offsetClipRRect = clipRRect.shift(offset);
//        if (needsCompositing) {
//            pushLayer(new ClipRRectLayer(clipRRect: offsetClipRRect), painter, offset, childPaintBounds: offsetBounds);
//        } else {
//            canvas
//            ..save()
//            ..clipRRect(offsetClipRRect)
//            ..saveLayer(offsetBounds, _defaultPaint);
//            painter(this, offset);
//            canvas
//            ..restore()
//            ..restore();
//        }
//    }

    // TODO(Migration/andrey): needs ClipPathLayer and clipPath support on canvas
//    /// Clip further painting using a path.
//    ///
//    /// * `needsCompositing` is whether the child needs compositing. Typically
//    ///   matches the value of [RenderObject.needsCompositing] for the caller.
//    /// * `offset` is the offset from the origin of the canvas' coordinate system
//    ///   to the origin of the caller's coordinate system.
//    /// * `bounds` is the region of the canvas (in the caller's coordinate system)
//    ///   into which `painter` will paint in.
//    /// * `clipPath` is the path (in the coordinate system of the caller) to use to
//    ///   clip the painting done by `painter`.
//    /// * `painter` is a callback that will paint with the `clipPath` applied. This
//    ///   function calls the `painter` synchronously.
//    void pushClipPath(bool needsCompositing, Offset offset, Rect bounds, Path clipPath, PaintingContextCallback painter) {
//        final Rect offsetBounds = bounds.shift(offset);
//        final Path offsetClipPath = clipPath.shift(offset);
//        if (needsCompositing) {
//            pushLayer(new ClipPathLayer(clipPath: offsetClipPath), painter, offset, childPaintBounds: offsetBounds);
//        } else {
//            canvas
//            ..save()
//            ..clipPath(clipPath.shift(offset))
//            ..saveLayer(bounds.shift(offset), _defaultPaint);
//            painter(this, offset);
//            canvas
//            ..restore()
//            ..restore();
//        }
//    }
    // TODO(Migration/andrey): needs Matrix4 with translationValues, multiply, translate; Canvas with transform
//    /// Transform further painting using a matrix.
//    ///
//    /// * `needsCompositing` is whether the child needs compositing. Typically
//    ///   matches the value of [RenderObject.needsCompositing] for the caller.
//    /// * `offset` is the offset from the origin of the canvas' coordinate system
//    ///   to the origin of the caller's coordinate system.
//    /// * `transform` is the matrix to apply to the painting done by `painter`.
//    /// * `painter` is a callback that will paint with the `transform` applied. This
//    ///   function calls the `painter` synchronously.
//    void pushTransform(bool needsCompositing, Offset offset, Matrix4 transform, PaintingContextCallback painter) {
//        final Matrix4 effectiveTransform = new Matrix4.translationValues(offset.dx, offset.dy, 0.0)
//        ..multiply(transform)..translate(-offset.dx, -offset.dy);
//        if (needsCompositing) {
//            pushLayer(
//                    new TransformLayer(transform: effectiveTransform),
//            painter,
//            offset,
//            childPaintBounds: MatrixUtils.inverseTransformRect(effectiveTransform, estimatedBounds),
//            );
//        } else {
//            canvas
//            ..save()
//            ..transform(effectiveTransform.storage);
//            painter(this, offset);
//            canvas
//            ..restore();
//        }
//    }
//
    // TODO(Migration/andrey): needs OpacityLayer
//    /// Blend further painting with an alpha value.
//    ///
//    /// * `offset` is the offset from the origin of the canvas' coordinate system
//    ///   to the origin of the caller's coordinate system.
//    /// * `alpha` is the alpha value to use when blending the painting done by
//    ///   `painter`. An alpha value of 0 means the painting is fully transparent
//    ///   and an alpha value of 255 means the painting is fully opaque.
//    /// * `painter` is a callback that will paint with the `alpha` applied. This
//    ///   function calls the `painter` synchronously.
//    ///
//    /// A [RenderObject] that uses this function is very likely to require its
//    /// [RenderObject.alwaysNeedsCompositing] property to return true. That informs
//    /// ancestor render objects that this render object will include a composited
//    /// layer, which, for example, causes them to use composited clips.
//    void pushOpacity(Offset offset, int alpha, PaintingContextCallback painter) {
//        pushLayer(new OpacityLayer(alpha: alpha), painter, offset);
//    }

    override fun toString(): String {
        return "${runtimeType()}#${hashCode()}(layer: $_containerLayer, " +
                "canvas bounds: $estimatedBounds)"
    }
}
