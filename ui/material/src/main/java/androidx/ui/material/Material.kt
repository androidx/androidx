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

package androidx.ui.material

import androidx.annotation.CallSuper
import androidx.ui.animation.Curve
import androidx.ui.animation.Curves
import androidx.ui.assert
import androidx.ui.core.Density
import androidx.ui.core.Dimension
import androidx.ui.core.Duration
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.adapter.DensityConsumer
import androidx.ui.core.adapter.Draw
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.dp
import androidx.ui.engine.geometry.BorderRadius
import androidx.ui.engine.geometry.Rect
import androidx.ui.material.borders.CircleBorder
import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.material.borders.ShapeBorder
import androidx.ui.material.clip.ClipPath
import androidx.ui.material.clip.PhysicalShape
import androidx.ui.material.clip.ShapeBorderClipper
import androidx.ui.painting.Canvas
import androidx.ui.painting.Color
import androidx.ui.scheduler.ticker.TickerProvider
import androidx.ui.vectormath64.Matrix4
import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable


// TODO("Migration|Andrey: Enums doesn't work in R4a module. Created tmp java class MaterialType")
///**
// * The various kinds of material in material design. Used to
// * configure the default behavior of [Material] widgets.
// *
// * See also:
// *
// *  * [Material], in particular [Material.type]
// *  * [MaterialEdges]
// */
//enum class MaterialType {
//    /** Rectangle using default theme canvas color. */
//    CANVAS,
//
//    /** Rounded edges, card theme color. */
//    CARD,
//
//    /** A circle, no color by default (used for floating action buttons). */
//    CIRCLE,
//
//    /** Rounded edges, no color by default (used for [MaterialButton] buttons). */
//    BUTTON,
//
//    /**
//     * A transparent piece of material that draws ink splashes and highlights.
//     *
//     * While the material metaphor describes child widgets as printed on the
//     * material itself and do not hide ink effects, in practice the [Material]
//     * widget draws child widgets on top of the ink effects.
//     * A [Material] with type transparency can be placed on top of opaque widgets
//     * to show ink effects on top of them.
//     *
//     * Prefer using the [Ink] widget for showing ink effects on top of opaque
//     * widgets.
//     */
//    TRANSPARENCY
//}

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
    MaterialType.CARD to BorderRadius.circular(2f),
    MaterialType.CIRCLE to null,
    MaterialType.BUTTON to BorderRadius.circular(2f),
    MaterialType.TRANSPARENCY to null
)

/** The default radius of an ink splash in logical pixels. */
val DefaultSplashRadius = 35.dp

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
    /**
     * A Children composable.
     */
    @Children var children: () -> Unit
) : Component() {

    /**
     * The kind of material to show (e.g., card or canvas). This
     * affects the shape of the widget, the roundness of its corners if
     * the shape is rectangular, and the default color.
     */
    var type: MaterialType = MaterialType.CANVAS
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
    var elevation: Dimension = 0.dp
    /**
     * The color to paint the material.
     *
     * Must be opaque. To create a transparent piece of material, use
     * [MaterialType.TRANSPARENCY].
     *
     * By default, the color is derived from the [type] of material.
     */
    var color: Color? = null
    /**
     * The color to paint the shadow below the material.
     *
     * Defaults to fully opaque black.
     */
    var shadowColor: Color = Color(0xFF000000.toInt())
    /**
     * If non-null, the corners of this box are rounded by this [BorderRadius].
     * Otherwise, the corners specified for the current [type] of material are
     * used.
     *
     * If [shape] is non null then the border radius is ignored.
     *
     * Must be null if [type] is [MaterialType.CIRCLE].
     */
    // TODO("Migration|Andrey: We probably need to merge borderRadius and shape as a rect with")
    // TODO("Migration|Andrey: rounded corners is just a specific implementation of shape.")
    var borderRadius: BorderRadius? = null
    /**
     * Defines the material"s shape as well its shadow.
     *
     * If shape is non null, the [borderRadius] is ignored and the material"s
     * clip boundary and shadow are defined by the shape.
     *
     * A shadow is only displayed if the [elevation] is greater than
     * zero.
     */
    var shape: ShapeBorder? = null
    /**
     * Defines the duration of animated changes for [shape], [elevation],
     * and [shadowColor].
     *
     * The default value is [ThemeChangeDuration].
     */
    var animationDuration: Duration = ThemeChangeDuration

    init {
        assert(!(shape != null && borderRadius != null))
        assert(type != MaterialType.CIRCLE && (borderRadius != null || shape != null))
    }


    // TODO("Migration|Andrey: Needs semantics in R4a")
//    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
//        super.debugFillProperties(properties)
//        properties.add(EnumProperty("type", type))
//        properties.add(DoubleProperty.create("elevation", elevation, defaultValue = 0.0))
//        properties.add(DiagnosticsProperty.create("color", color, defaultValue = null))
//        properties.add(
//            DiagnosticsProperty.create(
//                "shadowColor",
//                shadowColor,
//                defaultValue = Color(0xFF000000.toInt())
//            )
//        )
//        // TODO("Migration|Andrey: Needs TextStyle.debugFillProperties")
//        // textStyle?.debugFillProperties(properties, prefix= "textStyle.");
//        properties.add(DiagnosticsProperty.create("shape", shape, defaultValue = null))
//        properties.add(
//            DiagnosticsProperty.create(
//                "borderRadius", borderRadius,
//                defaultValue = null
//            )
//        )
//    }

    private fun getBackgroundColor(): Color? {
        if (color != null)
            return color
        return when (type) {
            MaterialType.CANVAS ->
                TODO("Migration|Andrey: Needs Theme") // Theme.of(context).canvasColor
            MaterialType.CARD ->
                TODO("Migration|Andrey: Needs Theme") // Theme.of(context).cardColor
            else -> null
        }
    }

    override fun compose() {
        val backgroundColor = getBackgroundColor()
        assert(backgroundColor != null || type == MaterialType.TRANSPARENCY)

        <MeasureBox> constraints, operations ->
            val measurable = operations.collect {
                <UseTickerProvider> vsync ->
                    val shape = getFinalShape()

                    if (type == MaterialType.TRANSPARENCY) {
                        <TransparentInterior shape>
                            <InkFeatures vsync color>
                                <children />
                            </InkFeatures>
                        </TransparentInterior>
                    } else {
                        <MaterialInterior
                            curve=Curves.fastOutSlowIn
                            duration=animationDuration
                            shape
                            elevation
                            color=backgroundColor!!
                            shadowColor>
                            <InkFeatures vsync color>
                                <children />
                            </InkFeatures>
                        </MaterialInterior>
                    }
                </UseTickerProvider>
            }
            operations.layout(constraints.maxWidth, constraints.maxHeight) {
                measurable.forEach { operations.measure(it, constraints).place(0.dp, 0.dp) }
            }
        </MeasureBox>
    }

    /**
     * Determines the shape for this Material.
     *
     * If a shape was specified, it will determine the shape.
     * If a borderRadius was specified, the shape is a rounded rectangle.
     * Otherwise, the shape is determined by the widget type as described in the
     * Material class documentation.
     */
    private fun getFinalShape(): ShapeBorder {
        val shape = shape
        if (shape != null) {
            return shape
        }
        val borderRadius = borderRadius
        if (borderRadius != null) {
            return RoundedRectangleBorder(borderRadius = borderRadius)
        }
        return when (type) {
            MaterialType.CANVAS,
            MaterialType.TRANSPARENCY ->
                RoundedRectangleBorder()
            MaterialType.CARD,
            MaterialType.BUTTON ->
                RoundedRectangleBorder(
                    borderRadius = MaterialEdges[type] ?: BorderRadius.Zero
                )
            MaterialType.CIRCLE ->
                CircleBorder()
        }
    }
}

@Composable
internal fun DrawShapeBorder(shape: ShapeBorder) {
    <DensityConsumer> density ->
        <Draw> canvas, parentSize ->
            shape.paint(canvas,
                density,
                Rect(0f, 0f, parentSize.width, parentSize.height),
                null)
        </Draw>
    </DensityConsumer>
}

@Composable
internal fun TransparentInterior(shape: ShapeBorder, @Children children: () -> Unit) {
    <ClipPath clipper=ShapeBorderClipper(shape)>
        <children />
    </ClipPath>
    <DrawShapeBorder shape/>
}

/**
 * The interior of non-transparent material.
 *
 * Animates [elevation], [shadowColor], and [shape].
 */
internal class MaterialInterior(
    /**
     * The border of the widget.
     *
     * This border will be painted, and in addition the outer path of the border
     * determines the physical shape.
     */
    var shape: ShapeBorder,
    /** The target z-coordinate at which to place this physical object. */
    var elevation: Dimension,
    /** The target background color. */
    var color: Color,
    /** The target shadow color. */
    var shadowColor: Color,
    var curve: Curve = Curves.linear,
    var duration: Duration,
    @Children var children: () -> Unit
) : Component() {

//    // TODO("Migration|Andrey: Needs semantics in R4a")
//    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
//        super.debugFillProperties(properties)
//        properties.add(DiagnosticsProperty.create("shape", shape))
//        properties.add(FloatProperty.create("elevation", elevation))
//        properties.add(DiagnosticsProperty.create("color", color))
//        properties.add(DiagnosticsProperty.create("shadowColor", shadowColor))
//    }

    override fun compose() {
        // TODO("Andrey: This widget was also applying border, elevation and shadowColor changes
        // with animations (class ImplicitlyAnimatedWidget).
        // We should reimplement this use case as part of our Swan animations.")
        <PhysicalShape clipper=ShapeBorderClipper(shape)
                       elevation
                       color
                       shadowColor>
            <children />
        </PhysicalShape>
        <DrawShapeBorder shape />
    }
}

internal val MaterialInkControllerAmbient = Ambient.of<MaterialInkController?>()

@Composable
fun MaterialInkControllerProvider(@Children children: (MaterialInkController) -> Unit) {
    <MaterialInkControllerAmbient.Consumer> inkFeatures ->
        if (inkFeatures == null) {
            val message = StringBuilder()
            message.appendln("No Material widget ancestor found.")
            message.appendln(
                "In material design, most widgets are conceptually \"printed\" on " +
                        "a sheet of material. In material library, that material" +
                        "is represented by the Material widget. It is the Material" +
                        "widget that renders ink splashes, for instance. Because of" +
                        "this, many material library widgets require that there" +
                        " be a Material widget in the tree above them."
            )
            message.appendln(
                "To introduce a Material widget, you can either directly " +
                        "include one, or use a widget that contains Material itself, " +
                        "such as a Card, Dialog, Drawer, or Scaffold."
            )
            throw IllegalStateException(message.toString())
        } else {
            <children p1=inkFeatures/>
        }
    </MaterialInkControllerAmbient.Consumer>
}

internal class InkFeatures(
    override var vsync: TickerProvider,
    /**
     * This is here to satisfy the MaterialInkController contract.
     * The actual painting of this color is done by a Container in the
     * MaterialState build method.
     */
    override var color: Color?,
    @Children var children: () -> Unit
) : Component(), MaterialInkController {

    override fun markNeedsPaint() {
        recompose()
    }

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

    // TODO("Migration|Andrey: Not sure we will need it")
//    override fun hitTestSelf(position: Offset) = true

    override fun compose() {
        <DensityConsumer> density ->
            <Draw> canvas, size ->
                val inkFeatures = inkFeatures
                if (inkFeatures != null && inkFeatures.isNotEmpty()) {
                    canvas.save()
                    canvas.clipRect(Rect(0f, 0f, size.width, size.height))
                    inkFeatures.forEach { it.paint(canvas, density) }
                    canvas.restore()
                }
            </Draw>
        </DensityConsumer>
        <MaterialInkControllerAmbient.Provider value=this>
            <children />
        </MaterialInkControllerAmbient.Provider>
    }

}

/**
 * A visual reaction on a piece of [Material].
 *
 * To add an ink feature to a piece of [Material], obtain the
 * [MaterialInkController] via [MaterialInkControllerProvider] and call
 * [MaterialInkController.addInkFeature].
 */
abstract class InkFeature(
    /**
     * The [MaterialInkController] associated with this [InkFeature].
     *
     * Typically used by subclasses to call
     * [MaterialInkController.markNeedsPaint] when they need to repaint.
     */
    val controller: MaterialInkController,
    /** The layout coordinates of the parent for this ink feature. */
    val coordinates: LayoutCoordinates,
    /** Called when the ink feature is no longer visible on the material. */
    val onRemoved: (() -> Unit)? = null
) {

    internal var debugDisposed = false

    /** Free up the resources associated with this ink feature. */
    @CallSuper
    open fun dispose() {
        assert(!debugDisposed)
        assert { debugDisposed = true; true; }
        // TODO("Migration|Andrey: I cast here as removeFeature is an internal method and not")
        // TODO("Migration|Andrey: a part of the interface as end users don't need to call it")
        (controller as InkFeatures).removeFeature(this)
        onRemoved?.invoke()
    }

    internal fun paint(canvas: Canvas, density: Density) {
        assert(!debugDisposed)
        // TODO("Migration|Andrey: Calculate transformation matrix using parents")
        // TODO("Migration|Andrey: Currently we don't have such a logic")
//        // find the chain of renderers from us to the feature's target layout
//        val descendants = mutableListOf(referenceBox)
//        var node = referenceBox
//        while (node != _controller) {
//            node = node.parent as RenderBox
//            descendants.add(node)
//        }
//         determine the transform that gets our coordinate system to be like theirs
        val transform = Matrix4.identity()
//        assert(descendants.size >= 2)
//
//        for (index in descendants.size - 1 downTo 1) {
//            descendants[index].applyPaintTransform(descendants[index - 1], transform)
//        }
        paintFeature(canvas, transform, density)
    }

    /**
     * Override this method to paint the ink feature.
     *
     * The transform argument gives the coordinate conversion from the coordinate
     * system of the canvas to the coordinate system of the target layout.
     */
    protected abstract fun paintFeature(canvas: Canvas, transform: Matrix4, density: Density)
}
