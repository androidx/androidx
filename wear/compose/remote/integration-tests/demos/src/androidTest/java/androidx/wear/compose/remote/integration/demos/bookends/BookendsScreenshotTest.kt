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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.stateDescription
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.player.compose.EnableEmbeddedPlayerRule
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.remote.testing.RemoteBaseContentTestRule.Player
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.test.core.app.ApplicationProvider
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.remote.integration.demos.bookends.material3.BookendsImplementation
import androidx.wear.compose.remote.integration.demos.bookends.material3.LocalBookendsImplementation
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteButton
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteIcon
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteSwitchButton
import androidx.wear.compose.remote.integration.demos.bookends.material3.RemoteText
import androidx.wear.compose.remote.integration.demos.bookends.player.WearMaterial3Plugins
import androidx.wear.compose.remote.material3.RemoteMaterialTheme
import androidx.wear.compose.remote.material3.RemoteTypography
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule

internal const val SCREENSHOT_GOLDEN_DIRECTORY = "wear/compose/remote/integration-tests/demos"

/**
 * Screenshot tests for the bookends components that have a remote-material3 equivalent, run once
 * per [BookendsImplementation] (see the subclasses) so the two sets of goldens can be compared.
 *
 * Both sets are played with the embedded player, which renders the `Custom` components with Wear
 * Compose Material3 via [WearMaterial3Plugins]. `TransformingLazyColumn`, the scaffolds and the
 * scroll indicator have no remote-material3 equivalent and are covered by
 * [WearMaterial3BookendsScreenshotTest] only.
 */
abstract class BookendsScreenshotTest(private val implementation: BookendsImplementation) {
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            remoteCreationDisplayInfo =
                createCreationDisplayInfo(
                    ApplicationProvider.getApplicationContext(),
                    Size(500f, 500f),
                ),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    @get:Rule
    val rule: TestRule =
        RuleChain.outerRule(EnableEmbeddedPlayerRule()).around(remoteComposeTestRule)

    protected val noopAction = hostAction("noopAction".rs, 1.rf)

    @Test
    fun button() {
        runScreenshotTest { RemoteButton(onClick = noopAction) { RemoteText("Button".rs) } }
    }

    @Test
    fun button_secondaryLabel() {
        runScreenshotTest {
            RemoteButton(
                onClick = noopAction,
                secondaryLabel = { RemoteText("Secondary label".rs) },
            ) {
                RemoteText("Primary label".rs)
            }
        }
    }

    @Test
    fun button_icon() {
        runScreenshotTest {
            RemoteButton(
                onClick = noopAction,
                secondaryLabel = { RemoteText("Secondary label".rs) },
                icon = {
                    RemoteIcon(
                        bitmap = RemoteImageBitmap(bookendsIconBitmap()),
                        contentDescription = null,
                    )
                },
            ) {
                RemoteText("Primary label".rs)
            }
        }
    }

    @Test
    fun button_disabled() {
        runScreenshotTest {
            RemoteButton(
                onClick = noopAction,
                enabled = false.rb,
                secondaryLabel = { RemoteText("Secondary label".rs) },
            ) {
                RemoteText("Disabled".rs)
            }
        }
    }

    @Test
    fun button_longLabel() {
        runScreenshotTest {
            RemoteButton(onClick = noopAction) {
                RemoteText("A very long label that is truncated by the button label maxLines".rs)
            }
        }
    }

    @Test
    fun switchButton_checked() {
        runScreenshotTest {
            RemoteSwitchButton(
                checked = true.rb,
                onCheckedChange = noopAction,
                secondaryLabel = { RemoteText("On".rs) },
                icon = {
                    RemoteIcon(
                        bitmap = RemoteImageBitmap(bookendsIconBitmap()),
                        contentDescription = null,
                    )
                },
            ) {
                RemoteText("Switch".rs)
            }
        }
    }

    @Test
    fun switchButton_unchecked() {
        runScreenshotTest {
            RemoteSwitchButton(
                checked = false.rb,
                onCheckedChange = noopAction,
                secondaryLabel = { RemoteText("Off".rs) },
                icon = {
                    RemoteIcon(
                        bitmap = RemoteImageBitmap(bookendsIconBitmap()),
                        contentDescription = null,
                    )
                },
            ) {
                RemoteText("Switch".rs)
            }
        }
    }

    @Test
    fun switchButton_disabled() {
        runScreenshotTest {
            RemoteSwitchButton(
                checked = true.rb,
                onCheckedChange = noopAction,
                enabled = false.rb,
                secondaryLabel = { RemoteText("Secondary label".rs) },
            ) {
                RemoteText("Disabled".rs)
            }
        }
    }

    @Test
    fun text() {
        runScreenshotTest {
            RemoteColumn(
                modifier = RemoteModifier.fillMaxWidth(),
                verticalArrangement = RemoteArrangement.spacedBy(4.rdp),
            ) {
                RemoteText("Default".rs)
                RemoteText(
                    "Start".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                )
                RemoteText(
                    "Center".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                RemoteText(
                    "End".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
                RemoteText("Colored".rs, color = RemoteColor(Color.Cyan))
                RemoteText(
                    "Max lines limits this text to a single line".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    maxLines = 1,
                )
            }
        }
    }

    /**
     * Runs a screenshot test of [content], emitted with [implementation] and played with the
     * embedded player, on a black background and by default centered on the screen.
     */
    protected fun runScreenshotTest(
        centered: Boolean = true,
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        remoteComposeTestRule.runScreenshotTest(
            profile = BookendsProfile,
            playComposableWrapper = ComposableWrappers.blackBackground,
            player = BookendsPlayer,
        ) {
            // Note: WearMaterial3 resolves typography on the host via MaterialTheme, whereas
            // RemoteMaterial3 resolves typography at creation time via RemoteMaterialTheme
            // (without it, standalone RemoteText falls back to 12.rsp instead of bodyLarge 16.sp).
            RemoteMaterialTheme(typography = BookendsRemoteTypography) {
                CompositionLocalProvider(LocalBookendsImplementation provides implementation) {
                    if (centered) {
                        // Note: Pre-register the font family string on the outer box's modifier so
                        // it is written to the wire buffer during the render pass before any child
                        // RemoteTextNode renders its textId (RemoteTextNode.render writes textId
                        // before startTextComponent writes fontFamilyId, and
                        // CoreText.registerListening does not listen to mFontFamilyId).
                        RemoteBox(
                            modifier =
                                RemoteModifier.fillMaxSize().padding(16.rdp).semantics {
                                    stateDescription = "roboto-flex".rs
                                },
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            content()
                        }
                    } else {
                        RemoteBox(
                            modifier =
                                RemoteModifier.fillMaxSize().semantics {
                                    stateDescription = "roboto-flex".rs
                                }
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }

    /** Plays the document like the demo host: embedded player with the Wear Material3 plugins. */
    private object BookendsPlayer : Player {
        @Composable
        override fun Play(coreDocument: CoreDocument, size: Size) {
            MaterialTheme {
                RcPlayer(
                    document = coreDocument,
                    modifier = Modifier.fillMaxSize(),
                    customPlugins = WearMaterial3Plugins,
                )
            }
        }
    }
}

// Note: Converting Wear Material3's Typography (`DeviceFontFamilyName("roboto-flex")` with
// variable font axes `wdth=110` and `wght=500`, plus `fontFeatureSettings = "pnum"`) via
// `RemoteTextStyle.fromTextStyle` currently drops two things:
// 1. `AndroidFont.variationSettings` is dropped whenever `TextStyle.fontFeatureSettings` is
//    non-empty instead of combining both.
// 2. `RemoteFontFamily.fromComposeFontFamily` returns `null` for `FontListFontFamily`
//    (`DeviceFontFamilyName("roboto-flex")`), falling back to `"sans-serif"` on the player.
// Clearing `fontFeatureSettings` and explicitly preserving `RemoteFontFamily.Named("roboto-flex")`
// keeps the variable font family and axes aligned between WearMaterial3 and RemoteMaterial3.
private val BookendsRemoteTypography: RemoteTypography =
    RemoteTypography(Typography().withoutFontFeatureSettings())
        .withFontFamily(RemoteFontFamily.Named("roboto-flex"))

private fun Typography.withoutFontFeatureSettings(): Typography =
    copy(
        displayLarge = displayLarge.copy(fontFeatureSettings = null),
        displayMedium = displayMedium.copy(fontFeatureSettings = null),
        displaySmall = displaySmall.copy(fontFeatureSettings = null),
        titleLarge = titleLarge.copy(fontFeatureSettings = null),
        titleMedium = titleMedium.copy(fontFeatureSettings = null),
        titleSmall = titleSmall.copy(fontFeatureSettings = null),
        labelLarge = labelLarge.copy(fontFeatureSettings = null),
        labelMedium = labelMedium.copy(fontFeatureSettings = null),
        labelSmall = labelSmall.copy(fontFeatureSettings = null),
        bodyLarge = bodyLarge.copy(fontFeatureSettings = null),
        bodyMedium = bodyMedium.copy(fontFeatureSettings = null),
        bodySmall = bodySmall.copy(fontFeatureSettings = null),
        bodyExtraSmall = bodyExtraSmall.copy(fontFeatureSettings = null),
        numeralExtraLarge = numeralExtraLarge.copy(fontFeatureSettings = null),
        numeralLarge = numeralLarge.copy(fontFeatureSettings = null),
        numeralMedium = numeralMedium.copy(fontFeatureSettings = null),
        numeralSmall = numeralSmall.copy(fontFeatureSettings = null),
        numeralExtraSmall = numeralExtraSmall.copy(fontFeatureSettings = null),
    )

private fun RemoteTypography.withFontFamily(fontFamily: RemoteFontFamily): RemoteTypography =
    copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        bodyExtraSmall = bodyExtraSmall.copy(fontFamily = fontFamily),
        numeralExtraLarge = numeralExtraLarge.copy(fontFamily = fontFamily),
        numeralLarge = numeralLarge.copy(fontFamily = fontFamily),
        numeralMedium = numeralMedium.copy(fontFamily = fontFamily),
        numeralSmall = numeralSmall.copy(fontFamily = fontFamily),
        numeralExtraSmall = numeralExtraSmall.copy(fontFamily = fontFamily),
    )
