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

package androidx.compose.remote.creation.compose.widgets

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles.PROFILE_WIDGETS
import androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.DisplayPool
import androidx.compose.remote.creation.compose.capture.RemoteComposeCapture
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.LayoutDirection
import java.io.ByteArrayInputStream

/** Helper to capture a document */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeHost(
    public val context: Context,
    public val apiLevel: Int = 6,
    public val profiles: Int = PROFILE_WIDGETS,
    public val onReadyCallback: (CoreDocument?) -> Unit,
    public val cRun: @Composable () -> Unit,
) {
    public var remoteComposeDocument: RemoteDocument? = null
    public var created: Boolean = false
    public var document: CoreDocument? = null

    public fun getDoc(): RemoteDocument? {
        if (remoteComposeDocument != null) {
            return remoteComposeDocument!!
        }
        if (!created) {
            val start = System.nanoTime()
            created = true
            rememberRemoteDocument(context, cRun)
        }
        if (document != null && document is CoreDocument) {
            remoteComposeDocument = RemoteDocument(document!!)
        }
        return remoteComposeDocument
    }

    private fun rememberRemoteDocument(baseContext: Context, content: @Composable () -> Unit) {
        rememberRemoteDocument(baseContext, apiLevel, profiles, content)
    }

    private fun rememberRemoteDocument(
        baseContext: Context,
        apiLevel: Int,
        profiles: Int,
        content: @Composable () -> Unit,
    ) {
        val connection = CreationDisplayInfo(1000, 1000, 440)
        val virtualDisplay = DisplayPool.allocate(context, connection)
        RemoteComposeCapture(
            context = baseContext,
            virtualDisplay = virtualDisplay,
            creationDisplayInfo = connection,
            layoutDirection = toLayoutDirection(context.resources.configuration.layoutDirection),
            immediateCapture = true,
            onPaint = { view, writer ->
                if (document == null) {
                    val buffer = writer.buffer()
                    val bufferSize = writer.bufferSize()
                    val inputStream = ByteArrayInputStream(buffer, 0, bufferSize)
                    val coreDocument = CoreDocument()
                    val rcBuffer = fromInputStream(inputStream)
                    coreDocument.initFromBuffer(rcBuffer)
                    document = coreDocument
                    DisplayPool.release(virtualDisplay)
                    onReadyCallback(document)
                }
                true
            },
            onCaptureReady = @Composable {},
            apiLevel = apiLevel,
            profiles = profiles,
            content = content,
        )
    }
}

/** Convert an Android layout direction to a compose [layout direction][LayoutDirection]. */
internal fun toLayoutDirection(androidLayoutDirection: Int): LayoutDirection {
    return when (androidLayoutDirection) {
        android.util.LayoutDirection.LTR -> LayoutDirection.Ltr
        android.util.LayoutDirection.RTL -> LayoutDirection.Rtl
        else -> throw IllegalArgumentException("Unknown layout direction: $androidLayoutDirection")
    }
}
