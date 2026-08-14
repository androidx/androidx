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

package androidx.compose.remote.creation.compose.capture

import androidx.annotation.RestrictTo
import androidx.collection.IntSet
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.Profile.SupportedOperationsProvider
import androidx.compose.remote.creation.profile.RemoteComposeWriterFactory

/**
 * Creates a custom [Profile] configured for Android RemoteCompose creation.
 *
 * This provides a Kotlin-friendly factory for [Profile] with sensible defaults, avoiding the need
 * to manually configure internal platform services and writer implementations.
 *
 * @param apiLevel The document API level supported by this profile. Defaults to
 *   [CoreDocument.DOCUMENT_API_LEVEL].
 * @param profileFlags The operation profile bitmask (from [RcProfiles]) specifying the profile
 *   category. Defaults to [RcProfiles.PROFILE_ANDROIDX].
 * @param supportedOperations An optional explicit set of supported operation IDs ([IntSet]). If
 *   specified, only operations in this set will be enabled in the document buffer. If null, the
 *   operations defined by [apiLevel] and [profileFlags] are used.
 * @return A [Profile] configured for Android RemoteCompose creation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun createCustomProfile(
    apiLevel: Int = CoreDocument.DOCUMENT_API_LEVEL,
    profileFlags: Int = RcProfiles.PROFILE_ANDROIDX,
    supportedOperations: IntSet? = null,
): Profile {
    val platform = AndroidxRcPlatformServices()
    val factory = RemoteComposeWriterFactory { creationDisplayInfo, profile, callback ->
        RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
    }
    return if (supportedOperations != null) {
        @Suppress("PrimitiveInCollection")
        val operationsSet =
            HashSet<Int>(supportedOperations.size).apply { supportedOperations.forEach { add(it) } }
        Profile(
            apiLevel,
            profileFlags,
            platform,
            SupportedOperationsProvider { operationsSet },
            factory,
        )
    } else {
        Profile(apiLevel, profileFlags, platform, factory)
    }
}
