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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.CreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.PendingIntentAwareWriter
import androidx.compose.remote.creation.compose.capture.RemoteComposeCapture
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.runtime.Composable
import androidx.glance.wear.WearWidgetRawContent
import kotlin.coroutines.resume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

internal object WearWidgetCapture {
    const val PENDING_INTENT_KEY = "pending_intents"

    /**
     * Directly capture a RemoteCompose document and gather the pending intents used in the layout.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("RestrictedApiAndroidX")
    internal suspend fun capture(
        context: Context,
        creationDisplayInfo: CreationDisplayInfo,
        content: @Composable @RemoteComposable () -> Unit,
    ): WearWidgetRawContent = suspendCancellableCoroutine { continuation ->
        val pendingIntents = WidgetPendingIntents()
        RemoteComposeCapture(
            context = context,
            creationDisplayInfo = creationDisplayInfo,
            immediateCapture = true,
            onPaint = { view, writer ->
                if (continuation.isActive) {
                    continuation.resume(
                        WearWidgetRawContent(
                            rcDocument = writer.buffer().copyOfRange(0, writer.bufferSize()),
                            extras = Bundle().addPendingIntents(pendingIntents),
                        )
                    )
                }
                true
            },
            onCaptureReady = @Composable {},
            profile = WearWidgetProfile(pendingIntents),
            content = content,
        )
        continuation.invokeOnCancellation {
            // TODO: can we cancel the capture?
        }
    }

    private fun Bundle.addPendingIntents(widgetPendingIntents: WidgetPendingIntents): Bundle {
        putParcelable(WearWidgetCapture.PENDING_INTENT_KEY, widgetPendingIntents.toBundle())
        return this
    }
}

/** The collected [PendingIntent] on the widget, to be sent as sidecar bundle with the document. */
internal class WidgetPendingIntents {
    private val pendingIntentList: MutableList<PendingIntent> = mutableListOf()

    fun store(pendingIntent: PendingIntent): Int {
        val existingIndex = pendingIntentList.indexOfFirst { it === pendingIntent }
        if (existingIndex != -1) {
            return existingIndex
        }

        pendingIntentList.add(pendingIntent)
        return pendingIntentList.lastIndex
    }

    fun get(index: Int): PendingIntent? = pendingIntentList.getOrNull(index)

    fun size() = pendingIntentList.size

    fun toBundle() =
        Bundle().apply {
            for (i in pendingIntentList.indices) {
                putParcelable("$i", get(i))
            }
        }
}

/**
 * A profile that allows us to store the PendingIntents. The idea being we store the [PendingIntent]
 * in a list and store its index in the list into the RemoteCompose document.
 */
@SuppressLint("RestrictedApiAndroidX")
internal class WearWidgetProfile(widgetPendingIntents: WidgetPendingIntents) :
    Profile(
        CoreDocument.DOCUMENT_API_LEVEL,
        RcProfiles.PROFILE_WEAR_WIDGETS,
        AndroidxRcPlatformServices(),
        { width, height, contentDescription, profile ->
            WearWidgetRemoteComposeWriter(
                widgetPendingIntents,
                width,
                height,
                contentDescription,
                apiLevel = CoreDocument.DOCUMENT_API_LEVEL,
                profiles = profile.operationsProfiles,
                platform = profile.platform,
            )
        },
    )

/**
 * A [RemoteComposeWriter] that stores [PendingIntent]s into the provided [WidgetPendingIntents],
 * and returns its stored index for the host to retrieve the corresponding [PendingIntent]
 */
@SuppressLint("RestrictedApiAndroidX")
internal class WearWidgetRemoteComposeWriter(
    val widgetPendingIntents: WidgetPendingIntents,
    width: Int,
    height: Int,
    contentDescription: String,
    apiLevel: Int,
    profiles: Int,
    platform: RcPlatformServices,
) :
    RemoteComposeWriter(width, height, contentDescription, apiLevel, profiles, platform),
    PendingIntentAwareWriter {

    override fun storePendingIntent(pendingIntent: PendingIntent) =
        widgetPendingIntents.store(pendingIntent)
}
