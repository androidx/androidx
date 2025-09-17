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

package androidx.compose.remote.player.compose.test.util

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.platform.AndroidxPlatformServices
import androidx.compose.remote.player.view.RemoteComposeDocument
import java.io.ByteArrayInputStream
import kotlin.apply

fun getCoreDocument(
    extraTags: Array<RemoteComposeWriter.HTag> = emptyArray(),
    content: RemoteComposeContextAndroid.() -> Unit,
): CoreDocument {
    val tags =
        arrayOf(
            RemoteComposeWriter.HTag(Header.DOC_CONTENT_DESCRIPTION, "Test"),
            RemoteComposeWriter.HTag(Header.DOC_DESIRED_FPS, 120),
        ) + extraTags
    val rcContext =
        RemoteComposeContextAndroid(AndroidxPlatformServices(), *tags) { apply(content) }
    return RemoteComposeDocument(
            ByteArrayInputStream(rcContext.mRemoteWriter.buffer(), 0, rcContext.bufferSize())
        )
        .document
}
