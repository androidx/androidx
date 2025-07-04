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

package androidx.core.telecom.reference.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.telecom.CallEndpointCompat

/**
 * Returns the appropriate [ImageVector] icon based on the type of the [CallEndpointCompat].
 *
 * @param endpoint The call endpoint to get the icon for.
 * @return The corresponding icon, or a default 'QuestionMark' icon for unknown types.
 */
fun getEndpointIcon(endpoint: CallEndpointCompat): ImageVector {
    return when (endpoint.type) {
        CallEndpointCompat.TYPE_SPEAKER -> Icons.Filled.Speaker
        CallEndpointCompat.TYPE_BLUETOOTH -> Icons.Filled.Bluetooth
        CallEndpointCompat.TYPE_WIRED_HEADSET -> Icons.Filled.Headset
        CallEndpointCompat.TYPE_EARPIECE -> Icons.Filled.PhoneInTalk
        else -> {
            Icons.Filled.QuestionMark
        }
    }
}

/**
 * Returns a user-friendly string name based on the type of the [CallEndpointCompat].
 *
 * @param endpoint The call endpoint to get the name for.
 * @return A human-readable string representing the endpoint type (e.g., "Speaker", "Bluetooth").
 */
fun getEndpointName(endpoint: CallEndpointCompat): String {
    return when (endpoint.type) {
        CallEndpointCompat.TYPE_SPEAKER -> "Speaker"
        CallEndpointCompat.TYPE_BLUETOOTH -> "Bluetooth"
        CallEndpointCompat.TYPE_WIRED_HEADSET -> "Headset"
        CallEndpointCompat.TYPE_EARPIECE -> "Earpiece"
        else -> {
            "Unknown"
        }
    }
}

// Helper function to convert endpoint type constants to readable strings
fun endpointTypeToString(type: Int): String {
    return when (type) {
        CallEndpointCompat.TYPE_EARPIECE -> "Earpiece"
        CallEndpointCompat.TYPE_BLUETOOTH -> "Bluetooth"
        CallEndpointCompat.TYPE_WIRED_HEADSET -> "Wired Headset"
        CallEndpointCompat.TYPE_SPEAKER -> "Speakerphone"
        CallEndpointCompat.TYPE_STREAMING -> "External" // Or specific streaming type if known
        else -> "Unknown"
    }
}
