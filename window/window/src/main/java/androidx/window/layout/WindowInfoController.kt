/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.window.layout

import android.content.Context
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.annotation.UiContext

/** An interface to interact with Window properties and layout. */
public abstract class WindowInfoController @RestrictTo(LIBRARY_GROUP) constructor() {

    /**
     * Dispatches a request to modify the visual state of the window.
     *
     * The requested state is evaluated against the task hierarchy by the system. It is
     * automatically cleared when the requesting task is no longer active.
     *
     * Note that this is a request; it is up to the system to determine whether the requested visual
     * state will be respected based on device policies and hardware capabilities. Applications
     * should observe [WindowLayoutInfo.engagementModes] to observe the actual applied state.
     *
     * Note: Throws `IllegalStateException` if not called on the main thread, or if the provided
     * context is not a UI context (e.g. an Application context).
     *
     * On Android 16 (API level 37) and above, this request is forwarded to the system. On lower API
     * levels, calling this method is a no-op.
     *
     * @param context The [Context] used to dispatch the request.
     * @param request The [VisualStateRequest] containing the desired visual state.
     */
    @MainThread
    public abstract fun requestVisualState(@UiContext context: Context, request: VisualStateRequest)

    public companion object {
        /** Gets an instance of [WindowInfoController] for the given [Context]. */
        @JvmStatic
        public fun getInstance(@UiContext context: Context): WindowInfoController {
            return if (Build.VERSION.SDK_INT >= 37) {
                WindowInfoControllerImpl()
            } else {
                EmptyWindowInfoController()
            }
        }
    }
}

/** An empty implementation for API levels below supported minimum. */
private class EmptyWindowInfoController : WindowInfoController() {
    override fun requestVisualState(context: Context, request: VisualStateRequest) {
        // No-op for unsupported API levels
    }
}
