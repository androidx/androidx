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
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.core.platform.AndroidCustomContext
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.remote.testing.RemoteBaseContentTestRule.Player
import androidx.compose.remote.testing.RemoteContentTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.BitmapMatcher
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that uses [RemoteContentTestRule] to set the Remote Compose content and uses
 * [AndroidXScreenshotTestRule] for screenshot testing.
 *
 * The content is played with the View based [RemoteDocumentPlayer]. To play the content with the
 * embedded player instead, use [RemoteEmbeddedScreenshotTestRule].
 */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class RemoteScreenshotTestRule(
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

    override fun apply(base: Statement, description: Description): Statement {
        return baseRule.apply(base, description)
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
        update: (RemoteComposePlayer) -> Unit = {},
        bitmapLoader: BitmapLoader? = null,
        typefaceResolver: TypefaceResolver? = null,
        customSupport: AndroidCustomContext? = null,
        playComposableWrapper: ComposableWrapper = ComposableWrappers.noop,
        player: Player? = null,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        setContent(
            remoteCreationDisplayInfo = remoteCreationDisplayInfo,
            profile = profile,
            creationComposableWrapper = creationComposableWrapper,
            onCoreDocumentCreated = onCoreDocumentCreated,
            update = update,
            bitmapLoader = bitmapLoader,
            typefaceResolver = typefaceResolver,
            customSupport = customSupport,
            playComposableWrapper = playComposableWrapper,
            player = player,
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
        update: (RemoteComposePlayer) -> Unit = {},
        bitmapLoader: BitmapLoader? = null,
        typefaceResolver: TypefaceResolver? = null,
        customSupport: AndroidCustomContext? = null,
        playComposableWrapper: ComposableWrapper = ComposableWrappers.noop,
        player: Player? = null,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        baseRule.setContent(
            remoteCreationDisplayInfo = remoteCreationDisplayInfo,
            profile = profile,
            creationComposableWrapper = creationComposableWrapper,
            onCoreDocumentCreated = onCoreDocumentCreated,
            player =
                player
                    ?: PlayerImpl(
                        update = update,
                        bitmapLoader = bitmapLoader,
                        typefaceResolver = typefaceResolver,
                        customSupport = customSupport,
                    ),
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

    private class PlayerImpl(
        private val update: (RemoteComposePlayer) -> Unit = {},
        private val bitmapLoader: BitmapLoader? = null,
        private val typefaceResolver: TypefaceResolver? = null,
        private val customSupport: AndroidCustomContext? = null,
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
                customSupport = customSupport,
            )
        }
    }
}
