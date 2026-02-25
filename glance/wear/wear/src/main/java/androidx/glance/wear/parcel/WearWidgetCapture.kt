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

package androidx.glance.wear.parcel

import android.content.Context
import android.os.Bundle
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.CapturedDocument
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import androidx.glance.wear.core.WearWidgetRawContent

internal object WearWidgetCapture {
    const val PENDING_INTENT_KEY = "pending_intents"

    /**
     * Directly capture a RemoteCompose document and gather the pending intents used in the layout.
     */
    internal suspend fun capture(
        context: Context,
        creationDisplayInfo: CreationDisplayInfo,
        content: @Composable @RemoteComposable () -> Unit,
    ): WearWidgetRawContent {
        val remoteDocument =
            captureSingleRemoteDocument(
                context = context,
                creationDisplayInfo = creationDisplayInfo,
                profile = RcPlatformProfiles.WEAR_WIDGETS,
                content = content,
            )
        return WearWidgetRawContent(
            rcDocument = remoteDocument.bytes,
            extras = remoteDocument.toBundle(),
        )
    }

    fun CapturedDocument.toBundle(): Bundle {
        return Bundle().addPendingIntentsFrom(this)
    }

    private fun Bundle.addPendingIntentsFrom(capturedDocument: CapturedDocument): Bundle {
        putParcelable(
            WearWidgetCapture.PENDING_INTENT_KEY,
            capturedDocument.toPendingIntentBundle(),
        )
        return this
    }

    private fun CapturedDocument.toPendingIntentBundle(): Bundle {
        return Bundle().apply {
            pendingIntents.forEach { key, pendingIntent -> putParcelable("$key", pendingIntent) }
        }
    }
}
