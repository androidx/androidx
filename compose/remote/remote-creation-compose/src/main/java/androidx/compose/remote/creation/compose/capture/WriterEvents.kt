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

package androidx.compose.remote.creation.compose.capture

import android.app.PendingIntent
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi

/**
 * A callback interface used during the capture process to write out the captured composable
 * information. This allows the capture system to be pass on types that can't be serialized into the
 * document such asn PendingIntent.
 *
 * Implementations of this interface will handle the serialization or transformation of the captured
 * composable tree into a desired output format, such as a binary file, a JSON representation, or a
 * network stream.
 */
@ExperimentalRemoteCreationComposeApi
public interface WriterEvents {

    /**
     * Notifies the producer of the document, that a [PendingIntent] was referenced and should be
     * associated with a id, that represents this PendingIntent for the lifetime of the capture
     * session.
     *
     * The id scheme is up to the producer of the document, and typically could be the index in a
     * list.
     *
     * @param pendingIntent The [PendingIntent] to store.
     * @return The id for the host to retrieve the corresponding [PendingIntent].
     */
    public fun storePendingIntent(pendingIntent: PendingIntent): Int = INVALID_ID

    /**
     * Called when an initial or new version of the document is available.
     *
     * @param documentBytes the bytes of the document.
     */
    public fun onDocumentAvailable(documentBytes: ByteArray)

    public companion object {
        /** Sentinel value indicating an invalid or unhandled ID for a stored object. */
        public const val INVALID_ID: Int = -1
    }
}
