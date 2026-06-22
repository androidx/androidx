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

package androidx.compose.remote.player.compose.test.utils

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.remote.testing.RemoteBaseContentTestRule.Player
import androidx.compose.remote.testing.RemoteContentTestRule
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.BitmapMatcher
import java.io.File
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that uses [RemoteContentTestRule] to set the Remote Compose content and uses
 * [AndroidXScreenshotTestRule] for screenshot testing.
 */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class RemoteScreenshotTestRule(
    moduleDirectory: String,
    val remoteCreationDisplayInfo: RemoteCreationDisplayInfo,
    val matcher: BitmapMatcher? = null,
    val bitmapLoader: BitmapLoader? = null,
) : TestRule {

    constructor(
        moduleDirectory: String,
        context: Context,
        matcher: BitmapMatcher? = null,
        bitmapLoader: BitmapLoader? = null,
    ) : this(
        moduleDirectory = moduleDirectory,
        remoteCreationDisplayInfo = createCreationDisplayInfo(context),
        matcher = matcher,
        bitmapLoader = bitmapLoader,
    )

    private val remoteContentTestRule: RemoteContentTestRule = RemoteContentTestRule()

    private val screenshotTestRule = AndroidXScreenshotTestRule(moduleDirectory)

    private val goldenScreenshotNameTestRule = GoldenScreenshotNameTestRule()

    private val delegateChain: RuleChain =
        RuleChain.outerRule(goldenScreenshotNameTestRule)
            .around(remoteContentTestRule)
            .around(screenshotTestRule)

    override fun apply(base: Statement, description: Description): Statement {
        return delegateChain.apply(base, description)
    }

    /** [ComposeContentTestRule] used by this [TestRule]. */
    val composeTestRule: ComposeContentTestRule = remoteContentTestRule.composeTestRule

    /**
     * This method takes two steps: [setContent] and [verifyScreenshot].
     *
     * Use when no interaction with the UI is needed before verifying the screenshot.
     */
    fun runScreenshotTest(
        remoteCreationDisplayInfo: RemoteCreationDisplayInfo? = null,
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        typefaceResolver: TypefaceResolver? = null,
        creationComposableWrapper: ComposableWrapper = ComposableWrappers.noop,
        onCoreDocumentCreated: ((CoreDocument) -> Unit)? = null,
        goldenScreenshotName: GoldenScreenshotName? = null,
        update: (RemoteComposePlayer) -> Unit = {},
        playComposableWrapper: ComposableWrapper = ComposableWrappers.noop,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        setContent(
            remoteCreationDisplayInfo = remoteCreationDisplayInfo,
            profile = profile,
            typefaceResolver = typefaceResolver,
            creationComposableWrapper = creationComposableWrapper,
            onCoreDocumentCreated = onCoreDocumentCreated,
            update = update,
            playComposableWrapper = playComposableWrapper,
            composable = composable,
        )

        verifyScreenshot(goldenScreenshotName)
    }

    fun setContent(
        remoteCreationDisplayInfo: RemoteCreationDisplayInfo? = null,
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        typefaceResolver: TypefaceResolver? = null,
        creationComposableWrapper: (@Composable (composable: @Composable () -> Unit) -> Unit) = {
            it()
        },
        onCoreDocumentCreated: ((CoreDocument) -> Unit)? = null,
        update: (RemoteComposePlayer) -> Unit = {},
        playComposableWrapper: (@Composable (composable: @Composable () -> Unit) -> Unit) = {
            it()
        },
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        setContentInternal(
            remoteCreationDisplayInfo = remoteCreationDisplayInfo ?: this.remoteCreationDisplayInfo,
            profile = profile,
            typefaceResolver = typefaceResolver,
            creationComposableWrapper = creationComposableWrapper,
            onCoreDocumentCreated = getOnCoreDocumentCreated(onCoreDocumentCreated),
            update = update,
            playComposableWrapper = playComposableWrapper,
            composable = composable,
        )
    }

    fun verifyScreenshot(goldenScreenshotName: GoldenScreenshotName? = null) {
        verifyScreenshotInternal(getGoldenScreenshotName((goldenScreenshotName)))
    }

    private fun setContentInternal(
        remoteCreationDisplayInfo: RemoteCreationDisplayInfo,
        profile: Profile,
        typefaceResolver: TypefaceResolver?,
        creationComposableWrapper: ComposableWrapper,
        onCoreDocumentCreated: ((CoreDocument) -> Unit)?,
        update: (RemoteComposePlayer) -> Unit,
        playComposableWrapper: ComposableWrapper,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = remoteCreationDisplayInfo,
            profile = profile,
            creationComposableWrapper = creationComposableWrapper,
            onCoreDocumentCreated = onCoreDocumentCreated,
            player =
                PlayerImpl(
                    update = update,
                    bitmapLoader = bitmapLoader,
                    typefaceResolver = typefaceResolver,
                ),
            playComposableWrapper =
                customPlayComposableWrapper(remoteCreationDisplayInfo, playComposableWrapper),
            composable = composable,
        )
    }

    private fun verifyScreenshotInternal(goldenScreenshotName: GoldenScreenshotName) {
        val screenshot =
            remoteContentTestRule.composeTestRule.onNodeWithTag(ROOT_TEST_TAG).captureToImage()

        if (matcher == null) {
            screenshot.assertAgainstGolden(screenshotTestRule, goldenScreenshotName.getName())
        } else {
            screenshot.assertAgainstGolden(
                screenshotTestRule,
                goldenScreenshotName.getName(),
                matcher,
            )
        }
    }

    private fun getOnCoreDocumentCreated(
        onCoreDocumentCreated: ((CoreDocument) -> Unit)?
    ): ((CoreDocument) -> Unit) = { coreDocument ->
        val name = getGoldenScreenshotName(null).getName()
        saveDocument(coreDocument.buffer, "$name.rc")
        saveDrawCommands(coreDocument, "$name.txt")
        onCoreDocumentCreated?.invoke(coreDocument)
    }

    private fun saveDrawCommands(coreDocument: CoreDocument, fileName: String) {
        try {
            val width = remoteCreationDisplayInfo.widthDp.value
            val height = remoteCreationDisplayInfo.heightDp.value
            val player = TestPlayer(coreDocument, width, height)
            val actualCommands = player.paint()

            saveTextFile(actualCommands.joinToString(separator = "\n"), fileName)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to generate draw commands", t)
        }
    }

    private fun saveTextFile(content: String, name: String) {
        try {
            val filePath = screenshotTestRule.deviceOutputDirectory
            val myFile = File(filePath, name)
            myFile.parentFile?.mkdirs() // Ensure parent directories are created
            myFile.writeText(content)
            Log.i(TAG, "Wrote draw commands to $name at ${myFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write draw commands to $name", e)
        }
    }

    private fun getGoldenScreenshotName(goldenScreenshotName: GoldenScreenshotName?) =
        goldenScreenshotName ?: goldenScreenshotNameTestRule.getGoldenScreenshotName()

    private fun customPlayComposableWrapper(
        remoteCreationDisplayInfo: RemoteCreationDisplayInfo,
        playComposableWrapper: (@Composable (composable: @Composable () -> Unit) -> Unit),
    ): (@Composable (composable: @Composable () -> Unit) -> Unit) = { content ->
        Box(
            modifier =
                Modifier.width(remoteCreationDisplayInfo.widthDp)
                    .height(remoteCreationDisplayInfo.heightDp)
                    .testTag(ROOT_TEST_TAG)
        ) {
            playComposableWrapper { content() }
        }
    }

    private fun saveDocument(buffer: RemoteComposeBuffer, name: String) {
        try {
            val filePath = screenshotTestRule.deviceOutputDirectory
            val myFile = File(filePath, name)
            buffer.write(buffer, myFile)
            Log.i(TAG, "Wrote RC doc " + myFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write RC document", e)
        }
    }

    companion object {
        const val ROOT_TEST_TAG: String = "ROOT_TEST_TAG"

        private const val TAG = "RemoteScreenshotTestRule"
    }

    private class PlayerImpl(
        private val update: (RemoteComposePlayer) -> Unit = {},
        private val bitmapLoader: BitmapLoader? = null,
        private val typefaceResolver: TypefaceResolver? = null,
    ) : Player {
        @Composable
        override fun Play(coreDocument: CoreDocument, size: Size) {
            RemoteDocumentPlayer(
                document = coreDocument,
                documentWidth = size.width.toInt(),
                documentHeight = size.height.toInt(),
                update = update,
                bitmapLoader = bitmapLoader,
                typefaceResolver = typefaceResolver,
            )
        }
    }
}
