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

@file:Suppress(
    "RestrictedApiAndroidX"
) // Referring to background, horizontalGradient, remote-material3

package androidx.wear.compose.remote.material3.previews

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.offset
import androidx.compose.remote.creation.compose.modifier.rotate
import androidx.compose.remote.creation.compose.modifier.scale
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.zIndex
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.horizontalGradient
import androidx.compose.remote.creation.compose.shaders.radialGradient
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.embedded.integration.previews.ExperimentalRemoteContentPreview
import androidx.compose.remote.player.compose.embedded.integration.previews.utils.PlayerImpl
import androidx.compose.remote.player.compose.embedded.integration.previews.utils.PlayerImplPreviewParameterProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.wear.compose.remote.material3.RemoteIcon
import androidx.wear.compose.remote.material3.previews.utils.TestImageVectors

// Individual-feature confirmation previews (tranche: P2 transforms/draw) for the embedded RcPlayer
// vs the legacy player. Parameterized over PlayerImpl.

/** graphicsLayer rotate: a square rotated 30 degrees. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcRotatePreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Stage {
            RemoteBox(
                modifier = RemoteModifier.rotate(30f.rf).size(120.rdp).background(Color(0xFF3F51B5))
            )
        }
    }

/** graphicsLayer scale: a square scaled to 0.6. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcScalePreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Stage {
            RemoteBox(
                modifier = RemoteModifier.scale(0.6f.rf).size(150.rdp).background(Color(0xFF4CAF50))
            )
        }
    }

/** zIndex: the red box is declared first but raised above the later blue box. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcZIndexPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Stage {
            RemoteBox(modifier = RemoteModifier.size(180.rdp)) {
                RemoteBox(
                    modifier =
                        RemoteModifier.offset(20.rdp, 20.rdp)
                            .size(110.rdp)
                            .zIndex(1f.rf)
                            .background(Color(0xFFF44336))
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.offset(60.rdp, 60.rdp)
                            .size(110.rdp)
                            .background(Color(0xFF2196F3))
                )
            }
        }
    }

/** horizontal gradient background, clipped to rounded corners. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcGradientHorizontalPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Stage {
            RemoteBox(
                modifier =
                    RemoteModifier.size(150.rdp)
                        .clip(RemoteRoundedCornerShape(24.rdp))
                        .background(
                            RemoteBrush.horizontalGradient(
                                colors =
                                    listOf(
                                        RemoteColor(Color(0xFFFFEB3B)),
                                        RemoteColor(Color(0xFFFF5722)),
                                    )
                            )
                        )
            )
        }
    }

/** radial gradient background. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcGradientRadialPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Stage {
            RemoteBox(
                modifier =
                    RemoteModifier.size(150.rdp)
                        .clip(RemoteRoundedCornerShape(24.rdp))
                        .background(
                            RemoteBrush.radialGradient(
                                0f.rf to RemoteColor(Color(0xFFFFFFFF)),
                                1f.rf to RemoteColor(Color(0xFF3F51B5)),
                            )
                        )
            )
        }
    }

/** RemoteIcon (vector rasterized through the image layout), tinted. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcIconPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Stage {
            RemoteIcon(
                imageVector = TestImageVectors.VolumeUp,
                contentDescription = null,
                modifier = RemoteModifier.size(120.rdp),
                tint = RemoteColor(Color(0xFF009688)),
            )
        }
    }

@Composable
@RemoteComposable
private fun Stage(content: @Composable @RemoteComposable () -> Unit) {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize(),
        contentAlignment = RemoteAlignment.Center,
        content = content,
    )
}
