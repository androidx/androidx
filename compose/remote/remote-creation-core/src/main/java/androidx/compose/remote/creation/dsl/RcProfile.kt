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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.creation.profile.Profile

/**
 * Encapsulates the configuration for a RemoteCompose document.
 *
 * @param profile The underlying [Profile] containing API level and operation bitmasks.
 * @param experimental Whether experimental operations are supported.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcProfile(public val profile: Profile, public val experimental: Boolean = false) {
    /** The platform services implementation associated with this profile. */
    public val platform: RcPlatformServices
        get() = profile.platform
}
