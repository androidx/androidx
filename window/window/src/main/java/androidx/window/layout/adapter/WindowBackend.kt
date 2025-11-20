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
package androidx.window.layout.adapter

import android.app.Activity
import android.content.Context
import android.inputmethodservice.InputMethodService
import androidx.annotation.RestrictTo
import androidx.annotation.UiContext
import androidx.core.util.Consumer
import androidx.window.RequiresWindowSdkExtension
import androidx.window.layout.SupportedPosture
import androidx.window.layout.WindowLayoutInfo
import java.util.concurrent.Executor

/**
 * Backing interface for [androidx.window.layout.WindowInfoTracker] instances that serve as the
 * default information supplier.
 */
internal interface WindowBackend {

    /**
     * Registers a callback for layout changes of the window for the supplied [UiContext]. Must be
     * called only after the it is attached to the window. The supplied [UiContext] should
     * correspond to a window or an area on the screen. It must be either an [Activity] or a
     * [UiContext] created with [Context#createWindowContext].
     *
     * @throws IllegalArgumentException when [context] is not an [UiContext].
     */
    fun registerLayoutChangeCallback(
        @UiContext context: Context,
        executor: Executor,
        callback: Consumer<WindowLayoutInfo>,
    )

    /** Unregisters a callback for window layout changes. */
    fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>)

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun hasRegisteredListeners(): Boolean {
        return false
    }

    /**
     * Returns a [List] of [SupportedPosture] for the device.
     *
     * @throws UnsupportedOperationException if the Window SDK version is less than 6.
     */
    @RequiresWindowSdkExtension(version = 6)
    @get:RequiresWindowSdkExtension(version = 6)
    val supportedPostures: List<SupportedPosture>

    /**
     * Returns the current [WindowLayoutInfo] for the given [Context].
     *
     * This API provides a convenient way to access the current [WindowLayoutInfo] without
     * registering a listener via [registerLayoutChangeCallback]. It simplifies the retrieval of
     * [WindowLayoutInfo] in scenarios like [Activity.onCreate].
     *
     * @param context a [Context] that corresponds to a window or an area on the screen. This can be
     *   an [Activity], a [Context] created with [Context.createWindowContext], or an
     *   [InputMethodService].
     * @return the current [WindowLayoutInfo] for the given [Context].
     * @throws UnsupportedOperationException if the Window SDK extension version is less than 9.
     * @throws IllegalArgumentException when [context] is not an [UiContext].
     */
    @RequiresWindowSdkExtension(version = 9)
    fun getCurrentWindowLayoutInfo(@UiContext context: Context): WindowLayoutInfo
}
