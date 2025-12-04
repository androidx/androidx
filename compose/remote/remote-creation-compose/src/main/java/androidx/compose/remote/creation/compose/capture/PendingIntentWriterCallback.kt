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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.capture

import android.app.PendingIntent
import androidx.annotation.RestrictTo

/**
 * A [WriterCallback] that can store [PendingIntent]s.
 *
 * This is used during the serialization of a Composable function to handle `PendingIntent` objects,
 * which cannot be directly serialized. Implementations of this interface are responsible for
 * storing the `PendingIntent` and providing a reference (an integer index) that can be used later
 * by the host to retrieve the original `PendingIntent` object.
 *
 * @see WriterCallback
 * @see storePendingIntent
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PendingIntentWriterCallback : WriterCallback {
    /**
     * Stores a [PendingIntent].
     *
     * @param pendingIntent The [PendingIntent] to store.
     * @return The index for the host to retrieve the corresponding [PendingIntent].
     */
    public fun storePendingIntent(pendingIntent: PendingIntent): Int
}
