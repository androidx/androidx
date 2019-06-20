/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.painting

import androidx.ui.Vertices
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.vectormath64.Matrix4

// TODO(mount/njawad): Separate the platform-independent API from the platform-dependent.
// TODO(Migration/njawad): Copy the class here
/**
 * An interface for recording graphical operations.
 *
 * [Canvas] objects are used in creating [Picture] objects, which can
 * themselves be used with a [SceneBuilder] to build a [Scene]. In
 * normal usage, however, this is all handled by the framework.
 *
 * A canvas has a current transformation matrix which is applied to all
 * operations. Initially, the transformation matrix is the identity transform.
 * It can be modified using the [translate], [scale], [rotate], [skew],
 * and [transform] methods.
 *
 * A canvas also has a current clip region which is applied to all operations.
 * Initially, the clip region is infinite. It can be modified using the
 * [clipRect], [clipRRect], and [clipPath] methods.
 *
 * The current transform and clip can be saved and restored using the stack
 * managed by the [save], [saveLayer], and [restore] methods.
 */

/**
 * Creates a canvas for recording graphical operations into the
 * given picture recorder.
 *
 * Graphical operations that affect pixels entirely outside the given
 * `cullRect` might be discarded by the implementation. However, the
 * implementation might draw outside these bounds if, for example, a command
 * draws partially inside and outside the `cullRect`. To ensure that pixels
 * outside a given region are discarded, consider using a [clipRect]. The
 * `cullRect` is optional; by default, all operations are kept.
 *
 * To end the recording, call [PictureRecorder.endRecording] on the
 * given recorder.
 */

// /* expect */ fun Canvas(image: Image): Canvas
//
// /* expect */ fun Canvas(
//    recorder: PictureRecorder,
//    cullRect: Rect = Rect.largest
// ): Canvas

/* expect */ typealias NativeCanvas = android.graphics.Canvas

interface Canvas {

    /**
     * Return an instance of the native primitive that implements the Canvas interface
     */
    val nativeCanvas: NativeCanvas

    /**
     * Saves a copy of the current transform and clip on the save stack.
     *
     * Call [restore] to pop the save stack.
     *
     * See also:
     *
     *  * [saveLayer], which does the same thing but additionally also groups the
     *    commands done until the matching [restore].
     */
    // TODO (njawad) replace with lambda overload when multi-child ComponentNode support is added
    fun save()

    /**
     * Pops the current save stack, if there is anything to pop.
     * Otherwise, does nothing.
     *
     * Use [save] and [saveLayer] to push state onto the stack.
     *
     * If the state was pushed with with [saveLayer], then this call will also
     * cause the new layer to be composited into the previous layer.
     */
    // TODO (njawad) remove when save with lambda receiver overload is supported with multi-child
    // ComponentNodes
    fun restore()

    /**
     * Saves a copy of the current transform and clip on the save stack, and then
     * creates a new group which subsequent calls will become a part of. When the
     * save stack is later popped, the group will be flattened into a layer and
     * have the given `paint`'s [Paint.colorFilter] and [Paint.blendMode]
     * applied.
     *
     * This lets you create composite effects, for example making a group of
     * drawing commands semi-transparent. Without using [saveLayer], each part of
     * the group would be painted individually, so where they overlap would be
     * darker than where they do not. By using [saveLayer] to group them
     * together, they can be drawn with an opaque color at first, and then the
     * entire group can be made transparent using the [saveLayer]'s paint.
     *
     * Call [restore] to pop the save stack and apply the paint to the group.
     *
     * ## Using saveLayer with clips
     *
     * When a rectangular clip operation (from [clipRect]) is not axis-aligned
     * with the raster buffer, or when the clip operation is not rectalinear (e.g.
     * because it is a rounded rectangle clip created by [clipRRect] or an
     * arbitrarily complicated path clip created by [clipPath]), the edge of the
     * clip needs to be anti-aliased.
     *
     * If two draw calls overlap at the edge of such a clipped region, without
     * using [saveLayer], the first drawing will be anti-aliased with the
     * background first, and then the second will be anti-aliased with the result
     * of blending the first drawing and the background. On the other hand, if
     * [saveLayer] is used immediately after establishing the clip, the second
     * drawing will cover the first in the layer, and thus the second alone will
     * be anti-aliased with the background when the layer is clipped and
     * composited (when [restore] is called).
     *
     * For example, this [CustomPainter.paint] method paints a clean white
     * rounded rectangle:
     *
     * ```dart
     * void paint(Canvas canvas, Size size) {
     *   Rect rect = Offset.zero & size;
     *   canvas.save();
     *   canvas.clipRRect(new RRect.fromRectXY(rect, 100.0, 100.0));
     *   canvas.saveLayer(rect, new Paint());
     *   canvas.drawPaint(new Paint()..color = Colors.red);
     *   canvas.drawPaint(new Paint()..color = Colors.white);
     *   canvas.restore();
     *   canvas.restore();
     * }
     * ```
     *
     * On the other hand, this one renders a red outline, the result of the red
     * paint being anti-aliased with the background at the clip edge, then the
     * white paint being similarly anti-aliased with the background _including
     * the clipped red paint_:
     *
     * ```dart
     * void paint(Canvas canvas, Size size) {
     *   // (this example renders poorly, prefer the example above)
     *   Rect rect = Offset.zero & size;
     *   canvas.save();
     *   canvas.clipRRect(new RRect.fromRectXY(rect, 100.0, 100.0));
     *   canvas.drawPaint(new Paint()..color = Colors.red);
     *   canvas.drawPaint(new Paint()..color = Colors.white);
     *   canvas.restore();
     * }
     * ```
     *
     * This point is moot if the clip only clips one draw operation. For example,
     * the following paint method paints a pair of clean white rounded
     * rectangles, even though the clips are not done on a separate layer:
     *
     * ```dart
     * void paint(Canvas canvas, Size size) {
     *   canvas.save();
     *   canvas.clipRRect(new RRect.fromRectXY(Offset.zero & (size / 2.0), 50.0, 50.0));
     *   canvas.drawPaint(new Paint()..color = Colors.white);
     *   canvas.restore();
     *   canvas.save();
     *   canvas.clipRRect(new RRect.fromRectXY(size.center(Offset.zero) & (size / 2.0), 50.0, 50.0));
     *   canvas.drawPaint(new Paint()..color = Colors.white);
     *   canvas.restore();
     * }
     * ```
     *
     * (Incidentally, rather than using [clipRRect] and [drawPaint] to draw
     * rounded rectangles like this, prefer the [drawRRect] method. These
     * examples are using [drawPaint] as a proxy for "complicated draw operations
     * that will get clipped", to illustrate the point.)
     *
     * ## Performance considerations
     *
     * Generally speaking, [saveLayer] is relatively expensive.
     *
     * There are a several different hardware architectures for GPUs (graphics
     * processing units, the hardware that handles graphics), but most of them
     * involve batching commands and reordering them for performance. When layers
     * are used, they cause the rendering pipeline to have to switch render
     * target (from one layer to another). Render target switches can flush the
     * GPU's command buffer, which typically means that optimizations that one
     * could get with larger batching are lost. Render target switches also
     * generate a lot of memory churn because the GPU needs to copy out the
     * current frame buffer contents from the part of memory that's optimized for
     * writing, and then needs to copy it back in once the previous render target
     * (layer) is restored.
     *
     * See also:
     *
     *  * [save], which saves the current state, but does not create a new layer
     *    for subsequent commands.
     *  * [BlendMode], which discusses the use of [Paint.blendMode] with
     *    [saveLayer].
     */
    @SuppressWarnings("deprecation")
    fun saveLayer(bounds: Rect?, paint: Paint)

    // TODO(Migration/njawad find equivalent implementation for _saveLayerWithoutBounds or not
//    void _saveLayerWithoutBounds(List<dynamic> paintObjects, ByteData paintData)
//    native 'Canvas_saveLayerWithoutBounds';
//    void _saveLayer(double left,
//    double top,
//    double right,
//    double bottom,
//    List<dynamic> paintObjects,
//    ByteData paintData) native 'Canvas_saveLayer';

    /**
     * Add a translation to the current transform, shifting the coordinate space
     * horizontally by the first argument and vertically by the second argument.
     */
    fun translate(dx: Float, dy: Float)

    /**
     * Add an axis-aligned scale to the current transform, scaling by the first
     * argument in the horizontal direction and the second in the vertical
     * direction.
     *
     * If [sy] is unspecified, [sx] will be used for the scale in both
     * directions.
     */
    fun scale(sx: Float, sy: Float = sx)

    /** Add a rotation to the current transform. The argument is in degrees clockwise. */
    fun rotate(degrees: Float)

    /**
     * Add an axis-aligned skew to the current transform, with the first argument
     * being the horizontal skew in degrees clockwise around the origin, and the
     * second argument being the vertical skew in degrees clockwise around the
     * origin.
     */
    fun skew(sx: Float, sy: Float)

    /**
     * Multiply the current transform by the specified 4⨉4 transformation matrix
     * specified as a list of values in column-major order.
     */
    fun concat(matrix4: Matrix4)

    /**
     * Reduces the clip region to the intersection of the current clip and the
     * given rectangle.
     *
     * If the clip is not axis-aligned with the display device, and
     * [Paint.isAntiAlias] is true, then the clip will be anti-aliased. If
     * multiple draw commands intersect with the clip boundary, this can result
     * in incorrect blending at the clip boundary. See [saveLayer] for a
     * discussion of how to address that.
     *
     * Use [ClipOp.difference] to subtract the provided rectangle from the
     * current clip.
     */
    @SuppressWarnings("deprecation")
    fun clipRect(rect: Rect, clipOp: ClipOp = ClipOp.intersect)

    /**
     * Reduces the clip region to the intersection of the current clip and the
     * given rounded rectangle.
     *
     * If [Paint.isAntiAlias] is true, then the clip will be anti-aliased. If
     * multiple draw commands intersect with the clip boundary, this can result
     * in incorrect blending at the clip boundary. See [saveLayer] for a
     * discussion of how to address that and some examples of using [clipRRect].
     */
    fun clipRRect(rrect: RRect)

    /**
     * Reduces the clip region to the intersection of the current clip and the
     * given [Path].
     *
     * If [Paint.isAntiAlias] is true, then the clip will be anti-aliased. If
     * multiple draw commands intersect with the clip boundary, this can result
     * in incorrect blending at the clip boundary. See [saveLayer] for a
     * discussion of how to address that.
     */
    fun clipPath(path: Path)

    /**
     * Paints the given [Color] onto the canvas, applying the given
     * [BlendMode], with the given color being the source and the background
     * being the destination.
     */
    fun drawColor(color: Color, blendMode: BlendMode)

    /**
     * Draws a line between the given points using the given paint. The line is
     * stroked, the value of the [Paint.style] is ignored for this call.
     *
     * The `p1` and `p2` arguments are interpreted as offsets from the origin.
     */
    fun drawLine(p1: Offset, p2: Offset, paint: Paint)

    /**
     * Fills the canvas with the given [Paint].
     *
     * To fill the canvas with a solid color and blend mode, consider
     * [drawColor] instead.
     */
    fun drawPaint(paint: Paint)

    /**
     * Draws a rectangle with the given [Paint]. Whether the rectangle is filled
     * or stroked (or both) is controlled by [Paint.style].
     */
    fun drawRect(rect: Rect, paint: Paint)

    /**
     * Draws a rounded rectangle with the given [Paint]. Whether the rectangle is
     * filled or stroked (or both) is controlled by [Paint.style].
     */
    fun drawRRect(rrect: RRect, paint: Paint)

    /**
     * Draws a shape consisting of the difference between two rounded rectangles
     * with the given [Paint]. Whether this shape is filled or stroked (or both)
     * is controlled by [Paint.style].
     *
     * This shape is almost but not quite entirely unlike an annulus.
     */
    fun drawDRRect(outer: RRect, inner: RRect, paint: Paint)

    /**
     * Draws an axis-aligned oval that fills the given axis-aligned rectangle
     * with the given [Paint]. Whether the oval is filled or stroked (or both) is
     * controlled by [Paint.style].
     */
    fun drawOval(rect: Rect, paint: Paint)

    /**
     * Draws a circle centered at the point given by the first argument and
     * that has the radius given by the second argument, with the [Paint] given in
     * the third argument. Whether the circle is filled or stroked (or both) is
     * controlled by [Paint.style].
     */
    fun drawCircle(center: Offset, radius: Float, paint: Paint)

    /**
     * Draw an arc scaled to fit inside the given rectangle. It starts from
     * startAngle degrees around the oval up to startAngle + sweepAngle
     * degrees around the oval, with zero degrees being the point on
     * the right hand side of the oval that crosses the horizontal line
     * that intersects the center of the rectangle and with positive
     * angles going clockwise around the oval. If useCenter is true, the arc is
     * closed back to the center, forming a circle sector. Otherwise, the arc is
     * not closed, forming a circle segment.
     *
     * This method is optimized for drawing arcs and should be faster than [Path.arcTo].
     */
    fun drawArc(
        rect: Rect,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    )

    /**
     * Draws the given [Path] with the given [Paint]. Whether this shape is
     * filled or stroked (or both) is controlled by [Paint.style]. If the path is
     * filled, then subpaths within it are implicitly closed (see [Path.close]).
     */
    fun drawPath(path: Path, paint: Paint)

    /**
     * Draws the given [Image] into the canvas with its top-left corner at the
     * given [Offset]. The image is composited into the canvas using the given [Paint].
     */
    fun drawImage(image: Image, topLeftOffset: Offset, paint: Paint)

//    void _drawImage(Image image,
//    double x,
//    double y,
//    List<dynamic> paintObjects,
//    ByteData paintData) native 'Canvas_drawImage';
//
    /**
     * Draws the subset of the given image described by the `src` argument into
     * the canvas in the axis-aligned rectangle given by the `dst` argument.
     *
     * This might sample from outside the `src` rect by up to half the width of
     * an applied filter.
     *
     * Multiple calls to this method with different arguments (from the same
     * image) can be batched into a single call to [drawAtlas] to improve
     * performance.
     */
    fun drawImageRect(image: Image, src: Rect, dst: Rect, paint: Paint)

    /**
     * Draw the given picture onto the canvas. To create a picture, see
     * [PictureRecorder].
     */
    fun drawPicture(picture: Picture)

    /**
     * Draws the text in the given [Paragraph] into this canvas at the given [Offset].
     *
     * The [Paragraph] object must have had [Paragraph.layout] called on it first.
     *
     * To align the text, set the `textAlign` on the [ParagraphStyle] object passed to the
     * [new ParagraphBuilder] constructor. For more details see [TextAlign] and the discussion at
     * [new ParagraphStyle].
     *
     * If the text is left aligned or justified, the left margin will be at the position specified
     * by the `offset` argument's [Offset.dx] coordinate.
     *
     * If the text is right aligned or justified, the right margin will be at the position described
     * by adding the [ParagraphConstraints.width] given to [Paragraph.layout], to the `offset`
     * argument's [Offset.dx] coordinate.
     *
     * If the text is centered, the centering axis will be at the position described by adding half
     * of the [ParagraphConstraints.width] given to [Paragraph.layout], to the `offset` argument's
     * [Offset.dx] coordinate.
     */
    // TODO(siyamed): Decide what to do with this method. Should it exist on Canvas?
//    fun drawParagraph(paragraph: Paragraph, offset: Offset) {
//        assert(paragraph != null)
//        assert(Offset.isValid(offset))
//        paragraph.paint(this, offset.dx, offset.dy)
//    }

    /**
     * Draws a sequence of points according to the given [PointMode].
     *
     * The `points` argument is interpreted as offsets from the origin.
     *
     * See also:
     *
     *  * [drawRawPoints], which takes `points` as a [Float32List] rather than a
     *    [List<Offset>].
     */
    fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint)

    /**
     * Draws a sequence of points according to the given [PointMode].
     *
     * The `points` argument is interpreted  as a list of pairs of floating point
     * numbers, where each pair represents an x and y offset from the origin.
     *
     * See also:
     *
     *  * [drawPoints], which takes `points` as a [List<Offset>] rather than a
     *    [List<Float32List>].
     */
    fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint)
//
//    void _drawPoints(List<dynamic> paintObjects,
//    ByteData paintData,
//    int pointMode,
//    Float32List points) native 'Canvas_drawPoints';
    fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint)

//    //
//    // See also:
//    //
//    //  * [drawRawAtlas], which takes its arguments as typed data lists rather
//    //    than objects.
    // TODO(Migration/njawad provide canvas atlas support with framework APIs)
//    void drawAtlas(Image atlas,
//    List<RSTransform> transforms,
//    List<Rect> rects,
//    List<Color> colors,
//    BlendMode blendMode,
//    Rect cullRect,
//    Paint paint) {
//        assert(atlas != null); // atlas is checked on the engine side
//        assert(transforms != null);
//        assert(rects != null);
//        assert(colors != null);
//        assert(blendMode != null);
//        assert(paint != null);
//
//        final int rectCount = rects.length;
//        if (transforms.length != rectCount)
//            throw new ArgumentError('"transforms" and "rects" lengths must match.');
//        if (colors.isNotEmpty && colors.length != rectCount)
//            throw new ArgumentError('If non-null, "colors" length must match that of "transforms" and "rects".');
//
//        final Float32List rstTransformBuffer = new Float32List(rectCount * 4);
//        final Float32List rectBuffer = new Float32List(rectCount * 4);
//
//        for (int i = 0; i < rectCount; ++i) {
//            final int index0 = i * 4;
//            final int index1 = index0 + 1;
//            final int index2 = index0 + 2;
//            final int index3 = index0 + 3;
//            final RSTransform rstTransform = transforms[i];
//            final Rect rect = rects[i];
//            assert(_rectIsValid(rect));
//            rstTransformBuffer[index0] = rstTransform.scos;
//            rstTransformBuffer[index1] = rstTransform.ssin;
//            rstTransformBuffer[index2] = rstTransform.tx;
//            rstTransformBuffer[index3] = rstTransform.ty;
//            rectBuffer[index0] = rect.left;
//            rectBuffer[index1] = rect.top;
//            rectBuffer[index2] = rect.right;
//            rectBuffer[index3] = rect.bottom;
//        }
//
//        final Int32List colorBuffer = colors.isEmpty ? null : _encodeColorList(colors);
//        final Float32List cullRectBuffer = cullRect?._value;
//
//        _drawAtlas(
//                paint._objects, paint._data, atlas, rstTransformBuffer, rectBuffer,
//                colorBuffer, blendMode.index, cullRectBuffer
//        );
//    }
//
//    //
//    // The `rstTransforms` argument is interpreted as a list of four-tuples, with
//    // each tuple being ([RSTransform.scos], [RSTransform.ssin],
//    // [RSTransform.tx], [RSTransform.ty]).
//    //
//    // The `rects` argument is interpreted as a list of four-tuples, with each
//    // tuple being ([Rect.left], [Rect.top], [Rect.right], [Rect.bottom]).
//    //
//    // The `colors` argument, which can be null, is interpreted as a list of
//    // 32-bit colors, with the same packing as [Color.value].
//    //
//    // See also:
//    //
//    //  * [drawAtlas], which takes its arguments as objects rather than typed
//    //    data lists.
    // TODO(Migration/njawad provide canvas atlas support with framework APIs)
//    void drawRawAtlas(Image atlas,
//    Float32List rstTransforms,
//    Float32List rects,
//    Int32List colors,
//    BlendMode blendMode,
//    Rect cullRect,
//    Paint paint) {
//        assert(atlas != null); // atlas is checked on the engine side
//        assert(rstTransforms != null);
//        assert(rects != null);
//        assert(colors != null);
//        assert(blendMode != null);
//        assert(paint != null);
//
//        final int rectCount = rects.length;
//        if (rstTransforms.length != rectCount)
//            throw new ArgumentError('"rstTransforms" and "rects" lengths must match.');
//        if (rectCount % 4 != 0)
//            throw new ArgumentError('"rstTransforms" and "rects" lengths must be a multiple of four.');
//        if (colors != null && colors.length * 4 != rectCount)
//            throw new ArgumentError('If non-null, "colors" length must be one fourth the length of "rstTransforms" and "rects".');
//
//        _drawAtlas(
//                paint._objects, paint._data, atlas, rstTransforms, rects,
//                colors, blendMode.index, cullRect?._value
//        );
//    }
//
//    void _drawAtlas(List<dynamic> paintObjects,
//    ByteData paintData,
//    Image atlas,
//    Float32List rstTransforms,
//    Float32List rects,
//    Int32List colors,
//    int blendMode,
//    Float32List cullRect) native 'Canvas_drawAtlas';
//
//    /// Draws a shadow for a [Path] representing the given material elevation.
//    ///
//    /// The `transparentOccluder` argument should be true if the occluding object
//    /// is not opaque.
//    ///
//    /// The arguments must not be null.
    // TODO(Migration/njawad provide canvas shadow support with framework APIs)
//    void drawShadow(Path path, Color color, double elevation, bool transparentOccluder) {
//        assert(path != null); // path is checked on the engine side
//        assert(color != null);
//        assert(transparentOccluder != null);
//        _drawShadow(path, color.value, elevation, transparentOccluder);
//    }
//    void _drawShadow(Path path,
//    int color,
//    double elevation,
//    bool transparentOccluder) native 'Canvas_drawShadow';
}
