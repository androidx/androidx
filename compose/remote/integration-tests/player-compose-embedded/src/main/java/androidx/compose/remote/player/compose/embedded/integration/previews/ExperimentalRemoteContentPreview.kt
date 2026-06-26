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

@file:SuppressLint("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded.integration.previews

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.embedded.integration.previews.utils.PlayerImpl
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.tooling.preview.RemoteContentPreview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.runBlocking

/**
 * Displays a Remote Compose Composable in the Android Studio Preview. Dispatches to either the
 * production legacy player or the experimental embedded Compose player.
 */
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
public fun ExperimentalRemoteContentPreview(
    modifier: Modifier = Modifier,
    profile: Profile = RcPlatformProfiles.ANDROIDX,
    playerImpl: PlayerImpl = PlayerImpl.JAVA,
    content: @RemoteComposable @Composable () -> Unit,
) {
    when (playerImpl) {
        PlayerImpl.JAVA -> {
            RemoteContentPreview(modifier = modifier, profile = profile, content = content)
        }
        PlayerImpl.COMPOSE -> {
            val context = LocalContext.current
            val remoteDocument = remember {
                runBlocking {
                    val bytes =
                        captureSingleRemoteDocument(
                                context = context,
                                profile = profile,
                                content = content,
                            )
                            .bytes
                    RemoteDocument(bytes)
                }
            }
            LaunchedEffect(Unit) {}
            Box(modifier = modifier) { RcPlayer(document = remoteDocument.document) }
        }
    }
}
