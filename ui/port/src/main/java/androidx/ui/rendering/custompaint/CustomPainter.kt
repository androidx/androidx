/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering.custompaint

import androidx.ui.VoidCallback
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.change_notifier.Listenable
import androidx.ui.foundation.diagnostics.describeIdentity
import androidx.ui.painting.Canvas

/**
 * The interface used by [CustomPaint] (in the widgets library) and
 * [RenderCustomPaint] (in the rendering library).
 *
 * To implement a custom painter, either subclass or implement this interface
 * to define your custom paint delegate. [CustomPaint] subclasses must
 * implement the [paint] and [shouldRepaint] methods, and may optionally also
 * implement the [hitTest] and [shouldRebuildSemantics] methods, and the
 * [semanticsBuilder] getter.
 *
 * The [paint] method is called whenever the custom object needs to be repainted.
 *
 * The [shouldRepaint] method is called when a new instance of the class
 * is provided, to check if the new instance actually represents different
 * information.
 *
 * The most efficient way to trigger a repaint is to either:
 *
 * * Extend this class and supply a `repaint` argument to the constructor of
 *   the [CustomPainter], where that object notifies its listeners when it is
 *   time to repaint.
 * * Extend [Listenable] (e.g. via [ChangeNotifier]) and implement
 *   [CustomPainter], so that the object itself provides the notifications
 *   directly.
 *
 * In either case, the [CustomPaint] widget or [RenderCustomPaint]
 * render object will listen to the [Listenable] and repaint whenever the
 * animation ticks, avoiding both the build and layout phases of the pipeline.
 *
 * The [hitTest] method is called when the user interacts with the underlying
 * render object, to determine if the user hit the object or missed it.
 *
 * The [semanticsBuilder] is called whenever the custom object needs to rebuild
 * its semantics information.
 *
 * The [shouldRebuildSemantics] method is called when a new instance of the
 * class is provided, to check if the new instance contains different
 * information that affects the semantics tree.
 *
 * ## Sample code
 *
 * This sample extends the same code shown for [RadialGradient] to create a
 * custom painter that paints a sky.
 *
 * // TODO("Migration|Andrey: Change to kotlin if we will keep this class")
 * ```dart
 * class Sky extends CustomPainter {
 *   @override
 *   void paint(Canvas canvas, Size size) {
 *     var rect = Offset.zero & size;
 *     var gradient = new RadialGradient(
 *       center: const Alignment(0.7, -0.6),
 *       radius: 0.2,
 *       colors: [const Color(0xFFFFFF00), const Color(0xFF0099FF)],
 *       stops: [0.4, 1.0],
 *     );
 *     canvas.drawRect(
 *       rect,
 *       new Paint()..shader = gradient.createShader(rect),
 *     );
 *   }
 *
 *   @override
 *   SemanticsBuilderCallback get semanticsBuilder {
 *     return (Size size) {
 *       // Annotate a RECTANGLE containing the picture of the sun
 *       // with the label "Sun". When text to speech feature is enabled on the
 *       // device, a user will be able to locate the sun on this picture by
 *       // touch.
 *       var rect = Offset.zero & size;
 *       var width = size.shortestSide * 0.4;
 *       rect = const Alignment(0.8, -0.9).inscribe(new Size(width, width), rect);
 *       return [
 *         new CustomPainterSemantics(
 *           rect: rect,
 *           properties: new SemanticsProperties(
 *             label: 'Sun',
 *             textDirection: TextDirection.ltr,
 *           ),
 *         ),
 *       ];
 *     };
 *   }
 *
 *   // Since this Sky painter has no fields, it always paints
 *   // the same thing and semantics information is the same.
 *   // Therefore we return false here. If we had fields (set
 *   // from the constructor) then we would return true if any
 *   // of them differed from the same fields on the oldDelegate.
 *   @override
 *   bool shouldRepaint(Sky oldDelegate) => false;
 *   @override
 *   bool shouldRebuildSemantics(Sky oldDelegate) => false;
 * }
 * ```
 *
 * See also:
 *
 *  * [Canvas], the class that a custom painter uses to paint.
 *  * [CustomPaint], the widget that uses [CustomPainter], and whose sample
 *    code shows how to use the above `Sky` class.
 *  * [RadialGradient], whose sample code section shows a different take
 *    on the sample code above.
 */
abstract class CustomPainter(
    /** The painter will repaint whenever `repaint` notifies its listeners. */
    private val repaint: Listenable? = null
) : Listenable {

    /**
     * Register a closure to be notified when it is time to repaint.
     *
     * The [CustomPainter] implementation merely forwards to the same method on
     * the [Listenable] provided to the constructor in the `repaint` argument, if
     * it was not null.
     */
    override fun addListener(listener: VoidCallback) {
        repaint?.addListener(listener)
    }

    /**
     * Remove a previously registered closure from the list of closures that the
     * object notifies when it is time to repaint.
     *
     * The [CustomPainter] implementation merely forwards to the same method on
     * the [Listenable] provided to the constructor in the `repaint` argument, if
     * it was not null.
     */
    override fun removeListener(listener: VoidCallback) {
        repaint?.removeListener(listener)
    }

    /**
     * Called whenever the object needs to paint. The given [Canvas] has its
     * coordinate space configured such that the origin is at the top left of the
     * box. The area of the box is the size of the [size] argument.
     *
     * Paint operations should remain inside the given area. Graphical operations
     * outside the bounds may be silently ignored, clipped, or not clipped.
     *
     * Implementations should be wary of correctly pairing any calls to
     * [Canvas.save]/[Canvas.saveLayer] and [Canvas.restore], otherwise all
     * subsequent painting on this canvas may be affected, with potentially
     * hilarious but confusing results.
     *
     * To paint text on a [Canvas], use a [TextPainter].
     *
     * To paint an image on a [Canvas]:
     *
     * 1. Obtain an [ImageStream], for example by calling [ImageProvider.resolve]
     *    on an [AssetImage] or [NetworkImage] object.
     *
     * 2. Whenever the [ImageStream]'s underlying [ImageInfo] object changes
     *    (see [ImageStream.addListener]), create a new instance of your custom
     *    paint delegate, giving it the new [ImageInfo] object.
     *
     * 3. In your delegate's [paint] method, call the [Canvas.drawImage],
     *    [Canvas.drawImageRect], or [Canvas.drawImageNine] methods to paint the
     *    [ImageInfo.image] object, applying the [ImageInfo.scale] value to
     *    obtain the correct rendering size.
     */
    abstract fun paint(canvas: Canvas, size: Size)

    /**
     * Returns a function that builds semantic information for the picture drawn
     * by this painter.
     *
     * If the returned function is null, this painter will not contribute new
     * [SemanticsNode]s to the semantics tree and the [CustomPaint] corresponding
     * to this painter will not create a semantics boundary. However, if
     * [CustomPaint.child] is not null, the child may contribute [SemanticsNode]s
     * to the tree.
     *
     * See also:
     *
     * * [SemanticsConfiguration.isSemanticBoundary], which causes new
     *   [SemanticsNode]s to be added to the semantics tree.
     * * [RenderCustomPaint], which uses this getter to build semantics.
     */
    open val semanticsBuilder: SemanticsBuilderCallback? = null

    /**
     * Called whenever a new instance of the custom painter delegate class is
     * provided to the [RenderCustomPaint] object, or any time that a new
     * [CustomPaint] object is created with a new instance of the custom painter
     * delegate class (which amounts to the same thing, because the latter is
     * implemented in terms of the former).
     *
     * If the new instance would cause [semanticsBuilder] to create different
     * semantics information, then this method should return true, otherwise it
     * should return false.
     *
     * If the method returns false, then the [semanticsBuilder] call might be
     * optimized away.
     *
     * It's possible that the [semanticsBuilder] will get called even if
     * [shouldRebuildSemantics] would return false. For example, it is called
     * when the [CustomPaint] is rendered for the very first time, or when the
     * box changes its size.
     *
     * By default this method delegates to [shouldRepaint] under the assumption
     * that in most cases semantics change when something new is drawn.
     */
    open fun shouldRebuildSemantics(oldDelegate: CustomPainter) = shouldRepaint(oldDelegate)

    /**
     * Called whenever a new instance of the custom painter delegate class is
     * provided to the [RenderCustomPaint] object, or any time that a new
     * [CustomPaint] object is created with a new instance of the custom painter
     * delegate class (which amounts to the same thing, because the latter is
     * implemented in terms of the former).
     *
     * If the new instance represents different information than the old
     * instance, then the method should return true, otherwise it should return
     * false.
     *
     * If the method returns false, then the [paint] call might be optimized
     * away.
     *
     * It's possible that the [paint] method will get called even if
     * [shouldRepaint] returns false (e.g. if an ancestor or descendant needed to
     * be repainted). It's also possible that the [paint] method will get called
     * without [shouldRepaint] being called at all (e.g. if the box changes
     * size).
     *
     * If a custom delegate has a particularly expensive paint function such that
     * repaints should be avoided as much as possible, a [RepaintBoundary] or
     * [RenderRepaintBoundary] (or other render object with
     * [RenderObject.isRepaintBoundary] set to true) might be helpful.
     */
    abstract fun shouldRepaint(oldDelegate: CustomPainter): Boolean

    /**
     * Called whenever a hit test is being performed on an object that is using
     * this custom paint delegate.
     *
     * The given point is relative to the same coordinate space as the last
     * [paint] call.
     *
     * The default behavior is to consider all points to be hits for
     * background painters, and no points to be hits for foreground painters.
     *
     * Return true if the given position corresponds to a point on the drawn
     * image that should be considered a "hit", false if it corresponds to a
     * point that should be considered outside the painted image, and null to use
     * the default behavior.
     */
    open fun hitTest(position: Offset?): Boolean? = null

    override fun toString(): String {
        return "${describeIdentity(this)} ${repaint?.toString() ?: ""})"
    }
}