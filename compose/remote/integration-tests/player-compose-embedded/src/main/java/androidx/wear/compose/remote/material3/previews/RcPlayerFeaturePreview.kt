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
@file:Suppress("RestrictedApiAndroidX") // Referring to background, verticalGradient

package androidx.wear.compose.remote.material3.previews

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.verticalGradient
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.player.compose.embedded.integration.previews.ExperimentalRemoteContentPreview
import androidx.compose.remote.player.compose.embedded.integration.previews.utils.PlayerImpl
import androidx.compose.remote.player.compose.embedded.integration.previews.utils.PlayerImplPreviewParameterProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter

/**
 * Individual-feature confirmation previews for the embedded [RcPlayer] vs the legacy player.
 *
 * Each preview is parameterized over [PlayerImpl], so it renders twice — once with the legacy
 * `RemoteDocumentPlayer` (Java) and once with the embedded `RcPlayer` (Compose) — making the two
 * directly diffable. Each isolates a single modifier/feature.
 */

/** A box whose background is clipped to uniform 28dp rounded corners. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcRoundedCornersPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Centered {
            RemoteBox(
                modifier =
                    RemoteModifier.size(140.rdp)
                        .clip(RemoteRoundedCornerShape(28.rdp))
                        .background(Color(0xFF3F51B5).rc)
            )
        }
    }

/** A box with four different corner radii (0 / 16 / 40 / 64 dp). */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcPerCornerRoundingPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Centered {
            RemoteBox(
                modifier =
                    RemoteModifier.size(140.rdp)
                        .clip(
                            RemoteRoundedCornerShape(
                                topStart = 0.rdp,
                                topEnd = 16.rdp,
                                bottomEnd = 40.rdp,
                                bottomStart = 64.rdp,
                            )
                        )
                        .background(Color(0xFF4CAF50).rc)
            )
        }
    }

/** A solid background color clipped to a circle (50% rounded corners). */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcBackgroundColorClipPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Centered {
            RemoteBox(
                modifier =
                    RemoteModifier.size(140.rdp)
                        .clip(RemoteRoundedCornerShape(percent = 50))
                        .background(Color(0xFFE91E63).rc)
            )
        }
    }

/** A vertical gradient background clipped to 28dp rounded corners. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcBackgroundGradientClipPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Centered {
            RemoteBox(
                modifier =
                    RemoteModifier.size(140.rdp)
                        .clip(RemoteRoundedCornerShape(28.rdp))
                        .background(
                            RemoteBrush.verticalGradient(
                                colors =
                                    listOf(
                                        RemoteColor(Color(0xFF00E5FF)),
                                        RemoteColor(Color(0xFF2962FF)),
                                    )
                            )
                        )
            )
        }
    }

@Composable
@RemoteComposable
private fun Centered(content: @Composable @RemoteComposable () -> Unit) {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize(),
        contentAlignment = RemoteAlignment.Center,
        content = content,
    )
}
