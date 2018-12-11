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

package androidx.ui.material

import androidx.annotation.CallSuper
import androidx.ui.VoidCallback
import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.Key
import androidx.ui.foundation.ValueChanged
import androidx.ui.foundation.diagnostics.DiagnosticLevel
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.foundation.diagnostics.IterableProperty
import androidx.ui.gestures.long_press.GestureLongPressCallback
import androidx.ui.gestures.tap.GestureTapCallback
import androidx.ui.gestures.tap.GestureTapDownCallback
import androidx.ui.gestures.tap.TapDownDetails
import androidx.ui.material.material.InkFeature
import androidx.ui.material.material.Material
import androidx.ui.material.material.RectCallback
import androidx.ui.material.material.RenderInkFeatures
import androidx.ui.painting.Color
import androidx.ui.painting.borderradius.BorderRadius
import androidx.ui.painting.borders.BoxShape
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.proxybox.HitTestBehavior
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget
import androidx.ui.widgets.framework.Widget
import androidx.ui.widgets.gesturedetector.GestureDetector

/**
 * An ink feature that displays a [color] "splash" in response to a user
 * gesture that can be confirmed or canceled.
 *
 * Subclasses call [confirm] when an input gesture is recognized. For
 * example a press event might trigger an ink feature that's confirmed
 * when the corresponding up event is seen.
 *
 * Subclasses call [cancel] when an input gesture is aborted before it
 * is recognized. For example a press event might trigger an ink feature
 * that's cancelled when the pointer is dragged out of the reference
 * box.
 *
 * The [InkWell] and [InkResponse] widgets generate instances of this
 * class.
 */
abstract class InteractiveInkFeature(
    controller: RenderInkFeatures,
    referenceBox: RenderBox,
    color: Color,
    onRemoved: VoidCallback? = null
) : InkFeature(controller, referenceBox, onRemoved) {

    /**
     * Called when the user input that triggered this feature's appearance was confirmed.
     *
     * Typically causes the ink to propagate faster across the material. By default this
     * method does nothing.
     */
    open fun confirm() {
    }

    /**
     * Called when the user input that triggered this feature's appearance was canceled.
     *
     * Typically causes the ink to gradually disappear. By default this method does
     * nothing.
     */
    open fun cancel() {
    }

    /** The ink's color. */
    var color: Color = color
        set(value) {
            if (value == field)
                return
            field = value
            controller.markNeedsPaint()
        }
}

/**
 * An encapsulation of an [InteractiveInkFeature] constructor used by [InkWell]
 * [InkResponse] and [ThemeData].
 *
 * Interactive ink feature implementations should provide a static const
 * `splashFactory` value that's an instance of this class. The `splashFactory`
 * can be used to configure an [InkWell], [InkResponse] or [ThemeData].
 *
 * See also:
 *
 *  * [InkSplash.SplashFactory]
 *  * [InkRipple.SplashFactory]
 */
abstract class InteractiveInkFeatureFactory {

    /**
     * The factory method.
     *
     * Subclasses should override this method to return a new instance of an
     * [InteractiveInkFeature].
     */
    abstract fun create(
        controller: RenderInkFeatures,
        referenceBox: RenderBox,
        position: Offset,
        color: Color,
        containedInkWell: Boolean = false,
        rectCallback: RectCallback? = null,
        borderRadius: BorderRadius? = null,
        radius: Double? = null,
        onRemoved: VoidCallback? = null
    ): InteractiveInkFeature
}

/**
 * An area of a [Material] that responds to touch. Has a configurable shape and
 * can be configured to clip splashes that extend outside its bounds or not.
 *
 * For a variant of this widget that is specialized for rectangular areas that
 * always clip splashes, see [InkWell].
 *
 * An [InkResponse] widget does two things when responding to a tap:
 *
 *  * It starts to animate a _highlight_. The shape of the highlight is
 *    determined by [highlightShape]. If it is a [BoxShape.CIRCLE], the
 *    default, then the highlight is a CIRCLE of fixed size centered in the
 *    [InkResponse]. If it is [BoxShape.RECTANGLE], then the highlight is a box
 *    the size of the [InkResponse] itself, unless [getRectCallback] is
 *    provided, in which case that callback defines the RECTANGLE. The color of
 *    the highlight is set by [highlightColor].
 *
 *  * Simultaneously, it starts to animate a _splash_. This is a growing CIRCLE
 *    initially centered on the tap location. If this is a [containedInkWell],
 *    the splash grows to the [radius] while remaining centered at the tap
 *    location. Otherwise, the splash migrates to the center of the box as it
 *    grows.
 *
 * The following two diagrams show how [InkResponse] looks when tapped if the
 * [highlightShape] is [BoxShape.CIRCLE] (the default) and [containedInkWell]
 * is false (also the default).
 *
 * The first diagram shows how it looks if the [InkResponse] is relatively
 * large:
 *
 * ![The highlight is a disc centered in the box, smaller than the child widget.](https://flutter.github.io/assets-for-api-docs/assets/material/ink_response_large.png)
 *
 * The second diagram shows how it looks if the [InkResponse] is small:
 *
 * ![The highlight is a disc overflowing the box, centered on the child.](https://flutter.github.io/assets-for-api-docs/assets/material/ink_response_small.png)
 *
 * The main thing to notice from these diagrams is that the splashes happily
 * exceed the bounds of the widget (because [containedInkWell] is false).
 *
 * The following diagram shows the effect when the [InkResponse] has a
 * [highlightShape] of [BoxShape.RECTANGLE] with [containedInkWell] set to
 * true. These are the values used by [InkWell].
 *
 * ![The highlight is a RECTANGLE the size of the box.](https://flutter.github.io/assets-for-api-docs/assets/material/ink_well.png)
 *
 * The [InkResponse] widget must have a [Material] widget as an ancestor. The
 * [Material] widget is where the ink reactions are actually painted. This
 * matches the material design premise wherein the [Material] is what is
 * actually reacting to touches by spreading ink.
 *
 * If a Widget uses this class directly, it should include the following line
 * at the top of its build function to call [debugCheckHasMaterial]:
 *
 * ```dart
 * assert(debugCheckHasMaterial(context));
 * ```
 *
 * ## Troubleshooting
 *
 * ### The ink splashes aren't visible!
 *
 * If there is an opaque graphic, e.g. painted using a [Container], [Image], or
 * [DecoratedBox], between the [Material] widget and the [InkResponse] widget,
 * then the splash won't be visible because it will be under the opaque graphic.
 * This is because ink splashes draw on the underlying [Material] itself, as
 * if the ink was spreading inside the material.
 *
 * The [Ink] widget can be used as a replacement for [Image], [Container], or
 * [DecoratedBox] to ensure that the image or decoration also paints in the
 * [Material] itself, below the ink.
 *
 * If this is not possible for some reason, e.g. because you are using an
 * opaque [CustomPaint] widget, alternatively consider using a second
 * [Material] above the opaque widget but below the [InkResponse] (as an
 * ancestor to the ink response). The [MaterialType.transparency] material
 * kind can be used for this purpose.
 *
 * See also:
 *
 *  * [GestureDetector], for listening for gestures without ink splashes.
 *  * [RaisedButton] and [FlatButton], two kinds of buttons in material design.
 *  * [IconButton], which combines [InkResponse] with an [Icon].
 */
open class InkResponse(
    key: Key? = null,
    /**
     * The widget below this widget in the tree.
     *
     * {@macro flutter.widgets.child}
     */
    val child: Widget,
    /** Called when the user taps this part of the material. */
    val onTap: GestureTapCallback? = null,
    /** Called when the user taps down this part of the material. */
    val onTapDown: GestureTapDownCallback? = null,
    /**
     * Called when the user cancels a tap that was started on this part of the
     * material.
     */
    val onTapCancel: GestureTapCallback? = null,
    /** Called when the user double taps this part of the material. */
    val onDoubleTap: GestureTapCallback? = null,
    /** Called when the user long-presses on this part of the material. */
    val onLongPress: GestureLongPressCallback? = null,
    /**
     * Called when this part of the material either becomes highlighted or stops
     * being highlighted.
     *
     * The value passed to the callback is true if this part of the material has
     * become highlighted and false if this part of the material has stopped
     * being highlighted.
     */
    val onHighlightChanged: ValueChanged<Boolean>? = null,
    /**
     * Whether this ink response should be clipped its bounds.
     *
     * This flag also controls whether the splash migrates to the center of the
     * [InkResponse] or not. If [containedInkWell] is true, the splash remains
     * centered around the tap location. If it is false, the splash migrates to
     * the center of the [InkResponse] as it grows.
     *
     * See also:
     *
     *  * [highlightShape], which determines the shape of the highlight.
     *  * [borderRadius], which controls the corners when the box is a RECTANGLE.
     *  * [getRectCallback], which controls the size and position of the box when
     *    it is a RECTANGLE.
     */
    val containedInkWell: Boolean = false,
    /**
     * The shape (e.g., CIRCLE, RECTANGLE) to use for the highlight drawn around
     * this part of the material.
     *
     * If the shape is [BoxShape.CIRCLE], then the highlight is centered on the
     * [InkResponse]. If the shape is [BoxShape.RECTANGLE], then the highlight
     * fills the [InkResponse], or the RECTANGLE provided by [getRectCallback] if
     * the callback is specified.
     *
     * See also:
     *
     *  * [containedInkWell], which controls clipping behavior.
     *  * [borderRadius], which controls the corners when the box is a RECTANGLE.
     *  * [highlightColor], the color of the highlight.
     *  * [getRectCallback], which controls the size and position of the box when
     *    it is a RECTANGLE.
     */
    val highlightShape: BoxShape = BoxShape.CIRCLE,
    /**
     * The radius of the ink splash.
     *
     * Splashes grow up to this size. By default, this size is determined from
     * the size of the RECTANGLE provided by [getRectCallback], or the size of
     * the [InkResponse] itself.
     *
     * See also:
     *
     *  * [splashColor], the color of the splash.
     *  * [splashFactory], which defines the appearance of the splash.
     */
    val radius: Double? = null,
    /**
     * The clipping radius of the containing rect.
     *
     * If this is null, it is interpreted as [BorderRadius.Zero].
     */
    val borderRadius: BorderRadius? = null,
    /**
     * The highlight color of the ink response. If this property is null then the
     * highlight color of the theme, [ThemeData.highlightColor], will be used.
     *
     * See also:
     *
     *  * [highlightShape], the shape of the highlight.
     *  * [splashColor], the color of the splash.
     *  * [splashFactory], which defines the appearance of the splash.
     */
    val highlightColor: Color? = null,
    /**
     * The splash color of the ink response. If this property is null then the
     * splash color of the theme, [ThemeData.splashColor], will be used.
     *
     * See also:
     *
     *  * [splashFactory], which defines the appearance of the splash.
     *  * [radius], the (maximum) size of the ink splash.
     *  * [highlightColor], the color of the highlight.
     */
    val splashColor: Color? = null,
    /**
     * Defines the appearance of the splash.
     *
     * Defaults to the value of the theme's splash factory: [ThemeData.splashFactory].
     *
     * See also:
     *
     *  * [radius], the (maximum) size of the ink splash.
     *  * [splashColor], the color of the splash.
     *  * [highlightColor], the color of the highlight.
     *  * [InkSplash.SplashFactory], which defines the default splash.
     *  * [InkRipple.SplashFactory], which defines a splash that spreads out
     *    more aggressively than the default.
     */
    val splashFactory: InteractiveInkFeatureFactory? = null,
    /**
     * Whether detected gestures should provide acoustic and/or haptic feedback.
     *
     * For example, on Android a tap will produce a clicking sound and a
     * long-press will produce a short vibration, when feedback is enabled.
     *
     * See also:
     *
     *  * [Feedback] for providing platform-specific feedback to certain actions.
     */
    val enableFeedback: Boolean = true,
    /**
     * Whether to exclude the gestures introduced by this widget from the
     * semantics tree.
     *
     * For example, a long-press gesture for showing a tooltip is usually
     * excluded because the tooltip itself is included in the semantics
     * tree directly and so having a gesture to show it would result in
     * duplication of information.
     */
    val excludeFromSemantics: Boolean = false
) : StatefulWidget(key) {

    /**
     * The RECTANGLE to use for the highlight effect and for clipping
     * the splash effects if [containedInkWell] is true.
     *
     * This method is intended to be overridden by descendants that
     * specialize [InkResponse] for unusual cases. For example,
     * [TableRowInkWell] implements this method to return the RECTANGLE
     * corresponding to the row that the widget is in.
     *
     * The default behavior returns null, which is equivalent to
     * returning the referenceBox argument's bounding box (though
     * slightly more efficient).
     */
    fun getRectCallback(referenceBox: RenderBox): RectCallback? = null

    /**
     * Asserts that the given context satisfies the prerequisites for
     * this class.
     *
     * This method is intended to be overridden by descendants that
     * specialize [InkResponse] for unusual cases. For example,
     * [TableRowInkWell] implements this method to verify that the widget is
     * in a table.
     */
    @CallSuper
    fun debugCheckContext(context: BuildContext): Boolean {
        assert(debugCheckHasMaterial(context))
        return true
    }

    override fun createState(): State<out StatefulWidget> {
        return InkResponseState(this)
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        val gestures = mutableListOf<String>()
        if (onTap != null)
            gestures.add("tap")
        if (onDoubleTap != null)
            gestures.add("double tap")
        if (onLongPress != null)
            gestures.add("long press")
        if (onTapDown != null)
            gestures.add("tap down")
        if (onTapCancel != null)
            gestures.add("tap cancel")
        properties.add(IterableProperty("gestures", gestures, ifEmpty = "<NONE>"))
        properties.add(
            DiagnosticsProperty.create(
                "containedInkWell",
                containedInkWell,
                level = DiagnosticLevel.fine
            )
        )
        properties.add(
            DiagnosticsProperty.create(
                "highlightShape",
                highlightShape,
                description = "${if (containedInkWell) "clipped to " else ""}$highlightShape",
                showName = false
            )
        )
    }
}

private class InkResponseState<T : InkResponse>(widget: T) : State<T>(widget) {
    // TODO("Migration|Andrey: Needs AutomaticKeepAliveClientMixin")
    /*with AutomaticKeepAliveClientMixin*/

    private var splashes: MutableSet<InteractiveInkFeature>? = null
    private var currentSplash: InteractiveInkFeature? = null
    private var lastHighlight: InkHighlight? = null

//    TODO("Migration|Andrey: Needs AutomaticKeepAliveClientMixin")
//    @override
//    bool get wantKeepAlive => lastHighlight != null || (splashes != null && splashes.isNotEmpty);

    fun updateHighlight(value: Boolean) {
        if (value == (lastHighlight != null && lastHighlight!!.active))
            return
        if (value) {
            if (lastHighlight == null) {
                val context = context!!
                val referenceBox = context.findRenderObject() as RenderBox
                lastHighlight = InkHighlight(
                    controller = Material(context),
                    referenceBox = referenceBox,
                    color = widget.highlightColor
                        ?: TODO("Migration|Andrey: Needs Theme"),
                    //     Theme.of(context).highlightColor
                    shape = widget.highlightShape,
                    borderRadius = widget.borderRadius,
                    rectCallback = widget.getRectCallback(referenceBox),
                    onRemoved = this::handleInkHighlightRemoval
                )
//                updateKeepAlive() TODO("Migration|Andrey: Needs AutomaticKeepAliveClientMixin")
            } else {
                lastHighlight!!.activate()
            }
        } else {
            lastHighlight!!.deactivate()
        }
        assert(value == (lastHighlight != null && lastHighlight!!.active))
        widget.onHighlightChanged?.invoke(value)
    }

    private fun handleInkHighlightRemoval() {
        assert(lastHighlight != null)
        lastHighlight = null
//        updateKeepAlive() TODO("Migration|Andrey: Needs AutomaticKeepAliveClientMixin")
    }

    private fun createInkFeature(details: TapDownDetails): InteractiveInkFeature {
        val context = this.context!!
        val inkController = Material(context)
        val referenceBox = context.findRenderObject() as RenderBox
        val position = referenceBox.globalToLocal(details.globalPosition)
        val color = widget.splashColor
            ?: TODO("Migration|Andrey: Needs Theme") /*Theme.of(context).splashColor;*/
        val rectCallback =
            if (widget.containedInkWell) widget.getRectCallback(referenceBox) else null
        val borderRadius = widget.borderRadius

        var splash: InteractiveInkFeature? = null
        val onRemoved = {
            val _splashes = splashes
            if (_splashes != null) {
                assert(_splashes.contains(splash))
                _splashes.remove(splash)
                if (currentSplash == splash)
                    currentSplash = null
//                updateKeepAlive(); TODO("Migration|Andrey: Needs AutomaticKeepAliveClientMixin")
            } // else we're probably in deactivate()
        }

        splash = (widget.splashFactory
            ?: TODO("Migration|Andrey: Needs Theme") /*Theme.of(context).splashFactory*/
                ).create(
            controller = inkController,
            referenceBox = referenceBox,
            position = position,
            color = color,
            containedInkWell = widget.containedInkWell,
            rectCallback = rectCallback,
            radius = widget.radius,
            borderRadius = borderRadius,
            onRemoved = onRemoved
        )

        return splash
    }

    private fun handleTapDown(details: TapDownDetails) {
        val splash = createInkFeature(details)
        splashes = splashes ?: mutableSetOf()
        val splashes = splashes!!
        splashes.add(splash)
        currentSplash = splash
        widget.onTapDown?.invoke(details)
//        updateKeepAlive() TODO("Migration|Andrey: Needs AutomaticKeepAliveClientMixin")
        updateHighlight(true)
    }

    internal fun handleTap(context: BuildContext) {
        currentSplash?.confirm()
        currentSplash = null
        updateHighlight(false)
        if (widget.onTap != null) {
            if (widget.enableFeedback)
//                Feedback.forTap(context); TODO("Migration|Andrey: Needs Feedback")
                widget.onTap!!()
        }
    }

    private fun handleTapCancel() {
        currentSplash?.cancel()
        currentSplash = null
        widget.onTapCancel?.invoke()
        updateHighlight(false)
    }

    private fun handleDoubleTap() {
        currentSplash?.confirm()
        currentSplash = null
        widget.onDoubleTap?.invoke()
    }

    internal fun handleLongPress(context: BuildContext) {
        currentSplash?.confirm()
        currentSplash = null
        if (widget.onLongPress != null) {
            if (widget.enableFeedback)
//                Feedback.forLongPress(context); TODO("Migration|Andrey: Needs Feedback")
                widget.onLongPress!!()
        }
    }

    override fun deactivate() {
        if (splashes != null) {
            val splashes = splashes!!
            this.splashes = null
            splashes.forEach { it.dispose() }
            currentSplash = null
        }
        assert(currentSplash == null)
        lastHighlight?.dispose()
        lastHighlight = null
        super.deactivate()
    }

    override fun build(context: BuildContext): Widget {
        assert(widget.debugCheckContext(context))
        // TODO("Migration|Andrey: Needs AutomaticKeepAliveClientMixin")
        //  super.build(context); // See AutomaticKeepAliveClientMixin.
        // TODO("Migration|Andrey: Needs Theme")
        // final ThemeData themeData = Theme.of(context);
        lastHighlight?.color = widget.highlightColor
                ?: TODO("Migration|Andrey: Needs Theme") // themeData.highlightColor
        currentSplash?.color = widget.splashColor
                ?: TODO("Migration|Andrey: Needs Theme") // themeData.splashColor
        val enabled = widget.onTap != null || widget.onDoubleTap !=
                null || widget.onLongPress != null

        val onTapDown = if (enabled) this::handleTapDown else null
        val onTap = if (!enabled) null else object : GestureTapCallback {
            override fun invoke() = handleTap(context)
        }
        val onTapCancel = if (enabled) this::handleTapCancel else null
        val onDoubleTap = if (widget.onDoubleTap != null) this::handleDoubleTap else null
        val onLongPress =
            if (widget.onLongPress == null) null else object : GestureLongPressCallback {
                override fun invoke() = handleLongPress(context)
            }

        return GestureDetector(
            onTapDown = onTapDown,
            onTap = onTap,
            onTapCancel = onTapCancel,
            onDoubleTap = onDoubleTap,
            onLongPress = onLongPress,
            behavior = HitTestBehavior.OPAQUE,
            child = widget.child,
            excludeFromSemantics = widget.excludeFromSemantics
        )
    }
}

/**
 * A rectangular area of a [Material] that responds to touch.
 *
 * For a variant of this widget that does not clip splashes, see [InkResponse].
 *
 * The following diagram shows how an [InkWell] looks when tapped, when using
 * default values.
 *
 * ![The highlight is a RECTANGLE the size of the box.](https://flutter.github.io/assets-for-api-docs/assets/material/ink_well.png)
 *
 * The [InkWell] widget must have a [Material] widget as an ancestor. The
 * [Material] widget is where the ink reactions are actually painted. This
 * matches the material design premise wherein the [Material] is what is
 * actually reacting to touches by spreading ink.
 *
 * If a Widget uses this class directly, it should include the following line
 * at the top of its build function to call [debugCheckHasMaterial]:
 *
 * ```dart
 * assert(debugCheckHasMaterial(context));
 * ```
 *
 * ## Troubleshooting
 *
 * ### The ink splashes aren't visible!
 *
 * If there is an opaque graphic, e.g. painted using a [Container], [Image], or
 * [DecoratedBox], between the [Material] widget and the [InkWell] widget, then
 * the splash won't be visible because it will be under the opaque graphic.
 * This is because ink splashes draw on the underlying [Material] itself, as
 * if the ink was spreading inside the material.
 *
 * The [Ink] widget can be used as a replacement for [Image], [Container], or
 * [DecoratedBox] to ensure that the image or decoration also paints in the
 * [Material] itself, below the ink.
 *
 * If this is not possible for some reason, e.g. because you are using an
 * opaque [CustomPaint] widget, alternatively consider using a second
 * [Material] above the opaque widget but below the [InkWell] (as an
 * ancestor to the ink well). The [MaterialType.transparency] material
 * kind can be used for this purpose.
 *
 * See also:
 *
 *  * [GestureDetector], for listening for gestures without ink splashes.
 *  * [RaisedButton] and [FlatButton], two kinds of buttons in material design.
 *  * [InkResponse], a variant of [InkWell] that doesn't force a rectangular
 *    shape on the ink reaction.
 */
class InkWell(
    key: Key? = null,
    child: Widget,
    onTap: GestureTapCallback? = null,
    onDoubleTap: GestureTapCallback? = null,
    onLongPress: GestureLongPressCallback? = null,
    onTapDown: GestureTapDownCallback? = null,
    onTapCancel: GestureTapCallback? = null,
    onHighlightChanged: ValueChanged<Boolean>? = null,
    highlightColor: Color? = null,
    splashColor: Color? = null,
    splashFactory: InteractiveInkFeatureFactory? = null,
    radius: Double? = null,
    borderRadius: BorderRadius? = null,
    enableFeedback: Boolean = true,
    excludeFromSemantics: Boolean = false
) : InkResponse(
    key = key,
    child = child,
    onTap = onTap,
    onDoubleTap = onDoubleTap,
    onLongPress = onLongPress,
    onTapDown = onTapDown,
    onTapCancel = onTapCancel,
    onHighlightChanged = onHighlightChanged,
    containedInkWell = true,
    highlightShape = BoxShape.RECTANGLE,
    highlightColor = highlightColor,
    splashColor = splashColor,
    splashFactory = splashFactory,
    radius = radius,
    borderRadius = borderRadius,
    enableFeedback = enableFeedback,
    excludeFromSemantics = excludeFromSemantics
)
