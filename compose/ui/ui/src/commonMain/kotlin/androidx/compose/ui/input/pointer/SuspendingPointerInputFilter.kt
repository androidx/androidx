/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.fastMapNotNull
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.synchronized
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job

/**
 * Receiver scope for awaiting pointer events in a call to
 * [PointerInputScope.awaitPointerEventScope].
 *
 * This is a restricted suspension scope. Code in this scope is always called undispatched and
 * may only suspend for calls to [awaitPointerEvent]. These functions
 * resume synchronously and the caller may mutate the result **before** the next await call to
 * affect the next stage of the input processing pipeline.
 */
@RestrictsSuspension
@JvmDefaultWithCompatibility
interface AwaitPointerEventScope : Density {
    /**
     * The measured size of the pointer input region. Input events will be reported with
     * a coordinate space of (0, 0) to (size.width, size,height) as the input region, with
     * (0, 0) indicating the upper left corner.
     */
    val size: IntSize

    /*
     * The additional space applied to each side of the layout area. This can be
     * non-[zero][Size.Zero] when `minimumTouchTargetSize` is set in [pointerInput].
     */
    val extendedTouchPadding: Size
        get() = Size.Zero

    /**
     * The [PointerEvent] from the most recent touch event.
     */
    val currentEvent: PointerEvent

    /**
     * The [ViewConfiguration] used to tune gesture detectors.
     */
    val viewConfiguration: ViewConfiguration

    /**
     * Suspend until a [PointerEvent] is reported to the specified input [pass].
     * [pass] defaults to [PointerEventPass.Main].
     *
     * [awaitPointerEvent] resumes **synchronously** in the restricted suspension scope. This
     * means that callers can react immediately to input after [awaitPointerEvent] returns
     * and affect both the current frame and the next handler or phase of the input processing
     * pipeline. Callers should mutate the returned [PointerEvent] before awaiting
     * another event to consume aspects of the event before the next stage of input processing runs.
     */
    suspend fun awaitPointerEvent(
        pass: PointerEventPass = PointerEventPass.Main
    ): PointerEvent

    /**
     * Runs [block] and returns the result of [block] or `null` if [timeMillis] has passed
     * before [timeMillis].
     */
    suspend fun <T> withTimeoutOrNull(
        timeMillis: Long,
        block: suspend AwaitPointerEventScope.() -> T
    ): T? = block()

    /**
     * Runs [block] and returns its results. An [PointerEventTimeoutCancellationException] is thrown
     * if [timeMillis] has passed before [block] completes.
     */
    suspend fun <T> withTimeout(
        timeMillis: Long,
        block: suspend AwaitPointerEventScope.() -> T
    ): T = block()
}

/**
 * Receiver scope for [Modifier.pointerInput] that permits
 * [handling pointer input][awaitPointerEventScope].
 */
// Design note: this interface does _not_ implement CoroutineScope, even though doing so
// would more easily permit the use of launch {} inside Modifier.pointerInput {} blocks without
// requiring an additional coroutineScope {} layer of nesting. As it is encouraged to define
// gesture detectors as suspending extensions with a PointerInputScope receiver, also making this
// interface implement CoroutineScope would be an invitation to break structured concurrency in
// these extensions, leaving other launched coroutines running in the calling scope.
@JvmDefaultWithCompatibility
interface PointerInputScope : Density {
    /**
     * The measured size of the pointer input region. Input events will be reported with
     * a coordinate space of (0, 0) to (size.width, size,height) as the input region, with
     * (0, 0) indicating the upper left corner.
     */
    val size: IntSize

    /**
     * The additional space applied to each side of the layout area when the layout is smaller
     * than [ViewConfiguration.minimumTouchTargetSize].
     */
    val extendedTouchPadding: Size
        get() = Size.Zero

    /**
     * The [ViewConfiguration] used to tune gesture detectors.
     */
    val viewConfiguration: ViewConfiguration

    /**
     * Intercept pointer input that children receive even if the pointer is out of bounds.
     *
     * If `true`, and a child has been moved out of this layout and receives an event, this
     * will receive that event. If `false`, a child receiving pointer input outside of the
     * bounds of this layout will not trigger any events in this.
     */
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    var interceptOutOfBoundsChildEvents: Boolean
        get() = false
        set(_) {}

    /**
     * Suspend and install a pointer input [block] that can await input events and respond to
     * them immediately. A call to [awaitPointerEventScope] will resume with [block]'s result after
     * it completes.
     *
     * More than one [awaitPointerEventScope] can run concurrently in the same [PointerInputScope]
     * by using [kotlinx.coroutines.launch]. [block]s are dispatched to in the order in which they
     * were installed.
     */
    suspend fun <R> awaitPointerEventScope(
        block: suspend AwaitPointerEventScope.() -> R
    ): R
}

private const val PointerInputModifierNoParamError =
    "Modifier.pointerInput must provide one or more 'key' parameters that define the identity of " +
        "the modifier and determine when its previous input processing coroutine should be " +
        "cancelled and a new effect launched for the new key."

/**
 * Create a modifier for processing pointer input within the region of the modified element.
 *
 * It is an error to call [pointerInput] without at least one `key` parameter.
 */
// This deprecated-error function shadows the varargs overload so that the varargs version
// is not used without key parameters.
@Suppress(
    "DeprecatedCallableAddReplaceWith",
    "UNUSED_PARAMETER",
    "unused",
    "ModifierFactoryUnreferencedReceiver"
)
@Deprecated(PointerInputModifierNoParamError, level = DeprecationLevel.ERROR)
fun Modifier.pointerInput(
    block: suspend PointerInputScope.() -> Unit
): Modifier = error(PointerInputModifierNoParamError)

/**
 * Create a modifier for processing pointer input within the region of the modified element.
 *
 * [pointerInput] [block]s may call [PointerInputScope.awaitPointerEventScope] to install a pointer
 * input handler that can [AwaitPointerEventScope.awaitPointerEvent] to receive and consume
 * pointer input events. Extension functions on [PointerInputScope] or [AwaitPointerEventScope]
 * may be defined to perform higher-level gesture detection. The pointer input handling [block]
 * will be cancelled and **re-started** when [pointerInput] is recomposed with a different [key1].
 *
 * When a [pointerInput] modifier is created by composition, if [block] captures any local
 * variables to operate on, two patterns are common for working with changes to those variables
 * depending on the desired behavior.
 *
 * Specifying the captured value as a [key][key1] parameter will cause [block] to cancel
 * and restart from the beginning if the value changes:
 *
 * @sample androidx.compose.ui.samples.keyedPointerInputModifier
 *
 * If [block] should **not** restart when a captured value is changed but the value should still
 * be updated for its next use, use
 * [rememberUpdatedState][androidx.compose.runtime.rememberUpdatedState] to update a value holder
 * that is accessed by [block]:
 *
 * @sample androidx.compose.ui.samples.rememberedUpdatedParameterPointerInputModifier
 */
fun Modifier.pointerInput(
    key1: Any?,
    block: suspend PointerInputScope.() -> Unit
): Modifier = this then SuspendPointerInputModifierNodeElement(
    key1 = key1,
    pointerInputHandler = block
)

/**
 * Create a modifier for processing pointer input within the region of the modified element.
 *
 * [pointerInput] [block]s may call [PointerInputScope.awaitPointerEventScope] to install a pointer
 * input handler that can [AwaitPointerEventScope.awaitPointerEvent] to receive and consume
 * pointer input events. Extension functions on [PointerInputScope] or [AwaitPointerEventScope]
 * may be defined to perform higher-level gesture detection. The pointer input handling [block]
 * will be cancelled and **re-started** when [pointerInput] is recomposed with a different [key1] or
 * [key2].
 *
 * When a [pointerInput] modifier is created by composition, if [block] captures any local
 * variables to operate on, two patterns are common for working with changes to those variables
 * depending on the desired behavior.
 *
 * Specifying the captured value as a [key][key1] parameter will cause [block] to cancel
 * and restart from the beginning if the value changes:
 *
 * @sample androidx.compose.ui.samples.keyedPointerInputModifier
 *
 * If [block] should **not** restart when a captured value is changed but the value should still
 * be updated for its next use, use
 * [rememberUpdatedState][androidx.compose.runtime.rememberUpdatedState] to update a value holder
 * that is accessed by [block]:
 *
 * @sample androidx.compose.ui.samples.rememberedUpdatedParameterPointerInputModifier
 */
fun Modifier.pointerInput(
    key1: Any?,
    key2: Any?,
    block: suspend PointerInputScope.() -> Unit
): Modifier = this then SuspendPointerInputModifierNodeElement(
    key1 = key1,
    key2 = key2,
    pointerInputHandler = block
)

/**
 * Create a modifier for processing pointer input within the region of the modified element.
 *
 * [pointerInput] [block]s may call [PointerInputScope.awaitPointerEventScope] to install a pointer
 * input handler that can [AwaitPointerEventScope.awaitPointerEvent] to receive and consume
 * pointer input events. Extension functions on [PointerInputScope] or [AwaitPointerEventScope]
 * may be defined to perform higher-level gesture detection. The pointer input handling [block]
 * will be cancelled and **re-started** when [pointerInput] is recomposed with any different [keys].
 *
 * When a [pointerInput] modifier is created by composition, if [block] captures any local
 * variables to operate on, two patterns are common for working with changes to those variables
 * depending on the desired behavior.
 *
 * Specifying the captured value as a [key][keys] parameter will cause [block] to cancel
 * and restart from the beginning if the value changes:
 *
 * @sample androidx.compose.ui.samples.keyedPointerInputModifier
 *
 * If [block] should **not** restart when a captured value is changed but the value should still
 * be updated for its next use, use
 * [rememberUpdatedState][androidx.compose.runtime.rememberUpdatedState] to update a value holder
 * that is accessed by [block]:
 *
 * @sample androidx.compose.ui.samples.rememberedUpdatedParameterPointerInputModifier
 */
fun Modifier.pointerInput(
    vararg keys: Any?,
    block: suspend PointerInputScope.() -> Unit
): Modifier = this then SuspendPointerInputModifierNodeElement(
    keys = keys,
    pointerInputHandler = block
)

internal class SuspendPointerInputModifierNodeElement(
    val key1: Any? = null,
    val key2: Any? = null,
    val keys: Array<out Any?>? = null,
    val pointerInputHandler: suspend PointerInputScope.() -> Unit
) : ModifierNodeElement<SuspendingPointerInputModifierNodeImpl>() {
    override fun InspectorInfo.inspectableProperties() {
        name = "pointerInput"
        properties["key1"] = key1
        properties["key2"] = key2
        properties["keys"] = keys
        properties["pointerInputHandler"] = pointerInputHandler
    }

    override fun create(): SuspendingPointerInputModifierNodeImpl {
        return SuspendingPointerInputModifierNodeImpl(pointerInputHandler)
    }

    override fun update(node: SuspendingPointerInputModifierNodeImpl) {
        node.pointerInputHandler = pointerInputHandler
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SuspendPointerInputModifierNodeElement) return false

        if (key1 != other.key1) return false
        if (key2 != other.key2) return false
        if (keys != null) {
            if (other.keys == null) return false
            if (!keys.contentEquals(other.keys)) return false
        } else if (other.keys != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key1?.hashCode() ?: 0
        result = 31 * result + (key2?.hashCode() ?: 0)
        result = 31 * result + (keys?.contentHashCode() ?: 0)
        return result
    }
}

private val EmptyPointerEvent = PointerEvent(emptyList())

/**
 * Supports suspending pointer event handling. This is used by [pointerInput], so in most cases you
 * should just use [pointerInput] for suspending pointer input. Creating a
 * [SuspendingPointerInputModifierNode] should only be needed when you want to delegate to
 * suspending pointer input as part of the implementation of a complex [Modifier.Node].
 */
fun SuspendingPointerInputModifierNode(
    pointerInputHandler: suspend PointerInputScope.() -> Unit
): SuspendingPointerInputModifierNode {
    return SuspendingPointerInputModifierNodeImpl(pointerInputHandler)
}

/**
 * Extends [PointerInputModifierNode] with a handler to execute asynchronously when an event occurs
 * and a function to reset that handler (cancels the existing coroutine and essentially resets the
 * handler's execution).
 * Note: The handler still executes lazily, meaning nothing will be done until a new event comes in.
 */
sealed interface SuspendingPointerInputModifierNode : PointerInputModifierNode {
    /**
     * Handler for pointer input events. When changed, any previously executing pointerInputHandler
     * will be canceled.
     */
    var pointerInputHandler: suspend PointerInputScope.() -> Unit

    /**
     * Resets the underlying coroutine used to run the handler for input pointer events. This
     * should be called whenever a large change has been made that forces the gesture detection to
     * be completely invalid.
     *
     * For example, if [pointerInputHandler] has different modes for detecting a gesture
     * (long press, double click, etc.), and by switching the modes, any currently-running gestures
     * are no longer valid.
     */
    fun resetPointerInputHandler()
}

/**
 * Implementation notes:
 * This class does a lot of lifting. [PointerInputModifierNode] receives, interprets, and, consumes
 * [PointerInputChange]s while the state (and the coroutineScope used to execute
 * [pointerInputHandler]) is retained in [Modifier.Node].
 *
 * [SuspendingPointerInputModifierNodeImpl] implements the [PointerInputScope] used to offer the
 * [Modifier.pointerInput] DSL and provides the [Density] from [LocalDensity] lazily from the
 * layout node when it is needed.
 *
 * Note: The coroutine that executes the passed pointer event handler is launched lazily when the
 * first event is fired (making it more efficient) and is cancelled via resetPointerInputHandler().
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class SuspendingPointerInputModifierNodeImpl(
    pointerInputHandler: suspend PointerInputScope.() -> Unit
) : Modifier.Node(),
    SuspendingPointerInputModifierNode,
    PointerInputScope,
    Density {

    override var pointerInputHandler = pointerInputHandler
        set(value) {
            resetPointerInputHandler()
            field = value
        }

    override val density: Float
        get() = requireLayoutNode().density.density

    override val fontScale: Float
        get() = requireLayoutNode().density.fontScale

    override val viewConfiguration
        get() = requireLayoutNode().viewConfiguration

    override val size: IntSize
        get() = boundsSize

    // The handler for pointer input events is now executed lazily when the first event fires.
    // This job indicates that pointer input handler job is running.
    private var pointerInputJob: Job? = null

    private var currentEvent: PointerEvent = EmptyPointerEvent

    /**
     * Actively registered input handlers from currently ongoing calls to [awaitPointerEventScope].
     * Must use `synchronized(pointerHandlers)` to access.
     */
    private val pointerHandlers =
        mutableVectorOf<SuspendingPointerInputModifierNodeImpl.PointerEventHandlerCoroutine<*>>()

    /**
     * Scratch list for dispatching to handlers for a particular phase.
     * Used to hold a copy of the contents of [pointerHandlers] during dispatch so that
     * resumed continuations may add/remove handlers without affecting the current dispatch pass.
     * Must only access on the UI thread.
     */
    private val dispatchingPointerHandlers =
        mutableVectorOf<SuspendingPointerInputModifierNodeImpl.PointerEventHandlerCoroutine<*>>()

    /**
     * The last pointer event we saw where at least one pointer was currently down; null otherwise.
     * Used to synthesize a fake "all pointers changed to up/all changes to down-state consumed"
     * event for propagating cancellation. This synthetic event corresponds to Android's
     * `MotionEvent.ACTION_CANCEL`.
     */
    private var lastPointerEvent: PointerEvent? = null

    /**
     * The size of the bounds of this input filter. Normally [PointerInputFilter.size] can
     * be used, but for tests, it is better to not rely on something set to an `internal`
     * method.
     */
    private var boundsSize: IntSize = IntSize.Zero

    override val extendedTouchPadding: Size
        get() {
            val minimumTouchTargetSize = viewConfiguration.minimumTouchTargetSize.toSize()
            val size = size
            val horizontal = max(0f, minimumTouchTargetSize.width - size.width) / 2f
            val vertical = max(0f, minimumTouchTargetSize.height - size.height) / 2f
            return Size(horizontal, vertical)
        }

    override var interceptOutOfBoundsChildEvents: Boolean = false

    override fun onDetach() {
        resetPointerInputHandler()
        super.onDetach()
    }

    // The handler for incoming pointer input events needs to be reset if the density changes.
    override fun onDensityChange() {
        resetPointerInputHandler()
    }

    // The handler for incoming pointer input events needs to be reset if the view configuration
    // changes.
    override fun onViewConfigurationChange() {
        resetPointerInputHandler()
    }

    /**
     * This cancels the existing coroutine and essentially resets pointerInputHandler's execution.
     * Note, the pointerInputHandler still executes lazily, meaning nothing will be done again
     * until a new event comes in.
     * More details: This is triggered from a LayoutNode if the Density or ViewConfiguration change
     * (in an older implementation using composed, these values were used as keys so it would reset
     * everything when either change, we do that manually now through this function). It is also
     * used for testing.
     */
    override fun resetPointerInputHandler() {
        val localJob = pointerInputJob
        if (localJob != null) {
            localJob.cancel(CancellationException())
            pointerInputJob = null
        }
    }

    /**
     * Snapshot the current [pointerHandlers] and run [block] on each one.
     * May not be called reentrant or concurrent with itself.
     *
     * Dispatches from first to last registered for [PointerEventPass.Initial] and
     * [PointerEventPass.Final]; dispatches from last to first for [PointerEventPass.Main].
     * This corresponds to the down/up/down dispatch behavior of each of these passes along
     * the hit test path through the Compose UI layout hierarchy.
     */
    private inline fun forEachCurrentPointerHandler(
        pass: PointerEventPass,
        block: (SuspendingPointerInputModifierNodeImpl.PointerEventHandlerCoroutine<*>) -> Unit
    ) {
        // Copy handlers to avoid mutating the collection during dispatch
        synchronized(pointerHandlers) {
            dispatchingPointerHandlers.addAll(pointerHandlers)
        }
        try {
            when (pass) {
                PointerEventPass.Initial, PointerEventPass.Final ->
                    dispatchingPointerHandlers.forEach(block)

                PointerEventPass.Main ->
                    dispatchingPointerHandlers.forEachReversed(block)
            }
        } finally {
            dispatchingPointerHandlers.clear()
        }
    }

    /**
     * Dispatch [pointerEvent] for [pass] to all [pointerHandlers] currently registered when
     * the call begins.
     */
    private fun dispatchPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass
    ) {
        forEachCurrentPointerHandler(pass) {
            it.offerPointerEvent(pointerEvent, pass)
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        boundsSize = bounds
        if (pass == PointerEventPass.Initial) {
            currentEvent = pointerEvent
        }

        // Coroutine lazily launches when first event comes in.
        if (pointerInputJob == null) {
            // 'start = CoroutineStart.UNDISPATCHED' required so handler doesn't miss first event.
            pointerInputJob = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                pointerInputHandler()
            }
        }

        dispatchPointerEvent(pointerEvent, pass)

        lastPointerEvent = pointerEvent.takeIf { event ->
            !event.changes.fastAll { it.changedToUpIgnoreConsumed() }
        }
    }

    override fun onCancelPointerInput() {
        // Synthesize a cancel event for whatever state we previously saw, if one is applicable.
        // A cancel event is one where all previously down pointers are now up, the change in
        // down-ness is consumed. Any pointers that were previously hovering are left unchanged.
        val lastEvent = lastPointerEvent ?: return

        if (lastEvent.changes.fastAll { !it.pressed }) {
            return // There aren't any pressed pointers, so we don't need to send any events.
        }
        val newChanges = lastEvent.changes.fastMapNotNull { old ->
            PointerInputChange(
                id = old.id,
                position = old.position,
                uptimeMillis = old.uptimeMillis,
                pressed = false,
                pressure = old.pressure,
                previousPosition = old.position,
                previousUptimeMillis = old.uptimeMillis,
                previousPressed = old.pressed,
                isInitiallyConsumed = old.pressed
            )
        }

        val cancelEvent = PointerEvent(newChanges)

        currentEvent = cancelEvent
        // Dispatch the synthetic cancel for all three passes
        dispatchPointerEvent(cancelEvent, PointerEventPass.Initial)
        dispatchPointerEvent(cancelEvent, PointerEventPass.Main)
        dispatchPointerEvent(cancelEvent, PointerEventPass.Final)

        lastPointerEvent = null

        // Cancels existing coroutine (Job) handling events.
        resetPointerInputHandler()
    }

    override suspend fun <R> awaitPointerEventScope(
        block: suspend AwaitPointerEventScope.() -> R
    ): R = suspendCancellableCoroutine { continuation ->
        val handlerCoroutine = PointerEventHandlerCoroutine(continuation)
        synchronized(pointerHandlers) {
            pointerHandlers += handlerCoroutine

            // NOTE: We resume the new continuation while holding this lock.
            // We do this since it runs in a RestrictsSuspension scope and therefore
            // will only suspend when awaiting a new event. We don't release this
            // synchronized lock until we know it has an awaiter and any future dispatch
            // would succeed.

            // We also create the coroutine with both a receiver and a completion continuation
            // of the handlerCoroutine itself; we don't use our currently available suspended
            // continuation as the resume point because handlerCoroutine needs to remove the
            // ContinuationInterceptor from the supplied CoroutineContext to have undispatched
            // behavior in our restricted suspension scope. This is required so that we can
            // process event-awaits synchronously and affect the next stage in the pipeline
            // without running too late due to dispatch.
            block.createCoroutine(handlerCoroutine, handlerCoroutine).resume(Unit)
        }

        // Restricted suspension handler coroutines can't propagate structured job cancellation
        // automatically as the context must be EmptyCoroutineContext; do it manually instead.
        continuation.invokeOnCancellation { handlerCoroutine.cancel(it) }
    }

    /**
     * Implementation of the inner coroutine created to run a single call to
     * [awaitPointerEventScope].
     *
     * [PointerEventHandlerCoroutine] implements [AwaitPointerEventScope] to provide the
     * input handler DSL, and [Continuation] so that it can wrap [completion] and remove the
     * [ContinuationInterceptor] from the calling context and run undispatched.
     */
    private inner class PointerEventHandlerCoroutine<R>(
        private val completion: Continuation<R>,
    ) : AwaitPointerEventScope,
        Density by this@SuspendingPointerInputModifierNodeImpl,
        Continuation<R> {

        private var pointerAwaiter: CancellableContinuation<PointerEvent>? = null
        private var awaitPass: PointerEventPass = PointerEventPass.Main

        override val currentEvent: PointerEvent
            get() = this@SuspendingPointerInputModifierNodeImpl.currentEvent
        override val size: IntSize
            get() = this@SuspendingPointerInputModifierNodeImpl.boundsSize
        override val viewConfiguration: ViewConfiguration
            get() = this@SuspendingPointerInputModifierNodeImpl.viewConfiguration
        override val extendedTouchPadding: Size
            get() = this@SuspendingPointerInputModifierNodeImpl.extendedTouchPadding

        fun offerPointerEvent(event: PointerEvent, pass: PointerEventPass) {
            if (pass == awaitPass) {
                pointerAwaiter?.run {
                    pointerAwaiter = null
                    resume(event)
                }
            }
        }

        // Called to run any finally blocks in the awaitPointerEventScope block
        fun cancel(cause: Throwable?) {
            pointerAwaiter?.cancel(cause)
            pointerAwaiter = null
        }

        // context must be EmptyCoroutineContext for restricted suspension coroutines
        override val context: CoroutineContext = EmptyCoroutineContext

        // Implementation of Continuation; clean up and resume our wrapped continuation.
        override fun resumeWith(result: Result<R>) {
            synchronized(pointerHandlers) {
                pointerHandlers -= this
            }
            completion.resumeWith(result)
        }

        override suspend fun awaitPointerEvent(
            pass: PointerEventPass
        ): PointerEvent = suspendCancellableCoroutine { continuation ->
            awaitPass = pass
            pointerAwaiter = continuation
        }

        override suspend fun <T> withTimeoutOrNull(
            timeMillis: Long,
            block: suspend AwaitPointerEventScope.() -> T
        ): T? {
            return try {
                withTimeout(timeMillis, block)
            } catch (_: PointerEventTimeoutCancellationException) {
                null
            }
        }

        override suspend fun <T> withTimeout(
            timeMillis: Long,
            block: suspend AwaitPointerEventScope.() -> T
        ): T {
            if (timeMillis <= 0L) {
                pointerAwaiter?.resumeWithException(
                    PointerEventTimeoutCancellationException(timeMillis)
                )
            }

            val job = coroutineScope.launch {
                // Delay twice because the timeout continuation needs to be lower-priority than
                // input events, not treated fairly in FIFO order. The second
                // micro-delay reposts it to the back of the queue, after any input events
                // that were posted but not processed during the first delay.
                delay(timeMillis - 1)
                delay(1)

                pointerAwaiter?.resumeWithException(
                    PointerEventTimeoutCancellationException(timeMillis)
                )
            }
            try {
                return block()
            } finally {
                job.cancel()
            }
        }
    }
}

/**
 * An exception thrown from [AwaitPointerEventScope.withTimeout] when the execution time
 * of the coroutine is too long.
 */
class PointerEventTimeoutCancellationException(
    time: Long
) : CancellationException("Timed out waiting for $time ms")
