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

package androidx.wear.compose.remote.material3.previews

import androidx.compose.remote.core.operations.layout.animation.AnimationSpec
import androidx.compose.remote.core.operations.utilities.easing.GeneralEasing
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteStateLayout
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.alpha
import androidx.compose.remote.creation.compose.modifier.animationSpec
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.offset
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.embedded.integration.previews.ExperimentalRemoteContentPreview
import androidx.compose.remote.player.compose.embedded.integration.previews.utils.PlayerImpl
import androidx.compose.remote.player.compose.embedded.integration.previews.utils.PlayerImplPreviewParameterProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter

// Individual-feature confirmation previews (tranche: P0 layout/sizing/text + P1
// border/offset/alpha)
// for the embedded RcPlayer vs the View player. Each is parameterized over PlayerImpl, so it
// renders once per player (Java / Compose) for direct diffing.

private const val BLUE = 0xFF3F51B5
private const val AMBER = 0xFFFFC107
private const val DARK = 0xFF263238
private const val RED = 0xFFF44336
private const val GREEN = 0xFF4CAF50
private const val BLUEISH = 0xFF2196F3

/** padding: a 24dp inset between the blue frame and the amber fill. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcPaddingPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Frame {
            RemoteBox(
                modifier = RemoteModifier.size(160.rdp).background(Color(BLUE).rc).padding(24.rdp)
            ) {
                RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color(AMBER).rc))
            }
        }
    }

/** Box contentAlignment = TopStart. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcBoxAlignTopStartPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Frame {
            RemoteBox(
                modifier = RemoteModifier.size(170.rdp).background(Color(DARK).rc),
                contentAlignment = RemoteAlignment.TopStart,
            ) {
                RemoteBox(modifier = RemoteModifier.size(56.rdp).background(Color(RED).rc))
            }
        }
    }

/** Box contentAlignment = BottomEnd. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcBoxAlignBottomEndPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Frame {
            RemoteBox(
                modifier = RemoteModifier.size(170.rdp).background(Color(DARK).rc),
                contentAlignment = RemoteAlignment.BottomEnd,
            ) {
                RemoteBox(modifier = RemoteModifier.size(56.rdp).background(Color(RED).rc))
            }
        }
    }

/** Column with spacedBy(14dp) arrangement. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcColumnSpacedPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Frame {
            RemoteColumn(verticalArrangement = RemoteArrangement.spacedBy(14.rdp)) {
                RemoteBox(RemoteModifier.size(120.rdp, 28.rdp).background(Color(RED).rc))
                RemoteBox(RemoteModifier.size(120.rdp, 28.rdp).background(Color(GREEN).rc))
                RemoteBox(RemoteModifier.size(120.rdp, 28.rdp).background(Color(BLUEISH).rc))
            }
        }
    }

/** Row with SpaceBetween arrangement across a fixed width. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcRowSpaceBetweenPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Frame {
            RemoteRow(
                modifier = RemoteModifier.size(190.rdp, 44.rdp),
                horizontalArrangement = RemoteArrangement.SpaceBetween,
            ) {
                RemoteBox(RemoteModifier.size(44.rdp).background(Color(RED).rc))
                RemoteBox(RemoteModifier.size(44.rdp).background(Color(GREEN).rc))
                RemoteBox(RemoteModifier.size(44.rdp).background(Color(BLUEISH).rc))
            }
        }
    }

/** Column with CenterHorizontally alignment of different-width children. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcColumnCenterAlignPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Frame {
            RemoteColumn(
                modifier = RemoteModifier.size(180.rdp, 150.rdp),
                verticalArrangement = RemoteArrangement.spacedBy(12.rdp),
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
            ) {
                RemoteBox(RemoteModifier.size(60.rdp, 30.rdp).background(Color(RED).rc))
                RemoteBox(RemoteModifier.size(140.rdp, 30.rdp).background(Color(GREEN).rc))
                RemoteBox(RemoteModifier.size(30.rdp, 30.rdp).background(Color(BLUEISH).rc))
            }
        }
    }

/** RemoteText styling: size, color, italic, bold. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcTextStyledPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Frame {
            RemoteText(
                text = "Styled".rs,
                color = Color(0xFF6200EE).rc,
                fontSize = 30.rsp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
            )
        }
    }

/** border: 6dp teal border on a rounded box. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcBorderPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Frame {
            RemoteBox(
                modifier =
                    RemoteModifier.size(140.rdp)
                        .clip(RemoteRoundedCornerShape(20.rdp))
                        .background(Color(0xFFEEEEEE).rc)
                        .border(
                            width = 6.rdp,
                            color = Color(0xFF009688).rc,
                            shape = RemoteRoundedCornerShape(20.rdp),
                        )
            )
        }
    }

/** offset: shift a box by (28dp, 18dp). */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcOffsetPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Frame {
            RemoteBox(
                modifier =
                    RemoteModifier.offset(x = 28.rdp, y = 18.rdp)
                        .size(96.rdp)
                        .background(Color(0xFFFF5722).rc)
            )
        }
    }

/** alpha: a black box at 0.4 alpha over white reads as grey. */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcAlphaPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Frame {
            RemoteBox(
                modifier =
                    RemoteModifier.size(120.rdp).alpha(0.4f.rf).background(Color(0xFF000000).rc)
            )
        }
    }

/** StateLayout shared element preview with compact card state. */
@Preview(showBackground = true, widthDp = 240, heightDp = 120)
@Composable
private fun RcStateLayoutSharedElementPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        val isExpanded = rememberMutableRemoteBoolean(false)
        Frame {
            RemoteBox(
                modifier =
                    RemoteModifier.clip(RemoteRoundedCornerShape(12.rdp))
                        .background(Color(0xFF1E1E2C).rc)
                        .clickable(action = valueChange(isExpanded, !isExpanded))
                        .padding(12.rdp)
            ) {
                RemoteStateLayout(currentState = isExpanded) { expanded ->
                    if (!expanded) {
                        RemoteRow(
                            verticalAlignment = RemoteAlignment.CenterVertically,
                            horizontalArrangement = RemoteArrangement.spacedBy(10.rdp),
                        ) {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 1,
                                            motionDuration = 400f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 400f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .size(36.rdp)
                                        .clip(RemoteRoundedCornerShape(18.rdp))
                                        .background(Color(0xFF6750A4).rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(text = "★".rs, fontSize = 16.rsp)
                            }
                            RemoteColumn(
                                modifier =
                                    RemoteModifier.animationSpec(
                                        animationId = 2,
                                        motionDuration = 400f,
                                        motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                        visibilityDuration = 400f,
                                        visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                        enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                        exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                    )
                            ) {
                                RemoteText(
                                    text = "Featured Card".rs,
                                    color = Color.White.rc,
                                    fontSize = 14.rsp,
                                )
                                RemoteText(
                                    text = "Tap to expand".rs,
                                    color = Color.Gray.rc,
                                    fontSize = 11.rsp,
                                )
                            }
                        }
                    } else {
                        RemoteColumn(
                            modifier = RemoteModifier.size(180.rdp),
                            verticalArrangement = RemoteArrangement.spacedBy(8.rdp),
                        ) {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 1,
                                            motionDuration = 400f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 400f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .size(width = 180.rdp, height = 70.rdp)
                                        .clip(RemoteRoundedCornerShape(8.rdp))
                                        .background(Color(0xFF6750A4).rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(text = "★ Hero View".rs, fontSize = 18.rsp)
                            }
                            RemoteColumn(
                                modifier =
                                    RemoteModifier.animationSpec(
                                        animationId = 2,
                                        motionDuration = 400f,
                                        motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                        visibilityDuration = 400f,
                                        visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                        enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                        exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                    )
                            ) {
                                RemoteText(
                                    text = "Featured Card (Expanded)".rs,
                                    color = Color.White.rc,
                                    fontSize = 14.rsp,
                                )
                                RemoteText(
                                    text = "Tap to collapse".rs,
                                    color = Color.Gray.rc,
                                    fontSize = 11.rsp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

/**
 * FitBox shared element transition preview: displays responsive layout adaptation across
 * constraints with shared element animation between alternatives.
 */
@Preview(showBackground = true, widthDp = 220, heightDp = 220)
@Composable
private fun RcFitBoxSharedElementsPreview(
    @PreviewParameter(PlayerImplPreviewParameterProvider::class) playerImpl: PlayerImpl
) =
    ExperimentalRemoteContentPreview(playerImpl = playerImpl) {
        Frame {
            RemoteBox(
                modifier =
                    RemoteModifier.clip(RemoteRoundedCornerShape(16.rdp))
                        .background(Color(0xFF1E1E2C).rc)
                        .padding(12.rdp)
            ) {
                RemoteFitBox(
                    modifier =
                        RemoteModifier.animationSpec(
                            animationId = 100,
                            motionDuration = 400f,
                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                            visibilityDuration = 400f,
                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                        )
                ) {
                    // Wide alternative (fits >= 170dp)
                    RemoteBox(modifier = RemoteModifier.width(170.rdp).height(56.rdp)) {
                        RemoteRow(
                            verticalAlignment = RemoteAlignment.CenterVertically,
                            horizontalArrangement = RemoteArrangement.spacedBy(10.rdp),
                        ) {
                            RemoteBox(
                                modifier =
                                    RemoteModifier.animationSpec(
                                            animationId = 1,
                                            motionDuration = 400f,
                                            motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                            visibilityDuration = 400f,
                                            visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                            enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                            exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                        )
                                        .size(36.rdp)
                                        .clip(RemoteRoundedCornerShape(18.rdp))
                                        .background(Color(0xFF7C4DFF).rc),
                                contentAlignment = RemoteAlignment.Center,
                            ) {
                                RemoteText(text = "🎵".rs, fontSize = 16.rsp)
                            }
                            RemoteColumn(
                                modifier =
                                    RemoteModifier.animationSpec(
                                        animationId = 2,
                                        motionDuration = 400f,
                                        motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                        visibilityDuration = 400f,
                                        visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                        enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                        exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                    )
                            ) {
                                RemoteText(
                                    text = "FitBox Player".rs,
                                    color = Color.White.rc,
                                    fontSize = 13.rsp,
                                )
                                RemoteText(
                                    text = "Responsive".rs,
                                    color = Color.Gray.rc,
                                    fontSize = 10.rsp,
                                )
                            }
                        }
                    }

                    // Compact alternative
                    RemoteBox(modifier = RemoteModifier.width(80.rdp).height(50.rdp)) {
                        RemoteBox(
                            modifier =
                                RemoteModifier.animationSpec(
                                        animationId = 1,
                                        motionDuration = 400f,
                                        motionEasingType = GeneralEasing.CUBIC_STANDARD,
                                        visibilityDuration = 400f,
                                        visibilityEasingType = GeneralEasing.CUBIC_STANDARD,
                                        enterAnimation = AnimationSpec.ANIMATION.FADE_IN,
                                        exitAnimation = AnimationSpec.ANIMATION.FADE_OUT,
                                    )
                                    .size(44.rdp)
                                    .clip(RemoteRoundedCornerShape(22.rdp))
                                    .background(Color(0xFF7C4DFF).rc),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            RemoteText(text = "🎵".rs, fontSize = 18.rsp)
                        }
                    }
                }
            }
        }
    }

@Composable
@RemoteComposable
private fun Frame(content: @Composable @RemoteComposable () -> Unit) {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize(),
        contentAlignment = RemoteAlignment.Center,
        content = content,
    )
}
