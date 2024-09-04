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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Receiver scope for [detectTapGestures]'s `onPress` lambda. This offers two methods to allow
 * waiting for the press to be released.
 */
@JvmDefaultWithCompatibility
interface PressGestureScope : Density {
    /**
     * Waits for the press to be released before returning. If the gesture was canceled by motion
     * being consumed by another gesture, [GestureCancellationException] will be thrown.
     */
    suspend fun awaitRelease()

    /**
     * Waits for the press to be released before returning. If the press was released, `true` is
     * returned, or if the gesture was canceled by motion being consumed by another gesture, `false`
     * is returned.
     */
    suspend fun tryAwaitRelease(): Boolean
}

private val NoPressGesture: suspend PressGestureScope.(Offset) -> Unit = {}

/**
 * Detects tap, double-tap, and long press gestures and calls [onTap], [onDoubleTap], and
 * [onLongPress], respectively, when detected. [onPress] is called when the press is detected and
 * the [PressGestureScope.tryAwaitRelease] and [PressGestureScope.awaitRelease] can be used to
 * detect when pointers have released or the gesture was canceled. The first pointer down and final
 * pointer up are consumed, and in the case of long press, all changes after the long press is
 * detected are consumed.
 *
 * Each function parameter receives an [Offset] representing the position relative to the containing
 * element. The [Offset] can be outside the actual bounds of the element itself meaning the numbers
 * can be negative or larger than the element bounds if the touch target is smaller than the
 * [ViewConfiguration.minimumTouchTargetSize].
 *
 * When [onDoubleTap] is provided, the tap gesture is detected only after the
 * [ViewConfiguration.doubleTapMinTimeMillis] has passed and [onDoubleTap] is called if the second
 * tap is started before [ViewConfiguration.doubleTapTimeoutMillis]. If [onDoubleTap] is not
 * provided, then [onTap] is called when the pointer up has been received.
 *
 * After the initial [onPress], if the pointer moves out of the input area, the position change is
 * consumed, or another gesture consumes the down or up events, the gestures are considered
 * canceled. That means [onDoubleTap], [onLongPress], and [onTap] will not be called after a gesture
 * has been canceled.
 *
 * If the first down event is consumed somewhere else, the entire gesture will be skipped, including
 * [onPress].
 */
@OptIn(ExperimentalTapGestureDetectorBehaviorApi::class)
suspend fun PointerInputScope.detectTapGestures(
    onDoubleTap: ((Offset) -> Unit)? = null,
    onLongPress: ((Offset) -> Unit)? = null,
    onPress: suspend PressGestureScope.(Offset) -> Unit = NoPressGesture,
    onTap: ((Offset) -> Unit)? = null
) = coroutineScope {
    // special signal to indicate to the sending side that it shouldn't intercept and consume
    // cancel/up events as we're only require down events
    val pressScope = PressGestureScopeImpl(this@detectTapGestures)
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        var resetJob =
            launch(start = coroutineStartForCurrentDispatchBehavior) { pressScope.reset() }
        // In some cases, coroutine cancellation of the reset job might still be processing when we
        // are already processing an up or cancel pointer event. We need to wait for the reset job
        // to cancel and complete so it can clean up properly (e.g. unlock the underlying mutex)
        suspend fun awaitResetOrSkip() {
            if (DetectTapGesturesEnableNewDispatchingBehavior) {
                resetJob.join()
            }
        }
        if (onPress !== NoPressGesture) launch { pressScope.onPress(down.position) }
        val upOrCancel: PointerInputChange?
        val cancelOrReleaseJob: Job?

        // wait for first tap up or long press
        if (onLongPress == null) {
            upOrCancel = waitForUpOrCancellation()
        } else {
            upOrCancel =
                when (val longPressResult = waitForLongPress()) {
                    LongPressResult.Success -> {
                        onLongPress.invoke(down.position)
                        consumeUntilUp()
                        launch(start = coroutineStartForCurrentDispatchBehavior) {
                            awaitResetOrSkip()
                            pressScope.release()
                        }
                        // End the current gesture
                        return@awaitEachGesture
                    }
                    is LongPressResult.Released -> longPressResult.finalUpChange
                    is LongPressResult.Canceled -> null
                }
        }

        if (upOrCancel == null) {
            cancelOrReleaseJob =
                launch(start = coroutineStartForCurrentDispatchBehavior) {
                    awaitResetOrSkip()
                    // tap-up was canceled
                    pressScope.cancel()
                }
        } else {
            upOrCancel.consume()
            cancelOrReleaseJob =
                launch(start = coroutineStartForCurrentDispatchBehavior) {
                    awaitResetOrSkip()
                    pressScope.release()
                }
        }

        if (upOrCancel != null) {
            // tap was successful.
            if (onDoubleTap == null) {
                onTap?.invoke(upOrCancel.position) // no need to check for double-tap.
            } else {
                // check for second tap
                val secondDown = awaitSecondDown(upOrCancel)

                if (secondDown == null) {
                    onTap?.invoke(upOrCancel.position) // no valid second tap started
                } else {
                    // Second tap down detected
                    resetJob =
                        launch(start = coroutineStartForCurrentDispatchBehavior) {
                            cancelOrReleaseJob.join()
                            pressScope.reset()
                        }
                    if (onPress !== NoPressGesture) {
                        launch { pressScope.onPress(secondDown.position) }
                    }

                    // Might have a long second press as the second tap
                    val secondUp =
                        if (onLongPress == null) {
                            waitForUpOrCancellation()
                        } else {
                            when (val longPressResult = waitForLongPress()) {
                                LongPressResult.Success -> {
                                    // The first tap was valid, but the second tap is a long press -
                                    // we
                                    // intentionally do not invoke onClick() for the first tap,
                                    // since the 'main'
                                    // gesture here is a long press, which canceled the double tap
                                    // / tap.

                                    // notify for the long press
                                    onLongPress.invoke(secondDown.position)
                                    consumeUntilUp()

                                    launch(start = coroutineStartForCurrentDispatchBehavior) {
                                        awaitResetOrSkip()
                                        pressScope.release()
                                    }
                                    return@awaitEachGesture
                                }
                                is LongPressResult.Released -> longPressResult.finalUpChange
                                is LongPressResult.Canceled -> null
                            }
                        }
                    if (secondUp != null) {
                        secondUp.consume()
                        launch(start = coroutineStartForCurrentDispatchBehavior) {
                            awaitResetOrSkip()
                            pressScope.release()
                        }
                        onDoubleTap(secondUp.position)
                    } else {
                        launch(start = coroutineStartForCurrentDispatchBehavior) {
                            awaitResetOrSkip()
                            pressScope.cancel()
                        }
                        onTap?.invoke(upOrCancel.position)
                    }
                }
            }
        }
    }
}

/**
 * Consumes all pointer events until nothing is pressed and then returns. This method assumes that
 * something is currently pressed.
 */
private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.changes.fastForEach { it.consume() }
    } while (event.changes.fastAny { it.pressed })
}

/**
 * Waits for [ViewConfiguration.doubleTapTimeoutMillis] for a second press event. If a second press
 * event is received before the time out, it is returned or `null` is returned if no second press is
 * received.
 */
private suspend fun AwaitPointerEventScope.awaitSecondDown(
    firstUp: PointerInputChange
): PointerInputChange? =
    withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
        val minUptime = firstUp.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
        var change: PointerInputChange
        // The second tap doesn't count if it happens before DoubleTapMinTime of the first tap
        do {
            change = awaitFirstDown()
        } while (change.uptimeMillis < minUptime)
        change
    }

/**
 * Shortcut for cases when we only need to get press/click logic, as for cases without long press
 * and double click we don't require channelling or any other complications.
 *
 * Each function parameter receives an [Offset] representing the position relative to the containing
 * element. The [Offset] can be outside the actual bounds of the element itself meaning the numbers
 * can be negative or larger than the element bounds if the touch target is smaller than the
 * [ViewConfiguration.minimumTouchTargetSize].
 */
@OptIn(ExperimentalTapGestureDetectorBehaviorApi::class)
internal suspend fun PointerInputScope.detectTapAndPress(
    onPress: suspend PressGestureScope.(Offset) -> Unit = NoPressGesture,
    onTap: ((Offset) -> Unit)? = null
) {
    val pressScope = PressGestureScopeImpl(this)
    coroutineScope {
        awaitEachGesture {
            val resetJob =
                launch(start = coroutineStartForCurrentDispatchBehavior) { pressScope.reset() }
            suspend fun awaitResetOrSkip() {
                if (DetectTapGesturesEnableNewDispatchingBehavior) {
                    resetJob.join()
                }
            }

            val down = awaitFirstDown().also { it.consume() }

            if (onPress !== NoPressGesture) {
                launch { pressScope.onPress(down.position) }
            }

            val up = waitForUpOrCancellation()
            if (up == null) {
                launch(start = coroutineStartForCurrentDispatchBehavior) {
                    awaitResetOrSkip()
                    // tap-up was canceled
                    pressScope.cancel()
                }
            } else {
                up.consume()
                launch(start = coroutineStartForCurrentDispatchBehavior) {
                    awaitResetOrSkip()
                    pressScope.release()
                }
                onTap?.invoke(up.position)
            }
        }
    }
}

@Deprecated(
    "Maintained for binary compatibility. Use version with PointerEventPass instead.",
    level = DeprecationLevel.HIDDEN
)
suspend fun AwaitPointerEventScope.awaitFirstDown(
    requireUnconsumed: Boolean = true
): PointerInputChange =
    awaitFirstDown(requireUnconsumed = requireUnconsumed, pass = PointerEventPass.Main)

/**
 * Reads events until the first down is received in the given [pass]. If [requireUnconsumed] is
 * `true` and the first down is already consumed in the pass, that gesture is ignored.
 */
suspend fun AwaitPointerEventScope.awaitFirstDown(
    requireUnconsumed: Boolean = true,
    pass: PointerEventPass = PointerEventPass.Main,
): PointerInputChange {
    var event: PointerEvent
    do {
        event = awaitPointerEvent(pass)
    } while (
        !event.changes.fastAll {
            if (requireUnconsumed) it.changedToDown() else it.changedToDownIgnoreConsumed()
        }
    )
    return event.changes[0]
}

@Deprecated(
    "Maintained for binary compatibility. Use version with PointerEventPass instead.",
    level = DeprecationLevel.HIDDEN
)
suspend fun AwaitPointerEventScope.waitForUpOrCancellation(): PointerInputChange? =
    waitForUpOrCancellation(PointerEventPass.Main)

/**
 * Whether the event is considered a deep press, and should trigger long click before the timeout
 * has been reached.
 */
internal expect val PointerEvent.isDeepPress: Boolean

/**
 * Reads events in the given [pass] until all pointers are up or the gesture was canceled. The
 * gesture is considered canceled when a pointer leaves the event region, a position change has been
 * consumed or a pointer down change event was already consumed in the given pass. If the gesture
 * was not canceled, the final up change is returned or `null` if the event was canceled.
 */
suspend fun AwaitPointerEventScope.waitForUpOrCancellation(
    pass: PointerEventPass = PointerEventPass.Main
): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent(pass)
        if (event.changes.fastAll { it.changedToUp() }) {
            // All pointers are up
            return event.changes[0]
        }

        if (
            event.changes.fastAny { it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding) }
        ) {
            return null // Canceled
        }

        // Check for cancel by position consumption. We can look on the Final pass of the
        // existing pointer event because it comes after the pass we checked above.
        val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
        if (consumeCheck.changes.fastAny { it.isConsumed }) {
            return null
        }
    }
}

/**
 * Reads events in the given [pass] until all pointers are up or the gesture was canceled. The
 * gesture is considered canceled when a pointer leaves the event region, a position change has been
 * consumed or a pointer down change event was already consumed in the given pass. If the gesture
 * was not canceled, the final up change is returned or `null` if the event was canceled.
 */
private suspend fun AwaitPointerEventScope.waitForLongPress(
    pass: PointerEventPass = PointerEventPass.Main
): LongPressResult {
    var result: LongPressResult = LongPressResult.Canceled
    try {
        withTimeout(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val event = awaitPointerEvent(pass)
                if (event.changes.fastAll { it.changedToUp() }) {
                    // All pointers are up
                    result = LongPressResult.Released(event.changes[0])
                    break
                }

                if (event.isDeepPress) {
                    result = LongPressResult.Success
                    break
                }

                if (
                    event.changes.fastAny {
                        it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding)
                    }
                ) {
                    result = LongPressResult.Canceled
                    break
                }

                // Check for cancel by position consumption. We can look on the Final pass of the
                // existing pointer event because it comes after the pass we checked above.
                val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
                if (consumeCheck.changes.fastAny { it.isConsumed }) {
                    result = LongPressResult.Canceled
                    break
                }
            }
        }
    } catch (_: PointerEventTimeoutCancellationException) {
        return LongPressResult.Success
    }
    return result
}

private sealed class LongPressResult {
    /** Long press was triggered */
    object Success : LongPressResult()

    /** All pointers were released without long press being triggered */
    class Released(val finalUpChange: PointerInputChange) : LongPressResult()

    /** The gesture was canceled */
    object Canceled : LongPressResult()
}

@Retention(AnnotationRetention.BINARY)
@RequiresOptIn("This API feature-flags new behavior and will be removed in the future.")
annotation class ExperimentalTapGestureDetectorBehaviorApi

/**
 * Whether to use more immediate coroutine dispatching in [detectTapGestures] and
 * [detectTapAndPress], true by default. This might affect some implicit timing guarantees. Please
 * file a bug if this change is affecting your use case.
 */
// This lint does not translate well to top-level declarations
@get:Suppress("GetterSetterNames")
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@ExperimentalTapGestureDetectorBehaviorApi
@get:ExperimentalTapGestureDetectorBehaviorApi
@set:ExperimentalTapGestureDetectorBehaviorApi
var DetectTapGesturesEnableNewDispatchingBehavior = false

@OptIn(ExperimentalTapGestureDetectorBehaviorApi::class)
private val coroutineStartForCurrentDispatchBehavior
    get() =
        if (DetectTapGesturesEnableNewDispatchingBehavior) {
            CoroutineStart.UNDISPATCHED
        } else {
            CoroutineStart.DEFAULT
        }

/** [detectTapGestures]'s implementation of [PressGestureScope]. */
internal class PressGestureScopeImpl(density: Density) : PressGestureScope, Density by density {
    private var isReleased = false
    private var isCanceled = false
    private val mutex = Mutex(locked = false)

    /** Called when a gesture has been canceled. */
    fun cancel() {
        isCanceled = true
        if (mutex.isLocked) {
            mutex.unlock()
        }
    }

    /** Called when all pointers are up. */
    fun release() {
        isReleased = true
        if (mutex.isLocked) {
            mutex.unlock()
        }
    }

    /** Called when a new gesture has started. */
    suspend fun reset() {
        mutex.lock()
        isReleased = false
        isCanceled = false
    }

    override suspend fun awaitRelease() {
        if (!tryAwaitRelease()) {
            throw GestureCancellationException("The press gesture was canceled.")
        }
    }

    override suspend fun tryAwaitRelease(): Boolean {
        if (!isReleased && !isCanceled) {
            mutex.lock()
            mutex.unlock()
        }
        return isReleased
    }
}
