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

package androidx.compose.remote.player.compose.test.utils.screenshot.rule

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.rememberRemoteDocument
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.BitmapMatcher
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withContext
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [org.junit.rules.TestRule] that takes screenshots of remote composable functions using devices
 * and the Remote Compose player.
 *
 * @param matcher The algorithm to be used to perform the matching. If null, it will let
 *   [androidx.compose.testutils.assertAgainstGolden] use its default.
 */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class RemoteComposeScreenshotTestRule(
    moduleDirectory: String,
    private val matcher: BitmapMatcher? = null,
    private val profile: Profile = RcPlatformProfiles.ANDROIDX,
) : ExternalResource() {
    private val composeTestRule = createComposeRule(StandardTestDispatcher())
    private val screenshotRule = AndroidXScreenshotTestRule(moduleDirectory)

    private val displayInfo = createCreationDisplayInfo(ApplicationProvider.getApplicationContext())

    private lateinit var testDescription: Description

    val clickEvents: MutableList<Pair<String, Any?>> = mutableListOf()

    private val testName =
        object : TestWatcher() {

            override fun starting(description: Description) {
                testDescription = description
            }
        }

    private val delegateChain: RuleChain =
        RuleChain.outerRule(testName).around(composeTestRule).around(screenshotRule)

    var bitmapLoader = BitmapLoader.UNSUPPORTED

    override fun apply(base: Statement, description: Description): Statement {
        return delegateChain.apply(base, description)
    }

    suspend fun captureDocument(
        context: Context,
        content: @Composable @RemoteComposable () -> Unit,
    ): CoreDocument {
        val document: ByteArray =
            withContext(Dispatchers.Main) {
                captureSingleRemoteDocument(context, content = content).bytes
            }

        val remoteComposeDocument =
            CoreDocument().apply {
                ByteArrayInputStream(document).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        return remoteComposeDocument
    }

    fun runTest(
        creationDisplayInfo: CreationDisplayInfo = displayInfo,
        backgroundColor: Color? = null,
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        setContent(
            creationDisplayInfo = creationDisplayInfo,
            backgroundColor = backgroundColor,
            content = content,
        )
    }

    fun runScreenshotTest(
        screenshotName: Description = testDescription,
        creationDisplayInfo: CreationDisplayInfo = displayInfo,
        layoutDirection: LayoutDirection? = null,
        backgroundColor: Color? = null,
        deviceConfigurationOverride: DeviceConfigurationOverride? = null,
        profile: Profile? = null,
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        setContent(
            creationDisplayInfo = creationDisplayInfo,
            layoutDirection = layoutDirection,
            backgroundColor = backgroundColor,
            deviceConfigurationOverride = deviceConfigurationOverride,
            profile = profile,
            content = content,
        )
        composeTestRule.verifyScreenshot(screenshotName, screenshotRule)
    }

    fun runScreenshotTest(
        screenshotName: Description = testDescription,
        creationDisplayInfo: CreationDisplayInfo = displayInfo,
        backgroundColor: Color? = null,
        document: CoreDocument,
        deviceConfigurationOverride: DeviceConfigurationOverride? = null,
        outerContent: (@Composable (content: @Composable @RemoteComposable () -> Unit) -> Unit)? =
            null,
    ) {
        composeTestRule.setContent {
            WithOverride(deviceConfigurationOverride) {
                val boxModifier =
                    Modifier.width(creationDisplayInfo.widthDp)
                        .height(creationDisplayInfo.heightDp)
                        .then(
                            if (backgroundColor != null) {
                                Modifier.background(backgroundColor)
                            } else {
                                Modifier
                            }
                        )
                        .testTag("playerRoot")

                val content: @Composable @RemoteComposable () -> Unit = {
                    RemoteDocumentPlayer(document, creationDisplayInfo)
                }
                Box(modifier = boxModifier) {
                    if (outerContent != null) {
                        outerContent(content)
                    } else {
                        content()
                    }
                }
            }
        }
        composeTestRule.verifyScreenshot(screenshotName, screenshotRule)
    }

    private fun setContent(
        creationDisplayInfo: CreationDisplayInfo = displayInfo,
        layoutDirection: LayoutDirection? = null,
        backgroundColor: Color?,
        deviceConfigurationOverride: DeviceConfigurationOverride? = null,
        profile: Profile? = null,
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        composeTestRule.setContent {
            WithOverride(layoutDirection?.let { DeviceConfigurationOverride.LayoutDirection(it) }) {
                WithOverride(deviceConfigurationOverride) {
                    val boxModifier =
                        Modifier.width(creationDisplayInfo.widthDp)
                            .height(creationDisplayInfo.heightDp)
                            .then(
                                if (backgroundColor != null) {
                                    Modifier.background(backgroundColor)
                                } else {
                                    Modifier
                                }
                            )
                            .testTag(ROOT_TEST_TAG)

                    Box(modifier = boxModifier) {
                        val document: CoreDocument? by
                            rememberRemoteDocument(
                                content = content,
                                creationDisplayInfo = creationDisplayInfo,
                                profile = profile ?: this@RemoteComposeScreenshotTestRule.profile,
                            )
                        document?.let { RemoteDocumentPlayer(it, creationDisplayInfo) }
                    }
                }
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

    @Composable
    private fun RemoteDocumentPlayer(
        document: CoreDocument,
        creationDisplayInfo: CreationDisplayInfo,
    ) {
        RemoteDocumentPlayer(
            document,
            creationDisplayInfo.width,
            creationDisplayInfo.height,
            debugMode = 1,
            bitmapLoader = bitmapLoader,
            onNamedAction = { name, value, _ -> clickEvents.add(Pair(name, value)) },
        )
    }

    fun ComposeContentTestRule.verifyScreenshot(
        testName: Description,
        screenshotRule: AndroidXScreenshotTestRule,
    ) {
        val goldenScreenshotName = testName.goldenIdentifier()
        val screenshot = onRoot().captureToImage()
        if (matcher != null) {
            screenshot.assertAgainstGolden(screenshotRule, goldenScreenshotName, matcher)
        } else {
            screenshot.assertAgainstGolden(screenshotRule, goldenScreenshotName)
        }
    }

    /**
     * Valid characters for golden identifiers are [A-Za-z0-9_-] TestParameterInjector adds '[' +
     * parameter_values + ']' + ',' to the test name.
     */
    fun Description.goldenIdentifier(): String {
        val testIdentifier = className.substringAfterLast('.') + "_" + methodName
        return testIdentifier.replace("[\\[$]".toRegex(), "_").replace("]", "")
    }

    fun assertRootNodeContainsColor(color: Color) {
        composeTestRule.onNodeWithTag(ROOT_TEST_TAG).captureToImage().assertContainsColor(color)
    }

    internal companion object {
        const val ROOT_TEST_TAG = "playerRoot"
    }
}
