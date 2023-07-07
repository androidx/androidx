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
import androidx.annotation.RestrictTo
import androidx.annotation.UiContext
import androidx.core.util.Consumer
import androidx.window.layout.WindowLayoutInfo
import java.util.concurrent.Executor

/**
 * Backing interface for [androidx.window.layout.WindowInfoTracker] instances that serve as the
 * default information supplier.
 */
internal interface WindowBackend {

    /**
     * Registers a callback for layout changes of the window for the supplied [UiContext].
     * Must be called only after the it is attached to the window.
     * The supplied [UiContext] should correspond to a window or an area on the screen. It must be
     * either an [Activity] or a [UiContext] created with [Context#createWindowContext].
     */
    fun registerLayoutChangeCallback(
        @UiContext context: Context,
        executor: Executor,
        callback: Consumer<WindowLayoutInfo>
    )

    /**
     * Unregisters a callback for window layout changes.
     */
    fun unregisterLayoutChangeCallback(callback: Consumer<WindowLayoutInfo>)

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun hasRegisteredListeners(): Boolean {
        return false
    }
}
