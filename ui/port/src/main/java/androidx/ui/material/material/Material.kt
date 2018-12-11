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

package androidx.ui.material.material

import androidx.annotation.CallSuper
import androidx.ui.VoidCallback
import androidx.ui.animation.Curve
import androidx.ui.animation.Curves
import androidx.ui.animation.Tween
import androidx.ui.assert
import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.TextStyle
import androidx.ui.foundation.Key
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.DoubleProperty
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.painting.borderradius.BorderRadius
import androidx.ui.painting.borders.CircleBorder
import androidx.ui.painting.borders.RoundedRectangleBorder
import androidx.ui.painting.borders.ShapeBorder
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.custompaint.CustomPainter
import androidx.ui.rendering.obj.PaintingContext
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.proxybox.RenderProxyBox
import androidx.ui.rendering.proxybox.ShapeBorderClipper
import androidx.ui.scheduler.ticker.TickerProvider
import androidx.ui.vectormath64.Matrix4
import androidx.ui.widgets.basic.ClipPath
import androidx.ui.widgets.basic.CustomPaint
import androidx.ui.widgets.basic.Directionality
import androidx.ui.widgets.basic.PhysicalShape
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget
import androidx.ui.widgets.framework.StatelessWidget
import androidx.ui.widgets.framework.TypeMatcher
import androidx.ui.widgets.framework.Widget
import androidx.ui.widgets.framework.key.GlobalKey
import androidx.ui.widgets.implicitanimation.AnimatedWidgetBaseState
import androidx.ui.widgets.implicitanimation.ImplicitlyAnimatedWidget
import androidx.ui.widgets.implicitanimation.TweenVisitor
import androidx.ui.widgets.tickerprovider.TickerProviderStateMixin

/**
 * Signature for the callback used by ink effects to obtain the RECTANGLE for the effect.
 *
 * Used by [InkHighlight] and [InkSplash], for example.
 */
typealias RectCallback = () -> Rect

/**
 * The various kinds of material in material design. Used to
 * configure the default behavior of [Material] widgets.
 *
 * See also:
 *
 *  * [Material], in particular [Material.type]
 *  * [MaterialEdges]
 */
enum class MaterialType {
    /** Rectangle using default theme canvas color. */
    CANVAS,

    /** Rounded edges, card theme color. */
    CARD,

    /** A circle, no color by default (used for floating action buttons). */
    CIRCLE,

    /** Rounded edges, no color by default (used for [MaterialButton] buttons). */
    BUTTON,

    /**
     * A transparent piece of material that draws ink splashes and highlights.
     *
     * While the material metaphor describes child widgets as printed on the
     * material itself and do not hide ink effects, in practice the [Material]
     * widget draws child widgets on top of the ink effects.
     * A [Material] with type transparency can be placed on top of opaque widgets
     * to show ink effects on top of them.
     *
     * Prefer using the [Ink] widget for showing ink effects on top of opaque
     * widgets.
     */
    TRANSPARENCY
}

/**
 * The border radii used by the various kinds of material in material design.
 *
 * See also:
 *
 *  * [MaterialType]
 *  * [Material]
 */
val MaterialEdges = mapOf(
    MaterialType.CANVAS to null,
    MaterialType.CARD to BorderRadius.circular(2.0),
    MaterialType.CIRCLE to null,
    MaterialType.BUTTON to BorderRadius.circular(2.0),
    MaterialType.TRANSPARENCY to null
)

/** The default radius of an ink splash in logical pixels. */
// TODO("Migration|Andrey:  Defined in dps, but will actually be drawn as pixels! Solve it!")
const val DefaultSplashRadius: Double = 35.0

/**
 * An interface for creating [InkSplash]s and [InkHighlight]s on a material.
 *
 * Typically obtained via [Material.of].
 */
interface MaterialInkController {
    /** The color of the material. */
    val color: Color?

    /**
     * The ticker provider used by the controller.
     *
     * Ink features that are added to this controller with [addInkFeature] should
     * use this vsync to drive their animations.
     */
    val vsync: TickerProvider

    /**
     * Add an [InkFeature], such as an [InkSplash] or an [InkHighlight].
     *
     * The ink feature will paint as part of this controller.
     */
    fun addInkFeature(feature: InkFeature)

    /** Notifies the controller that one of its ink features needs to repaint. */
    fun markNeedsPaint()
}

/**
 * A piece of material.
 *
 * The Material widget is responsible for:
 *
 * 1. Clipping: Material clips its widget sub-tree to the shape specified by
 *    [shape], [type], and [borderRadius].
 * 2. Elevation: Material elevates its widget sub-tree on the Z axis by
 *    [elevation] pixels, and draws the appropriate shadow.
 * 3. Ink effects: Material shows ink effects implemented by [InkFeature]s
 *    like [InkSplash] and [InkHighlight] below its children.
 *
 * ## The Material Metaphor
 *
 * Material is the central metaphor in material design. Each piece of material
 * exists at a given elevation, which influences how that piece of material
 * visually relates to other pieces of material and how that material casts
 * shadows.
 *
 * Most user interface elements are either conceptually printed on a piece of
 * material or themselves made of material. Material reacts to user input using
 * [InkSplash] and [InkHighlight] effects. To trigger a reaction on the
 * material, use a [MaterialInkController] obtained via [Material.of].
 *
 * In general, the features of a [Material] should not change over time (e.g. a
 * [Material] should not change its [color], [shadowColor] or [type]).
 * Changes to [elevation] and [shadowColor] are animated for [animationDuration].
 * Changes to [shape] are animated if [type] is not [MaterialType.TRANSPARENCY]
 * and [ShapeBorder.lerp] between the previous and next [shape] values is
 * supported. Shape changes are also animated for [animationDuration].
 *
 *
 * ## Shape
 *
 * The shape for material is determined by [shape], [type], and [borderRadius].
 *
 *  - If [shape] is non null, it determines the shape.
 *  - If [shape] is null and [borderRadius] is non null, the shape is a
 *    rounded rectangle, with corners specified by [borderRadius].
 *  - If [shape] and [borderRadius] are null, [type] determines the
 *    shape as follows:
 *    - [MaterialType.CANVAS]: the default material shape is a rectangle.
 *    - [MaterialType.CARD]: the default material shape is a rectangle with
 *      rounded edges. The edge radii is specified by [MaterialEdges].
 *    - [MaterialType.CIRCLE]: the default material shape is a CIRCLE.
 *    - [MaterialType.BUTTON]: the default material shape is a rectangle with
 *      rounded edges. The edge radii is specified by [MaterialEdges].
 *    - [MaterialType.TRANSPARENCY]: the default material shape is a rectangle.
 *
 * ## Border
 *
 * If [shape] is not null, then its border will also be painted (if any).
 *
 * ## Layout change notifications
 *
 * If the layout changes (e.g. because there"s a list on the material, and it"s
 * been scrolled), a [LayoutChangedNotification] must be dispatched at the
 * relevant subtree. This in particular means that transitions (e.g.
 * [SlideTransition]) should not be placed inside [Material] widgets so as to
 * move subtrees that contain [InkResponse]s, [InkWell]s, [Ink]s, or other
 * widgets that use the [InkFeature] mechanism. Otherwise, in-progress ink
 * features (e.g., ink splashes and ink highlights) won"t move to account for
 * the layout.
 *
 * See also:
 *
 * * [MergeableMaterial], a piece of material that can split and remerge.
 * * [Card], a wrapper for a [Material] of [type] [MaterialType.CARD].
 * * <https://material.google.com/>
 *
 * Creates a piece of material.
 *
 * The [type], [elevation], [shadowColor], and [animationDuration] arguments
 * must not be null.
 *
 * If a [shape] is specified, then the [borderRadius] property must be
 * null and the [type] property must not be [MaterialType.CIRCLE]. If the
 * [borderRadius] is specified, then the [type] property must not be
 * [MaterialType.CIRCLE]. In both cases, these restrictions are intended to
 * catch likely errors.
 */
class Material(
    key: Key? = null,
    /**
     * The kind of material to show (e.g., card or canvas). This
     * affects the shape of the widget, the roundness of its corners if
     * the shape is rectangular, and the default color.
     */
    val type: MaterialType = MaterialType.CANVAS,
    /**
     * The z-coordinate at which to place this material. This controls the size
     * of the shadow below the material.
     *
     * If this is non-zero, the contents of the material are clipped, because the
     * widget conceptually defines an independent printed piece of material.
     *
     * Defaults to 0. Changing this value will cause the shadow to animate over
     * [animationDuration].
     */
    val elevation: Double = 0.0,
    /**
     * The color to paint the material.
     *
     * Must be opaque. To create a transparent piece of material, use
     * [MaterialType.TRANSPARENCY].
     *
     * By default, the color is derived from the [type] of material.
     */
    val color: Color? = null,
    /**
     * The color to paint the shadow below the material.
     *
     * Defaults to fully opaque black.
     */
    val shadowColor: Color = Color(0xFF000000.toInt()),
    /** The typographical style to use for text within this material. */
    val textStyle: TextStyle? = null,
    /**
     * If non-null, the corners of this box are rounded by this [BorderRadius].
     * Otherwise, the corners specified for the current [type] of material are
     * used.
     *
     * If [shape] is non null then the border radius is ignored.
     *
     * Must be null if [type] is [MaterialType.CIRCLE].
     */
    val borderRadius: BorderRadius? = null,
    /**
     * Defines the material"s shape as well its shadow.
     *
     * If shape is non null, the [borderRadius] is ignored and the material"s
     * clip boundary and shadow are defined by the shape.
     *
     * A shadow is only displayed if the [elevation] is greater than
     * zero.
     */
    val shape: ShapeBorder? = null,
    /**
     * Defines the duration of animated changes for [shape], [elevation],
     * and [shadowColor].
     *
     * The default value is [ThemeChangeDuration].
     */
    val animationDuration: Duration = ThemeChangeDuration,
    /**
     * The widget below this widget in the tree.
     *
     * {@macro flutter.widgets.child}
     */
    val child: Widget? = null
) : StatefulWidget(key) {

    init {
        assert(!(shape != null && borderRadius != null))
        assert(type != MaterialType.CIRCLE && (borderRadius != null || shape != null))
    }

    override fun createState(): State<out StatefulWidget> = MaterialState(this)

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(EnumProperty("type", type))
        properties.add(DoubleProperty.create("elevation", elevation, defaultValue = 0.0))
        properties.add(DiagnosticsProperty.create("color", color, defaultValue = null))
        properties.add(
            DiagnosticsProperty.create(
                "shadowColor",
                shadowColor,
                defaultValue = Color(0xFF000000.toInt())
            )
        )
        // TODO("Migration|Andrey: Needs TextStyle.debugFillProperties")
        // textStyle?.debugFillProperties(properties, prefix= "textStyle.");
        properties.add(DiagnosticsProperty.create("shape", shape, defaultValue = null))
        properties.add(
            DiagnosticsProperty.create(
                "borderRadius", borderRadius,
                defaultValue = null
            )
        )
    }
}

/**
 * The ink controller from the closest instance of this class that
 * encloses the given context.
 *
 * Typical usage is as follows:
 *
 * ```kotlin
 * val inkController = Material(context);
 * ```
 */
fun Material(context: BuildContext): RenderInkFeatures {
    return context.ancestorRenderObjectOfType(TypeMatcher.create<RenderInkFeatures>())
            as RenderInkFeatures
}

private class MaterialState(material: Material) :
    TickerProviderStateMixin<Material>(material) {
    private val inkFeatureRenderer = GlobalKey.withLabel<MaterialState>("ink renderer")

    private fun getBackgroundColor(context: BuildContext): Color? {
        if (widget.color != null)
            return widget.color
        return when (widget.type) {
            MaterialType.CANVAS ->
                TODO("Migration|Andrey: Needs Theme") // Theme.of(context).canvasColor
            MaterialType.CARD ->
                TODO("Migration|Andrey: Needs Theme") // Theme.of(context).cardColor
            else -> null
        }
    }

    override fun build(context: BuildContext): Widget {
        val backgroundColor = getBackgroundColor(context)
        assert(backgroundColor != null || widget.type == MaterialType.TRANSPARENCY)
        var contents = widget.child
        if (contents != null) {
            // TODO("Migration|Andrey: needs AnimatedDefaultTextStyle")
//            contents = AnimatedDefaultTextStyle (
//                    style: widget.textStyle ?? Theme.of(context).textTheme.body1,
//            duration: widget.animationDuration,
//            child: contents
//            )
        }
        // TODO("Migration|Andrey: needs LayoutChangedNotification")
        contents = /*NotificationListener < LayoutChangedNotification > (*/
//                onNotification: (LayoutChangedNotification notification) {
//            final RenderInkFeatures renderer =
//                    inkFeatureRenderer.currentContext.findRenderObject();
//            renderer.didChangeLayout();
//            return true;
//        },
                /*child:*/ InkFeatures(
            key = inkFeatureRenderer,
            color = backgroundColor,
            child = contents!!,
            vsync = this
        )
//        )

        // TODO("Migration|Andrey: needs AnimatedPhysicalModel")
//        // PhysicalModel has a temporary workaround for a performance issue that
//        // speeds up rectangular non transparent material (the workaround is to
//        // skip the call to ui.Canvas.saveLayer if the border radius is 0).
//        // Until the saveLayer performance issue is resolved, we're keeping this
//        // special case here for canvas material type that is using the default
//        // shape (RECTANGLE). We could go down this fast path for explicitly
//        // specified rectangles (e.g shape RoundedRectangleBorder with radius 0, but
//        // we choose not to as we want the change from the fast-path to the
//        // slow-path to be noticeable in the construction site of Material.
//        if (widget.type == MaterialType.canvas && widget.shape == null && widget.borderRadius == null) {
//            return new AnimatedPhysicalModel (
//                    curve: Curves.fastOutSlowIn,
//            duration: widget.animationDuration,
//            shape: BoxShape.RECTANGLE,
//            borderRadius: BorderRadius.zero,
//            elevation: widget.elevation,
//            color: backgroundColor,
//            shadowColor: widget.shadowColor,
//            animateColor: false,
//            child: contents,
//            );
//        }

        val shape = getShape()

        if (widget.type == MaterialType.TRANSPARENCY)
            return transparentInterior(shape = shape, contents = contents)

        return MaterialInterior(
            curve = Curves.fastOutSlowIn,
            duration = widget.animationDuration,
            shape = shape,
            elevation = widget.elevation,
            color = backgroundColor!!,
            shadowColor = widget.shadowColor,
            child = contents
        )
    }

    /**
     * Determines the shape for this Material.
     *
     * If a shape was specified, it will determine the shape.
     * If a borderRadius was specified, the shape is a rounded rectangle.
     * Otherwise, the shape is determined by the widget type as described in the
     * Material class documentation.
     */
    private fun getShape(): ShapeBorder {
        if (widget.shape != null)
            return widget.shape!!
        if (widget.borderRadius != null)
            return RoundedRectangleBorder(borderRadius = widget.borderRadius!!)
        return when (widget.type) {
            MaterialType.CANVAS,
            MaterialType.TRANSPARENCY ->
                RoundedRectangleBorder()
            MaterialType.CARD,
            MaterialType.BUTTON ->
                RoundedRectangleBorder(
                    borderRadius = widget.borderRadius ?: MaterialEdges[widget.type]
                    ?: BorderRadius.Zero
                )
            MaterialType.CIRCLE ->
                CircleBorder()
        }
    }
}

internal fun transparentInterior(shape: ShapeBorder, contents: Widget): Widget {
    return ClipPath(
        child = ShapeBorderPaint(
            child = contents,
            shape = shape
        ),
        clipper = ShapeBorderClipper(
            shape = shape
        )
    )
}

private class ShapeBorderPaint(
    val child: Widget,
    val shape: ShapeBorder
) : StatelessWidget() {

    override fun build(context: BuildContext): Widget {
        return CustomPaint(
            child = child,
            foregroundPainter = ShapeBorderPainter(shape, Directionality.of(context))
        )
    }
}

private class ShapeBorderPainter(
    val border: ShapeBorder,
    val textDirection: TextDirection
) : CustomPainter() {

    override fun paint(canvas: Canvas, size: Size) {
        border.paint(canvas, Offset.zero and size, textDirection)
    }

    override fun shouldRepaint(oldDelegate: CustomPainter) =
        (oldDelegate as ShapeBorderPainter).border != border
}

/**
 * The interior of non-transparent material.
 *
 * Animates [elevation], [shadowColor], and [shape].
 */
class MaterialInterior(
    key: Key? = null,
    /**
     * The widget below this widget in the tree.
     *
     * {@macro flutter.widgets.child}
     */
    val child: Widget,
    /**
     * The border of the widget.
     *
     * This border will be painted, and in addition the outer path of the border
     * determines the physical shape.
     */
    val shape: ShapeBorder,
    /** The target z-coordinate at which to place this physical object. */
    val elevation: Double,
    /** The target background color. */
    val color: Color,
    /** The target shadow color. */
    val shadowColor: Color,
    curve: Curve = Curves.linear,
    duration: Duration
) : ImplicitlyAnimatedWidget(key, curve, duration) {

    override fun createState() = MaterialInteriorState(this)

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("shape", shape))
        properties.add(DoubleProperty.create("elevation", elevation))
        properties.add(DiagnosticsProperty.create("color", color))
        properties.add(DiagnosticsProperty.create("shadowColor", shadowColor))
    }
}

class MaterialInteriorState(widget: MaterialInterior) :
    AnimatedWidgetBaseState<MaterialInterior>(widget) {

    private var elevation: Tween<Double>? = null
    private var shadowColor: Tween<Color>? = null
    private var border: Tween<ShapeBorder>? = null

    override fun forEachTween(visitor: TweenVisitor) {
        elevation = visitor(elevation, widget.elevation) { value: Double -> Tween(begin = value) }
        shadowColor =
                visitor(shadowColor, widget.shadowColor) { value: Color -> Tween(begin = value) }
        border = visitor(border, widget.shape) { value: ShapeBorder -> Tween(begin = value) }
    }

    override fun build(context: BuildContext): Widget {
        // TODO("Migration|Andrey: find Kotlin friendly way of creating tweens w/o nulls.")
        val shape = border!!.evaluate(animation!!)
        return PhysicalShape(
            child = ShapeBorderPaint(
                child = widget.child,
                shape = shape
            ),
            clipper = ShapeBorderClipper(
                shape = shape,
                textDirection = Directionality.of(context)
            ),
            elevation = elevation!!.evaluate(animation!!),
            color = widget.color,
            shadowColor = shadowColor!!.evaluate(animation!!)
        )
    }
}

class RenderInkFeatures(
    child: RenderBox? = null,
    /**
     * This class should exist in a 1:1 relationship with a MaterialState object,
     * since there's no current support for dynamically changing the ticker
     * provider.
     */
    override val vsync: TickerProvider,
    /**
     * This is here to satisfy the MaterialInkController contract.
     * The actual painting of this color is done by a Container in the
     * MaterialState build method.
     */
    override var color: Color?
) : RenderProxyBox(child), MaterialInkController {

    private var inkFeatures: MutableList<InkFeature>? = null

    override fun addInkFeature(feature: InkFeature) {
        assert(!feature.debugDisposed)
        assert(feature.controller == this)
        inkFeatures = inkFeatures ?: mutableListOf()
        assert(!inkFeatures!!.contains(feature))
        inkFeatures!!.add(feature)
        markNeedsPaint()
    }

    internal fun removeFeature(feature: InkFeature) {
        assert(inkFeatures != null)
        inkFeatures!!.remove(feature)
        markNeedsPaint()
    }

    internal fun didChangeLayout() {
        if (inkFeatures != null && inkFeatures!!.isNotEmpty()) {
            markNeedsPaint()
        }
    }

    override fun hitTestSelf(position: Offset) = true

    override fun paint(context: PaintingContext, offset: Offset) {
        val inkFeatures = inkFeatures
        if (inkFeatures != null && inkFeatures.isNotEmpty()) {
            val canvas = context.canvas
            canvas.save()
            canvas.translate(offset.dx, offset.dy)
            canvas.clipRect(Offset.zero and size)
            inkFeatures.forEach { it.paint(canvas) }
            canvas.restore()
        }
        super.paint(context, offset)
    }
}

internal class InkFeatures(
    key: Key?,
    val color: Color?,
    val vsync: TickerProvider,
    child: Widget
) : SingleChildRenderObjectWidget(key, child) {

    // This widget must be owned by a MaterialState, which must be provided as the vsync.
    // This relationship must be 1:1 and cannot change for the lifetime of the MaterialState.

    override fun createRenderObject(context: BuildContext): RenderObject {
        return RenderInkFeatures(
            color = color,
            vsync = vsync
        )
    }

    override fun updateRenderObject(context: BuildContext, renderObject: RenderObject) {
        (renderObject as RenderInkFeatures).also {
            it.color = color
            assert(vsync == it.vsync)
        }
    }
}

/**
 * A visual reaction on a piece of [Material].
 *
 * To add an ink feature to a piece of [Material], obtain the
 * [MaterialInkController] via [Material.of] and call
 * [MaterialInkController.addInkFeature].
 */
abstract class InkFeature constructor(
    /**
     * The [MaterialInkController] associated with this [InkFeature].
     *
     * Typically used by subclasses to call
     * [MaterialInkController.markNeedsPaint] when they need to repaint.
     */
    controller: RenderInkFeatures,
    /** The render box whose visual position defines the frame of reference for this ink feature. */
    val referenceBox: RenderBox,
    /** Called when the ink feature is no longer visible on the material. */
    val onRemoved: VoidCallback? = null
) {

    // TODO("Migration|Andrey: Try to merge MaterialInkController with RenderInkFeatures and remove
    // the backing field after it.")
    private var _controller: RenderInkFeatures = controller
    val controller: MaterialInkController get() = _controller

    internal var debugDisposed = false

    /** Free up the resources associated with this ink feature. */
    @CallSuper
    open fun dispose() {
        assert(!debugDisposed)
        assert { debugDisposed = true; true; }
        _controller.removeFeature(this)
        onRemoved?.invoke()
    }

    internal fun paint(canvas: Canvas) {
        assert(referenceBox.attached)
        assert(!debugDisposed)
        // find the chain of renderers from us to the feature's referenceBox
        val descendants = mutableListOf(referenceBox)
        var node = referenceBox
        while (node != _controller) {
            node = node.parent as RenderBox
            descendants.add(node)
        }
        // determine the transform that gets our coordinate system to be like theirs
        val transform = Matrix4.identity()
        assert(descendants.size >= 2)

        for (index in descendants.size - 1 downTo 1) {
            descendants[index].applyPaintTransform(descendants[index - 1], transform)
        }
        paintFeature(canvas, transform)
    }

    /**
     * Override this method to paint the ink feature.
     *
     * The transform argument gives the coordinate conversion from the coordinate
     * system of the canvas to the coordinate system of the [referenceBox].
     */
    protected abstract fun paintFeature(canvas: Canvas, transform: Matrix4)
}
