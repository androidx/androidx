/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.authoring

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RestrictTo
import androidx.ink.brush.Brush
import androidx.input.motionprediction.MotionEventPredictor

/**
 * [View.OnTouchListener] for creating and updating shapes in an [InProgressShapesView] based on the
 * touch events received by a view. To cancel a shape for a specific pointer, call
 * [InProgressShapesView.cancelShape] with the relevant [MotionEvent] and pointer id. To cancel all
 * pending shapes, call [InProgressShapesView.cancelUnfinishedShapes]. Pointers whose shapes are
 * canceled (or not started) will be ignored on subsequent calls to [ShapeGestureCallback.onTouch].
 *
 * Properties that govern the creation of new shapes should be updated some time before the relevant
 * call to [ShapeGestureCallback.onTouch]. For example, this could occur when the user switches to a
 * new shape spec, or it could wait until [onTouch]:
 * ```
 * fun onTouch(view: View, event: MotionEvent): Boolean {
 *   shapeGestureCallback.shapeSpecForNewShapes = currentShapeSpec
 *   shapeGestureCallback.onTouch(view, event)
 * }
 * ```
 *
 * This can be composed with other handlers if touch gestures do not always correspond to shapes.
 * For example, code that skips creating some shapes might be structured like this:
 * ```
 * fun onTouch(view: View, event: MotionEvent): Boolean {
 *   if (event.actionMasked == MotionEvent.ACTION_DOWN && shouldSkipShape(event)) {
 *     return true;
 *   }
 *   return shapeGestureCallback.onTouch(view, event)
 * }
 * ```
 *
 * Code that recognizes specific pointers as something other than a shape might be structured like
 * this:
 * ```
 * fun onTouch(view: View, event: MotionEvent): Boolean {
 *   var handled = false
 *   if (gestureHandler.onTouch(event)) {
 *     handled = true
 *     inProgressShapesView.cancelShape(event, pointerId)
 *   }
 *   if (shapeGestureCallback.onTouch(view, event)) {
 *     handled = true
 *   }
 *   return handled
 * }
 * ```
 *
 * Code that recognizes the entire gesture as something other than a shape might be structured like
 * this:
 * ```
 * fun onTouch(view: View, event: MotionEvent): Boolean {
 *   if (gestureHandler.onTouch(event)) {
 *     inProgressShapesView.cancelUnfinishedShapes()
 *     return true
 *   }
 *   return shapeGestureCallback.onTouch(view, event)
 * }
 * ```
 *
 * This can be used in an override of [View.onTouchEvent] in a custom [View] class, or passed to
 * [View.setOnTouchListener] for a stock [View] class. In the latter case, composition can be done
 * in an anonymous subclass:
 * ```
 * view.setOnTouchListener(
 *   object : View.OnTouchListener {
 *     override fun onTouch(view: View, event: MotionEvent): Boolean {
 *       ...
 *       return shapeGestureCallback.onTouch(view, event)
 *     }
 *   }
 * )
 * ```
 *
 * @param inProgressShapesView [InProgressShapesView] to create and update shapes in.
 * @param shapeSpecForNewShapes [ShapeSpecT] to use for new shapes, can be updated.
 * @param motionEventToWorldTransformForNewShapes A mutable [Matrix] to transform motion events into
 *   world coordinates for new shapes.
 * @param shapeToWorldTransformForNewShapes A mutable [Matrix] to transform shapes into world
 *   coordinates.
 * @param isRestrictedToSingleShape If `true`, then only the first pointer should be treated as a
 *   shape.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalCustomShapeWorkflowApi
public class ShapeGestureCallback<ShapeSpecT : Any>(
    private val inProgressShapesView: InProgressShapesView<ShapeSpecT, *, *>,
    public var shapeSpecForNewShapes: ShapeSpecT,
    public val motionEventToWorldTransformForNewShapes: Matrix = Matrix(),
    public val shapeToWorldTransformForNewShapes: Matrix = Matrix(),
    isRestrictedToSingleShape: Boolean = false,
) : View.OnTouchListener {
    private val delegate =
        object :
            GestureCallback(
                view = inProgressShapesView,
                motionEventToWorldTransformForNewShapes = motionEventToWorldTransformForNewShapes,
                shapeToWorldTransformForNewShapes = shapeToWorldTransformForNewShapes,
                isRestrictedToSingleShape = isRestrictedToSingleShape,
            ) {
            override fun onStartShape(
                event: MotionEvent,
                pointerId: Int,
                motionEventToWorldTransform: Matrix,
                shapeToWorldTransform: Matrix,
            ) {
                inProgressShapesView.startShape(
                    event = event,
                    pointerId = pointerId,
                    shapeSpec = shapeSpecForNewShapes,
                    motionEventToWorldTransform = motionEventToWorldTransform,
                    shapeToWorldTransform = shapeToWorldTransform,
                )
            }

            override fun onAddToShape(
                event: MotionEvent,
                pointerId: Int,
                prediction: MotionEvent?,
            ): Boolean =
                inProgressShapesView.addToShape(
                    event = event,
                    pointerId = pointerId,
                    prediction = prediction,
                )

            override fun onFinishShape(event: MotionEvent, pointerId: Int): Boolean =
                inProgressShapesView.finishShape(event = event, pointerId = pointerId)

            override fun onCancelShape(event: MotionEvent, pointerId: Int): Boolean =
                inProgressShapesView.cancelShape(event = event, pointerId = pointerId)
        }

    public var isRestrictedToSingleShape: Boolean by delegate::isRestrictedToSingleShape

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean = delegate.onTouch(v, event)
}

/**
 * [View.OnTouchListener] for creating and updating strokes in an [InProgressStrokesView] based on
 * the touch events received by a view. To cancel a stroke for a specific pointer, call
 * [InProgressStrokesView.cancelStroke] with the relevant [MotionEvent] and pointer id. To cancel
 * all pending strokes, call [InProgressStrokesView.cancelUnfinishedStrokes]. Pointers whose strokes
 * are canceled (or not started) will be ignored on subsequent calls to
 * [StrokeGestureCallback.onTouch].
 *
 * Properties that govern the creation of new strokes should be updated some time before the
 * relevant call to [StrokeGestureCallback.onTouch]. For example, this could occur when the user
 * switches to a new brush type, or it could wait until [onTouch]:
 * ```
 * fun onTouch(view: View, event: MotionEvent): Boolean {
 *   strokeGestureCallback.brushForNewStrokes = currentBrush
 *   strokeGestureCallback.onTouch(view, event)
 * }
 * ```
 *
 * This can be composed with other handlers if touch gestures do not always correspond to strokes.
 * For example, code that skips creating some strokes might be structured like this:
 * ```
 * fun onTouch(view: View, event: MotionEvent): Boolean {
 *   if (event.actionMasked == MotionEvent.ACTION_DOWN && shouldSkipStroke(event)) {
 *     return true;
 *   }
 *   return strokeGestureCallback.onTouch(view, event)
 * }
 * ```
 *
 * Code that recognizes specific pointers as something other than a stroke might be structured like
 * this:
 * ```
 * fun onTouch(view: View, event: MotionEvent): Boolean {
 *   var handled = false
 *   if (gestureHandler.onTouch(event)) {
 *     handled = true
 *     inProgressStrokesView.cancelStroke(event, pointerId)
 *   }
 *   if (strokeGestureCallback.onTouch(view, event)) {
 *     handled = true
 *   }
 *   return handled
 * }
 * ```
 *
 * Code that recognizes the entire gesture as something other than a stroke might be structured like
 * this:
 * ```
 * fun onTouch(view: View, event: MotionEvent): Boolean {
 *   if (gestureHandler.onTouch(event)) {
 *     inProgressStrokesView.cancelUnfinishedStrokes()
 *     return true
 *   }
 *   return strokeGestureCallback.onTouch(view, event)
 * }
 * ```
 *
 * This can be used in an override of [View.onTouchEvent] in a custom [View] class, or passed to
 * [View.setOnTouchListener] for a stock [View] class. In the latter case, composition can be done
 * in an anonymous subclass:
 * ```
 * view.setOnTouchListener(
 *   object : View.OnTouchListener {
 *     override fun onTouch(view: View, event: MotionEvent): Boolean {
 *       ...
 *       return strokeGestureCallback.onTouch(view, event)
 *     }
 *   }
 * )
 * ```
 *
 * @param inProgressStrokesView [InProgressStrokesView] to create and update strokes in.
 * @param brushForNewStrokes [Brush] to use for new strokes, can be updated.
 * @param motionEventToWorldTransformForNewStrokes A mutable [Matrix] to transform motion events
 *   into world coordinates for new strokes.
 * @param strokeToWorldTransformForNewStrokes A mutable [Matrix] to transform strokes into world
 *   coordinates.
 * @param isRestrictedToSingleStroke If `true`, then only the first pointer should be treated as a
 *   stroke.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
public class StrokeGestureCallback(
    private val inProgressStrokesView: InProgressStrokesView,
    public var brushForNewStrokes: Brush,
    public val motionEventToWorldTransformForNewStrokes: Matrix = Matrix(),
    public val strokeToWorldTransformForNewStrokes: Matrix = Matrix(),
    isRestrictedToSingleStroke: Boolean = false,
) : View.OnTouchListener {
    private val delegate =
        object :
            GestureCallback(
                view = inProgressStrokesView,
                motionEventToWorldTransformForNewShapes = motionEventToWorldTransformForNewStrokes,
                shapeToWorldTransformForNewShapes = strokeToWorldTransformForNewStrokes,
                isRestrictedToSingleShape = isRestrictedToSingleStroke,
            ) {
            override fun onStartShape(
                event: MotionEvent,
                pointerId: Int,
                motionEventToWorldTransform: Matrix,
                shapeToWorldTransform: Matrix,
            ) {
                inProgressStrokesView.startStroke(
                    event = event,
                    pointerId = pointerId,
                    brush = brushForNewStrokes,
                    motionEventToWorldTransform = motionEventToWorldTransform,
                    strokeToWorldTransform = shapeToWorldTransform,
                )
            }

            override fun onAddToShape(
                event: MotionEvent,
                pointerId: Int,
                prediction: MotionEvent?,
            ): Boolean =
                inProgressStrokesView.addToStroke(
                    event = event,
                    pointerId = pointerId,
                    prediction = prediction,
                )

            override fun onFinishShape(event: MotionEvent, pointerId: Int): Boolean =
                inProgressStrokesView.finishStroke(event = event, pointerId = pointerId)

            override fun onCancelShape(event: MotionEvent, pointerId: Int): Boolean =
                inProgressStrokesView.cancelStroke(event = event, pointerId = pointerId)
        }

    public var isRestrictedToSingleStroke: Boolean by delegate::isRestrictedToSingleShape

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean = delegate.onTouch(v, event)
}

/** Common touch logic between [StrokeGestureCallback] and [ShapeGestureCallback]. */
private abstract class GestureCallback(
    view: View,
    val motionEventToWorldTransformForNewShapes: Matrix = Matrix(),
    val shapeToWorldTransformForNewShapes: Matrix = Matrix(),
    var isRestrictedToSingleShape: Boolean = false,
) : View.OnTouchListener {
    private val motionEventPredictor = MotionEventPredictor.newInstance(view)

    abstract fun onStartShape(
        event: MotionEvent,
        pointerId: Int,
        motionEventToWorldTransform: Matrix,
        shapeToWorldTransform: Matrix,
    )

    abstract fun onAddToShape(event: MotionEvent, pointerId: Int, prediction: MotionEvent?): Boolean

    abstract fun onFinishShape(event: MotionEvent, pointerId: Int): Boolean

    abstract fun onCancelShape(event: MotionEvent, pointerId: Int): Boolean

    /**
     * Update the relevant shapes based on the pointers on [event]. If the event's action is
     * [MotionEvent.ACTION_DOWN] or [MotionEvent.ACTION_POINTER_DOWN], a new shape is started for
     * the action's pointer (the pointer corresponding to [MotionEvent.getActionIndex]). If the
     * event's action is [MotionEvent.ACTION_MOVE], in progress shapes are updated for each pointer
     * in the event. If the event's action is [MotionEvent.ACTION_UP] or
     * [MotionEvent.ACTION_POINTER_UP], the shape for the action's pointer is canceled if
     * FLAG_CANCELED is set in the event flags and finished otherwise. If the event's action is
     * [MotionEvent.ACTION_CANCEL], all in progress shapes are canceled. The shape most recently
     * started for a pointer with the relevant pointer ID is considered to correspond to that
     * pointer. If there is no such shape for an update, the update is ignored.
     *
     * @param view [View] that originally received the [event].
     * @param event [MotionEvent] to be processed.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        if (view == null || event == null) return false
        motionEventPredictor.record(event)
        val primaryPointerId = event.getPointerId(event.actionIndex)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (
                    isRestrictedToSingleShape &&
                        event.actionMasked == MotionEvent.ACTION_POINTER_DOWN
                ) {
                    return false
                }
                view.requestUnbufferedDispatch(event)
                onStartShape(
                    event,
                    pointerId = primaryPointerId,
                    motionEventToWorldTransform = motionEventToWorldTransformForNewShapes,
                    shapeToWorldTransform = shapeToWorldTransformForNewShapes,
                )
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                var updated = false
                var prediction: MotionEvent? = null
                try {
                    prediction = motionEventPredictor.predict()
                    for (pointerIndex in 0 until event.pointerCount) {
                        if (
                            onAddToShape(
                                event,
                                pointerId = event.getPointerId(pointerIndex),
                                prediction = prediction,
                            )
                        ) {
                            updated = true
                        }
                    }
                } finally {
                    prediction?.recycle()
                }
                return updated
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP ->
                return if (event.flags and MotionEvent.FLAG_CANCELED != 0) {
                    onCancelShape(event, pointerId = primaryPointerId)
                } else {
                    onFinishShape(event, pointerId = primaryPointerId)
                }
            MotionEvent.ACTION_CANCEL -> return onCancelShape(event, pointerId = primaryPointerId)
        }
        return false
    }
}
