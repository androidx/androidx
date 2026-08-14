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

import androidx.collection.IntSet
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.Profile.SupportedOperationsProvider
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.creation.profile.RemoteComposeWriterFactory

/**
 * Creates a [Profile] configured for Android RemoteCompose creation.
 *
 * This provides a Kotlin-friendly factory for [Profile] with sensible defaults, avoiding the need
 * to manually configure internal platform services and writer implementations.
 *
 * For predefined profiles, see [RcPlatformProfiles], such as [RcPlatformProfiles.ANDROIDX].
 *
 * @param docApiLevel The document API level supported by this profile. Defaults to the latest
 *   document API level ([CoreDocument.DOCUMENT_API_LEVEL]).
 * @param profileFlags The operation profile bitmask (from [RcProfiles]) specifying the profile
 *   category. Defaults to [RcProfiles.PROFILE_ANDROIDX].
 * @param supportedOperations An optional explicit set of supported operation IDs. If specified,
 *   only operations in this set will be enabled in the document buffer. If null, all the operations
 *   defined by [docApiLevel] and [profileFlags] are used.
 * @return A [Profile] configured for Android RemoteCompose creation.
 */
public fun createProfile(
    docApiLevel: Int = CoreDocument.DOCUMENT_API_LEVEL,
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
            docApiLevel,
            profileFlags,
            platform,
            SupportedOperationsProvider { operationsSet },
            factory,
        )
    } else {
        Profile(docApiLevel, profileFlags, platform, factory)
    }
}
