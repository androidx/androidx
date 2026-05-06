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

package androidx.compose.remote.creation.compose.v2

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.FontWeightAdjustment
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class CaptureRemoteDocumentV2Test {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule("compose/remote/remote-creation-compose")

    private val DisplaySize = 200
    private val ContainerSizeDp
        @Composable get() = with(LocalDensity.current) { DisplaySize.toDp() }

    @Test
    fun captureDocumentV2_withDefaultDensity() = runTest {
        val creationDisplayInfo = RemoteCreationDisplayInfo(DisplaySize, DisplaySize, 160, 1.0f)
        val bytes =
            captureSingleRemoteDocument(
                    creationDisplayInfo = creationDisplayInfo,
                    context = context,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(Color.DarkGray.rc)
                                .border(2.rdp, Color.Blue.rc)
                    ) {
                        RemoteText(text = "Hello world!".rs)
                    }
                }
                .bytes
        val doc = parseToCoreDocument(bytes)

        composeTestRule.setContent {
            Box(Modifier.requiredSize(ContainerSizeDp)) {
                RemoteDocumentPlayer(doc, DisplaySize, DisplaySize)
            }
        }

        composeTestRule
            .onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, "captureDocumentV2_defaultDensity")
    }

    @Test
    fun captureDocumentV2_withCustomDensity() = runTest {
        val creationDisplayInfo = RemoteCreationDisplayInfo(DisplaySize, DisplaySize, 160, 1.0f)
        val bytes =
            captureSingleRemoteDocument(
                    creationDisplayInfo = creationDisplayInfo,
                    remoteDensity = RemoteDensity(3f.rf, 1.5f.rf),
                    context = context,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(Color.DarkGray.rc)
                                .border(2.rdp, Color.Blue.rc)
                    ) {
                        RemoteText(text = "Hello world!".rs)
                    }
                }
                .bytes
        val doc = parseToCoreDocument(bytes)

        composeTestRule.setContent {
            Box(Modifier.requiredSize(ContainerSizeDp)) {
                RemoteDocumentPlayer(doc, DisplaySize, DisplaySize)
            }
        }

        composeTestRule
            .onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, "captureDocumentV2_customDensity")
    }

    @Test
    fun captureDocumentV2_withHostDensity() = runTest {
        val creationDisplayInfo = RemoteCreationDisplayInfo(DisplaySize, DisplaySize, 160, 1.0f)
        val bytes =
            captureSingleRemoteDocument(
                    creationDisplayInfo = creationDisplayInfo,
                    remoteDensity = RemoteDensity.Host,
                    context = context,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(Color.DarkGray.rc)
                                .border(2.rdp, Color.Blue.rc)
                    ) {
                        RemoteText(text = "Hello world!".rs)
                    }
                }
                .bytes
        val doc = parseToCoreDocument(bytes)

        composeTestRule.setContent {
            Box(Modifier.requiredSize(ContainerSizeDp)) {
                RemoteDocumentPlayer(doc, DisplaySize, DisplaySize)
            }
        }

        composeTestRule
            .onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, "captureDocumentV2_hostDensity")
    }

    @Test
    fun captureDocumentV2_withHostDensityAndDefaultDensityOverride() = runTest {
        val bytes =
            captureSingleRemoteDocument(remoteDensity = RemoteDensity.Host, context = context) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(Color.DarkGray.rc)
                                .border(2.rdp, Color.Blue.rc)
                    ) {
                        RemoteText(text = "Hello world!".rs)
                    }
                }
                .bytes
        val doc = parseToCoreDocument(bytes)

        composeTestRule.setContent {
            WithOverride(deviceConfigurationOverride = DeviceConfigurationOverride.Density(1.0f)) {
                Box(Modifier.requiredSize(ContainerSizeDp)) {
                    RemoteDocumentPlayer(doc, DisplaySize, DisplaySize)
                }
            }
        }

        composeTestRule
            .onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(
                screenshotRule,
                "captureDocumentV2_hostDensityAndDefaultDensityOverride",
            )
    }

    @Test
    fun captureDocumentV2_withHostDensityAndDensityOverride() = runTest {
        val bytes =
            captureSingleRemoteDocument(remoteDensity = RemoteDensity.Host, context = context) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .background(Color.DarkGray.rc)
                                .border(2.rdp, Color.Blue.rc)
                    ) {
                        RemoteText(text = "Hello world!".rs)
                    }
                }
                .bytes
        val doc = parseToCoreDocument(bytes)

        composeTestRule.setContent {
            WithOverride(deviceConfigurationOverride = DeviceConfigurationOverride.Density(2.0f)) {
                Box(Modifier.requiredSize(ContainerSizeDp)) {
                    RemoteDocumentPlayer(doc, DisplaySize, DisplaySize)
                }
            }
        }

        composeTestRule
            .onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, "captureDocumentV2_hostDensityAndDensityOverride")
    }

    @Test
    fun captureDocumentV2_withCustomFontScalingContextConstructor() = runTest {
        val customFontContext = context.withFontScale(2.0f)
        val creationDisplayInfo = createCreationDisplayInfo(customFontContext, Size(100f, 100f))
        val bytes =
            captureSingleRemoteDocument(
                    creationDisplayInfo = creationDisplayInfo,
                    context = customFontContext,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .border(2.rdp, Color.Blue.rc)
                                .background(Color.DarkGray.rc)
                    ) {
                        RemoteText(text = "Hello world!".rs, fontSize = 12.rsp)
                    }
                }
                .bytes
        val doc = parseToCoreDocument(bytes)

        composeTestRule.setContent {
            Box(Modifier.size(100.dp)) { RemoteDocumentPlayer(doc, 100, 100) }
        }

        composeTestRule
            .onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, "captureDocumentV2_customFontScale")
    }

    @Test
    fun captureDocumentV2_withCustomFontScalingRemoteDensityOverride30SP() = runTest {
        val creationDisplayInfo = createCreationDisplayInfo(context, Size(100f, 100f))
        val bytes =
            captureSingleRemoteDocument(
                    creationDisplayInfo = creationDisplayInfo,
                    context = context,
                    remoteDensity =
                        RemoteDensity(context.resources.displayMetrics.density.rf, 1.5f.rf),
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .border(2.rdp, Color.Blue.rc)
                                .background(Color.DarkGray.rc)
                    ) {
                        RemoteText(text = "Hello world!".rs, fontSize = 30.rsp)
                    }
                }
                .bytes
        val doc = parseToCoreDocument(bytes)

        composeTestRule.setContent {
            Box(Modifier.size(100.dp)) { RemoteDocumentPlayer(doc, 100, 100) }
        }

        composeTestRule
            .onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, "captureDocumentV2_customFontScaleNonlinear30sp")
    }

    @Test
    fun captureDocumentV2_withCustomFontScalingRemoteDensityOverrideNonLinear18SP() = runTest {
        val creationDisplayInfo = createCreationDisplayInfo(context, Size(100f, 100f))
        val bytes =
            captureSingleRemoteDocument(
                    creationDisplayInfo = creationDisplayInfo,
                    context = context,
                    remoteDensity =
                        RemoteDensity(context.resources.displayMetrics.density.rf, 2f.rf),
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .border(2.rdp, Color.Blue.rc)
                                .background(Color.DarkGray.rc)
                    ) {
                        RemoteText(text = "Hello world!".rs, fontSize = 18.rsp)
                    }
                }
                .bytes
        val doc = parseToCoreDocument(bytes)

        composeTestRule.setContent {
            Box(Modifier.size(100.dp)) { RemoteDocumentPlayer(doc, 100, 100) }
        }

        composeTestRule
            .onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, "captureDocumentV2_customFontScaleNonlinear18sp")
    }

    @Test
    @Ignore("b/509925295")
    fun captureDocumentV2_withCustomFontScaleOverride() = runTest {
        val creationDisplayInfo = createCreationDisplayInfo(context, Size(100f, 100f))
        val bytes =
            captureSingleRemoteDocument(
                    creationDisplayInfo = creationDisplayInfo,
                    context = context,
                ) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxSize()
                                .border(2.rdp, Color.Blue.rc)
                                .background(Color.DarkGray.rc)
                    ) {
                        RemoteColumn {
                            RemoteText(text = "No font scale override".rs, fontSize = 12.rsp)
                            DeviceConfigurationOverride(
                                DeviceConfigurationOverride.FontScale(2.0f) then
                                    DeviceConfigurationOverride.FontWeightAdjustment(200)
                            ) {
                                RemoteText(text = "Font scale override".rs, fontSize = 12.rsp)
                            }
                        }
                    }
                }
                .bytes
        val doc = parseToCoreDocument(bytes)

        composeTestRule.setContent {
            Box(Modifier.size(100.dp)) { RemoteDocumentPlayer(doc, 100, 100) }
        }

        composeTestRule
            .onRoot()
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, "captureDocumentV2_fontScaleOverride")
    }

    private fun parseToCoreDocument(bytes: ByteArray): CoreDocument {
        return CoreDocument().apply {
            ByteArrayInputStream(bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }
    }

    @Composable
    private fun WithOverride(
        deviceConfigurationOverride: DeviceConfigurationOverride?,
        content: @Composable () -> Unit,
    ) {
        if (deviceConfigurationOverride != null) {
            DeviceConfigurationOverride(deviceConfigurationOverride, content)
        } else {
            content()
        }
    }

    private fun DeviceConfigurationOverride.Companion.Density(
        density: Float
    ): DeviceConfigurationOverride = DeviceConfigurationOverride { contentUnderTest ->
        val context = LocalContext.current
        val config =
            Configuration(LocalConfiguration.current).apply {
                densityDpi = (density * DisplayMetrics.DENSITY_DEFAULT).toInt()
            }
        val overriddenContext =
            ContextThemeWrapper(context, 0).apply { applyOverrideConfiguration(config) }
        CompositionLocalProvider(
            LocalContext provides overriddenContext,
            LocalConfiguration provides config,
            LocalDensity provides Density(density, LocalDensity.current.fontScale),
            content = contentUnderTest,
        )
    }

    private fun Context.withFontScale(fontScale: Float): Context {
        val configuration =
            Configuration(context.resources.configuration).apply { this.fontScale = fontScale }

        return context.createConfigurationContext(configuration)
    }
}
