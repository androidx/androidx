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

import android.view.KeyEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.CreationDisplayInfo
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RemoteComposePlayerFlags
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** The Remote Compose players, for tests that run against each of them. */
enum class PlayerUnderTest {
    /** The embedded player, `RcPlayer`, which plays documents with Compose. */
    Embedded,

    /** The View player, `RemoteComposePlayer`, hosted in Compose by `RemoteDocumentPlayer`. */
    View;

    companion object {
        /** The players, as parameters for `ParameterizedRobolectricTestRunner`. */
        @JvmStatic fun parameters(): List<Array<Any>> = entries.map { arrayOf(it) }
    }
}

/**
 * Plays documents with [player] in a Compose host, and drives them with input dispatched through
 * the host window, so the same test runs against both players. Interactions are observed through
 * the host named actions the document runs ([takeNamedActions]).
 */
@OptIn(ExperimentalRemotePlayerApi::class, ExperimentalComposeUiApi::class)
class RemotePlayerTestRule(val player: PlayerUnderTest) : TestRule {

    val composeRule: ComposeContentTestRule = createComposeRule()

    private val namedActions = mutableListOf<Pair<String, Any?>>()
    private lateinit var hostView: View
    private lateinit var inputModeManager: InputModeManager

    override fun apply(base: Statement, description: Description): Statement =
        composeRule.apply(
            object : Statement() {
                override fun evaluate() {
                    val previous = RemoteComposePlayerFlags.isEmbeddedPlayerEnabled
                    try {
                        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled =
                            player == PlayerUnderTest.Embedded
                        base.evaluate()
                    } finally {
                        RemoteComposePlayerFlags.isEmbeddedPlayerEnabled = previous
                    }
                }
            },
            description,
        )

    /** Plays [document], sized [width] x [height] dp (pixels, at the test density of 1). */
    fun play(document: CoreDocument, width: Int, height: Int) {
        composeRule.setContent {
            hostView = LocalView.current
            inputModeManager = LocalInputModeManager.current
            Box(Modifier.size(width.dp, height.dp)) {
                when (player) {
                    PlayerUnderTest.Embedded ->
                        RcPlayer(
                            document = document,
                            onNamedAction = { name, value, _ -> namedActions += name to value },
                        )
                    PlayerUnderTest.View ->
                        RemoteDocumentPlayer(
                            document = document,
                            documentWidth = width,
                            documentHeight = height,
                            onNamedAction = { name, value, _ -> namedActions += name to value },
                        )
                }
            }
        }
        // Start in non-touch mode, as when navigating with a hardware keyboard, where Compose
        // clickables are focusable.
        setTouchMode(false)
        if (player == PlayerUnderTest.View) {
            // Trigger a draw pass so the View-backed player lays out its RemoteCompose components.
            composeRule.onRoot().captureToImage()
        }
    }

    /** Switches the window, and Compose's input mode, in or out of touch mode. */
    private fun setTouchMode(inTouchMode: Boolean) {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(inTouchMode)
        composeRule.runOnIdle {
            inputModeManager.requestInputMode(
                if (inTouchMode) InputMode.Touch else InputMode.Keyboard
            )
        }
        composeRule.waitForIdle()
    }

    /**
     * Gives the host view focus, as the window does when focus moves into it. Both players then
     * focus their first focus target.
     */
    fun focusHost() {
        composeRule.runOnIdle { hostView.requestFocus() }
        composeRule.waitForIdle()
    }

    /**
     * Presses and releases [keyCode] through the host window.
     *
     * @return whether the key down was handled.
     */
    fun pressKey(keyCode: Int, metaState: Int = 0): Boolean {
        // A hardware key press puts the window in non-touch mode.
        setTouchMode(false)
        val handled = composeRule.runOnIdle {
            val root = hostView.rootView
            val handled =
                root.dispatchKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, metaState))
            root.dispatchKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0, metaState))
            handled
        }
        composeRule.waitForIdle()
        return handled
    }

    /** Taps at ([x], [y]) in the host. */
    fun tap(x: Float, y: Float) {
        // A touch puts the window in touch mode.
        setTouchMode(true)
        composeRule.onRoot().performTouchInput { click(Offset(x, y)) }
        composeRule.waitForIdle()
    }

    /** Returns the named actions run since the last call, as (name, value) pairs. */
    fun takeNamedActions(): List<Pair<String, Any?>> = composeRule.runOnIdle {
        namedActions.toList().also { namedActions.clear() }
    }
}

/**
 * Writes a [width] x [height] document with [content] using the experimental AndroidX profile,
 * which has the event and focus operations, and inflates it.
 */
fun experimentalDocument(
    width: Int,
    height: Int,
    content: (RemoteComposeWriter) -> Unit,
): CoreDocument {
    val profile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
        }
    val writer = profile.create(CreationDisplayInfo(width, height, 160), "test")
    writer.root { content(writer) }
    return CoreDocument(RemoteClock.SYSTEM).apply {
        ByteArrayInputStream(writer.buffer(), 0, writer.bufferSize()).use {
            initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
        }
    }
}
