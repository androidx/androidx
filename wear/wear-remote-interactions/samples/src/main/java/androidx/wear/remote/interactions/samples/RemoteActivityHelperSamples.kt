/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.wear.remote.interactions.samples

import android.content.Intent
import androidx.annotation.Sampled
import androidx.wear.remote.interactions.RemoteActivityHelper

@Sampled
suspend fun RemoteActivityAvailabilitySample(
    remoteActivityHelper: RemoteActivityHelper,
    remoteIntent: Intent
) {
    remoteActivityHelper.availabilityStatus.collect { status ->
        when (status) {
            RemoteActivityHelper.STATUS_UNAVAILABLE ->
                TODO("Hide or present alternative flow as remote flow is not available.")
            RemoteActivityHelper.STATUS_TEMPORARILY_UNAVAILABLE ->
                TODO("Present education to user to connect devices or bring to proximity.")
            RemoteActivityHelper.STATUS_AVAILABLE,
            RemoteActivityHelper.STATUS_UNKNOWN ->
                // Present normal remote device flow when we don't know (old devices)
                // or when we know it is available.
                remoteActivityHelper.startRemoteActivity(remoteIntent)
        }
    }
}
