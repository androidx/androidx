/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.withMatrix
import androidx.ink.authoring.internal.CanvasInProgressStrokesRenderHelperV21
import androidx.ink.authoring.internal.CanvasInProgressStrokesRenderHelperV29
import androidx.ink.authoring.internal.CanvasInProgressStrokesRenderHelperV33
import androidx.ink.authoring.internal.FinishedStroke
import androidx.ink.authoring.internal.InProgressStrokesManager
import androidx.ink.authoring.internal.InProgressStrokesRenderHelper
import androidx.ink.authoring.latency.LatencyData
import androidx.ink.authoring.latency.LatencyDataCallback
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.rendering.android.TextureBitmapStore
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import androidx.test.espresso.idling.CountingIdlingResource
import java.util.concurrent.TimeUnit
import kotlin.math.hypot

// See https://www.nist.gov/pml/owm/si-units-length
private const val CM_PER_INCH = 2.54f

/** Displays in-progress ink strokes as [MotionEvent] user inputs are provided to it. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
@OptIn(ExperimentalLatencyDataApi::class)
public class InProgressStrokesView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, @AttrRes defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    /**
     * Force the HWUI-based high latency implementation to be used under the hood, even if the
     * system supports low latency inking. This takes precedence over [useNewTPlusRenderHelper]. The
     * only reason a developer may want to do this today is if they have a hard product requirement
     * for wet ink strokes to appear in z order between two different HWUI elements - e.g. above a
     * canvas of content but below a floating overlay button/toolbar. This is deprecated because
     * tools to achieve layered rendering are being developed, and soon this API to force high
     * latency wet rendering will be removed in favor of using the best rendering strategy that the
     * device OS level allows.
     *
     * This must be set to its desired value before the first call to [startStroke] or [eagerInit].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @Deprecated("Prefer to allow the underlying implementation details to be chosen automatically.")
    public var useHighLatencyRenderHelper: Boolean = false

    /**
     * Opt into using a new implementation of the wet rendering strategy that is compatible with
     * Android T (API 33) and above. This flag is only temporary to allow for safe, gradual rollout,
     * and will be removed when [CanvasInProgressStrokesRenderHelperV33] is fully rolled out.
     * [useHighLatencyRenderHelper] takes precedence over this.
     *
     * This must be set to its desired value before the first call to [startStroke] or [eagerInit].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    @Deprecated("Prefer to allow the underlying implementation details to be chosen automatically.")
    public var useNewTPlusRenderHelper: Boolean = false

    /**
     * Set a minimum delay from when the user finishes a stroke until rendering is handed off to the
     * client's dry layer via [InProgressStrokesFinishedListener.onStrokesFinished]. This value
     * would ideally be long enough that quick subsequent strokes - such as for fast handwriting -
     * are processed and later handed off as one group, but short enough that the handoff can take
     * place during short, natural pauses in handwriting.
     *
     * If handoff is ever needed as soon as safely possible, call [requestHandoff].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public var handoffDebounceTimeMs: Long = 0L
        @UiThread
        set(value) {
            require(value >= 0L) { "Debounce time must not be negative, received $value" }
            field = value
            // Don't force initialization to set this value, otherwise the properties that must be
            // set
            // before initialization would be harder to set. Hold onto it and pass it down to the
            // InProgressStrokesManager when it gets initialized.
            if (isInitialized()) {
                inProgressStrokesManager.setHandoffDebounceTimeMs(value)
            }
        }

    /**
     * [TextureBitmapStore] used by the default value for [rendererFactory].
     *
     * By default, this is a no-op implementation that does not load any brush textures. The factory
     * functions are called when the renderer is initialized, so if this will be changed to
     * something that does load and store texture images, it must be set before the first call to
     * [startStroke] or [eagerInit].
     */
    // Needed on both property and on getter for AndroidX build, but the Kotlin compiler doesn't
    // like it on the getter so suppress its complaint.
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalInkCustomBrushApi
    @get:ExperimentalInkCustomBrushApi
    @set:ExperimentalInkCustomBrushApi
    public var textureBitmapStore: TextureBitmapStore = TextureBitmapStore { null }
        set(value) {
            check(!isInitialized()) { "Cannot set textureBitmapStore after initialization." }
            field = value
        }

    /**
     * A function that creates a [CanvasStrokeRenderer] when invoked. The default implementation of
     * this will automatically account for the Android OS version of the device. If you choose to
     * replace the default with an alternate implementation, then you must set this variable before
     * the first call to [startStroke] or [eagerInit].
     */
    public var rendererFactory: () -> CanvasStrokeRenderer = {
        @OptIn(ExperimentalInkCustomBrushApi::class) CanvasStrokeRenderer.create(textureBitmapStore)
    }
        set(value) {
            check(!isInitialized()) { "Cannot set rendererFactory after initialization." }
            field = value
        }

    /**
     * Denote an area of this [InProgressStrokesView] where no ink should be visible. This is useful
     * for UI elements that float on top of (in Z order) the drawing surface - without this, a user
     * would be able to draw in-progress ("wet") strokes on top of those UI elements, but then when
     * the stroke is finished, it will appear as a dry stroke underneath of the UI element. If this
     * mask is set to the shape and position of the floating UI element, then the ink will never be
     * rendered in that area, making it appear as if it's being drawn underneath the UI element.
     *
     * This technique is most convincing when the UI element is opaque. Often there are parts of the
     * UI element that are translucent, such as drop shadows, or anti-aliasing along the edges. The
     * result will look a little different between wet and dry strokes for those cases, but it can
     * be a worthwhile tradeoff compared to the alternative of drawing wet strokes on top of that UI
     * element.
     */
    public var maskPath: Path? = null
        set(value) {
            field = value
            renderHelper?.maskPath = value
        }

    /**
     * The transform matrix to convert [MotionEvent] coordinates, as passed to [startStroke],
     * [addToStroke], and [finishStroke], into coordinates of this [InProgressStrokesView] for
     * rendering. Defaults to the identity matrix, for the recommended case where
     * [InProgressStrokesView] exactly overlays the [android.view.View] that has the touch listener
     * from which [MotionEvent] instances are being forwarded.
     */
    public var motionEventToViewTransform: Matrix = Matrix()
        set(value) {
            field.set(value)
            // Don't force initialization to set this value, otherwise the properties that must be
            // set
            // before initialization would be harder to set. Hold onto it and pass it down to the
            // InProgressStrokesManager when it gets initialized.
            if (isInitialized()) {
                inProgressStrokesManager.motionEventToViewTransform = value
            }
        }

    /**
     * Allows a test to easily wait until all in-progress strokes are completed and handed off.
     * There is no reason to set this in non-test code. The recommended approach is to include this
     * small object within production code, but actually registering it and making use of it would
     * be exclusive to test code.
     *
     * https://developer.android.com/training/testing/espresso/idling-resource#integrate-recommended-approach
     */
    public var inProgressStrokeCounter: CountingIdlingResource? = null
        set(value) {
            field = value
            // Don't force initialization to set this value, otherwise the properties that must be
            // set
            // before initialization would be harder to set. Hold onto it and pass it down to the
            // InProgressStrokesManager when it gets initialized.
            if (isInitialized()) {
                inProgressStrokesManager.inProgressStrokeCounter = value
            }
        }

    /**
     * An optional callback for reporting latency of the processing of input events for in-progress
     * strokes. Clients may implement the [LatencyDataCallback] interface and set this field to
     * receive latency measurements.
     *
     * Notes for clients: Do not hold references to the [LatencyData] passed into this callback.
     * After this callback returns, the [LatencyData] instance will immediately become invalid: it
     * will be deleted or recycled. Also, to avoid stalling the UI thread, implementers should
     * minimize the amount of computation in this callback, and should also avoid allocations (since
     * allocation may trigger the garbage collector).
     */
    // Needed on both property and on getter for AndroidX build, but the Kotlin compiler doesn't
    // like it on the getter so suppress its complaint.
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLatencyDataApi
    @get:ExperimentalLatencyDataApi
    @set:ExperimentalLatencyDataApi
    public var latencyDataCallback: LatencyDataCallback? = null

    private val renderHelperCallback =
        object : InProgressStrokesRenderHelper.Callback {

            override fun onDraw() = inProgressStrokesManager.onDraw()

            override fun onDrawComplete() = inProgressStrokesManager.onDrawComplete()

            override fun reportEstimatedPixelPresentationTime(timeNanos: Long) =
                inProgressStrokesManager.reportEstimatedPixelPresentationTime(timeNanos)

            override fun setCustomLatencyDataField(setter: (LatencyData, Long) -> Unit) =
                inProgressStrokesManager.setCustomLatencyDataField(setter)

            override fun handOffAllLatencyData() = inProgressStrokesManager.handOffAllLatencyData()

            override fun setPauseStrokeCohortHandoffs(paused: Boolean) =
                inProgressStrokesManager.setPauseStrokeCohortHandoffs(paused)

            override fun onStrokeCohortHandoffToHwui(
                strokeCohort: Map<InProgressStrokeId, FinishedStroke>
            ) = inProgressStrokesManager.onStrokeCohortHandoffToHwui(strokeCohort)

            override fun onStrokeCohortHandoffToHwuiComplete() =
                inProgressStrokesManager.onStrokeCohortHandoffToHwuiComplete()
        }

    private val finishedStrokesListeners = mutableSetOf<InProgressStrokesFinishedListener>()

    private val finishedStrokes = mutableMapOf<InProgressStrokeId, Stroke>()

    // Most callers can use inProgressStrokesManager, but isInitialized() needs direct access to the
    // delegate's isInitialized method.
    private val inProgressStrokesManagerDelegate = lazy {
        InProgressStrokesManager(
                inProgressStrokesRenderHelper(),
                ::postOnAnimation,
                ::post,
                // When InProgressStrokesManager calls back to report a LatencyData, report it in
                // turn to
                // the client using the callback that they provided.
                latencyDataCallback = { latencyDataCallback?.onLatencyData(it) },
            )
            .also {
                it.addListener(inProgressStrokesManagerListener)
                // While initializing the InProgressStrokesManager, pass along any properties that
                // had been
                // set pre-initialization.
                it.motionEventToViewTransform = motionEventToViewTransform
                it.inProgressStrokeCounter = inProgressStrokeCounter
                it.setHandoffDebounceTimeMs(handoffDebounceTimeMs)
            }
    }
    private val inProgressStrokesManager by inProgressStrokesManagerDelegate

    private val inProgressStrokesManagerListener =
        object : InProgressStrokesManager.Listener {
            override fun onAllStrokesFinished(strokes: Map<InProgressStrokeId, FinishedStroke>) {
                finishedStrokesView.addStrokes(strokes)

                val newlyFinishedStrokes = mutableMapOf<InProgressStrokeId, Stroke>()
                for ((strokeId, finishedStroke) in strokes) {
                    newlyFinishedStrokes[strokeId] = finishedStroke.stroke
                }

                finishedStrokes.putAll(newlyFinishedStrokes)
                for (listener in finishedStrokesListeners) {

                    listener.onStrokesFinished(newlyFinishedStrokes)
                }
            }
        }

    private var renderHelper: InProgressStrokesRenderHelper? = null

    private val finishedStrokesView =
        FinishedStrokesView(
            context,
            createRenderer = rendererFactory,
        )

    private fun inProgressStrokesRenderHelper(): InProgressStrokesRenderHelper {
        val existingInstance = renderHelper
        if (existingInstance != null) return existingInstance

        val renderer = rendererFactory()

        @Suppress("ObsoleteSdkInt") // TODO(b/262911421): Should not need to suppress.
        val result =
            @Suppress("DEPRECATION")
            if (useHighLatencyRenderHelper) {
                CanvasInProgressStrokesRenderHelperV21(
                    this,
                    renderHelperCallback,
                    renderer,
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (useNewTPlusRenderHelper) {
                    CanvasInProgressStrokesRenderHelperV33(
                        this,
                        renderHelperCallback,
                        renderer,
                    )
                } else {
                    // Newer OS versions on Lenovo P12 Pro hit an issue with the v29 implementation
                    // of the
                    // offscreen frame buffer. It works fine on the v33 implementation, but if the
                    // v33 version
                    // is not enabled, use the v29 version without the offscreen frame buffer.
                    CanvasInProgressStrokesRenderHelperV29(
                        this,
                        renderHelperCallback,
                        renderer,
                        useOffScreenFrameBuffer = false,
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                CanvasInProgressStrokesRenderHelperV29(
                    this,
                    renderHelperCallback,
                    renderer,
                    useOffScreenFrameBuffer = true,
                )
            } else {
                CanvasInProgressStrokesRenderHelperV21(
                    this,
                    renderHelperCallback,
                    renderer,
                )
            }

        result.maskPath = maskPath
        renderHelper = result

        return result
    }

    private fun isInitialized() = inProgressStrokesManagerDelegate.isInitialized()

    /**
     * Add a listener to be notified when strokes are finished. These strokes will continue to be
     * rendered within this view until [removeFinishedStrokes] is called. All of the strokes that
     * have been delivered to listeners but have not yet been removed with [removeFinishedStrokes]
     * are available through [getFinishedStrokes].
     */
    public fun addFinishedStrokesListener(listener: InProgressStrokesFinishedListener) {
        finishedStrokesListeners.add(listener)
    }

    /** Removes a listener that had previously been added with [addFinishedStrokesListener]. */
    public fun removeFinishedStrokesListener(listener: InProgressStrokesFinishedListener) {
        finishedStrokesListeners.remove(listener)
    }

    /**
     * Eagerly initialize rather than waiting for the first stroke to be drawn. Since initialization
     * can be somewhat heavyweight, doing this as soon as it's likely for the user to start drawing
     * can prevent initialization from introducing latency to the first stroke.
     */
    public fun eagerInit() {
        // Getting the lazy value kicks off its initialization.
        @Suppress("UNUSED_VARIABLE") val unused = inProgressStrokesManager
    }

    /**
     * Start building a stroke with the [event] data for [pointerId].
     *
     * @param event The first [MotionEvent] as part of a Stroke's input data, typically an
     *   ACTION_DOWN.
     * @param pointerId The id of the relevant pointer in the [event].
     * @param motionEventToWorldTransform The matrix that transforms [event] coordinates into the
     *   client app's "world" coordinates, which typically is defined by how a client app's document
     *   is panned/zoomed/rotated. This defaults to the identity matrix, in which case the world
     *   coordinate space is the same as the [MotionEvent] coordinates, but the caller should pass
     *   in their own value reflecting a coordinate system that is independent of the device's pixel
     *   density (e.g. scaled by 1 / [android.util.DisplayMetrics.density]) and any pan/zoom/rotate
     *   gestures that have been applied to the "camera" which portrays the "world" on the device
     *   screen. This matrix must be invertible.
     * @param strokeToWorldTransform An optional matrix that transforms this stroke into the client
     *   app's "world" coordinates, which allows the coordinates of the stroke to be defined in
     *   something other than world coordinates. Defaults to the identity matrix, in which case the
     *   stroke coordinate space is the same as world coordinate space. This matrix must be
     *   invertible.
     * @param brush Brush specification for the stroke being started. Note that if
     *   [motionEventToWorldTransform] and [strokeToWorldTransform] combine to a [MotionEvent] to
     *   stroke coordinates transform that scales stroke coordinate units to be very different in
     *   size than screen pixels, then it is recommended to update the value of [Brush.epsilon] to
     *   reflect that.
     * @return The Stroke ID of the stroke being built, later used to identify which stroke is being
     *   added to, finished, or canceled.
     * @throws IllegalArgumentException if [motionEventToWorldTransform] or [strokeToWorldTransform]
     *   is not invertible.
     */
    @JvmOverloads
    public fun startStroke(
        event: MotionEvent,
        pointerId: Int,
        brush: Brush,
        motionEventToWorldTransform: Matrix = Matrix(),
        strokeToWorldTransform: Matrix = Matrix(),
    ): InProgressStrokeId =
        inProgressStrokesManager.startStroke(
            event,
            pointerId,
            motionEventToWorldTransform,
            strokeToWorldTransform,
            brush,
            strokeUnitLengthCm =
                strokeUnitLengthCm(motionEventToWorldTransform, strokeToWorldTransform),
        )

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
                // Compute (stroke -> cm) = (MotionEvent -> cm) * (stroke -> MotionEvent)
                // This assumes that MotionEvent's coordinate space is hardware pixels.
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
     * Start building a stroke with the provided [input].
     *
     * @param input The [StrokeInput] that started a stroke.
     * @param brush Brush specification for the stroke being started. Note that if
     *   [motionEventToWorldTransform] and [strokeToWorldTransform] combine to a [MotionEvent] to
     *   stroke coordinates transform that scales stroke coordinate units to be very different in
     *   size than screen pixels, then it is recommended to update the value of [Brush.epsilon] to
     *   reflect that.
     * @return The Stroke ID of the stroke being built, later used to identify which stroke is being
     *   added to, finished, or canceled.
     */
    public fun startStroke(input: StrokeInput, brush: Brush): InProgressStrokeId =
        inProgressStrokesManager.startStroke(input, brush)

    /**
     * Add [event] data for [pointerId] to already started stroke with [strokeId].
     *
     * @param event the next [MotionEvent] as part of a Stroke's input data, typically an
     *   ACTION_MOVE.
     * @param pointerId the id of the relevant pointer in the [event].
     * @param strokeId the Stroke that is to be built upon with [event].
     * @param prediction optional predicted [MotionEvent] containing predicted inputs between event
     *   and the time of the next frame, as generated by
     *   [androidx.input.motionprediction.MotionEventPredictor.predict].
     */
    @JvmOverloads
    public fun addToStroke(
        event: MotionEvent,
        pointerId: Int,
        strokeId: InProgressStrokeId,
        prediction: MotionEvent? = null,
    ): Unit =
        inProgressStrokesManager.addToStroke(
            event,
            pointerId,
            strokeId,
            makeCorrectPrediction(prediction),
        )

    /**
     * Add [inputs] to already started stroke with [strokeId].
     *
     * @param inputs the next [StrokeInputBatch] to be added to the stroke.
     * @param strokeId the Stroke that is to be built upon with [inputs].
     * @param prediction optional [StrokeInputBatch] containing predicted inputs after this portion
     *   of the stroke.
     */
    @JvmOverloads
    public fun addToStroke(
        inputs: StrokeInputBatch,
        strokeId: InProgressStrokeId,
        prediction: StrokeInputBatch = ImmutableStrokeInputBatch.EMPTY,
    ): Unit = inProgressStrokesManager.addToStroke(inputs, strokeId, prediction)

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
     * Complete the building of a stroke.
     *
     * @param event the last [MotionEvent] as part of a stroke, typically an ACTION_UP.
     * @param pointerId the id of the relevant pointer in the [event].
     * @param strokeId the stroke that is to be finished with the latest event.
     */
    public fun finishStroke(
        event: MotionEvent,
        pointerId: Int,
        strokeId: InProgressStrokeId
    ): Unit = inProgressStrokesManager.finishStroke(event, pointerId, strokeId)

    /**
     * Complete the building of a stroke.
     *
     * @param input the last [StrokeInput] in the stroke.
     * @param strokeId the stroke that is to be finished with the latest event.
     */
    public fun finishStroke(input: StrokeInput, strokeId: InProgressStrokeId): Unit =
        inProgressStrokesManager.finishStroke(input, strokeId)

    /**
     * Cancel the building of a stroke.
     *
     * @param strokeId the stroke to cancel.
     * @param event The [MotionEvent] that led to this cancellation, if applicable.
     */
    @JvmOverloads
    public fun cancelStroke(strokeId: InProgressStrokeId, event: MotionEvent? = null): Unit =
        inProgressStrokesManager.cancelStroke(strokeId, event)

    /**
     * Request that [handoffDebounceTimeMs] be temporarily ignored to hand off rendering to the
     * client's dry layer via [InProgressStrokesFinishedListener.onStrokesFinished]. This will be
     * done as soon as safely possible, still at a time when a rendering flicker can be avoided.
     * Afterwards, handoff debouncing will resume as normal.
     *
     * This API is experimental for now, as one approach to address start-of-stroke latency for fast
     * subsequent strokes.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public fun requestHandoff(): Unit = inProgressStrokesManager.requestImmediateHandoff()

    /**
     * Make a best effort to end all currently in progress strokes, which will include a callback to
     * [InProgressStrokesFinishedListener.onStrokesFinished] during this function's execution if
     * there are any strokes to hand off. In normal operation, prefer to call [finishStroke] or
     * [cancelStroke] for each of your in progress strokes and wait for the callback to
     * [InProgressStrokesFinishedListener.onStrokesFinished], possibly accelerated by
     * [requestHandoff] if you have set a non-zero value for [handoffDebounceTimeMs]. This function
     * is for situations where an immediate shutdown is necessary, such as
     * [android.app.Activity.onPause]. This must be called on the UI thread, and will block it for
     * up to [timeoutMillis] milliseconds. Note that if this is called when the app is still visible
     * on screen, then the visual behavior is undefined - the stroke content may flicker.
     *
     * @param cancelAllInProgress If `true`, treat any unfinished strokes as if you called
     *   [cancelStroke] with their [InProgressStrokeId], so they will not be visible and not
     *   included in the return value of [getFinishedStrokes]. If `false`, treat unfinished strokes
     *   as if you called [finishStroke] with their [InProgressStrokeId], which will keep them
     *   visible and included in the return value of [getFinishedStrokes].
     * @param timeout The maximum time that will be spent waiting before returning. If this is not
     *   positive, then this will not wait at all.
     * @param timeoutUnit The [TimeUnit] for [timeout].
     * @return `true` if and only if the flush completed successfully. Note that not all
     *   configurations support flushing, and flushing is best effort, so this is not guaranteed to
     *   return `true`.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
    public fun flush(
        timeout: Long,
        timeoutUnit: TimeUnit,
        cancelAllInProgress: Boolean = false,
    ): Boolean {
        if (!isInitialized()) {
            // Nothing to flush if it's not initialized.
            return true
        }
        return inProgressStrokesManager.flush(timeout, timeoutUnit, cancelAllInProgress)
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
    @VisibleForTesting
    internal fun sync(timeout: Long, timeoutUnit: TimeUnit) {
        if (isInitialized()) {
            // Nothing to sync if it's not initialized.
            inProgressStrokesManager.sync(timeout, timeoutUnit)
        }
    }

    /**
     * Returns all the finished strokes that are still being rendered by this view. The IDs of these
     * strokes should be passed to [removeFinishedStrokes] when they are handed off to another view.
     */
    public fun getFinishedStrokes(): Map<InProgressStrokeId, Stroke> {
        return finishedStrokes
    }

    /**
     * Stop this view from rendering the strokes with the given IDs.
     *
     * This should be called in the same UI thread run loop (HWUI frame) as when the strokes start
     * being rendered elsewhere in the view hierarchy. This means they are saved in a location where
     * they will be picked up in a view's next call to [onDraw], and that view's [invalidate] method
     * has been called. If these two operations are not done within the same UI thread run loop
     * (usually side by side - see example below), then there will be brief rendering errors -
     * either a visual gap where the stroke is not drawn during a frame, or a double draw where the
     * stroke is drawn twice and translucent strokes appear more opaque than they should.
     */
    public fun removeFinishedStrokes(strokeIds: Set<InProgressStrokeId>) {
        for (id in strokeIds) finishedStrokes.remove(id)
        finishedStrokesView.removeStrokes(strokeIds)
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addView(finishedStrokesView)
    }
}

/**
 * Renders finished strokes until the client says they are ready to render the strokes themselves
 * with [InProgressStrokesView.removeFinishedStrokes].
 */
@SuppressLint("ViewConstructor") // Not inflated through XML
private class FinishedStrokesView(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    // Lazy, since many clients will call removeFinishedStrokes immediately with the callback and
    // never need to render strokes within this holding view.
    createRenderer: () -> CanvasStrokeRenderer,
) : View(context, attrs, defStyleAttr) {

    private val renderer by lazy(createRenderer)

    private val finishedStrokes = mutableMapOf<InProgressStrokeId, FinishedStroke>()

    fun addStrokes(strokes: Map<InProgressStrokeId, FinishedStroke>) {
        finishedStrokes.putAll(strokes)
        invalidate()
    }

    fun removeStrokes(strokeIds: Set<InProgressStrokeId>) {
        for (strokeId in strokeIds) finishedStrokes.remove(strokeId)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        @Suppress("UNUSED_VARIABLE")
        for ((strokeId, finishedStroke) in finishedStrokes) {
            canvas.withMatrix(finishedStroke.strokeToViewTransform) {
                renderer.draw(canvas, finishedStroke.stroke, finishedStroke.strokeToViewTransform)
            }
        }
    }
}
