/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.java.layout

import android.app.Activity
import android.content.Context
import android.inputmethodservice.InputMethodService
import androidx.annotation.UiContext
import androidx.core.util.Consumer
import androidx.window.java.core.CallbackToFlowAdapter
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowLayoutInfo
import java.util.concurrent.Executor

/**
 * An adapter for [WindowInfoTracker] that allows listening for [WindowLayoutInfo] updates using
 * callbacks instead of `Flow`.
 *
 * @deprecated This adapter is no longer necessary the callback-based listeners have been added to
 *   WindowLayoutInfo. Use [WindowInfoTracker] directly.
 */
@Deprecated(
    "This adapter is no longer necessary. Use WindowInfoTracker directly.",
    ReplaceWith("WindowInfoTracker", "androidx.window.layout.WindowInfoTracker"),
)
public class WindowInfoTrackerCallbackAdapter
private constructor(
    private val tracker: WindowInfoTracker,
    private val callbackToFlowAdapter: CallbackToFlowAdapter,
) : WindowInfoTracker by tracker {

    /** @deprecated Use [WindowInfoTracker] constructor instead. */
    @Deprecated(
        "This constructor is no longer necessary. Use WindowInfoTracker directly.",
        ReplaceWith("tracker"),
    )
    public constructor(tracker: WindowInfoTracker) : this(tracker, CallbackToFlowAdapter())

    /**
     * Registers a listener to consume [WindowLayoutInfo] values of the [Activity] window from a
     * [Flow]. If the same consumer is registered twice then this method is a no-op.
     *
     * @param activity an [Activity] that hosts a [Window].
     * @param executor that the consumer will invoke on.
     * @param consumer for [WindowLayoutInfo] values.
     * @see WindowInfoTracker.windowLayoutInfo
     * @deprecated Use [WindowInfoTracker.registerWindowLayoutInfoListener] instead.
     */
    @Deprecated(
        "Use WindowInfoTracker.registerWindowLayoutInfoListener(Activity, Executor, Consumer).",
        ReplaceWith("tracker.registerWindowLayoutInfoListener(activity, executor, listener)"),
    )
    public fun addWindowLayoutInfoListener(
        activity: Activity,
        executor: Executor,
        consumer: Consumer<WindowLayoutInfo>,
    ) {
        callbackToFlowAdapter.connect(executor, consumer, tracker.windowLayoutInfo(activity))
    }

    /**
     * Registers a [UiContext] listener to consume [WindowLayoutInfo] values from a [Flow]. If the
     * same consumer is registered twice then this method is a no-op.
     *
     * @param context a [UiContext] such as an [Activity], created with
     *   [Context#createWindowContext] or is a [InputMethodService].
     * @param executor that the consumer will invoke on.
     * @param consumer for [WindowLayoutInfo] values.
     * @see WindowInfoTracker.windowLayoutInfo
     * @deprecated Use [WindowInfoTracker.registerWindowLayoutInfoListener] instead.
     */
    @Deprecated(
        "Use WindowInfoTracker.registerWindowLayoutInfoListener(Context, Executor, Consumer).",
        ReplaceWith("tracker.registerWindowLayoutInfoListener(context, executor, listener)"),
    )
    public fun addWindowLayoutInfoListener(
        @UiContext context: Context,
        executor: Executor,
        consumer: Consumer<WindowLayoutInfo>,
    ) {
        callbackToFlowAdapter.connect(executor, consumer, tracker.windowLayoutInfo(context))
    }

    /**
     * Registers a [UiContext] listener to consume [WindowLayoutInfo] values. If the same listener
     * is registered twice then this method is a no-op. Overridden here for compatibility.
     *
     * @param context a [UiContext] such as an [Activity].
     * @param executor that the listener will invoke on.
     * @param listener for [WindowLayoutInfo] values.
     * @see WindowInfoTracker.windowLayoutInfo
     * @deprecated Use [WindowInfoTracker.registerWindowLayoutInfoListener] instead.
     */
    @Deprecated(
        "Use WindowInfoTracker.registerWindowLayoutInfoListener(Context, Executor, Consumer)."
    )
    public override fun registerWindowLayoutInfoListener(
        @UiContext context: Context,
        executor: Executor,
        listener: Consumer<WindowLayoutInfo>,
    ) {
        tracker.registerWindowLayoutInfoListener(context, executor, listener)
    }

    /**
     * Remove a listener to stop consuming [WindowLayoutInfo] values from a [Flow]. If the listener
     * has already been removed then this is a no-op.
     *
     * @see WindowInfoTracker.windowLayoutInfo
     * @deprecated Use [WindowInfoTracker.unregisterWindowLayoutInfoListener] instead.
     */
    @Deprecated(
        "Use WindowInfoTracker.unregisterWindowLayoutInfoListener(Consumer).",
        ReplaceWith("tracker.unregisterWindowLayoutInfoListener(listener)"),
    )
    public fun removeWindowLayoutInfoListener(consumer: Consumer<WindowLayoutInfo>) {
        callbackToFlowAdapter.disconnect(consumer)
    }

    /**
     * Unregisters a listener to stop consuming [WindowLayoutInfo] values. Overridden here for
     * compatibility.
     *
     * @param listener The listener to be removed.
     * @see WindowInfoTracker.windowLayoutInfo
     * @deprecated Use [WindowInfoTracker.unregisterWindowLayoutInfoListener] instead.
     */
    @Deprecated("Use WindowInfoTracker.unregisterWindowLayoutInfoListener(Consumer).")
    public override fun unregisterWindowLayoutInfoListener(listener: Consumer<WindowLayoutInfo>) {
        tracker.unregisterWindowLayoutInfoListener(listener)
    }
}
