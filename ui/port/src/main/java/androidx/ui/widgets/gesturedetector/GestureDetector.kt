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

package androidx.ui.widgets.gesturedetector

import androidx.ui.foundation.Key
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.diagnostics.DiagnosticLevel
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticsNode
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.foundation.diagnostics.IterableProperty
import androidx.ui.gestures.drag_details.DragDownDetails
import androidx.ui.gestures.drag_details.DragEndDetails
import androidx.ui.gestures.drag_details.DragStartDetails
import androidx.ui.gestures.drag_details.DragUpdateDetails
import androidx.ui.gestures.drag_details.GestureDragDownCallback
import androidx.ui.gestures.drag_details.GestureDragStartCallback
import androidx.ui.gestures.drag_details.GestureDragUpdateCallback
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.long_press.GestureLongPressCallback
import androidx.ui.gestures.long_press.LongPressGestureRecognizer
import androidx.ui.gestures.monodrag.GestureDragCancelCallback
import androidx.ui.gestures.monodrag.GestureDragEndCallback
import androidx.ui.gestures.monodrag.HorizontalDragGestureRecognizer
import androidx.ui.gestures.monodrag.PanGestureRecognizer
import androidx.ui.gestures.monodrag.VerticalDragGestureRecognizer
import androidx.ui.gestures.multitap.DoubleTapGestureRecognizer
import androidx.ui.gestures.recognizer.GestureRecognizer
import androidx.ui.gestures.scale.GestureScaleEndCallback
import androidx.ui.gestures.scale.GestureScaleStartCallback
import androidx.ui.gestures.scale.GestureScaleUpdateCallback
import androidx.ui.gestures.scale.ScaleGestureRecognizer
import androidx.ui.gestures.tap.GestureTapCallback
import androidx.ui.gestures.tap.GestureTapCancelCallback
import androidx.ui.gestures.tap.GestureTapDownCallback
import androidx.ui.gestures.tap.GestureTapUpCallback
import androidx.ui.gestures.tap.TapDownDetails
import androidx.ui.gestures.tap.TapGestureRecognizer
import androidx.ui.gestures.tap.TapUpDetails
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.rendering.proxybox.HitTestBehavior
import androidx.ui.rendering.proxybox.RenderSemanticsGestureHandler
import androidx.ui.semantics.SemanticsAction
import androidx.ui.widgets.basic.Listener
import androidx.ui.widgets.framework.BuildContext
import androidx.ui.widgets.framework.SingleChildRenderObjectWidget
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget
import androidx.ui.widgets.framework.StatelessWidget
import androidx.ui.widgets.framework.Widget
import kotlin.reflect.KClass

// TODO(Migration/shepshapard): Porting tests requires widget testing infastructure.
/**
 * Factory for creating gesture recognizers.
 *
 * `T` is the type of gesture recognizer this class manages.
 *
 * Used by [RawGestureDetector.gestures].
 */
abstract class GestureRecognizerFactory<T : GestureRecognizer> {

    /** Must return an instance of T. */
    abstract fun constructor(): T

    /**
     * Must configure the given instance (which will have been created by
     * `constructor`).
     *
     * This normally means setting the callbacks.
     */
    abstract fun initializer(instance: T)

    // TODO(Migration/shepshapard): Is this possible?
//    open fun _debugAssertTypeMatches() : Boolean {
//        assert(type == T, "GestureRecognizerFactory of type $T was used where type $type was specified.")
//        return true
//    }
}

/** Signature for closures that implement [GestureRecognizerFactory.constructor]. */
typealias GestureRecognizerFactoryConstructor<T> = () -> T

/** Signature for closures that implement [GestureRecognizerFactory.initializer]. */
typealias GestureRecognizerFactoryInitializer<T> = (T) -> Unit

/**
 * Factory for creating gesture recognizers that delegates to callbacks.
 *
 * Used by [RawGestureDetector.gestures].
 */
class GestureRecognizerFactoryWithHandlers<T : GestureRecognizer>(
    private val constructor: GestureRecognizerFactoryConstructor<T>,
    private val initializer: GestureRecognizerFactoryInitializer<T>
) : GestureRecognizerFactory<T>() {

    override fun constructor(): T {
        return constructor.invoke()
    }

    override fun initializer(instance: T) {
        return initializer.invoke(instance)
    }
}

/**
 * A widget that detects gestures.
 *
 * Attempts to recognize gestures that correspond to its non-null callbacks.
 *
 * If this widget has a child, it defers to that child for its sizing behavior.
 * If it does not have a child, it grows to fit the parent instead.
 *
 * By default a GestureDetector with an invisible child ignores touches;
 * this behavior can be controlled with [behavior].
 *
 * GestureDetector also listens for accessibility events and maps
 * them to the callbacks. To ignore accessibility events, set
 * [excludeFromSemantics] to true.
 *
 * See <http://flutter.io/gestures/> for additional information.
 *
 * Material design applications typically react to touches with ink splash
 * effects. The [InkWell] class implements this effect and can be used in place
 * of a [GestureDetector] for handling taps.
 *
 * ## Sample code
 *
 * This example makes a rectangle react to being tapped by setting the
 * `_lights` field:
 *
 * ```dart
 * new GestureDetector(
 *   onTap: () {
 *     setState(() { _lights = true; });
 *   },
 *   child: new Container(
 *     color: Colors.yellow,
 *     child: new Text('TURN LIGHTS ON'),
 *   ),
 * )
 * ```
 *
 * ## Debugging
 *
 * To see how large the hit test box of a [GestureDetector] is for debugging
 * purposes, set [debugPaintPointersEnabled] to true.
 *
 * Pan and scale callbacks cannot be used simultaneously because scale is a
 * superset of pan. Simply use the scale callbacks instead.
 *
 * Horizontal and vertical drag callbacks cannot be used simultaneously
 * because a combination of a horizontal and vertical drag is a pan. Simply
 * use the pan callbacks instead.
 *
 * By default, gesture detectors contribute semantic information to the tree
 * that is used by assistive technology.
 */
class GestureDetector(
    key: Key? = null,
    /**
     * The widget below this widget in the tree.
     *
     * {@macro flutter.widgets.child}
     */
    val child: Widget?,
    /**
     * A pointer that might cause a tap has contacted the screen at a particular
     * location.
     */
    private val onTapDown: GestureTapDownCallback? = null,
    /**
     * A pointer that will trigger a tap has stopped contacting the screen at a
     * particular location.
     */
    private val onTapUp: GestureTapUpCallback? = null,
    /** A tap has occurred. */
    private val onTap: GestureTapCallback? = null,
    /**
     * The pointer that previously triggered [onTapDown] will not end up causing
     * a tap.
     */
    private val onTapCancel: GestureTapCancelCallback? = null,
    /**
     * The user has tapped the screen at the same location twice in quick
     * succession.
     */
    private val onDoubleTap: GestureTapCallback? = null,
    /**
     * A pointer has remained in contact with the screen at the same location for
     * a long period of time.
     */
    private val onLongPress: GestureLongPressCallback? = null,
    /** A pointer has contacted the screen and might begin to move vertically. */
    private val onVerticalDragDown: GestureDragDownCallback? = null,
    /** A pointer has contacted the screen and has begun to move vertically. */
    private val onVerticalDragStart: GestureDragStartCallback? = null,
    /**
     * A pointer that is in contact with the screen and moving vertically has
     * moved in the vertical direction.
     */
    private val onVerticalDragUpdate: GestureDragUpdateCallback? = null,
    /**
     * A pointer that was previously in contact with the screen and moving
     * vertically is no longer in contact with the screen and was moving at a
     * specific velocity when it stopped contacting the screen.
     */
    private val onVerticalDragEnd: GestureDragEndCallback? = null,
    /**
     * The pointer that previously triggered [onVerticalDragDown] did not
     * complete.
     */
    private val onVerticalDragCancel: GestureDragCancelCallback? = null,
    /** A pointer has contacted the screen and might begin to move horizontally. */
    private val onHorizontalDragDown: GestureDragDownCallback? = null,
    /** A pointer has contacted the screen and has begun to move horizontally. */
    private val onHorizontalDragStart: GestureDragStartCallback? = null,
    /**
     * A pointer that is in contact with the screen and moving horizontally has
     * moved in the horizontal direction.
     */
    private val onHorizontalDragUpdate: GestureDragUpdateCallback? = null,
    /**
     * A pointer that was previously in contact with the screen and moving
     * horizontally is no longer in contact with the screen and was moving at a
     * specific velocity when it stopped contacting the screen.
     */
    private val onHorizontalDragEnd: GestureDragEndCallback? = null,
    /**
     * The pointer that previously triggered [onHorizontalDragDown] did not
     * complete.
     */
    private val onHorizontalDragCancel: GestureDragCancelCallback? = null,
    /** A pointer has contacted the screen and might begin to move. */
    private val onPanDown: GestureDragDownCallback? = null,
    /** A pointer has contacted the screen and has begun to move. */
    private val onPanStart: GestureDragStartCallback? = null,
    /** A pointer that is in contact with the screen and moving has moved again. */
    private val onPanUpdate: GestureDragUpdateCallback? = null,
    /**
     * A pointer that was previously in contact with the screen and moving
     * is no longer in contact with the screen and was moving at a specific
     * velocity when it stopped contacting the screen.
     */
    private val onPanEnd: GestureDragEndCallback? = null,
    /** The pointer that previously triggered [onPanDown] did not complete. */
    private val onPanCancel: GestureDragCancelCallback? = null,
    /**
     * The pointers in contact with the screen have established a focal point and
     * initial scale of 1.0.
     */
    private val onScaleStart: GestureScaleStartCallback? = null,
    /**
     * The pointers in contact with the screen have indicated a new focal point
     * and/or scale.
     */
    private val onScaleUpdate: GestureScaleUpdateCallback? = null,
    /** The pointers are no longer in contact with the screen. */
    private val onScaleEnd: GestureScaleEndCallback? = null,
    /**
     * How this gesture detector should behave during hit testing.
     *
     * This defaults to [HitTestBehavior.deferToChild] if [child] is not null and
     * [HitTestBehavior.translucent] if child is null.
     */
    behavior: HitTestBehavior? = null,
    /**
     * Whether to exclude these gestures from the semantics tree. For
     * example, the long-press gesture for showing a tooltip is
     * excluded because the tooltip itself is included in the semantics
     * tree directly and so having a gesture to show it would result in
     * duplication of information.
     */
    private val excludeFromSemantics: Boolean = false
) : StatelessWidget(key = key) {

    /** How this gesture detector behaves during hit testing. */
    val behavior: HitTestBehavior =
        behavior ?: if (child != null) {
            HitTestBehavior.DEFER_TO_CHILD
        } else {
            HitTestBehavior.TRANSLUCENT
        }

    init {

        androidx.ui.assert {
            val haveVerticalDrag = onVerticalDragStart != null ||
                    onVerticalDragUpdate != null ||
                    onVerticalDragEnd != null
            val haveHorizontalDrag = onHorizontalDragStart != null ||
                    onHorizontalDragUpdate != null ||
                    onHorizontalDragEnd != null
            val havePan = onPanStart != null ||
                    onPanUpdate != null ||
                    onPanEnd != null
            val haveScale = onScaleStart != null ||
                    onScaleUpdate != null ||
                    onScaleEnd != null

            if (havePan || haveScale) {
                if (havePan && haveScale) {
                    throw FlutterError(
                        "Incorrect GestureDetector arguments.\n" +
                                "Having both a pan gesture recognizer and a scale gesture " +
                                "recognizer is redundant; scale is a superset of pan. Just " +
                                "use the scale gesture recognizer."
                    )
                }
                val recognizer = if (havePan) "pan" else "scale"
                if (haveVerticalDrag && haveHorizontalDrag) {
                    throw FlutterError(
                        "Incorrect GestureDetector arguments.\n" +
                                "Simultaneously having a vertical drag gesture recognizer, a " +
                                "horizontal drag gesture recognizer, and a $recognizer gesture " +
                                "recognizer will result in the $recognizer gesture recognizer " +
                                "being ignored, since the other two will catch all drags."
                    )
                }
            }

            true
        }
    }

    override fun build(context: BuildContext): Widget {
        val gestures:
                MutableMap<KClass<*>, GestureRecognizerFactoryWithHandlers<out GestureRecognizer>> =
            mutableMapOf()

        if (onTapDown != null || onTapUp != null || onTap != null || onTapCancel != null) {
            gestures[TapGestureRecognizer::class] = GestureRecognizerFactoryWithHandlers({
                TapGestureRecognizer(debugOwner = this)
            }, { instance ->
                instance.onTap = onTap
                instance.onTapUp = onTapUp
                instance.onTapDown = onTapDown
                instance.onTapCancel = onTapCancel
            })
        }

        if (onDoubleTap != null) {
            gestures[DoubleTapGestureRecognizer::class] = GestureRecognizerFactoryWithHandlers({
                DoubleTapGestureRecognizer(debugOwner = this)
            }, { instance ->
                instance.onDoubleTap = onDoubleTap
            })
        }

        if (onLongPress != null) {
            gestures[LongPressGestureRecognizer::class] = GestureRecognizerFactoryWithHandlers({
                LongPressGestureRecognizer(debugOwner = this)
            }, { instance ->
                instance.onLongPress = onLongPress
            })
        }

        if (onVerticalDragDown != null ||
            onVerticalDragStart != null ||
            onVerticalDragUpdate != null ||
            onVerticalDragEnd != null ||
            onVerticalDragCancel != null
        ) {
            gestures[VerticalDragGestureRecognizer::class] = GestureRecognizerFactoryWithHandlers({
                VerticalDragGestureRecognizer(debugOwner = this)
            }, { instance ->
                instance.apply {
                    onDown = onVerticalDragDown
                    onStart = onVerticalDragStart
                    onUpdate = onVerticalDragUpdate
                    onEnd = onVerticalDragEnd
                    onCancel = onVerticalDragCancel
                }
            })
        }

        if (onHorizontalDragDown != null ||
            onHorizontalDragStart != null ||
            onHorizontalDragUpdate != null ||
            onHorizontalDragEnd != null ||
            onHorizontalDragCancel != null
        ) {
            gestures[HorizontalDragGestureRecognizer::class] =
                    GestureRecognizerFactoryWithHandlers({
                        HorizontalDragGestureRecognizer(debugOwner = this)
                    }, { instance ->
                        instance.apply {
                            onDown = onHorizontalDragDown
                            onStart = onHorizontalDragStart
                            onUpdate = onHorizontalDragUpdate
                            onEnd = onHorizontalDragEnd
                            onCancel = onHorizontalDragCancel
                        }
                    })
        }

        if (onPanDown != null ||
            onPanStart != null ||
            onPanUpdate != null ||
            onPanEnd != null ||
            onPanCancel != null
        ) {
            gestures[PanGestureRecognizer::class] = GestureRecognizerFactoryWithHandlers({
                PanGestureRecognizer(debugOwner = this)
            }, { instance ->
                instance.apply {
                    onDown = onPanDown
                    onStart = onPanStart
                    onUpdate = onPanUpdate
                    onEnd = onPanEnd
                    onCancel = onPanCancel
                }
            })
        }

        if (onScaleStart != null ||
            onScaleUpdate != null ||
            onScaleEnd != null
        ) {
            gestures[ScaleGestureRecognizer::class] = GestureRecognizerFactoryWithHandlers({
                ScaleGestureRecognizer(debugOwner = this)
            }, { instance ->
                instance.apply {
                    onStart = onScaleStart
                    onUpdate = onScaleUpdate
                    onEnd = onScaleEnd
                }
            })
        }

        return RawGestureDetector(
            // TODO(migration/shepshapard): Had to do this nasty casting somewhere.  This should be
            // improved in the long run.
            gestures = gestures as Map<KClass<*>, GestureRecognizerFactory<GestureRecognizer>>,
            behavior = behavior,
            excludeFromSemantics = excludeFromSemantics,
            child = child
        )
    }
}

// TODO(migration/shepshapard): Convert below sample dart code to kotlin.
/**
 * A widget that detects gestures described by the given gesture
 * factories.
 *
 * For common gestures, use a [GestureRecognizer].
 * [RawGestureDetector] is useful primarily when developing your
 * own gesture recognizers.
 *
 * Configuring the gesture recognizers requires a carefully constructed map, as
 * described in [gestures] and as shown in the example below.
 *
 * ## Sample code
 *
 * This example shows how to hook up a [TapGestureRecognizer]. It assumes that
 * the code is being used inside a [State] object with a `_last` field that is
 * then displayed as the child of the gesture detector.
 *
 * ```dart
 * new RawGestureDetector(
 *   gestures: <Type, GestureRecognizerFactory>{
 *     TapGestureRecognizer: new GestureRecognizerFactoryWithHandlers<TapGestureRecognizer>(
 *       () => new TapGestureRecognizer(),
 *       (TapGestureRecognizer instance) {
 *         instance
 *           ..onTapDown = (TapDownDetails details) { setState(() { _last = 'down'; }); }
 *           ..onTapUp = (TapUpDetails details) { setState(() { _last = 'up'; }); }
 *           ..onTap = () { setState(() { _last = 'tap'; }); }
 *           ..onTapCancel = () { setState(() { _last = 'cancel'; }); };
 *       },
 *     ),
 *   },
 *   child: new Container(width: 300.0, height: 300.0, color: Colors.yellow, child: new Text(_last)),
 * )
 * ```
 *
 * See also:
 *
 *  * [GestureDetector], a less flexible but much simpler widget that does the same thing.
 *  * [Listener], a widget that reports raw pointer events.
 *  * [GestureRecognizer], the class that you extend to create a custom gesture recognizer.
 *
 * By default, gesture detectors contribute semantic information to the tree
 * that is used by assistive technology. This can be controlled using
 * [excludeFromSemantics].
 */
class RawGestureDetector(
    key: Key? = null,
    /**
     * The widget below this widget in the tree.
     *
     * {@macro flutter.widgets.child}
     */
    val child: Widget? = null,
    /**
     * The gestures that this widget will attempt to recognize.
     *
     * This should be a map from [GestureRecognizer] subclasses to
     * [GestureRecognizerFactory] subclasses specialized with the same type.
     *
     * This value can be late-bound at layout time using
     * [RawGestureDetectorState.replaceGestureRecognizers].
     */
    val gestures: Map<KClass<*>, GestureRecognizerFactory<GestureRecognizer>>,
    /**
     * How this gesture detector should behave during hit testing.
     *
     * This defaults to [HitTestBehavior.deferToChild] if [child] is not null and
     * [HitTestBehavior.translucent] if child is null.
     */
    behavior: HitTestBehavior? = null,
    /**
     * Whether to exclude these gestures from the semantics tree. For
     * example, the long-press gesture for showing a tooltip is
     * excluded because the tooltip itself is included in the semantics
     * tree directly and so having a gesture to show it would result in
     * duplication of information.
     */
    val excludeFromSemantics: Boolean = false
) : StatefulWidget(
    key = key
) {

    /** How this gesture detector behaves during hit testing. */
    val behavior: HitTestBehavior =
        behavior ?: if (child != null) {
            HitTestBehavior.DEFER_TO_CHILD
        } else {
            HitTestBehavior.TRANSLUCENT
        }

    override fun createState(): State<StatefulWidget> {
        return RawGestureDetectorState(this) as State<StatefulWidget>
    }
}

/** State for a [RawGestureDetector]. */
class RawGestureDetectorState(widget: RawGestureDetector) :
    State<RawGestureDetector>(widget = widget) {
    internal var recognizers: MutableMap<KClass<*>, GestureRecognizer> = mutableMapOf()

    override fun initState() {
        super.initState()
        this.syncAll(widget.gestures)
    }

    override fun didUpdateWidget(oldWidget: RawGestureDetector) {
        super.didUpdateWidget(oldWidget)
        this.syncAll(widget.gestures)
    }

    /**
     * This method can be called after the build phase, during the
     * layout of the nearest descendant [RenderObjectWidget] of the
     * gesture detector, to update the list of active gesture
     * recognizers.
     *
     * The typical use case is [Scrollable]s, which put their viewport
     * in their gesture detector, and then need to know the dimensions
     * of the viewport and the viewport's child to determine whether
     * the gesture detector should be enabled.
     *
     * The argument should follow the same conventions as
     * [RawGestureDetector.gestures]. It acts like a temporary replacement for
     * that value until the next build.
     */
    // TODO(Migration/shepshapard): The findRenderObject call made in this method is nullable and
    // that seems problematic.  This method wasn't needed at this time so commented out for now.
    /*fun replaceGestureRecognizers(
        gestures: MutableMap<KClass<*>, GestureRecognizerFactory<GestureRecognizer>>
    ) {
        androidx.ui.assert {
            if (!context!!.findRenderObject()?.owner!!.debugDoingLayout) {
                throw FlutterError(
                    "Unexpected call to replaceGestureRecognizers() method of " +
                            "RawGestureDetectorState.\n" +
                            "The replaceGestureRecognizers() method can only be called during " +
                            "the layout phase. To set the gesture recognizers at other times, " +
                            "trigger a new build using setState() and provide the new gesture " +
                            "recognizers as constructor arguments to the corresponding " +
                            "RawGestureDetector or GestureDetector object."
                )
            }
            true
        }

        syncAll(gestures)
        if (!widget.excludeFromSemantics) {
            val semanticsGestureHandler = context!!.findRenderObject()
            context!!.visitChildElements { element ->
                (element.widget as GestureSemantics).updateHandlers(semanticsGestureHandler)
            }
        }
    }*/

    /**
     * This method can be called outside of the build phase to filter the list of
     * available semantic actions.
     *
     * The actual filtering is happening in the next frame and a frame will be
     * scheduled if non is pending.
     *
     * This is used by [Scrollable] to configure system accessibility tools so
     * that they know in which direction a particular list can be scrolled.
     *
     * If this is never called, then the actions are not filtered. If the list of
     * actions to filter changes, it must be called again.
     */
    fun replaceSemanticsActions(actions: Set<SemanticsAction>) {
        androidx.ui.assert {
            val element = context
            if (element!!.owner!!.debugBuilding) {
                throw FlutterError(
                    "Unexpected call to replaceSemanticsActions() method of " +
                            "RawGestureDetectorState.\n" +
                            "The replaceSemanticsActions() method can only be called outside of " +
                            "the build phase."
                )
            }
            true
        }
        if (!widget.excludeFromSemantics) {
            (context!!.findRenderObject() as RenderSemanticsGestureHandler).validActions =
                    actions // will call _markNeedsSemanticsUpdate(), if required.
        }
    }

    override fun dispose() {
        for (recognizer: GestureRecognizer in recognizers.values) {
            recognizer.dispose()
        }
        this.recognizers.clear()
        super.dispose()
    }

    private fun syncAll(gestures: Map<KClass<*>, GestureRecognizerFactory<GestureRecognizer>>) {
        val oldRecognizers: Map<KClass<*>, GestureRecognizer> = recognizers
        recognizers = mutableMapOf()
        for ((recognizerType, gestureRecognizerFactory) in gestures.entries) {
            // TODO(Migration/shepshapard) : Not possible?
            // assert(gestures.get(type)._debugAssertTypeMatches(type))
            assert(!recognizers.containsKey(recognizerType))

            val gestureRecognizer =
                oldRecognizers[recognizerType] ?: gestureRecognizerFactory.constructor()
            assert(gestureRecognizer::class == recognizerType)
            recognizers[recognizerType] = gestureRecognizer
            gestureRecognizerFactory.initializer(gestureRecognizer)
        }
        for ((recognizerType, gestureRecognizerFactory) in oldRecognizers.entries) {
            if (!recognizers.containsKey(recognizerType)) {
                gestureRecognizerFactory.dispose()
            }
        }
    }

    private fun handlePointerDown(event: PointerDownEvent) {
        for (recognizer: GestureRecognizer in recognizers.values) {
            recognizer.addPointer(event)
        }
    }

    internal fun handleSemanticsTap() {
        val recognizer: GestureRecognizer? =
            recognizers[TapGestureRecognizer::class]
        assert(recognizer != null)
        val tapRecognizer = recognizer as TapGestureRecognizer
        tapRecognizer.onTapDown?.let { it(TapDownDetails()) }
        tapRecognizer.onTapUp?.let { it(TapUpDetails()) }
        tapRecognizer.onTap?.let { it() }
    }

    internal fun handleSemanticsLongPress() {
        val recognizer: GestureRecognizer? =
            recognizers[LongPressGestureRecognizer::class]
        assert(recognizer != null)
        val longRecognizer = recognizer as LongPressGestureRecognizer
        longRecognizer.onLongPress?.let { it() }
    }

    internal fun handleSemanticsHorizontalDragUpdate(updateDetails: DragUpdateDetails) {
        val dragRecognizer: GestureRecognizer? =
            recognizers[HorizontalDragGestureRecognizer::class]
        if (dragRecognizer != null) {
            val recognizer = dragRecognizer as HorizontalDragGestureRecognizer
            recognizer.onDown?.let {
                it(DragDownDetails())
            }
            recognizer.onStart?.let {
                it(DragStartDetails())
            }
            recognizer.onUpdate?.let {
                it(updateDetails)
            }
            recognizer.onEnd?.let {
                it(DragEndDetails(primaryVelocity = 0.0))
            }
            return
        }
        val panRecognizer: GestureRecognizer? =
            recognizers.get(PanGestureRecognizer::class)
        if (panRecognizer != null) {
            val recognizer = panRecognizer as PanGestureRecognizer
            recognizer.onDown?.let {
                it(DragDownDetails())
            }
            recognizer.onStart?.let {
                it(DragStartDetails())
            }
            recognizer.onUpdate?.let {
                it(updateDetails)
            }
            recognizer.onEnd?.let {
                it(DragEndDetails())
            }
            return
        }
    }

    internal fun handleSemanticsVerticalDragUpdate(updateDetails: DragUpdateDetails) {
        val dragRecognizer: GestureRecognizer? =
            recognizers.get(VerticalDragGestureRecognizer::class)
        if (dragRecognizer != null) {
            val recognizer = dragRecognizer as VerticalDragGestureRecognizer
            recognizer.onDown?.let {
                it(DragDownDetails())
            }
            recognizer.onStart?.let {
                it(DragStartDetails())
            }
            recognizer.onUpdate?.let {
                it(updateDetails)
            }
            recognizer.onEnd?.let {
                it(DragEndDetails(primaryVelocity = 0.0))
            }
            return
        }
        val panRecognizer: GestureRecognizer? =
            recognizers.get(PanGestureRecognizer::class)
        if (panRecognizer != null) {
            val recognizer = panRecognizer as VerticalDragGestureRecognizer
            recognizer.onDown?.let {
                it(DragDownDetails())
            }
            recognizer.onStart?.let {
                it(DragStartDetails())
            }
            recognizer.onUpdate?.let {
                it(updateDetails)
            }
            recognizer.onEnd?.let {
                it(DragEndDetails())
            }
            return
        }
    }

    override fun build(context: BuildContext): Widget {
        var result: Widget = Listener(
            onPointerDown = ::handlePointerDown,
            behavior = widget.behavior,
            child = widget.child
        )
        if (!widget.excludeFromSemantics) {
            result = GestureSemantics(owner = this, child = result)
        }
        return result
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        // TODO(migration/shepshapard): Need some other reasonable symbol for being in this state.
        if (recognizers == null) {
            properties.add(DiagnosticsNode.message("DISPOSED"))
        } else {
            val gestures: MutableList<String> =
                recognizers.values.map { recognizer ->
                    recognizer.debugDescription
                }.toMutableList()
            properties.add(IterableProperty("gestures", gestures, ifEmpty = "<none>"))
            properties.add(
                IterableProperty(
                    "recognizers",
                    recognizers.values,
                    level = DiagnosticLevel.fine
                )
            )
        }
        properties.add(EnumProperty("behavior", widget.behavior, defaultValue = null))
    }
}

private class GestureSemantics(
    key: Key? = null,
    child: Widget? = null,
    val owner: RawGestureDetectorState
) : SingleChildRenderObjectWidget(
    key = key, child = child
) {

    override fun createRenderObject(context: BuildContext): RenderSemanticsGestureHandler {
        return RenderSemanticsGestureHandler(
            onTap = onTapHandler,
            onLongPress = onLongPressHandler,
            onHorizontalDragUpdate = onHorizontalDragUpdateHandler,
            onVerticalDragUpdate = onVerticalDragUpdateHandler
        )
    }

    override fun updateRenderObject(
        context: BuildContext,
        renderObject: RenderObject
    ) {
        this.updateHandlers(renderObject as RenderSemanticsGestureHandler)
    }

    internal fun updateHandlers(renderObject: RenderObject) {
        (renderObject as RenderSemanticsGestureHandler).onTap = onTapHandler
    }

    private val onTapHandler: GestureTapCallback?
        get() =
            if (owner.recognizers.containsKey(TapGestureRecognizer::class))
                owner::handleSemanticsTap
            else null

    private val onLongPressHandler: GestureTapCallback?
        get() =
            if (owner.recognizers.containsKey(LongPressGestureRecognizer::class))
                owner::handleSemanticsLongPress
            else null

    private val onHorizontalDragUpdateHandler: GestureDragUpdateCallback?
        get() =
            if (owner.recognizers.containsKey(HorizontalDragGestureRecognizer::class) ||
                owner.recognizers.containsKey(PanGestureRecognizer::class))
                owner::handleSemanticsHorizontalDragUpdate
            else null

    private val onVerticalDragUpdateHandler: GestureDragUpdateCallback?
        get() =
            if (owner.recognizers.containsKey(VerticalDragGestureRecognizer::class) ||
                owner.recognizers.containsKey(PanGestureRecognizer::class))
                owner::handleSemanticsVerticalDragUpdate
            else null
}