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

@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.integration.demos.bookends

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.embedded.rememberRcPlayerState
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteAppScaffold
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteButton
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteIcon
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteScreenScaffold
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteText
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteTransformingLazyColumn
import androidx.wear.compose.remote.integration.demos.bookends.player.WearMaterial3Plugins

/**
 * The remote document: written against the bookends Material3-shaped API, which emits `Custom`
 * components rather than drawing them.
 */
@RemoteComposable
@Composable
fun BookendsDemoContent() {
    val clicks = rememberMutableRemoteInt(0)
    val increment = valueChange(clicks, clicks + 1)
    val icon = remember { RemoteImageBitmap(bookendsIconBitmap()) }

    RemoteAppScaffold {
        RemoteScreenScaffold {
            RemoteTransformingLazyColumn {
                item { RemoteText("Bookends".rs, textAlign = TextAlign.Center) }
                item {
                    RemoteButton(
                        onClick = increment,
                        secondaryLabel = {
                            RemoteText("Clicks: ".rs + clicks.toRemoteString(), maxLines = 1)
                        },
                        icon = { RemoteIcon(bitmap = icon, contentDescription = null) },
                    ) {
                        RemoteText("Tap me".rs)
                    }
                }
                items(5) { index ->
                    RemoteButton(onClick = increment) { RemoteText("Item $index".rs) }
                }
                item {
                    RemoteButton(onClick = increment, enabled = false.rb) {
                        RemoteText("Disabled".rs)
                    }
                }
            }
        }
    }
}

/** `Custom` layouts are only available in the experimental operations profile. */
internal val BookendsProfile: Profile =
    Profile(
        RcPlatformProfiles.ANDROIDX.apiLevel,
        RcPlatformProfiles.ANDROIDX.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
        RcPlatformProfiles.ANDROIDX.platform,
        RcPlatformProfiles.ANDROIDX.profileFactory,
    )

/**
 * The host: plays the captured document with the embedded player, rendering the `Custom` components
 * with Wear Compose Material3 via [WearMaterial3Plugins].
 */
@OptIn(ExperimentalRemotePlayerApi::class)
@Composable
fun BookendsDemo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(Unit) {
        bytes =
            captureSingleRemoteDocument(context = context, profile = BookendsProfile) {
                    BookendsDemoContent()
                }
                .bytes
    }

    val currentBytes = bytes ?: return
    RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true
    val document = remember(currentBytes) { RemoteDocument(currentBytes).document }
    RcPlayer(
        state = rememberRcPlayerState(document),
        modifier = modifier.fillMaxSize(),
        customPlugins = WearMaterial3Plugins,
    )
}

/**
 * Hosts [BookendsDemo] full screen, outside of the demo app's own `AppScaffold`, since the document
 * provides its own `AppScaffold` and `TimeText`.
 */
class BookendsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { BookendsDemo() } }
    }
}
