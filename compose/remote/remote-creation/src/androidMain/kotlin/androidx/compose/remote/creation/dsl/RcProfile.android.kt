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

package androidx.compose.remote.creation.dsl

import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.Profile.SupportedOperationsProvider
import androidx.compose.remote.creation.profile.RemoteComposeWriterFactory

internal actual fun createPlatformProfile(
    docApiLevel: Int,
    profileFlags: Int,
    supportedOperations: IntArray?,
): Profile {
    val platform = AndroidxRcPlatformServices()
    val factory = RemoteComposeWriterFactory { displayInfo, profile, callback ->
        RemoteComposeWriterAndroid(displayInfo, null, profile, callback)
    }
    return if (supportedOperations != null) {
        @Suppress("PrimitiveInCollection") val ops = HashSet<Int>(supportedOperations.size)
        for (op in supportedOperations) {
            ops.add(op)
        }
        Profile(
            docApiLevel,
            profileFlags,
            platform,
            SupportedOperationsProvider { ops },
            factory,
        )
    } else {
        Profile(docApiLevel, profileFlags, platform, factory)
    }
}
