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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.dsl.Modifier as RcModifier
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcImeAction
import androidx.compose.remote.creation.dsl.RcInputFilter
import androidx.compose.remote.creation.dsl.RcKeyboardType
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.fillMaxWidth
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Text input played by the embedded [RcPlayer]: the text component becomes a text field. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerTextInputTest {
    @get:Rule val rule = RcPlayerTestRule()

    private fun document(
        commitOnEveryKeystroke: Boolean = true,
        filter: RcInputFilter = RcInputFilter.None,
        maxLength: Int = 0,
        singleLine: Boolean = true,
        keyboardType: RcKeyboardType = RcKeyboardType.Text,
    ): CoreDocument {
        val bytes =
            createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX, isExperimental = true)) {
                val name = remoteText("")
                Column(
                    modifier = RcModifier.fillMaxSize(),
                    horizontal = RcHorizontalPositioning.Start,
                    vertical = RcColumnVerticalPositioning.Top,
                ) {
                    EditableText(
                        text = name,
                        modifier = RcModifier.fillMaxWidth(),
                        keyboardType = keyboardType,
                        imeAction = RcImeAction.Done,
                        singleLine = singleLine,
                        commitOnEveryKeystroke = commitOnEveryKeystroke,
                        filter = filter,
                        maxLength = maxLength,
                        hint = remoteText("Your name"),
                    )
                    Text(text = "Hello " join name, maxLines = 5)
                }
            }
        return CoreDocument().apply {
            initFromBuffer(RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes)))
        }
    }

    private fun play(document: CoreDocument) {
        rule.setContent { Box(Modifier.size(300.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()
    }

    @Test
    fun editableText_rendersAsTextField_withHint() {
        play(document())

        rule.onNode(hasSetTextAction()).assertIsDisplayed()
        rule.onNodeWithText("Your name").assertIsDisplayed()
        rule.onNodeWithText("Hello ").assertIsDisplayed()
    }

    @Test
    fun typing_updatesTheBoundString_onEveryKeystroke() {
        play(document())

        rule.onNode(hasSetTextAction()).performTextInput("Nicolas")
        rule.waitForIdle()

        rule.onNodeWithText("Hello Nicolas").assertIsDisplayed()
    }

    @Test
    fun commitOnDone_updatesTheBoundString_onlyOnImeAction() {
        play(document(commitOnEveryKeystroke = false))

        rule.onNode(hasSetTextAction()).performTextInput("Nicolas")
        rule.waitForIdle()
        rule.onNodeWithText("Hello ").assertIsDisplayed()
        rule.onNode(hasText("Hello Nicolas")).assertDoesNotExist()

        rule.onNode(hasSetTextAction()).performImeAction()
        rule.waitForIdle()
        rule.onNodeWithText("Hello Nicolas").assertIsDisplayed()
    }

    @Test
    fun filterAndMaxLength_areApplied() {
        play(document(filter = RcInputFilter.Digits, maxLength = 3))

        rule.onNode(hasSetTextAction()).performTextInput("4a2b1c9")
        rule.waitForIdle()

        rule.onNodeWithText("Hello 421").assertIsDisplayed()
    }

    @Test
    fun multiLine_acceptsNewlines() {
        play(document(singleLine = false))

        rule.onNode(hasSetTextAction()).performTextInput("line one\nline two")
        rule.waitForIdle()

        rule.onNodeWithText("Hello line one\nline two").assertIsDisplayed()
    }

    @Test
    fun singleLine_dropsNewlines() {
        play(document(singleLine = true))

        rule.onNode(hasSetTextAction()).performTextInput("line one\nline two")
        rule.waitForIdle()

        rule.onNode(hasText("Hello line one\nline two")).assertDoesNotExist()
    }

    @OptIn(ExperimentalRemotePlayerApi::class)
    @Test
    fun textInputLayout_isExecutedBuiltIn_notReportedAsUnsupported() {
        val experimental = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
        val unsupported = RcPlayerFeatureSet.Empty.unsupportedOpcodes(experimental)

        assertThat(Operations.LAYOUT_TEXT_INPUT in unsupported).isFalse()
    }

    @Test
    fun passwordField_isObfuscated_butBindsTheRealText() {
        play(document(keyboardType = RcKeyboardType.Password))

        rule.onNode(hasSetTextAction()).performTextInput("secret")
        rule.waitForIdle()

        // The bound string receives the actual text...
        rule.onNodeWithText("Hello secret").assertIsDisplayed()
        // ...while the field itself is a secure (masked) field.
        rule
            .onNode(hasSetTextAction())
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }
}
