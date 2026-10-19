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

package androidx.wear.compose.remote.material3

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.embedded.RcPlayerState
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.util.EnableEmbeddedPlayerRule
import kotlinx.coroutines.runBlocking
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that captures a Remote Compose document, renders it in the embedded [RcPlayer], and
 * exposes a [DocumentPainter] that advances the Compose test clock and reads the rendered pixels.
 */
class DocumentPainterRule(val canvasSize: Float = DefaultCanvasSize) : TestRule {
    private val enableEmbeddedPlayerRule = EnableEmbeddedPlayerRule()
    private val captureRule = RemoteCaptureTestRule()
    private val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val playerState = mutableStateOf<RcPlayerState?>(null)
    private var contentSet = false

    override fun apply(base: Statement, description: Description): Statement =
        RuleChain.outerRule(enableEmbeddedPlayerRule)
            .around(captureRule)
            .around(composeRule)
            .apply(base, description)

    /**
     * Captures [content] into a Remote Compose document with [clock] and renders it in [RcPlayer]
     * with the Compose test clock paused.
     */
    fun paintDocument(
        clock: RemoteClock = RemoteClock.SYSTEM,
        content: @Composable @RemoteComposable () -> Unit,
    ): DocumentPainter {
        val displayInfo = createCreationDisplayInfo(context, Size(canvasSize, canvasSize))
        val document = runBlocking {
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = displayInfo,
                clock = clock,
                content = content,
            )
        }
        composeRule.mainClock.autoAdvance = false
        val state = RcPlayerState(document)
        playerState.value = state
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                val currentState = playerState.value
                Box(
                    modifier =
                        Modifier.size(displayInfo.widthDp, displayInfo.heightDp).testTag(PlayerTag)
                ) {
                    if (currentState != null) {
                        key(currentState) { RcPlayer(state = currentState) }
                    }
                }
            }
        } else {
            Snapshot.sendApplyNotifications()
        }
        composeRule.mainClock.advanceTimeByFrame()
        return DocumentPainter(state)
    }

    /** Captures [content] into a serialized Remote Compose document byte array. */
    fun captureBytes(content: @Composable @RemoteComposable () -> Unit): ByteArray = runBlocking {
        captureSingleRemoteDocument(context = context, content = content).bytes
    }

    /** Captures [content] into a Remote Compose document, without rendering it. */
    fun captureDocument(content: @Composable @RemoteComposable () -> Unit): CoreDocument =
        runBlocking {
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo =
                    createCreationDisplayInfo(context, Size(canvasSize, canvasSize)),
                content = content,
            )
        }

    /**
     * Drives the embedded [RcPlayer] rendering [state], advancing the Compose test clock and
     * reading pixels from the player's captured image.
     */
    inner class DocumentPainter internal constructor(val state: RcPlayerState) {
        private var cachedBitmap: Bitmap? = null
        private var cachedPixels: IntArray? = null

        private val bitmap: Bitmap
            get() =
                cachedBitmap
                    ?: composeRule
                        .onNodeWithTag(PlayerTag)
                        .captureToImage()
                        .asAndroidBitmap()
                        .also { cachedBitmap = it }

        private val pixels: IntArray
            get() =
                cachedPixels
                    ?: IntArray(bitmap.width * bitmap.height).also {
                        bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                        cachedPixels = it
                    }

        val width: Int
            get() = bitmap.width

        val height: Int
            get() = bitmap.height

        /** Advances the Compose test clock by [millis] milliseconds. */
        fun advanceTimeBy(millis: Long) {
            invalidate()
            composeRule.mainClock.advanceTimeBy(millis)
        }

        /** Updates the named float variable [name] to [value] and advances a frame. */
        fun setFloat(name: String, value: Float) {
            invalidate()
            state.floatState(name).value = value
            Snapshot.sendApplyNotifications()
            composeRule.mainClock.advanceTimeByFrame()
        }

        /** Updates the named boolean variable [name] to [value] and advances a frame. */
        fun setBoolean(name: String, value: Boolean) {
            invalidate()
            state.booleanState(name).value = value
            Snapshot.sendApplyNotifications()
            composeRule.mainClock.advanceTimeByFrame()
        }

        fun colorAt(x: Int, y: Int): Color = Color(pixels[y * width + x])

        fun countPixels(predicate: (Color) -> Boolean): Int = pixels.count { predicate(Color(it)) }

        /** Returns the indices of the pixels whose color matches [predicate]. */
        fun pixelsWhere(predicate: (Color) -> Boolean): Set<Int> =
            pixels.indices.filterTo(HashSet()) { predicate(Color(pixels[it])) }

        private fun invalidate() {
            cachedBitmap = null
            cachedPixels = null
        }
    }

    companion object {
        const val DefaultCanvasSize = 500f
        private const val PlayerTag = "player"
    }
}
