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

@file:Suppress("RestrictedApiAndroidX") // Referring to drawCircle, drawLine, drawOval

package androidx.wear.compose.remote.material3.previews

import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.compose.embedded.integration.previews.ExperimentalRemoteContentPreview
import androidx.compose.remote.player.compose.embedded.integration.previews.utils.PlayerImpl
import androidx.compose.remote.player.compose.embedded.integration.previews.utils.PlayerImplPreviewParameterProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter

// Individual-feature confirmation previews (tranche: P5 RemoteCanvas draw primitives) for the
// embedded RcPlayer vs the legacy player. Parameterized over PlayerImpl.

private fun fill(argb: Long) = RemotePaint { color = RemoteColor(Color(argb)) }

/** Canvas drawRect: an inset filled rectangle. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcCanvasRectPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Stage {
            RemoteCanvas(modifier = RemoteModifier.size(180.rdp)) {
                drawRect(
                    paint = fill(0xFF3F51B5),
                    topLeft = RemoteOffset(30f.rf, 30f.rf),
                    size = RemoteSize(width - 60f.rf, height - 60f.rf),
                )
            }
        }
    }

/** Canvas drawCircle: a centered filled circle. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcCanvasCirclePreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Stage {
            RemoteCanvas(modifier = RemoteModifier.size(180.rdp)) {
                drawCircle(paint = fill(0xFFE91E63), center = center, radius = 80f.rf)
            }
        }
    }

/** Canvas drawOval: a wide filled oval. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcCanvasOvalPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Stage {
            RemoteCanvas(modifier = RemoteModifier.size(180.rdp)) {
                drawOval(
                    paint = fill(0xFF4CAF50),
                    topLeft = RemoteOffset(20f.rf, 55f.rf),
                    size = RemoteSize(width - 40f.rf, height - 110f.rf),
                )
            }
        }
    }

/** Canvas drawLine: a thick stroked diagonal. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcCanvasLinePreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Stage {
            RemoteCanvas(modifier = RemoteModifier.size(180.rdp)) {
                val stroke = RemotePaint {
                    color = RemoteColor(Color(0xFFFF5722))
                    style = PaintingStyle.Stroke
                    strokeWidth = 14f.rf
                    strokeCap = StrokeCap.Round
                }
                drawLine(
                    paint = stroke,
                    start = RemoteOffset(20f.rf, 20f.rf),
                    end = RemoteOffset(width - 20f.rf, height - 20f.rf),
                )
            }
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
