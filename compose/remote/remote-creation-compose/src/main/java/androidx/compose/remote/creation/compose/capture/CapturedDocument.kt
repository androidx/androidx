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
import androidx.collection.IntObjectMap

/**
 * Represents the result of a remote Composable capture operation.
 *
 * This class encapsulates all the necessary data to reconstruct and display a RemoteCompose
 * document. It contains the serialized view hierarchy as a byte array and a map of any associated
 * `PendingIntent`s that need to be resolved on the host side.
 *
 * @property bytes The bytes of the Remote Compose document.
 * @property pendingIntents A map of integer identifiers to `PendingIntent` objects.
 */
public class CapturedDocument
internal constructor(
    public val bytes: ByteArray,
    public val pendingIntents: IntObjectMap<PendingIntent>,
)
