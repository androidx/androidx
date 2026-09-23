/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.collection.buildObjectIntMap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.toRemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonEnabled
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonExtraSmall
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonFilled
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonLarge
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonOutlined
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonSmall
import androidx.wear.compose.remote.material3.previews.RemoteIconButtonTonal
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.TestImageVectors
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteIconButtonTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(180f, 180f))

    @Test
    fun remote_icon_button_enabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteIconButtonEnabled() }
        }
    }

    @Test
    fun remote_icon_button_large() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteIconButtonLarge() }
        }
    }

    @Test
    fun remote_icon_button_small() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteIconButtonSmall() }
        }
    }

    @Test
    fun remote_icon_button_extra_small() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteIconButtonExtraSmall() }
        }
    }

    @Test
    fun remote_icon_button_filled_enabled() {
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            ComponentContainer { RemoteIconButtonFilled() }
        }
    }

    @Test
    fun remote_icon_button_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = creationDisplayInfo,
            creationComposableWrapper = ComposableWrappers.rtl,
        ) {
            ComponentContainer { RemoteIconButtonEnabled() }
        }
    }

    @Test
    fun remote_icon_button_disabled() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer {
                RemoteIconButton(testAction, enabled = RemoteBoolean(false)) {
                    RemoteIcon(
                        imageVector = TestImageVectors.VolumeUp,
                        contentDescription = null,
                        modifier = RemoteModifier.size(RemoteIconButtonDefaults.DefaultIconSize),
                    )
                }
            }
        }
    }

    @Test
    fun remote_icon_button_tonal_enabled() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteIconButtonTonal() }
        }
    }

    @Test
    fun remote_icon_button_tonal_disabled() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer {
                RemoteIconButton(
                    testAction,
                    enabled = RemoteBoolean(false),
                    colors = RemoteIconButtonDefaults.filledTonalIconButtonColors(),
                ) {
                    RemoteIcon(
                        modifier = RemoteModifier.size(RemoteIconButtonDefaults.SmallIconSize),
                        imageVector = TestImageVectors.VolumeUp,
                        contentDescription = null,
                    )
                }
            }
        }
    }

    @Test
    fun remote_icon_button_outline_enabled() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteIconButtonOutlined() }
        }
    }

    @Test
    fun remote_icon_button_outline_disabled() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer {
                RemoteIconButton(
                    testAction,
                    border = 1.rdp,
                    borderColor = RemoteMaterialTheme.colorScheme.outline,
                    enabled = RemoteBoolean(false),
                    colors = RemoteIconButtonDefaults.outlinedIconButtonColors(),
                ) {
                    RemoteIcon(
                        modifier = RemoteModifier.size(RemoteIconButtonDefaults.SmallIconSize),
                        imageVector = TestImageVectors.VolumeUp,
                        contentDescription = null,
                    )
                }
            }
        }
    }

    @Test
    fun remote_icon_button_dynamic_color() {
        val colorOverrides = buildObjectIntMap {
            put("WearM3.primary", Color(0xFFB8D0A0).toArgb())
            put("WearM3.onPrimary", Color(0xFF24361A).toArgb())
            put("WearM3.surfaceContainer", Color(0xFF1C1D1A).toArgb())
            put("WearM3.onSurface", Color(0xFFE2E3DC).toArgb())
        }
        remoteComposeTestRule.runScreenshotTest(
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            remoteCreationDisplayInfo = creationDisplayInfo,
            update = { player ->
                colorOverrides.forEach { name, colorInt ->
                    player.setUserLocalColor(name, colorInt)
                }
            },
        ) {
            ComponentContainer { RemoteIconButtonEnabled() }
        }
    }

    @Ignore("b/556172440 - Enable once corner equality and embedded player icon mask fixes land")
    @OptIn(ExperimentalRemotePlayerApi::class)
    @Test
    fun remote_icon_button_filled_add_matches_wear_m3() {
        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = true
        try {
            val density = context.resources.displayMetrics.density
            val sizeDp = 52.dp
            val sizePx = 52f * density
            val displayInfo = createCreationDisplayInfo(context, Size(sizePx, sizePx))
            val showWearM3 = mutableStateOf(false)
            val capturedDoc = mutableStateOf<CoreDocument?>(null)

            remoteComposeTestRule.setContent(
                remoteCreationDisplayInfo = displayInfo,
                profile = RcPlatformProfiles.WEAR_WIDGETS,
                onCoreDocumentCreated = { capturedDoc.value = it },
                playComposableWrapper = {
                    if (!showWearM3.value) {
                        capturedDoc.value?.let { doc ->
                            RcPlayer(
                                document = doc,
                                modifier = Modifier.size(sizeDp),
                            )
                        }
                    } else {
                        Box(
                            modifier =
                                Modifier.size(sizeDp)
                                    .background(Color.Black)
                                    .testTag("WEAR_M3_ICON_BUTTON"),
                            contentAlignment = Alignment.Center,
                        ) {
                            MaterialTheme {
                                IconButton(
                                    onClick = {},
                                    enabled = true,
                                    colors = IconButtonDefaults.filledIconButtonColors(),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier =
                                            Modifier.size(IconButtonDefaults.DefaultIconSize),
                                    )
                                }
                            }
                        }
                    }
                },
            ) {
                RemoteMaterialTheme {
                    RemoteBox(
                        modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc),
                        contentAlignment = RemoteAlignment.Center,
                    ) {
                        RemoteIconButton(
                            onClick = Action.Empty,
                            enabled = true.rb,
                            colors = RemoteIconButtonDefaults.filledIconButtonColors(),
                        ) {
                            RemoteIcon(
                                imageVector = Icons.Filled.Add.toRemoteImageVector(),
                                contentDescription = null,
                                modifier =
                                    RemoteModifier.size(RemoteIconButtonDefaults.DefaultIconSize),
                            )
                        }
                    }
                }
            }
            remoteComposeTestRule.composeTestRule.waitForIdle()
            val remoteBitmap =
                remoteComposeTestRule.composeTestRule
                    .onNodeWithTag(RemoteScreenshotTestRule.ROOT_TEST_TAG)
                    .captureToImage()
                    .asAndroidBitmap()

            showWearM3.value = true
            remoteComposeTestRule.composeTestRule.waitForIdle()
            val wearBitmap =
                remoteComposeTestRule.composeTestRule
                    .onNodeWithTag("WEAR_M3_ICON_BUTTON")
                    .captureToImage()
                    .asAndroidBitmap()

            assertThat(remoteBitmap.sameAs(wearBitmap)).isTrue()
        } finally {
            RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = false
        }
    }
}

private val testAction = hostAction("testAction".rs, 1.rf)
