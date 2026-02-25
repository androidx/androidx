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
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableIntObjectMap
import androidx.core.graphics.withMatrix
import androidx.core.view.ViewCompat
import androidx.ink.authoring.internal.CanvasInProgressStrokesRenderHelperV21
import androidx.ink.authoring.internal.CanvasInProgressStrokesRenderHelperV29
import androidx.ink.authoring.internal.CanvasInProgressStrokesRenderHelperV33
import androidx.ink.authoring.internal.FinishedStroke
import androidx.ink.authoring.internal.InProgressStrokesManager
import androidx.ink.authoring.internal.InProgressStrokesRenderHelper
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.authoring.latency.LatencyDataCallback
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import androidx.test.espresso.idling.CountingIdlingResource
import java.util.concurrent.TimeUnit
import kotlin.math.hypot

// See https://www.nist.gov/pml/owm/si-units-length
private const val CM_PER_INCH = 2.54f

/**
 * Displays in-progress shapes, as defined by a [ShapeWorkflow], as [MotionEvent] user inputs are
 * provided incrementally. This is a more generalized version of [InProgressStrokesView] for
 * specialized developer needs for custom shapes. For normal usage, [InProgressStrokesView] is
 * recommended. For a Jetpack Compose equivalent which also provides a default input handler, see
 * [androidx.ink.authoring.compose.InProgressShapes] instead.
 */
@ExperimentalCustomShapeWorkflowApi
@OptIn(ExperimentalLatencyDataApi::class)
public class InProgressShapesView<
    ShapeSpecT : Any,
    InProgressShapeT : InProgressShape<ShapeSpecT, CompletedShapeT>,
    CompletedShapeT : Any,
>
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    /**
     * Force the HWUI-based high latency implementation to be used under the hood, even if the
     * system supports low latency inking.
     *
     * This must be set to its desired value before the first call to [startShape] or [eagerInit].
     */
    @Deprecated("Prefer to allow the underlying implementation details to be chosen automatically.")
    internal var useHighLatencyRenderHelper: Boolean = false

    /**
     * Set a minimum delay from when the user finishes a shape until rendering is handed off to the
     * client's dry layer via [InProgressShapesCompletedListener.onShapesCompleted]. This value
     * would ideally be long enough that quick subsequent shapes - such as for fast handwriting -
     * are processed and later handed off as one group, but short enough that the handoff can take
     * place during short, natural pauses in handwriting.
     *
     * If handoff is ever needed as soon as safely possible, call [requestHandoff].
     */
    internal var handoffDebounceTimeMs: Long = 0L
        @UiThread
        set(value) {
            require(value >= 0L) { "Debounce time must not be negative, received $value" }
            field = value
            // Don't force initialization to set this value, otherwise the properties that must be
            // set
            // before initialization would be harder to set. Hold onto it and pass it down to the
            // InProgressStrokesManager when it gets initialized.
            initializedState?.inProgressStrokesManager?.setHandoffDebounceDurationMs(value)
        }

    /**
     * A [ShapeWorkflow] to be used as an alternative to the standard Ink behavior provided by
     * [InProgressStrokesView]. It must be set before the first call to [startShape] or [eagerInit].
     */
    public var customShapeWorkflow: ShapeWorkflow<ShapeSpecT, InProgressShapeT, CompletedShapeT>? =
        null
        set(value) {
            check(initializedState == null) {
                "Cannot set customShapeAdapter after initialization."
            }
            field = value
        }

    /**
     * Used by [InProgressStrokesView] to defer initialization until the last moment, to allow
     * pre-initialization properties to be set on it.
     */
    internal var customShapeWorkflowFactory:
        (() -> ShapeWorkflow<ShapeSpecT, InProgressShapeT, CompletedShapeT>)? =
        null

    /**
     * Denote an area of this [InProgressShapesView] where no ink should be visible. A value of
     * `null` indicates that shapes will be visible anywhere they are drawn. This is useful for UI
     * elements that float on top of (in Z order) the drawing surface - without this, a user would
     * be able to draw in-progress ("wet") shapes on top of those UI elements, but then when the
     * shape is finished, it will appear as a dry shape underneath of the UI element. If this mask
     * is set to the shape and position of the floating UI element, then the ink will never be
     * rendered in that area, making it appear as if it's being drawn underneath the UI element.
     *
     * This technique is most convincing when the UI element is opaque. Often there are parts of the
     * UI element that are translucent, such as drop shadows, or anti-aliasing along the edges. The
     * result will look a little different between wet and dry shapes for those cases, but it can be
     * a worthwhile tradeoff compared to the alternative of drawing wet shapes on top of that UI
     * element.
     *
     * Note that this parameter does not affect the contents of the shapes at all, nor how they
     * appear when drawn in a separate composable after
     * [InProgressShapesCompletedListener.onShapesCompleted] is called - just how the shapes appear
     * when they are still in progress in this view.
     */
    public var maskPath: Path? = null
        set(value) {
            field = value
            initializedState?.let { it.renderHelper.maskPath = value }
        }

    /**
     * The transform matrix to convert [MotionEvent] coordinates, as passed to [startShape],
     * [addToShape], and [finishShape], into coordinates of this [InProgressShapesView] for
     * rendering. Defaults to the identity matrix, for the recommended case where
     * [InProgressShapesView] exactly overlays the [android.view.View] that has the touch listener
     * from which [MotionEvent] instances are being forwarded.
     */
    public var motionEventToViewTransform: Matrix = Matrix()
        set(value) {
            field.set(value)
            // Don't force initialization to set this value, otherwise the properties that must be
            // set
            // before initialization would be harder to set. Hold onto it and pass it down to the
            // InProgressStrokesManager when it gets initialized.
            initializedState?.let { it.inProgressStrokesManager.motionEventToViewTransform = value }
        }

    /**
     * Allows a test to easily wait until all in-progress shapes are completed and handed off. There
     * is no reason to set this in non-test code.
     */
    @VisibleForTesting
    public var inProgressShapeCounter: CountingIdlingResource? = null
        set(value) {
            field = value
            // Don't force initialization to set this value, otherwise the properties that must be
            // set
            // before initialization would be harder to set. Hold onto it and pass it down to the
            // InProgressStrokesManager when it gets initialized.
            initializedState?.let { it.inProgressStrokesManager.inProgressStrokeCounter = value }
        }

    // Note: public experimental properties are not allowed because the accessors will not appear
    // experimental to Java clients. There are public accessors for this property below.
    @ExperimentalLatencyDataApi private var latencyDataCallback: LatencyDataCallback? = null

    /**
     * An optional callback for reporting latency of the processing of input events for in-progress
     * shapes. Clients may implement the [LatencyDataCallback] interface and set this field to
     * receive latency measurements.
     *
     * Notes for clients: Do not hold references to the [LatencyData] passed into this callback.
     * After this callback returns, the [LatencyData] instance will immediately become invalid: it
     * will be deleted or recycled. Also, to avoid stalling the UI thread, implementers should
     * minimize the amount of computation in this callback, and should also avoid allocations (since
     * allocation may trigger the garbage collector).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @ExperimentalLatencyDataApi
    public fun getLatencyDataCallback(): LatencyDataCallback? {
        return latencyDataCallback
    }

    /**
     * Sets the callback for reporting latency of the processing of input events for in-progress
     * shapes.
     *
     * See [getLatencyDataCallback]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @ExperimentalLatencyDataApi
    public fun setLatencyDataCallback(value: LatencyDataCallback?) {
        latencyDataCallback = value
    }

    /**
     * State that is only available after [startShape] or [eagerInit] have been called. Manage
     * initialization of this explicitly rather than using [Lazy] to avoid accidentally breaking the
     * documented contract of when initialization takes place.
     */
    private var initializedState: InitializedState? = null

    /**
     * Force initialization to happen and get the resulting initialized value. Initialization is
     * publicly documented to only occur on [startShape] and [eagerInit], so don't add it anywhere
     * else.
     */
    @OptIn(ExperimentalCustomShapeWorkflowApi::class)
    private fun ensureInit(): InitializedState {
        return initializedState
            ?: InitializedState(
                    customShapeWorkflow
                        ?: run {
                            val adapterFactory =
                                checkNotNull(customShapeWorkflowFactory) {
                                    "Must set `InProgressShapesView.customShapeAdapter` before calling " +
                                        "`startShape` or `eagerInit`. Consider using `InProgressStrokesView` instead " +
                                        "for easier initialization and recommended behavior."
                                }
                            adapterFactory().also { customShapeWorkflow = it }
                        }
                )
                .also {
                    initializedState = it
                    if (isAttachedToWindow) {
                        it.addFinishedShapesView()
                    }
                }
    }

    /** Contains everything that is initialized with [startShape] or [eagerInit]. */
    private inner class InitializedState(
        shapeWorkflow: ShapeWorkflow<ShapeSpecT, InProgressShapeT, CompletedShapeT>
    ) {

        val finishedStrokesView = FinishedShapesView(context, shapeWorkflow)

        private val inProgressStrokesManagerListener =
            object : InProgressStrokesManager.Listener<CompletedShapeT> {
                override fun onAllStrokesFinished(
                    strokes: Map<InProgressStrokeId, FinishedStroke<CompletedShapeT>>
                ) {
                    finishedStrokesView.addStrokes(strokes)
                    val newlyFinishedStrokes = mutableMapOf<InProgressStrokeId, CompletedShapeT>()
                    for ((strokeId, finishedStroke) in strokes) {
                        newlyFinishedStrokes[strokeId] = finishedStroke.stroke
                    }
                    finishedStrokes.putAll(newlyFinishedStrokes)
                    for (listener in finishedStrokesListeners) {
                        listener.onShapesCompleted(newlyFinishedStrokes)
                    }
                }
            }

        val renderHelperCallback =
            object : InProgressStrokesRenderHelper.Callback<CompletedShapeT> {

                override fun onDraw() = inProgressStrokesManager.onDraw()

                override fun onDrawComplete() = inProgressStrokesManager.onDrawComplete()

                override fun reportEstimatedPixelPresentationTime(timeNanos: Long) =
                    inProgressStrokesManager.reportEstimatedPixelPresentationTime(timeNanos)

                override fun setCustomLatencyDataField(setter: (LatencyData, Long) -> Unit) =
                    inProgressStrokesManager.setCustomLatencyDataField(setter)

                override fun handOffAllLatencyData() =
                    inProgressStrokesManager.handOffAllLatencyData()

                override fun setPauseStrokeCohortHandoffs(paused: Boolean) =
                    inProgressStrokesManager.setPauseStrokeCohortHandoffs(paused)

                override fun onStrokeCohortHandoffToHwui(
                    strokeCohort: Map<InProgressStrokeId, FinishedStroke<CompletedShapeT>>
                ) = inProgressStrokesManager.onStrokeCohortHandoffToHwui(strokeCohort)

                override fun onStrokeCohortHandoffToHwuiComplete() =
                    inProgressStrokesManager.onStrokeCohortHandoffToHwuiComplete()
            }

        val renderHelper:
            InProgressStrokesRenderHelper<ShapeSpecT, InProgressShapeT, CompletedShapeT> =
            shapeWorkflow.inProgressShapeRenderer
                .let { renderer ->
                    @Suppress(
                        "ObsoleteSdkInt",
                        "DEPRECATION",
                    ) // TODO(b/262911421): Should not need to suppress.
                    if (useHighLatencyRenderHelper) {
                        CanvasInProgressStrokesRenderHelperV21(
                            this@InProgressShapesView,
                            renderHelperCallback,
                            renderer,
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        CanvasInProgressStrokesRenderHelperV33(
                            this@InProgressShapesView,
                            renderHelperCallback,
                            renderer,
                        )
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        CanvasInProgressStrokesRenderHelperV29(
                            this@InProgressShapesView,
                            renderHelperCallback,
                            renderer,
                        )
                    } else {
                        CanvasInProgressStrokesRenderHelperV21(
                            this@InProgressShapesView,
                            renderHelperCallback,
                            renderer,
                        )
                    }
                }
                .also { it.maskPath = maskPath }

        val inProgressStrokesManager =
            InProgressStrokesManager(
                    renderHelper,
                    shapeWorkflow,
                    ::postOnAnimation,
                    ::post,
                    // When InProgressStrokesManager calls back to report a LatencyData, report it
                    // in turn to
                    // the client using the callback that they provided.
                    latencyDataCallback = { latencyDataCallback?.onLatencyData(it) },
                )
                .also {
                    it.addListener(inProgressStrokesManagerListener)
                    // While initializing the InProgressStrokesManager, pass along any properties
                    // that had
                    // been set pre-initialization.
                    it.motionEventToViewTransform = motionEventToViewTransform
                    it.inProgressStrokeCounter = inProgressShapeCounter
                    it.setHandoffDebounceDurationMs(handoffDebounceTimeMs)
                }

        fun addFinishedShapesView() {
            addView(finishedStrokesView, 0)
        }

        fun removeFinishedShapesView() {
            removeView(finishedStrokesView)
        }
    }

    private val finishedStrokesListeners =
        mutableSetOf<InProgressShapesCompletedListener<CompletedShapeT>>()

    /*
     * The finished strokes still being rendered by this view, with map iteration order in stroke
     * z-order from back to front. This mirrors the contents of [finishedStrokesView.finishedStrokes],
     * except that it maps to [CompletedShape] objects instead of to the wrapper [FinishedStroke]
     * objects.
     */
    private val finishedStrokes = mutableMapOf<InProgressStrokeId, CompletedShapeT>()

    // The simplified version of the API assumes that there is only one stroke in progress with a
    // given pointer ID at a time (i.e. that each stroke in a gesture is finished or cancelled
    // before
    // strokes in the next gesture are started, input for strokes from different gestures are not
    // interleaved).
    private val pointerIdToInProgressStrokeId = MutableIntObjectMap<InProgressStrokeId>()

    /**
     * Add a listener to be notified when shapes are finished. These shapes will continue to be
     * rendered within this view until [removeCompletedShapes] is called. All of the shapes that
     * have been delivered to listeners but have not yet been removed with [removeCompletedShapes]
     * are available through [getCompletedShapes].
     */
    public fun addCompletedShapesListener(
        listener: InProgressShapesCompletedListener<CompletedShapeT>
    ) {
        finishedStrokesListeners.add(listener)
    }

    /** Removes a listener that had previously been added with [addCompletedShapesListener]. */
    public fun removeCompletedShapesListener(
        listener: InProgressShapesCompletedListener<CompletedShapeT>
    ) {
        finishedStrokesListeners.remove(listener)
    }

    /** Removes all listeners that had previously been added with [addCompletedShapesListener]. */
    public fun clearCompletedShapesListeners() {
        finishedStrokesListeners.clear()
    }

    /**
     * Eagerly initialize rather than waiting for the first shape to be drawn. Since initialization
     * can be somewhat heavyweight, doing this as soon as it's likely for the user to start drawing
     * can prevent initialization from introducing latency to the first shape.
     */
    public fun eagerInit() {
        @Suppress("UNUSED_VARIABLE") val unused = ensureInit()
    }

    /**
     * Start building a shape using a particular pointer within a [MotionEvent]. This would
     * typically be followed by many calls to [addToShape], and the sequence would end with a call
     * to either [finishShape] or [cancelShape].
     *
     * In most circumstances, prefer to use this function over [startShape] that accepts a
     * [StrokeInput]. Using this function to start a shape must only be followed by the
     * [MotionEvent] variants of [addToShape] and [finishShape] for the same shape.
     *
     * For optimum performance, it is strongly recommended to call [View.requestUnbufferedDispatch]
     * using [event] and the [View] that generated [event] alongside calling this function. When
     * requested this way, unbuffered dispatch mode will automatically end when the gesture is
     * complete.
     *
     * @param event The first [MotionEvent] as part of a shape's input data, typically one with a
     *   [MotionEvent.getActionMasked] value of [MotionEvent.ACTION_DOWN] or
     *   [MotionEvent.ACTION_POINTER_DOWN], but not restricted to those action types.
     * @param pointerId The identifier of the pointer within [event] to be used for inking, as
     *   determined by [MotionEvent.getPointerId] and used as an input to
     *   [MotionEvent.findPointerIndex]. Note that this is the ID of the pointer, not its index.
     * @param shapeSpec Specification for the shape being started.
     * @param motionEventToWorldTransform The matrix that transforms [event] coordinates into the
     *   client app's "world" coordinates, which typically is defined by how a client app's document
     *   is panned/zoomed/rotated. This defaults to the identity matrix, in which case the world
     *   coordinate space is the same as the [MotionEvent] coordinates, but the caller should pass
     *   in their own value reflecting a coordinate system that is independent of the device's pixel
     *   density (e.g. scaled by 1 / [android.util.DisplayMetrics.density]) and any pan/zoom/rotate
     *   gestures that have been applied to the "camera" which portrays the "world" on the device
     *   screen. This matrix must be invertible.
     * @param shapeToWorldTransform Allows an object-specific (shape-specific) coordinate space to
     *   be defined in relation to the caller's "world" coordinate space. This defaults to the
     *   identity matrix, which is typical for many use cases at the time of shape construction. In
     *   typical use cases, shape coordinates and world coordinates may start to differ from one
     *   another after shape creation as a particular shape is manipulated within the world, e.g. it
     *   may be moved, scaled, or rotated relative to other content within an app's document. This
     *   matrix must be invertible.
     * @return The [InProgressStrokeId] of the shape being built, later used to identify which shape
     *   is being updated with [addToShape] or ended with [finishShape] or [cancelShape]. Callers
     *   that assume shapes map one-to-one with pointers in a gesture (as is typical) can skip
     *   storing this return value and use the overrides of [addToShape], [finishShape], and
     *   [cancelShape] that just take a [MotionEvent] and a [pointerId].
     * @throws IllegalArgumentException if [motionEventToWorldTransform] or [shapeToWorldTransform]
     *   is not invertible.
     */
    @JvmOverloads
    public fun startShape(
        event: MotionEvent,
        pointerId: Int,
        shapeSpec: ShapeSpecT,
        motionEventToWorldTransform: Matrix = IDENTITY_MATRIX,
        shapeToWorldTransform: Matrix = IDENTITY_MATRIX,
    ): InProgressStrokeId =
        ensureInit()
            .inProgressStrokesManager
            .startStroke(
                event,
                pointerId,
                motionEventToWorldTransform,
                shapeToWorldTransform,
                shapeSpec,
                strokeUnitLengthCm =
                    strokeUnitLengthCm(motionEventToWorldTransform, shapeToWorldTransform),
            )
            .also { strokeId -> pointerIdToInProgressStrokeId.put(pointerId, strokeId) }

    private fun strokeUnitLengthCm(
        motionEventToWorldTransform: Matrix,
        strokeToWorldTransform: Matrix,
    ): Float {
        val strokeToCmTransform =
            Matrix().also {
                // Compute (world -> MotionEvent) = (MotionEvent -> world)^-1
                require(motionEventToWorldTransform.invert(it)) {
                    "motionEventToWorldTransform must be invertible, but was $motionEventToWorldTransform"
                }
                // Compute (stroke -> MotionEvent) = (world -> MotionEvent) * (stroke -> world)
                it.preConcat(strokeToWorldTransform)
                // Compute (stroke -> screen) = (MotionEvent -> screen) * (stroke -> MotionEvent)
                ViewCompat.transformMatrixToGlobal(this, it)
                // Compute (stroke -> cm) = (screen -> cm) * (stroke -> screen)
                val metrics = context.resources.displayMetrics
                it.postScale(CM_PER_INCH / metrics.xdpi, CM_PER_INCH / metrics.ydpi)
            }
        // Compute the scaling factor that is being applied by the (stroke -> cm) transform. If the
        // transform is isotropic (which it should be, unless the client app is doing something
        // weird),
        // then the vertical and horizontal scaling factors will be the same, but just in case
        // they're
        // not, average them together.
        val values = FloatArray(9)
        strokeToCmTransform.getValues(values)
        return 0.5f * (hypot(values[0], values[1]) + hypot(values[3], values[4]))
    }

    /**
     * Start building a shape with the provided [input]. This would typically be followed by many
     * calls to [addToShape], and the sequence would end with a call to either [finishShape] or
     * [cancelShape].
     *
     * In most circumstances, the [startShape] overload that accepts a [MotionEvent] is more
     * convenient. However, this overload using a [StrokeInput] is available for cases where the
     * input data may not come directly from a [MotionEvent], such as receiving events over a
     * network connection. Using this function to start a shape can only be followed by the
     * [StrokeInput] variants of [addToShape] and [finishShape] for the same shape.
     *
     * If there is a way to request unbuffered dispatch from the source of the input data used here,
     * equivalent to [View.requestUnbufferedDispatch] for unbuffered [MotionEvent] data, then be
     * sure to request it for optimal performance.
     *
     * @param input The [StrokeInput] that started a shape.
     * @param shapeSpec Specification for the shape being started.
     * @param shapeToViewTransform The [Matrix] that converts shape coordinates as provided in
     *   [input] into the coordinate space of this [InProgressShapesView] for rendering.
     * @return The [InProgressStrokeId] of the shape being built, later used to identify which shape
     *   is being updated with [addToShape] or ended with [finishShape] or [cancelShape].
     */
    @JvmOverloads
    public fun startShape(
        input: StrokeInput,
        shapeSpec: ShapeSpecT,
        shapeToViewTransform: Matrix = IDENTITY_MATRIX,
    ): InProgressStrokeId =
        ensureInit().inProgressStrokesManager.startStroke(input, shapeSpec, shapeToViewTransform)

    /**
     * Add input data, from a particular pointer within a [MotionEvent], to an existing shape. The
     * shape must have been started with an overload of [startShape] that accepts a [MotionEvent].
     *
     * @param event The next [MotionEvent] as part of a shape's input data, typically one with
     *   [MotionEvent.getActionMasked] of [MotionEvent.ACTION_MOVE].
     * @param pointerId The identifier of the pointer within [event] to be used for inking, as
     *   determined by [MotionEvent.getPointerId] and used as an input to
     *   [MotionEvent.findPointerIndex]. Note that this is the ID of the pointer, not its index.
     * @param shapeId The [InProgressStrokeId] of the shape to be built upon.
     * @param prediction Predicted [MotionEvent] containing predicted inputs between [event] and the
     *   time of the next frame. This value typically comes from
     *   [androidx.input.motionprediction.MotionEventPredictor.predict]. It is technically optional,
     *   but it is strongly recommended to achieve the best performance.
     */
    @JvmOverloads
    public fun addToShape(
        event: MotionEvent,
        pointerId: Int,
        shapeId: InProgressStrokeId,
        prediction: MotionEvent? = null,
    ) {
        initializedState
            ?.inProgressStrokesManager
            ?.addToStroke(event, pointerId, shapeId, makeCorrectPrediction(prediction))
    }

    /**
     * Add [event] data for [pointerId] to the corresponding in-progress shape, if present. The
     * shape must have been started with an overload of [startShape] that accepts a [MotionEvent].
     *
     * @param event The next [MotionEvent] as part of a shape's input data, typically a
     *   [MotionEvent.ACTION_MOVE].
     * @param pointerId The index of the relevant pointer in the [event]. If [pointerId] does not
     *   correspond to an in-progress shape, this call is ignored and `false` is returned.
     * @param prediction An optional predicted [MotionEvent] containing predicted inputs between
     *   event and the time of the next frame, as generated by
     *   [androidx.input.motionprediction.MotionEventPredictor.predict].
     * @return Whether the pointer corresponds to an in-progress shape.
     */
    @JvmOverloads
    public fun addToShape(
        event: MotionEvent,
        pointerId: Int,
        prediction: MotionEvent? = null,
    ): Boolean {
        addToShape(
            event,
            pointerId,
            pointerIdToInProgressStrokeId[pointerId] ?: return false,
            prediction,
        )
        return true
    }

    /**
     * Add input data from a [StrokeInputBatch] to an existing shape. The shape must have been
     * started with an overload of [startShape] that accepts a [StrokeInput].
     *
     * @param inputs The next [StrokeInputBatch] to be added to the shape.
     * @param shapeId The [InProgressStrokeId] of the shape to be built upon.
     * @param prediction Predicted [StrokeInputBatch] containing predicted inputs between [inputs]
     *   and the time of the next frame. This can technically be empty, but it is strongly
     *   recommended for it to be non-empty to achieve the best performance.
     */
    @JvmOverloads
    public fun addToShape(
        inputs: StrokeInputBatch,
        shapeId: InProgressStrokeId,
        prediction: StrokeInputBatch = ImmutableStrokeInputBatch.EMPTY,
    ) {
        initializedState?.inProgressStrokesManager?.addToStroke(inputs, shapeId, prediction)
    }

    /**
     * Temporary helper to clean prediction input to avoid crashing on multi-pointer draw. Remove
     * once prediction motionevents are cleaned up.
     *
     * TODO b/306361370 - Remove this function when prediction motionevents contain clean eventtime
     * data.
     */
    private fun makeCorrectPrediction(event: MotionEvent?): MotionEvent? {
        if (event == null) return null
        if (event.eventTime == 0L) {
            Log.e(
                "InProgressStrokesView",
                "prediction motionevent has eventTime = 0L and is being ignored.",
            )
            return null
        }
        for (index in 0 until event.historySize) {
            if (event.getHistoricalEventTime(index) == 0L) {
                Log.e(
                    "InProgressStrokesView",
                    "Prediction motionevent has historicalEventTime[$index] = 0L and is being ignored.",
                )
                return null
            }
        }
        return event
    }

    /**
     * Complete the building of a shape, with the last input data coming from a particular pointer
     * of a [MotionEvent]. The shape must have been started with an overload of [startShape] that
     * accepts a [MotionEvent].
     *
     * The resulting [CompletedShapeT] will be passed to [InProgressShapesCompletedListener]
     * (registered with [addCompletedShapesListener]) shortly after all currently in-progress shapes
     * are finished.
     *
     * Does nothing if a shape with the given [shapeId] is not in progress.
     *
     * @param event The last [MotionEvent] as part of a shape's input data, typically one with
     *   [MotionEvent.getActionMasked] of [MotionEvent.ACTION_UP] or
     *   [MotionEvent.ACTION_POINTER_UP], but can also be other actions.
     * @param pointerId The identifier of the pointer within [event] to be used for inking, as
     *   determined by [MotionEvent.getPointerId] and used as an input to
     *   [MotionEvent.findPointerIndex]. Note that this is the ID of the pointer, not its index.
     * @param shapeId The [InProgressStrokeId] of the shape to be finished.
     */
    public fun finishShape(event: MotionEvent, pointerId: Int, shapeId: InProgressStrokeId) {
        // Remove the strokeId from the map. If it corresponded to this pointer ID (the usual case),
        // we can do that in the fast way.
        if (!pointerIdToInProgressStrokeId.remove(pointerId, shapeId)) {
            pointerIdToInProgressStrokeId.removeIf { _, v -> v == shapeId }
        }
        initializedState?.inProgressStrokesManager?.finishStroke(event, pointerId, shapeId)
    }

    /**
     * Finish the corresponding in-progress shape with [event] data for [pointerId], if present. The
     * shape must have been started with an overload of [startShape] that accepts a [MotionEvent].
     * The resulting [CompletedShapeT] will be passed to [InProgressShapesCompletedListener]
     * (registered with [addCompletedShapesListener]) shortly after all currently in-progress shapes
     * are finished.
     *
     * @param event the last [MotionEvent] as part of a shape, typically a [MotionEvent.ACTION_UP].
     * @param pointerId the id of the relevant pointer in the [event]. If [pointerId] does not
     *   correspond to an in-progress shape, this call is ignored and `false` is returned.
     * @return Whether the pointer corresponded to an in-progress shape.
     */
    public fun finishShape(event: MotionEvent, pointerId: Int): Boolean {
        val strokeId = pointerIdToInProgressStrokeId.remove(pointerId) ?: return false
        val state = initializedState ?: return false
        state.inProgressStrokesManager.finishStroke(event, pointerId, strokeId)
        return true
    }

    /**
     * Finish providing inputs for the shape represented by the given [shapeId], with the last input
     * data coming from a [StrokeInput]. The shape must have been started with an overload of
     * [startShape] that accepts a [StrokeInput]. The resulting [CompletedShapeT] will be passed to
     * [InProgressShapesCompletedListener] (registered with [addCompletedShapesListener]) shortly
     * after all currently in-progress shapes are finished.
     *
     * @param input The last [StrokeInput] in the shape.
     * @param shapeId The [InProgressStrokeId] of the shape to be finished.
     */
    public fun finishShape(input: StrokeInput, shapeId: InProgressStrokeId) {
        // In general, use of the StrokeInput[Batch] API won't be mixed with the MotionEvent API
        // (especially the version that isn't keeping track of StrokeId explicitly), so this map
        // will be empty. Even if not, we would expect it to be short.
        pointerIdToInProgressStrokeId.removeIf { _, v -> v == shapeId }
        initializedState?.inProgressStrokesManager?.finishStroke(input, shapeId)
    }

    /**
     * Cancel the building of a shape. It will no longer be visible within this
     * [InProgressShapesView], and no [CompletedShapeT] object will come through
     * [InProgressShapesCompletedListener] for the given [shapeId].
     *
     * This is typically done for one of three reasons:
     * 1. A [MotionEvent] with [MotionEvent.getActionMasked] of [MotionEvent.ACTION_CANCEL]. This
     *    tends to be when an entire gesture has been canceled, for example when a parent [View]
     *    uses [android.view.ViewGroup.onInterceptTouchEvent] to intercept and handle the gesture
     *    itself.
     * 2. A [MotionEvent] with [MotionEvent.getFlags] containing [MotionEvent.FLAG_CANCELED]. This
     *    tends to be when the system has detected an unintentional touch, such as from the user
     *    resting their palm on the screen while writing or drawing, after some events from that
     *    unintentional pointer have already been delivered.
     * 3. An app's business logic reinterprets a gesture previously used for inking as something
     *    else, and the earlier inking may be seen as unintentional. For example, an app that uses
     *    single-pointer gestures for inking and dual-pointer gestures for pan/zoom/rotate will
     *    start inking when the first pointer goes down, but when the second pointer goes down it
     *    may want to cancel the shape from the first pointer rather than leave the small ink marks
     *    on the screen.
     *
     * Does nothing if a shape with the given [shapeId] is not in progress.
     *
     * @param shapeId The [InProgressStrokeId] of the shape to be canceled.
     * @param event The [MotionEvent] that led to this cancellation, if applicable.
     */
    @JvmOverloads
    public fun cancelShape(shapeId: InProgressStrokeId, event: MotionEvent? = null) {
        // Linear scan, but we expect the number of in-progress strokes to be small.
        pointerIdToInProgressStrokeId.removeIf { _, v -> v == shapeId }
        initializedState?.inProgressStrokesManager?.cancelStroke(shapeId, event)
    }

    /**
     * Cancel the corresponding in-progress shape with [event] data for [pointerId], if present. The
     * shape must have been started with an overload of [startShape] that accepts a [MotionEvent].
     *
     * @param event The [MotionEvent] that led to this cancellation, typically an ACTION_CANCEL.
     * @param pointerId the id of the relevant pointer in the [event].
     * @return Whether the pointer corresponded to an in-progress shape.
     */
    public fun cancelShape(event: MotionEvent, pointerId: Int): Boolean {
        val strokeId = pointerIdToInProgressStrokeId.remove(pointerId) ?: return false
        val state = initializedState ?: return false
        state.inProgressStrokesManager.cancelStroke(strokeId, event)
        return true
    }

    /** Cancel all in-progress shapes. */
    public fun cancelUnfinishedShapes() {
        initializedState?.inProgressStrokesManager?.cancelUnfinishedStrokes()
    }

    /** Returns true if there are any in-progress shapes. */
    public fun hasUnfinishedShapes(): Boolean {
        val state = initializedState ?: return false
        return state.inProgressStrokesManager.hasUnfinishedStrokes()
    }

    /**
     * Request that [handoffDebounceTimeMs] be temporarily ignored to hand off rendering to the
     * client's dry layer via [InProgressStrokesFinishedListener.onStrokesFinished]. This will be
     * done as soon as safely possible, still at a time when a rendering flicker can be avoided.
     * Afterwards, handoff debouncing will resume as normal.
     *
     * This API is experimental for now, as one approach to address start-of-shape latency for fast
     * subsequent shapes.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    public fun requestHandoff() {
        initializedState?.inProgressStrokesManager?.requestImmediateHandoff()
    }

    /**
     * Make a best effort to end all currently in progress shapes, which will include a callback to
     * [InProgressShapesCompletedListener.onShapesCompleted] during this function's execution if
     * there are any shapes to hand off. In normal operation, prefer to call [finishShape] or
     * [cancelShape] for each of your in progress shapes and wait for the callback to
     * [InProgressStrokesFinishedListener.onStrokesFinished], possibly accelerated by
     * [requestHandoff] if you have set a non-zero value for [handoffDebounceTimeMs]. This function
     * is for situations where an immediate shutdown is necessary, such as
     * [android.app.Activity.onPause]. This must be called on the UI thread, and will block it for
     * up to a given timeout duration. Note that if this is called when the app is still visible on
     * screen, then the visual behavior is undefined - the shape content may flicker.
     *
     * @param cancelAllInProgress If `true`, treat any unfinished shapes as if you called
     *   [cancelShape] with their [InProgressStrokeId], so they will not be visible and not included
     *   in the return value of [getCompletedShapes]. If `false`, treat unfinished shapes as if you
     *   called [finishShape] with their [InProgressStrokeId], which will keep them visible and
     *   included in the return value of [getCompletedShapes].
     * @param timeout The maximum time that will be spent waiting before returning. If this is not
     *   positive, then this will not wait at all.
     * @param timeoutUnit The [TimeUnit] for [timeout].
     * @return `true` if and only if the flush completed successfully. Note that not all
     *   configurations support flushing, and flushing is best effort, so this is not guaranteed to
     *   return `true`.
     */
    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    public fun flush(
        timeout: Long,
        timeoutUnit: TimeUnit,
        cancelAllInProgress: Boolean = false,
    ): Boolean {
        val state = initializedState ?: return true // Nothing to flush if it's not initialized.
        pointerIdToInProgressStrokeId.clear()
        return state.inProgressStrokesManager.flush(timeout, timeoutUnit, cancelAllInProgress)
    }

    /**
     * For testing only. Wait up to [timeout] ([timeoutUnit]) until the queued actions have all been
     * processed. This must be called on the UI thread, and blocks it to run synchronously. This is
     * useful for tests to know that certain events have been processed, to be able to assert that a
     * screenshot will look a certain way, or that certain callbacks should be scheduled/delivered.
     * Do not call this from production code.
     *
     * In some ways this is similar to [flush], which is intended for production use in certain
     * circumstances.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @VisibleForTesting
    public fun sync(timeout: Long, timeoutUnit: TimeUnit) {
        // Nothing to sync if it's not initialized.
        initializedState?.inProgressStrokesManager?.sync(timeout, timeoutUnit)
    }

    /**
     * Returns all the finished shapes that are still being rendered by this view, with map
     * iteration order in the z-order that the shapes are being rendered, from back to front. This
     * is the same order that shapes were started with [startShape]. The IDs of these shapes should
     * be passed to [removeCompletedShapes] when they are handed off to another view.
     */
    public fun getCompletedShapes(): Map<InProgressStrokeId, CompletedShapeT> {
        return finishedStrokes
    }

    /**
     * Stop this view from rendering the shapes with the given IDs.
     *
     * This should be called in the same UI thread run loop (HWUI frame) as when the shapes start
     * being rendered elsewhere in the view hierarchy. This means they are saved in a location where
     * they will be picked up in a view's next call to [onDraw], and that view's [invalidate] method
     * has been called. If these two operations are not done within the same UI thread run loop
     * (usually side by side - see example below), then there will be brief rendering errors -
     * either a visual gap where the shape is not drawn during a frame, or a double draw where the
     * shape is drawn twice and translucent shapes appear more opaque than they should.
     */
    @UiThread
    public fun removeCompletedShapes(strokeIds: Set<InProgressStrokeId>) {
        for (id in strokeIds) finishedStrokes.remove(id)
        initializedState?.finishedStrokesView?.removeStrokes(strokeIds)
    }

    protected override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initializedState?.addFinishedShapesView()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        initializedState?.removeFinishedShapesView()
    }

    private companion object {
        val IDENTITY_MATRIX = Matrix()
    }
}

/**
 * Renders finished shapes until the client says they are ready to render the shapes themselves with
 * [InProgressShapesView.removeCompletedShapes].
 */
@OptIn(ExperimentalCustomShapeWorkflowApi::class)
@SuppressLint("ViewConstructor") // Not inflated through XML
private class FinishedShapesView<CompletedShapeT : Any>(
    context: Context,
    private val shapeWorkflow: ShapeWorkflow<*, *, CompletedShapeT>,
) : View(context) {

    /** The raw timestamp used for animation progress calculations. */
    private var animationFrameElapsedTimeMillis = 0L

    /** Registered just while the view is attached to update [animationFrameElapsedTimeMillis]. */
    private val choreographerCallback: Choreographer.FrameCallback =
        Choreographer.FrameCallback { frameTimeNanos ->
            animationFrameElapsedTimeMillis = frameTimeNanos / 1_000_000
            if (isAttachedToWindow) {
                Choreographer.getInstance().postFrameCallback(choreographerCallback)
            }
        }

    /*
     * The finished shapes still being rendered by this view, with map iteration order in shape
     * z-order from back to front.
     */
    private val finishedStrokes =
        mutableMapOf<InProgressStrokeId, FinishedStroke<CompletedShapeT>>()

    /**
     * Adds strokes to be rendered by this view. The newly-added strokes will be rendered in front
     * of all other strokes that are already rendered by the view.
     *
     * @param strokes The strokes to add, with map iteration order in stroke z-order from back to
     *   front.
     */
    fun addStrokes(strokes: Map<InProgressStrokeId, FinishedStroke<CompletedShapeT>>) {
        finishedStrokes.putAll(strokes)
        invalidate()
    }

    fun removeStrokes(strokeIds: Set<InProgressStrokeId>) {
        for (strokeId in strokeIds) finishedStrokes.remove(strokeId)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val renderer = shapeWorkflow.completedShapeRenderer
        var scheduleNextFrameImmediately = false
        for ((_, finishedStroke) in finishedStrokes) {
            canvas.withMatrix(finishedStroke.strokeToViewTransform) {
                renderer.draw(
                    canvas,
                    finishedStroke.stroke,
                    finishedStroke.strokeToViewTransform,
                    systemElapsedTimeMillis = animationFrameElapsedTimeMillis,
                )
                if (renderer.changesWithTime(finishedStroke.stroke)) {
                    scheduleNextFrameImmediately = true
                }
            }
        }
        if (scheduleNextFrameImmediately) {
            postInvalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Choreographer.getInstance().postFrameCallback(choreographerCallback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(choreographerCallback)
    }
}
