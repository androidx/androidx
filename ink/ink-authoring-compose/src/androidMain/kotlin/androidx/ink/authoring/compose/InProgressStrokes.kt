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

package androidx.ink.authoring.compose

import android.view.MotionEvent
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.setFrom
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor
import java.util.concurrent.TimeUnit

/**
 * A [Composable] that displays in-progress ink strokes as it receives user pointer input.
 *
 * Multiple simultaneous strokes are supported. This implementation handles all best practices for
 * the fastest, lowest latency rendering of real-time user input. For an alternative compatible with
 * Android Views, see [InProgressStrokesView].
 *
 * For performance reasons, it is recommended to only have one [InProgressStrokes] [Composable] on
 * screen at a time, and to avoid unnecessarily resizing it or changing whether it is present on
 * screen.
 *
 * This will process pointers (and display the resulting strokes) that are not already consumed by
 * an earlier touch listener - when
 * [androidx.compose.ui.input.pointer.PointerInputChange.isConsumed] is `false`. If a pointer's down
 * event arrives to this composable already consumed, it will not be drawn, even if later events for
 * that pointer arrive unconsumed. If a pointer's initial events are not consumed by arrival, then
 * it will be drawn, but if its later events have been consumed by arrival then the stroke for that
 * pointer will be canceled and removed from this composable's UI. If all of a pointer's events from
 * down through moves through up arrive to this composable unconsumed, then the entire stroke will
 * be drawn and finished, and the finished stroke will be made available by a call to
 * [onStrokesFinished]. All strokes that were in progress simultaneously will be delivered in the
 * same callback, running on the UI thread. To avoid visual flicker when transitioning rendering
 * from this composable to another composable, during this callback the finished strokes must be
 * added to some mutable state that triggers a recomposition that will render those strokes,
 * typically using [androidx.ink.rendering.android.canvas.CanvasStrokeRenderer].
 *
 * @param defaultBrush [Brush] specification for the stroke being started. If this is `null`,
 *   nothing will be drawn, but the internal rendering resources will be preserved to avoid paying
 *   the cost for reinitialization when drawing is needed again. If different [Brush] values are
 *   needed for different simultaneous strokes, use [nextBrush] to return a different [Brush] each
 *   time. Note that the overall scaling factor of [pointerEventToWorldTransform] and
 *   [strokeToWorldTransform] combined should be related to the value of [Brush.epsilon] - in
 *   general, the larger the combined `pointerEventToStrokeTransform` scaling factor is, the smaller
 *   on screen the stroke units are, so [Brush.epsilon] should be a larger quantity of stroke units
 *   to maintain a similar screen size.
 * @param nextBrush A way to override [defaultBrush], which will be called at the start of each
 *   pointer.
 * @param pointerEventToWorldTransform The matrix that transforms
 *   [androidx.compose.ui.input.pointer.PointerEvent] coordinates into the caller's "world"
 *   coordinates, which typically is defined by how the caller's document is panned/zoomed/rotated.
 *   This defaults to the identity matrix, in which case the world coordinate space is the same as
 *   the input event coordinates, but the caller should pass in their own value reflecting a
 *   coordinate system that is independent of the device's pixel density (e.g. scaled by 1 /
 *   [android.util.DisplayMetrics.density]) and any pan/zoom/rotate gestures that have been applied
 *   to the "camera" which portrays the "world" on the device screen. This matrix must be
 *   invertible.
 * @param strokeToWorldTransform Allows an object-specific (stroke-specific) coordinate space to be
 *   defined in relation to the caller's "world" coordinate space. This defaults to the identity
 *   matrix, which is typical for many use cases at the time of stroke construction. In typical use
 *   cases, stroke coordinates and world coordinates may start to differ from one another only after
 *   stroke authoring as a finished stroke is manipulated within the world, e.g. it may be moved,
 *   scaled, or rotated relative to other content within an app's document. This matrix must be
 *   invertible.
 * @param maskPath An area of this composable where no ink will be visible. A value of `null`
 *   indicates that strokes will be visible anywhere they are drawn. This is useful for UI elements
 *   that float on top of (in Z order) the drawing surface - without this, a user would be able to
 *   draw in-progress ("wet") strokes on top of those UI elements, but then when the stroke is
 *   finished, it will appear as a dry stroke underneath of the UI element. If this mask is set to
 *   the shape and position of the floating UI element, then the ink will never be rendered in that
 *   area, making it appear as if it's being drawn underneath the UI element. This technique is most
 *   convincing when the UI element is opaque. Often there are parts of the UI element that are
 *   translucent, such as drop shadows, or anti-aliasing along the edges. The result will look a
 *   little different between wet and dry strokes for those cases, but it can be a worthwhile
 *   tradeoff compared to the alternative of drawing wet strokes on top of that UI element. Note
 *   that this parameter does not affect the contents of the strokes at all, nor how they appear
 *   when drawn in a separate composable after [onStrokesFinished] is called - just how the strokes
 *   appear when they are still in progress in this composable.
 * @param textureBitmapStore Allows texture asset images to be loaded, corresponding to texture
 *   layers that may be specified by [defaultBrush] or [nextBrush].
 * @param onStrokesFinished Called when there are no longer any in-progress strokes for a short
 *   period. All strokes that were in progress simultaneously will be delivered in the same
 *   callback, running on the UI thread. The callback should pass the strokes to another
 *   [Composable], triggering its recomposition within the same UI thread run loop as the callback
 *   being received, for that other [Composable] to render the strokes without any brief rendering
 *   errors (flickers). These flickers may come from a gap where a stroke is drawn in neither this
 *   [Composable] nor the app's [Composable] during a frame, or a double draw where the stroke is
 *   drawn twice and translucent strokes appear more opaque than they should. Be careful about
 *   passing strokes to asynchronous frameworks during this callback, as they may switch threads
 *   during the handoff from producer to consumer, and even if the final consumer is on the UI
 *   thread, it may not be in the same UI thread run loop and lead to a flicker.
 */
@Composable
@OptIn(ExperimentalInkCustomBrushApi::class)
public fun InProgressStrokes(
    defaultBrush: Brush?,
    nextBrush: () -> Brush? = { defaultBrush },
    pointerEventToWorldTransform: Matrix = IDENTITY_MATRIX,
    strokeToWorldTransform: Matrix = IDENTITY_MATRIX,
    maskPath: Path? = null,
    textureBitmapStore: TextureBitmapStore = TextureBitmapStore { null },
    onStrokesFinished: (List<Stroke>) -> Unit,
) {
    InProgressStrokes(
        nextBrush = nextBrush,
        nextPointerEventToWorldTransform = { pointerEventToWorldTransform },
        nextStrokeToWorldTransform = { strokeToWorldTransform },
        maskPath = maskPath,
        onStrokesFinished = onStrokesFinished,
    )
}

@OptIn(ExperimentalInkCustomBrushApi::class)
@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
@Composable
public fun InProgressStrokes(
    nextBrush: () -> Brush?,
    nextPointerEventToWorldTransform: () -> Matrix = { IDENTITY_MATRIX },
    nextStrokeToWorldTransform: () -> Matrix = { IDENTITY_MATRIX },
    maskPath: Path? = null,
    textureBitmapStore: TextureBitmapStore = TextureBitmapStore { null },
    onSyncAvailable: ((Long, TimeUnit) -> Unit) -> Unit = {},
    onStrokesFinished: (List<Stroke>) -> Unit,
) {
    var inProgressStrokesView by remember { mutableStateOf<InProgressStrokesView?>(null) }
    var motionEventPredictor by remember { mutableStateOf<MotionEventPredictor?>(null) }
    val containingView = LocalView.current

    @Suppress("PrimitiveInCollection") // Need Snapshot-compatible collection for correctness
    val changeIdsToMotionEventPointerIds = remember { mutableStateMapOf<PointerId, Int>() }
    @Suppress("PrimitiveInCollection") // Need Snapshot-compatible collection for correctness
    val inProgressPointers = remember { mutableStateMapOf<PointerId, InProgressPointer>() }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .pointerInput(nextPointerEventToWorldTransform, nextStrokeToWorldTransform) {
                    awaitEachGesture {
                        do {
                            val ipsv =
                                checkNotNull(inProgressStrokesView) {
                                    "InProgressStrokesView should have been initialized in AndroidView.factory"
                                }
                            val predictor =
                                checkNotNull(motionEventPredictor) {
                                    "MotionEventPredictor should have been initialized in AndroidView.factory"
                                }

                            val event = awaitPointerEvent()

                            // Ignore hover events, but don't prevent up events from being
                            // processed.
                            if (
                                changeIdsToMotionEventPointerIds.isEmpty() &&
                                    !event.changes.fastAny { it.pressed }
                            ) {
                                return@awaitEachGesture
                            }

                            if (event.type == PointerEventType.Press) {
                                // Do not use the position (x, y) data of a non-local MotionEvent.
                                val nonLocalCoordinatesMotionEvent =
                                    checkNotNull(event.motionEvent) {
                                        "Only PointerEvents that come from Android MotionEvents are supported - " +
                                            "synthetic PointerEvents are not supported."
                                    }

                                // Compose doesn't expose its mapping from PointerInputChange.id to
                                // MotionEvent
                                // pointerId, so keep track of it ourselves. This relies on the fact
                                // that in a
                                // MotionEvent (and therefore a PointerEvent on Android), only one
                                // pointer can go
                                // down per event.
                                event.changes.fastForEach { change ->
                                    if (change.changedToDownIgnoreConsumed()) {
                                        val motionEventPointerId =
                                            nonLocalCoordinatesMotionEvent.getPointerId(
                                                nonLocalCoordinatesMotionEvent.actionIndex
                                            )
                                        changeIdsToMotionEventPointerIds[change.id] =
                                            motionEventPointerId
                                        // Only one pointer can go down per event.
                                        return@fastForEach
                                    }
                                }
                            }

                            val localCoordinatesMotionEvent =
                                event.obtainLocalMotionEvent(changeIdsToMotionEventPointerIds)

                            // Remove any entries no longer present or no longer pressed. This will
                            // include a
                            // pointer currently going up, which is why this is done after
                            // `obtainLocalMotionEvent`, but the below logic has the MotionEvent
                            // pointer ID for
                            // that pointer in the InProgressPointer object.
                            val changeIdToMotionEventPointerIdIter =
                                changeIdsToMotionEventPointerIds.iterator()
                            for (entry in changeIdToMotionEventPointerIdIter) {
                                if (
                                    !event.changes.fastAny { change ->
                                        change.id == entry.key && change.pressed
                                    }
                                ) {
                                    changeIdToMotionEventPointerIdIter.remove()
                                }
                            }

                            if (localCoordinatesMotionEvent != null) {
                                predictor.record(localCoordinatesMotionEvent)
                            }
                            val predictedMotionEvent =
                                if (event.type == PointerEventType.Move) {
                                    predictor.predict()
                                } else {
                                    null
                                }
                            try {
                                event.changes.fastForEach { change ->
                                    val currentPointer = inProgressPointers[change.id]
                                    if (currentPointer != null && change.isConsumed) {
                                        ipsv.cancelStroke(currentPointer.strokeId)
                                        inProgressPointers.remove(change.id)
                                    } else if (
                                        currentPointer == null &&
                                            change.changedToDown() &&
                                            localCoordinatesMotionEvent != null
                                    ) {
                                        val motionEventPointerId =
                                            changeIdsToMotionEventPointerIds[change.id]
                                        checkNotNull(motionEventPointerId) {
                                            "changeId ${change.id} not mapped to a pointerId."
                                        }
                                        val brush = nextBrush()
                                        if (brush != null) {
                                            val strokeId =
                                                ipsv.startStroke(
                                                    event = localCoordinatesMotionEvent,
                                                    pointerId = motionEventPointerId,
                                                    brush = brush,
                                                    motionEventToWorldTransform =
                                                        android.graphics.Matrix().apply {
                                                            setFrom(
                                                                nextPointerEventToWorldTransform()
                                                            )
                                                        },
                                                    strokeToWorldTransform =
                                                        android.graphics.Matrix().apply {
                                                            setFrom(nextStrokeToWorldTransform())
                                                        },
                                                )
                                            inProgressPointers[change.id] =
                                                InProgressPointer(
                                                    strokeId = strokeId,
                                                    motionEventPointerId = motionEventPointerId,
                                                )
                                            change.consume()
                                        }
                                    } else if (
                                        currentPointer != null &&
                                            change.changedToUp() &&
                                            localCoordinatesMotionEvent != null
                                    ) {
                                        inProgressPointers.remove(change.id)
                                        ipsv.finishStroke(
                                            localCoordinatesMotionEvent,
                                            currentPointer.motionEventPointerId,
                                            currentPointer.strokeId,
                                        )
                                        change.consume()
                                    } else if (
                                        currentPointer != null &&
                                            change.positionChanged() &&
                                            localCoordinatesMotionEvent != null
                                    ) {
                                        ipsv.addToStroke(
                                            localCoordinatesMotionEvent,
                                            currentPointer.motionEventPointerId,
                                            currentPointer.strokeId,
                                            predictedMotionEvent,
                                        )
                                        change.consume()
                                    }
                                }
                            } finally {
                                localCoordinatesMotionEvent?.recycle()
                                predictedMotionEvent?.recycle()
                            }
                        } while (event.changes.fastAny { it.pressed })
                    }
                }
                // Use MotionEvent/View interop APIs to configure ASAP delivery of input events.
                // Don't add
                // any real touch handling logic here, as the event processing of
                // pointerInteropFilter
                // relative to pointerInput is inconsistent in its order - for example by delivering
                // down
                // events in a different order to siblings than move events, causing ordered event
                // consumption logic to be confusing and brittle.
                .pointerInteropFilter {
                    // Even though this is its own separate touch listener, calling
                    // requestUnbufferedDispatch
                    // here makes all the touch listeners (most importantly, awaitPointerEvent
                    // above) deliver
                    // new input events more frequently than once per frame. This is required to
                    // take full
                    // advantage of low latency rendering.
                    // TODO: b/373662249 - Use Compose API once it exists for unbuffered dispatch.
                    containingView.requestUnbufferedDispatch(it)
                    // It's only necessary to request unbuffered dispatch once per gesture, so
                    // return false
                    // here to not receive more events than necessary.
                    false
                }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                InProgressStrokesView(context).also { ipsv ->
                    ipsv.textureBitmapStore = textureBitmapStore
                    ipsv.eagerInit()
                    inProgressStrokesView = ipsv
                    // Wrap InProgressStrokesView.sync in a lambda that the caller can execute if
                    // desired
                    // from tests.
                    onSyncAvailable { timeout, timeoutUnit -> ipsv.sync(timeout, timeoutUnit) }
                    motionEventPredictor = MotionEventPredictor.newInstance(ipsv)
                }
            },
            update = { ipsv ->
                ipsv.clearFinishedStrokesListeners()
                ipsv.addFinishedStrokesListener(
                    object : InProgressStrokesFinishedListener {
                        override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                            onStrokesFinished(strokes.values.toList())
                            // Must recompose from callback strokes, cannot wait until a later
                            // frame.
                            ipsv.removeFinishedStrokes(strokes.keys)
                        }
                    }
                )
                ipsv.maskPath = maskPath?.asAndroidPath()
            },
        )
    }
}

private class InProgressPointer(
    val strokeId: InProgressStrokeId,
    /**
     * The pointer ID in all [MotionEvent] objects, including the non-local and local real events,
     * and the always-local predicted events.
     */
    val motionEventPointerId: Int,
)

// When there are more than this many active pointers, instances of PointerProperties and
// PointerCoords, and the arrays that they go in, will be allocated with each input and potentially
// make drawing latency higher and less consistent. Many apps only tend to support 1 inking pointer
// at a time as multi-pointer gestures are used for pan/zoom/rotate, but if someone uses more than
// two hands worth of fingers to draw with then they probably aren't noticing subtle latency
// differences. This fallback is both simpler and more performant in the common cases (1 pointer, or
// very few) compared to growing the size of the scratch instances arrays or managing them within a
// list or a map.
private const val MAX_POINTER_COUNT_FOR_SCRATCH_INSTANCES = 10
private val scratchPointerPropertiesArrayByPointerCount =
    Array<Array<MotionEvent.PointerProperties>?>(MAX_POINTER_COUNT_FOR_SCRATCH_INSTANCES) { _ ->
        null
    }
private val scratchPointerCoordsArrayByPointerCount =
    Array<Array<MotionEvent.PointerCoords>?>(MAX_POINTER_COUNT_FOR_SCRATCH_INSTANCES) { _ -> null }

private fun obtainPointerPropertiesArray(pointerCount: Int): Array<MotionEvent.PointerProperties> {
    if (pointerCount > MAX_POINTER_COUNT_FOR_SCRATCH_INSTANCES) {
        return Array(pointerCount) { _ -> MotionEvent.PointerProperties() }
    } else {
        val cacheIndex = pointerCount - 1
        val cached = scratchPointerPropertiesArrayByPointerCount[cacheIndex]
        if (cached != null) return cached
        val newArray = Array(pointerCount) { _ -> MotionEvent.PointerProperties() }
        scratchPointerPropertiesArrayByPointerCount[cacheIndex] = newArray
        return newArray
    }
}

private fun obtainPointerCoordsArray(pointerCount: Int): Array<MotionEvent.PointerCoords> {
    if (pointerCount > MAX_POINTER_COUNT_FOR_SCRATCH_INSTANCES) {
        return Array(pointerCount) { _ -> MotionEvent.PointerCoords() }
    } else {
        val cacheIndex = pointerCount - 1
        val cached = scratchPointerCoordsArrayByPointerCount[cacheIndex]
        if (cached != null) return cached
        val newArray = Array(pointerCount) { _ -> MotionEvent.PointerCoords() }
        scratchPointerCoordsArrayByPointerCount[cacheIndex] = newArray
        return newArray
    }
}

/**
 * Obtains a [MotionEvent] representing the same data as the provided [PointerEvent].
 * [PointerEvent.motionEvent] is similar, but with positions that are in the coordinate space of the
 * [androidx.compose.ui.platform.ComposeView] that may be several layers higher in the composable
 * hierarchy. This function returns a [MotionEvent] with position (x, y) data that is transformed
 * into the coordinate space of the [InProgressStrokes] composable.
 *
 * The result must be recycled with [MotionEvent.recycle] after use. Returns `null` if and only if
 * the [PointerEvent] doesn't have a [PointerEvent.motionEvent] - can be for certain types of
 * cancellation.
 */
private fun PointerEvent.obtainLocalMotionEvent(
    @Suppress("PrimitiveInCollection") // Backed by a Snapshot-compatible collection for correctness
    changeIdsToMotionEventPointerIds: Map<PointerId, Int>
): MotionEvent? {
    val nonLocalMotionEvent = motionEvent ?: return null
    val pointerCount = nonLocalMotionEvent.pointerCount
    val pointerProperties = obtainPointerPropertiesArray(pointerCount)
    val pointerCoords = obtainPointerCoordsArray(pointerCount)

    for (pointerIndex in 0 until pointerCount) {
        // Copy most of the data, including pressure/tilt/orientation, from the nonLocalMotionEvent,
        // but
        // then replace the position data with local positions from PointerInputChange below.
        nonLocalMotionEvent.getPointerProperties(pointerIndex, pointerProperties[pointerIndex])
        nonLocalMotionEvent.getPointerCoords(pointerIndex, pointerCoords[pointerIndex])

        val motionEventPointerId = nonLocalMotionEvent.getPointerId(pointerIndex)
        val change =
            changes.fastFirstOrNull {
                changeIdsToMotionEventPointerIds[it.id] == motionEventPointerId
            }
                // This pointerId is not tracked in changes, so don't localize the position
                // coordinates for
                // this pointer.
                ?: continue
        pointerCoords[pointerIndex].apply {
            x = change.position.x
            y = change.position.y
        }
    }
    // Primary values for each pointer
    val motionEvent =
        MotionEvent.obtain(
            /* downTime = */ nonLocalMotionEvent.downTime,
            /* eventTime = */ nonLocalMotionEvent.eventTime,
            /* action = */ nonLocalMotionEvent.action,
            /* pointerCount = */ pointerCount,
            /* pointerProperties = */ pointerProperties,
            /* pointerCoords = */ pointerCoords,
            /* metaState = */ nonLocalMotionEvent.metaState,
            /* buttonState = */ nonLocalMotionEvent.buttonState,
            /* xPrecision = */ nonLocalMotionEvent.xPrecision,
            /* yPrecision = */ nonLocalMotionEvent.yPrecision,
            /* deviceId = */ nonLocalMotionEvent.deviceId,
            /* edgeFlags = */ nonLocalMotionEvent.edgeFlags,
            /* source = */ nonLocalMotionEvent.source,
            /* flags = */ nonLocalMotionEvent.flags,
        )

    // Check that every pointer has the same historical account and same timestamps. MotionEvent
    // APIs
    // require this, so this PointerEvent created from a MotionEvent should also have it, but
    // PointerEvent APIs replicate some of this data and the APIs don't inherently guarantee that
    // the
    // historical counts and historical timestamps align.
    val mustMatch = changes[0]
    check(!changes.fastAny { it.historical.size != mustMatch.historical.size }) {
        "Some pointers have a different history count than the others."
    }
    val historyCount = mustMatch.historical.size

    for (historicalIndex in 0 until historyCount) {
        val historicalUptimeMs = mustMatch.historical[historicalIndex].uptimeMillis
        // Re-use same array of PointerCoords - copied into native memory during obtain and
        // addBatch.
        for (pointerIndex in 0 until pointerCount) {
            // Copy most of the data, including pressure/tilt/orientation, from the
            // nonLocalMotionEvent,
            // but then replace the position data with local positions from PointerInputChange
            // below.
            nonLocalMotionEvent.getHistoricalPointerCoords(
                pointerIndex,
                historicalIndex,
                pointerCoords[pointerIndex],
            )
            val motionEventPointerId = nonLocalMotionEvent.getPointerId(pointerIndex)
            val change =
                changes.fastFirstOrNull {
                    changeIdsToMotionEventPointerIds[it.id] == motionEventPointerId
                } ?: continue
            val historicalChange = change.historical[historicalIndex]
            check(historicalChange.uptimeMillis == historicalUptimeMs) {
                "Some pointers have different historical timestamps than the others."
            }
            pointerCoords[pointerIndex].apply {
                // The PointerCoords already is full of data from nonLocalMotionEvent, including
                // pressure,
                // tilt, and orientation, so only the position needs to be updated to be in local
                // coordinates.
                x = historicalChange.position.x
                y = historicalChange.position.y
            }
        }

        motionEvent.addBatch(
            /* eventTime = */ historicalUptimeMs,
            /* pointerCoords = */ pointerCoords,
            /* metaState = */ nonLocalMotionEvent.metaState,
        )
    }
    return motionEvent
}

private val IDENTITY_MATRIX = Matrix()
