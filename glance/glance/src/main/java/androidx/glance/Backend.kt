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

import androidx.annotation.RestrictTo

/**
 * Represents what backend is in use during composition/translation.
 *
 * TODO: unrestrict in followup cl
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class Backend {
    RemoteView,
    RemoteCompose,
}

/**
 * Represents what backend a developer's AppWidget would like to use
 *
 * TODO: revisit the concept of BackendPreference and Backend. b/461555982
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // TODO: unrestrict in next CL
@JvmInline
public value class GlanceBackendPreference(public val value: Int) {
    public companion object {
        public val RemoteViews: GlanceBackendPreference = GlanceBackendPreference(0)
        public val RemoteCompose: GlanceBackendPreference = GlanceBackendPreference(1)
        public val Default: GlanceBackendPreference = GlanceBackendPreference(2)
    }
}
