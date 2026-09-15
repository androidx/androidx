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
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.visibility
import androidx.compose.remote.creation.compose.state.RemoteFloatArray.Companion.createNamedRemoteFloatArray
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteBoolean
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteColor
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteInt
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteString
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerStateTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    private fun createDocument(content: @Composable () -> Unit): CoreDocument = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bytes = captureSingleRemoteDocument(context = context, content = content).bytes
        CoreDocument(RemoteClock.SYSTEM).apply {
            ByteArrayInputStream(bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }
    }

    @Test
    fun testPreCompositionSetAndGet() {
        val document = createDocument {
            val progress = rememberNamedRemoteFloat("progress") { 0.25f.rf }
            val count = rememberNamedRemoteInt("count", 10)
            val flag = rememberNamedRemoteBoolean("flag", false)
            val title = rememberNamedRemoteString("title", "Initial Title")
            val color = rememberNamedRemoteColor("accent", Color.Red)

            val widthDp = progress.asRemoteDp()
            val heightDp = count.toRemoteFloat().asRemoteDp()
            val visibilityInt =
                flag.select(Component.Visibility.VISIBLE.ri, Component.Visibility.GONE.ri)

            RemoteBox(
                modifier =
                    RemoteModifier.size(widthDp, heightDp)
                        .background(color)
                        .semantics { contentDescription = title }
                        .visibility(visibilityInt)
            )
        }

        val playerState = RcPlayerState(document)
        var progress by playerState.floatState("progress")
        var count by playerState.intState("count")
        var flag by playerState.booleanState("flag")
        var title by playerState.stringState("title")
        var accent by playerState.colorState("accent")

        // Read initial defaults before setting
        assertThat(progress).isEqualTo(0.25f)
        assertThat(count).isEqualTo(10)
        assertThat(flag).isFalse()
        assertThat(title).isEqualTo("Initial Title")
        assertThat(accent).isEqualTo(Color.Red)

        // Set overrides before composition
        progress = 75f
        count = 42
        flag = true
        title = "Updated Title"
        accent = Color.Green

        // Verify get immediately reflects changes
        assertThat(progress).isEqualTo(75f)
        assertThat(count).isEqualTo(42)
        assertThat(flag).isTrue()
        assertThat(title).isEqualTo("Updated Title")
        assertThat(accent).isEqualTo(Color.Green)

        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(state = playerState)
            }
        }

        rule.waitForIdle()

        // Verify RcPlayer rendered with the overridden values
        rule
            .onNodeWithContentDescription("Updated Title")
            .assertExists()
            .assertWidthIsEqualTo(75.dp)
            .assertHeightIsEqualTo(42.dp)
    }

    @Test
    fun testReactiveRecompositionOnSet() {
        val document = createDocument {
            val text = rememberNamedRemoteString("message", "Hello")
            RemoteText(text = text)
        }

        val playerState = RcPlayerState(document)
        var message by playerState.stringState("message")

        rule.setContent {
            RcPlayer(state = playerState)
        }

        rule.waitForIdle()
        rule.onNodeWithText("Hello", useUnmergedTree = true).assertExists()

        // Mutate state via delegated property
        rule.runOnUiThread {
            message = "World"
        }

        rule.waitForIdle()
        rule.onNodeWithText("World", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Hello", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun testTwoWayStateDelegation() {
        val document = createDocument {
            val progress = rememberNamedRemoteFloat("progress") { 10f.rf }
            RemoteBox(
                modifier =
                    RemoteModifier.size(progress.asRemoteDp(), 50.rdp).semantics {
                        contentDescription = "box".rs
                    }
            )
        }

        val playerState = RcPlayerState(document)
        var progress by playerState.floatState("progress")

        rule.setContent {
            RcPlayer(state = playerState)
        }

        rule.waitForIdle()
        assertThat(progress).isEqualTo(10f)
        rule.onNodeWithContentDescription("box").assertWidthIsEqualTo(10.dp)

        // Mutate via delegate property
        rule.runOnUiThread {
            progress = 80f
        }

        rule.waitForIdle()
        assertThat(progress).isEqualTo(80f)
        rule.onNodeWithContentDescription("box").assertWidthIsEqualTo(80.dp)
    }

    @Test
    fun testClearOverrideRestoresInitialValue() {
        val document = createDocument {
            val progress = rememberNamedRemoteFloat("progress") { 20f.rf }
            val count = rememberNamedRemoteInt("count", 5)
            val title = rememberNamedRemoteString("title", "Initial")
            RemoteBox(
                modifier =
                    RemoteModifier.size(progress.asRemoteDp(), count.toRemoteFloat().asRemoteDp())
                        .semantics { contentDescription = title }
            )
        }

        val playerState = RcPlayerState(document)
        var progress by playerState.floatState("progress")
        var count by playerState.intState("count")
        var title by playerState.stringState("title")

        progress = 90f
        count = 30
        title = "Modified"

        assertThat(progress).isEqualTo(90f)
        assertThat(count).isEqualTo(30)
        assertThat(title).isEqualTo("Modified")

        playerState.clearOverride("progress")
        assertThat(progress).isEqualTo(20f)

        playerState.clearOverride("count")
        assertThat(count).isEqualTo(5)

        playerState.clearOverride("title")
        assertThat(title).isEqualTo("Initial")
    }

    @Test
    fun testNameResolutionWithDefaultPrefix() {
        val document = createDocument {
            // By default, rememberNamedRemoteFloat uses Domain.User -> "USER:user_var"
            val userVar = rememberNamedRemoteFloat("user_var") { 15f.rf }
            // An unprefixed variable
            val rawVar =
                rememberNamedRemoteFloat("raw_var", domain = RemoteState.Domain.None) { 25f.rf }
            RemoteBox(
                modifier =
                    RemoteModifier.size(userVar.asRemoteDp(), rawVar.asRemoteDp()).semantics {
                        contentDescription = "box".rs
                    }
            )
        }

        val playerState = RcPlayerState(document)

        // "user_var" resolves to "USER:user_var"
        var userVar by playerState.floatState("user_var")
        assertThat(userVar).isEqualTo(15f)
        userVar = 40f
        assertThat(userVar).isEqualTo(40f)

        // "raw_var" without prefix in document resolves to "raw_var"
        var rawVar by playerState.floatState("raw_var")
        assertThat(rawVar).isEqualTo(25f)
        rawVar = 50f
        assertThat(rawVar).isEqualTo(50f)

        // Explicit fully-qualified name with colon
        var qualifiedUserVar by playerState.floatState("USER:user_var")
        qualifiedUserVar = 60f
        assertThat(userVar).isEqualTo(60f)
    }

    @Test
    fun testNameResolutionWithNullDefaultPrefix() {
        val document = createDocument {
            val rawVar =
                rememberNamedRemoteFloat("unprefixed_var", domain = RemoteState.Domain.None) {
                    100f.rf
                }
            RemoteBox(modifier = RemoteModifier.size(rawVar.asRemoteDp(), 10.rdp))
        }

        // defaultPrefix = null means no prefix and no colon
        val playerState = RcPlayerState(document, defaultPrefix = null)
        var unprefixedVar by playerState.floatState("unprefixed_var")
        assertThat(unprefixedVar).isEqualTo(100f)

        unprefixedVar = 150f
        assertThat(unprefixedVar).isEqualTo(150f)
    }

    @Test
    fun testBitmapState() {
        val initialAndroidBitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val initialBitmap = initialAndroidBitmap.asImageBitmap()
        val document = createDocument {
            val remoteImage = rememberNamedRemoteImageBitmap("custom_image") { initialBitmap }
            RemoteImage(remoteBitmap = remoteImage, contentDescription = "image".rs)
        }

        val playerState = RcPlayerState(document)
        var customImage by playerState.bitmapState("custom_image")

        val retrievedAndroidBitmap = customImage
        assertThat(retrievedAndroidBitmap).isNotNull()
        assertThat(retrievedAndroidBitmap!!.sameAs(initialAndroidBitmap)).isTrue()

        val newBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        customImage = newBitmap
        assertThat(customImage).isEqualTo(newBitmap)

        playerState.clearOverride("custom_image")
        assertThat(customImage!!.sameAs(initialAndroidBitmap)).isTrue()

        // Also test clearing via setting null
        customImage = newBitmap
        assertThat(customImage).isEqualTo(newBitmap)
        customImage = null
        assertThat(customImage!!.sameAs(initialAndroidBitmap)).isTrue()
    }

    @Test
    fun testFloatArrayState() {
        val document = createDocument {
            val chartData = createNamedRemoteFloatArray("chartData", floatArrayOf(10f, 20f, 30f))
            RemoteBox(modifier = RemoteModifier.size(chartData[0].asRemoteDp()))
        }

        val playerState = RcPlayerState(document)
        var chartData by playerState.floatArrayState("chartData")
        assertThat(chartData).isEqualTo(floatArrayOf(10f, 20f, 30f))

        val updated = floatArrayOf(40f, 50f, 60f)
        chartData = updated
        assertThat(chartData).isEqualTo(updated)

        playerState.clearOverride("chartData")
        assertThat(chartData).isEqualTo(floatArrayOf(10f, 20f, 30f))
    }
}
