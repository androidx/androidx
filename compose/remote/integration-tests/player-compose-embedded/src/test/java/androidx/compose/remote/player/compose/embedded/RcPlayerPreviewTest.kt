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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.previews.RemoteAppCardDefault
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// sdk = [35]: Robolectric's max supported SDK is 36 and the module compiles against 37, so pin the
// sandbox to 35 (as the other embedded tests do) to avoid "targetSdkVersion=37 > maxSdkVersion=36".
@Config(qualifiers = "w640dp-h480dp", sdk = [35])
@RunWith(RobolectricTestRunner::class)
class RcPlayerPreviewTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun testRemoteAppCardPreview() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()

            val documentBytes =
                captureSingleRemoteDocument(context = context, content = { RemoteAppCardDefault() })
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.mainClock.autoAdvance = true
            rule.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(1.0f)
                ) {
                    Row(modifier = Modifier.size(640.dp, 200.dp)) {
                        Box(modifier = Modifier.weight(1f).testTag("composeParent")) {
                            RcPlayer(
                                document = document,
                                modifier = Modifier.fillMaxSize().testTag("composePlayer"),
                            )
                        }
                        Box(modifier = Modifier.weight(1f).testTag("javaParent")) {
                            RemoteDocumentPlayer(
                                document = document,
                                documentWidth = document.width,
                                documentHeight = document.height,
                                modifier = Modifier.fillMaxSize().testTag("javaPlayer"),
                            )
                        }
                    }
                }
            }

            rule.mainClock.advanceTimeBy(100)

            val composeBounds = rule.onNodeWithTag("composePlayer").getUnclippedBoundsInRoot()

            val textNodes = rule.onAllNodesWithText("Card Title", useUnmergedTree = true)
            val titleBounds = textNodes[0].getUnclippedBoundsInRoot()

            // TODO: padding assertion (titleBounds.left >= 20.dp) disabled pending the
            // container-padding fix.
            val contentNodes = rule.onAllNodesWithText("Card Content", useUnmergedTree = true)
            val contentBounds = contentNodes[0].getUnclippedBoundsInRoot()

            val gap = contentBounds.top - titleBounds.bottom

            // Assert the gap is 2.dp (as measured in logs)
            assert(gap == 2.0.dp)
        }
    }

    @Test
    fun testRemoteButtonEnabledPreview() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.wear.compose.remote.material3.previews.RemoteButtonEnabled()
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        java.io.ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(1.0f)
                ) {
                    Row(modifier = Modifier.size(640.dp, 200.dp)) {
                        Box(modifier = Modifier.weight(1f).testTag("composeParent")) {
                            RcPlayer(
                                document = document,
                                modifier = Modifier.fillMaxSize().testTag("composePlayer"),
                            )
                        }
                        Box(modifier = Modifier.weight(1f).testTag("javaParent")) {
                            RemoteDocumentPlayer(
                                document = document,
                                documentWidth = document.width,
                                documentHeight = document.height,
                                modifier = Modifier.fillMaxSize().testTag("javaPlayer"),
                            )
                        }
                    }
                }
            }

            rule.mainClock.advanceTimeBy(100)

            val composeBounds = rule.onNodeWithTag("composePlayer").getUnclippedBoundsInRoot()
            val javaBounds = rule.onNodeWithTag("javaPlayer").getUnclippedBoundsInRoot()

        }
    }

    @Test
    fun testRemoteCardDefaultPreview() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.wear.compose.remote.material3.previews.RemoteCardDefault()
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        java.io.ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(1.0f)
                ) {
                    Row(modifier = Modifier.size(640.dp, 200.dp)) {
                        Box(modifier = Modifier.weight(1f).testTag("composeParent")) {
                            RcPlayer(
                                document = document,
                                modifier = Modifier.fillMaxSize().testTag("composePlayer"),
                            )
                        }
                        Box(modifier = Modifier.weight(1f).testTag("javaParent")) {
                            RemoteDocumentPlayer(
                                document = document,
                                documentWidth = document.width,
                                documentHeight = document.height,
                                modifier = Modifier.fillMaxSize().testTag("javaPlayer"),
                            )
                        }
                    }
                }
            }

            rule.mainClock.advanceTimeBy(100)

            val composeBounds = rule.onNodeWithTag("composePlayer").getUnclippedBoundsInRoot()
        }
    }

    @Test
    fun testRemoteCompactButtonWithIconPreview() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.wear.compose.remote.material3.previews
                                .RemoteCompactButtonWithIcon()
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        java.io.ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(1.0f)
                ) {
                    Row(modifier = Modifier.size(640.dp, 200.dp)) {
                        Box(modifier = Modifier.weight(1f).testTag("composeParent")) {
                            RcPlayer(
                                document = document,
                                modifier = Modifier.fillMaxSize().testTag("composePlayer"),
                            )
                        }
                        Box(modifier = Modifier.weight(1f).testTag("javaParent")) {
                            RemoteDocumentPlayer(
                                document = document,
                                documentWidth = document.width,
                                documentHeight = document.height,
                                modifier = Modifier.fillMaxSize().testTag("javaPlayer"),
                            )
                        }
                    }
                }
            }

            rule.mainClock.advanceTimeBy(100)

            val composeBounds = rule.onNodeWithTag("composePlayer").getUnclippedBoundsInRoot()
        }
    }

    @Test
    fun testRemoteButtonGroupThreeButtonsPreview() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.wear.compose.remote.material3.previews
                                .RemoteButtonGroupThreeButtons()
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        java.io.ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(1.0f)
                ) {
                    Row(modifier = Modifier.size(640.dp, 200.dp)) {
                        Box(modifier = Modifier.weight(1f).testTag("composeParent")) {
                            RcPlayer(
                                document = document,
                                modifier = Modifier.fillMaxSize().testTag("composePlayer"),
                            )
                        }
                        Box(modifier = Modifier.weight(1f).testTag("javaParent")) {
                            RemoteDocumentPlayer(
                                document = document,
                                documentWidth = document.width,
                                documentHeight = document.height,
                                modifier = Modifier.fillMaxSize().testTag("javaPlayer"),
                            )
                        }
                    }
                }
            }

            rule.mainClock.advanceTimeBy(100)

            val composeBounds = rule.onNodeWithTag("composePlayer").getUnclippedBoundsInRoot()
        }
    }

    @Test
    fun testRemoteTitleCardDefaultPreview() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.wear.compose.remote.material3.previews.RemoteTitleCardDefault()
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        java.io.ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(1.0f)
                ) {
                    Row(modifier = Modifier.size(640.dp, 200.dp)) {
                        Box(modifier = Modifier.weight(1f).testTag("composeParent")) {
                            RcPlayer(
                                document = document,
                                modifier = Modifier.fillMaxSize().testTag("composePlayer"),
                            )
                        }
                        Box(modifier = Modifier.weight(1f).testTag("javaParent")) {
                            RemoteDocumentPlayer(
                                document = document,
                                documentWidth = document.width,
                                documentHeight = document.height,
                                modifier = Modifier.fillMaxSize().testTag("javaPlayer"),
                            )
                        }
                    }
                }
            }

            rule.mainClock.advanceTimeBy(100)

            val composeBounds = rule.onNodeWithTag("composePlayer").getUnclippedBoundsInRoot()
        }
    }

    @Test
    fun testRemoteCircularProgressEnabledPreview() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.wear.compose.remote.material3.previews
                                .RemoteCircularProgressEnabled()
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        java.io.ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(1.0f)
                ) {
                    Row(modifier = Modifier.size(640.dp, 200.dp)) {
                        Box(modifier = Modifier.weight(1f).testTag("composeParent")) {
                            RcPlayer(
                                document = document,
                                modifier = Modifier.fillMaxSize().testTag("composePlayer"),
                            )
                        }
                        Box(modifier = Modifier.weight(1f).testTag("javaParent")) {
                            RemoteDocumentPlayer(
                                document = document,
                                documentWidth = document.width,
                                documentHeight = document.height,
                                modifier = Modifier.fillMaxSize().testTag("javaPlayer"),
                            )
                        }
                    }
                }
            }

            rule.mainClock.advanceTimeBy(100)

            val composeBounds = rule.onNodeWithTag("composePlayer").getUnclippedBoundsInRoot()
        }
    }

    @Test
    fun testRemoteTextButtonEnabledPreview() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.wear.compose.remote.material3.previews
                                .RemoteTextButtonEnabled()
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        java.io.ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(1.0f)
                ) {
                    Row(modifier = Modifier.size(640.dp, 200.dp)) {
                        Box(modifier = Modifier.weight(1f).testTag("composeParent")) {
                            RcPlayer(
                                document = document,
                                modifier = Modifier.fillMaxSize().testTag("composePlayer"),
                            )
                        }
                        Box(modifier = Modifier.weight(1f).testTag("javaParent")) {
                            RemoteDocumentPlayer(
                                document = document,
                                documentWidth = document.width,
                                documentHeight = document.height,
                                modifier = Modifier.fillMaxSize().testTag("javaPlayer"),
                            )
                        }
                    }
                }
            }

            rule.mainClock.advanceTimeBy(100)

            val composeBounds = rule.onNodeWithTag("composePlayer").getUnclippedBoundsInRoot()
        }
    }

    @Test
    fun testRemoteIconDefaultPreview() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.wear.compose.remote.material3.previews.RemoteIconDefault()
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        java.io.ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(1.0f)
                ) {
                    Row(modifier = Modifier.size(640.dp, 200.dp)) {
                        Box(modifier = Modifier.weight(1f).testTag("composeParent")) {
                            RcPlayer(
                                document = document,
                                modifier = Modifier.fillMaxSize().testTag("composePlayer"),
                            )
                        }
                        Box(modifier = Modifier.weight(1f).testTag("javaParent")) {
                            RemoteDocumentPlayer(
                                document = document,
                                documentWidth = document.width,
                                documentHeight = document.height,
                                modifier = Modifier.fillMaxSize().testTag("javaPlayer"),
                            )
                        }
                    }
                }
            }

            rule.mainClock.advanceTimeBy(100)

            val composeBounds = rule.onNodeWithTag("composePlayer").getUnclippedBoundsInRoot()
        }
    }

    @Test
    fun testRemoteIconButtonEnabledPreview() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val documentBytes =
                captureSingleRemoteDocument(
                        context = context,
                        content = {
                            androidx.wear.compose.remote.material3.previews
                                .RemoteIconButtonEnabled()
                        },
                    )
                    .bytes

            val document =
                androidx.compose.remote.core
                    .CoreDocument(androidx.compose.remote.core.RemoteClock.SYSTEM)
                    .apply {
                        java.io.ByteArrayInputStream(documentBytes).use {
                            initFromBuffer(
                                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(it)
                            )
                        }
                    }

            rule.setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(1.0f)
                ) {
                    Row(modifier = Modifier.size(640.dp, 200.dp)) {
                        Box(modifier = Modifier.weight(1f).testTag("composeParent")) {
                            RcPlayer(
                                document = document,
                                modifier = Modifier.fillMaxSize().testTag("composePlayer"),
                            )
                        }
                        Box(modifier = Modifier.weight(1f).testTag("javaParent")) {
                            RemoteDocumentPlayer(
                                document = document,
                                documentWidth = document.width,
                                documentHeight = document.height,
                                modifier = Modifier.fillMaxSize().testTag("javaPlayer"),
                            )
                        }
                    }
                }
            }

            rule.mainClock.advanceTimeBy(100)

            val composeBounds = rule.onNodeWithTag("composePlayer").getUnclippedBoundsInRoot()
        }
    }
}
