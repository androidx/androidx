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
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.EnableEmbeddedPlayerRule
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.remote.testing.RemoteBaseContentTestRule.Player
import androidx.compose.remote.testing.RemoteContentTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.BitmapMatcher
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that uses [RemoteContentTestRule] to set the Remote Compose content and uses
 * [AndroidXScreenshotTestRule] for screenshot testing.
 *
 * The content is played with the embedded player ([RcPlayer]).
 * [androidx.compose.remote.player.compose.RemoteComposePlayerFlags.isEmbeddedPlayerEnabled] is
 * enabled for the duration of each test (see [EnableEmbeddedPlayerRule]). To play the content with
 * the View based player instead, use [RemoteScreenshotTestRule].
 */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class RemoteEmbeddedScreenshotTestRule(
    moduleDirectory: String,
    val remoteCreationDisplayInfo: RemoteCreationDisplayInfo,
    val matcher: BitmapMatcher? = null,
) : TestRule {

    constructor(
        moduleDirectory: String,
        context: Context,
        matcher: BitmapMatcher? = null,
    ) : this(
        moduleDirectory = moduleDirectory,
        remoteCreationDisplayInfo = createCreationDisplayInfo(context),
        matcher = matcher,
    )

    private val baseRule =
        RemoteBaseScreenshotTestRule(
            moduleDirectory = moduleDirectory,
            remoteCreationDisplayInfo = remoteCreationDisplayInfo,
            matcher = matcher,
        )

    private val delegateChain: RuleChain =
        RuleChain.outerRule(EnableEmbeddedPlayerRule()).around(baseRule)

    override fun apply(base: Statement, description: Description): Statement {
        return delegateChain.apply(base, description)
    }

    /** [ComposeContentTestRule] used by this [TestRule]. */
    val composeTestRule: ComposeContentTestRule = baseRule.composeTestRule

    /**
     * This method takes two steps: [setContent] and [verifyScreenshot].
     *
     * Use when no interaction with the UI is needed before verifying the screenshot.
     */
    fun runScreenshotTest(
        goldenScreenshotName: GoldenScreenshotName? = null,
        remoteCreationDisplayInfo: RemoteCreationDisplayInfo? = null,
        // creation params
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        creationComposableWrapper: ComposableWrapper = ComposableWrappers.noop,
        onCoreDocumentCreated: ((CoreDocument) -> Unit)? = null,
        // play params
        typefaceResolver: TypefaceResolver? = null,
        playComposableWrapper: ComposableWrapper = ComposableWrappers.noop,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        setContent(
            remoteCreationDisplayInfo = remoteCreationDisplayInfo,
            profile = profile,
            creationComposableWrapper = creationComposableWrapper,
            onCoreDocumentCreated = onCoreDocumentCreated,
            typefaceResolver = typefaceResolver,
            playComposableWrapper = playComposableWrapper,
            composable = composable,
        )

        verifyScreenshot(goldenScreenshotName)
    }

    fun setContent(
        remoteCreationDisplayInfo: RemoteCreationDisplayInfo? = null,
        // creation params
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        creationComposableWrapper: ComposableWrapper = ComposableWrappers.noop,
        onCoreDocumentCreated: ((CoreDocument) -> Unit)? = null,
        // play params
        typefaceResolver: TypefaceResolver? = null,
        playComposableWrapper: ComposableWrapper = ComposableWrappers.noop,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        baseRule.setContent(
            remoteCreationDisplayInfo = remoteCreationDisplayInfo,
            profile = profile,
            creationComposableWrapper = creationComposableWrapper,
            onCoreDocumentCreated = onCoreDocumentCreated,
            player = EmbeddedPlayer(typefaceResolver = typefaceResolver),
            playComposableWrapper = playComposableWrapper,
            composable = composable,
        )
    }

    fun verifyScreenshot(goldenScreenshotName: GoldenScreenshotName? = null) {
        baseRule.verifyScreenshot(goldenScreenshotName)
    }

    companion object {
        const val ROOT_TEST_TAG: String = RemoteBaseScreenshotTestRule.ROOT_TEST_TAG
    }

    /** Plays the document with the embedded player ([RcPlayer]). */
    private class EmbeddedPlayer(private val typefaceResolver: TypefaceResolver?) : Player {
        @OptIn(ExperimentalRemotePlayerApi::class)
        @Composable
        override fun Play(coreDocument: CoreDocument, size: Size) {
            RcPlayer(document = coreDocument, typefaceResolver = typefaceResolver)
        }
    }
}
