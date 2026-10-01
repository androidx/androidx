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

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.testing.RemoteBaseContentTestRule.Player
import androidx.compose.remote.testing.RemoteContentTestRule
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.BitmapMatcher
import java.io.File
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Common implementation shared by the Remote Compose screenshot [TestRule]s, such as
 * [RemoteScreenshotTestRule] and [RemoteEmbeddedScreenshotTestRule].
 *
 * Uses [RemoteContentTestRule] to create and play the Remote Compose content with a given [Player]
 * and [AndroidXScreenshotTestRule] to verify it against golden screenshots. The created document
 * and its draw commands are saved to the device output directory for debugging.
 */
internal class RemoteBaseScreenshotTestRule(
    moduleDirectory: String,
    val remoteCreationDisplayInfo: RemoteCreationDisplayInfo,
    val matcher: BitmapMatcher?,
) : TestRule {

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

    /** Creates the document from [composable] and plays it with [player]. */
    fun setContent(
        remoteCreationDisplayInfo: RemoteCreationDisplayInfo?,
        // creation params
        profile: Profile,
        creationComposableWrapper: ComposableWrapper,
        onCoreDocumentCreated: ((CoreDocument) -> Unit)?,
        // play params
        player: Player,
        playComposableWrapper: ComposableWrapper,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        val displayInfo = remoteCreationDisplayInfo ?: this.remoteCreationDisplayInfo
        remoteContentTestRule.setContent(
            remoteCreationDisplayInfo = displayInfo,
            profile = profile,
            creationComposableWrapper = creationComposableWrapper,
            onCoreDocumentCreated = getOnCoreDocumentCreated(onCoreDocumentCreated),
            player = player,
            playComposableWrapper = customPlayComposableWrapper(displayInfo, playComposableWrapper),
            composable = composable,
        )
    }

    fun verifyScreenshot(goldenScreenshotName: GoldenScreenshotName?) {
        val name = getGoldenScreenshotName(goldenScreenshotName).getName()
        val screenshot =
            remoteContentTestRule.composeTestRule.onNodeWithTag(ROOT_TEST_TAG).captureToImage()

        if (matcher == null) {
            screenshot.assertAgainstGolden(screenshotTestRule, name)
        } else {
            screenshot.assertAgainstGolden(screenshotTestRule, name, matcher)
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
}
