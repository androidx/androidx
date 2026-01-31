/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.glance

import android.os.Build
import android.os.Bundle
import androidx.annotation.RestrictTo

/** Represents what backend is in use during composition/translation. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class Backend {
    RemoteView,
    RemoteCompose,
}

/**
 * Debug flags to force Glance to use either the RemoteView or RemoteCompose backends. Pass in the
 * appWidgetOptions bundle to force glance to use either remote views or remote compose. This is
 * intended for debugging, eg, to see if a widget displays the same using both options.
 *
 * Set to true to force remote views.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public const val GLANCE_OPTION_APPWIDGET_FORCE_BACKEND: String =
    "androidx.glance.appwidget.forceBackend"
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val BACKEND_REMOTE_VIEW: Int = 0
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public const val BACKEND_REMOTE_COMPOSE: Int = 1

/**
 * @return True if >= api 36 && the host is not forcing remote views.
 *
 *     @return the backend override, if set, otherwise null.
 *
 * TODO: b/462177167 Right now we call this twice. Can we call this once and have it be a constant
 *   throughout the session?
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun getBackendOverride(hostOptions: Bundle?): Backend? {
    val isApiSufficientForRemoteCompose = Build.VERSION.SDK_INT >= 36

    val noSelection = -1
    val hostOverrideOrdinal: Int =
        hostOptions?.getInt(GLANCE_OPTION_APPWIDGET_FORCE_BACKEND, -1) ?: noSelection
    val backendOverride: Backend? =
        when (hostOverrideOrdinal) {
            BACKEND_REMOTE_VIEW -> Backend.RemoteView
            BACKEND_REMOTE_COMPOSE -> Backend.RemoteCompose
            else -> null
        }

    return when {
        backendOverride == Backend.RemoteCompose && isApiSufficientForRemoteCompose ->
            Backend.RemoteCompose
        backendOverride == Backend.RemoteView -> Backend.RemoteView
        else -> null
    }
}
