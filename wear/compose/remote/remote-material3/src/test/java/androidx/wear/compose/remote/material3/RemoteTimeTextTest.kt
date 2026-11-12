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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.DrawArc
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.util.TestProfiles
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteTimeTextTest {
    @get:Rule val captureRule = RemoteCaptureTestRule()

    @get:Rule val composeTestRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun timeOnly() = runTest {
        runDocumentTest {
            RemoteTimeText(modifier = RemoteModifier.fillMaxSize(), time = "10:09".rs)
        }
    }

    @Test
    fun timeWithText() = runTest {
        runDocumentTest {
            RemoteTimeText(
                modifier = RemoteModifier.fillMaxSize(),
                time = "10:09".rs,
                leadingText = "paused".rs,
                trailingText = "eta 13 min".rs,
            )
        }
    }

    @Test
    fun withFontConfigured() = runTest {
        runDocumentTest {
            RemoteTimeText(
                modifier = RemoteModifier.fillMaxSize(),
                time = "10:09".rs,
                fontSize = 15.rsp,
                fontFamily = RemoteFontFamily.SansSerif,
            )
        }
    }

    @Test
    fun background_isDrawnBehindTheText() {
        val operations = captureOperations {
            RemoteTimeText(modifier = RemoteModifier.fillMaxSize(), time = "10:09".rs)
        }

        val arcIndex = operations.indexOfFirst { it is DrawArc }
        val textIndex = operations.indexOfFirst { it is DrawTextOnCircle }
        assertThat(operations.count { it is DrawArc }).isEqualTo(1)
        assertThat(arcIndex).isAtLeast(0)
        assertThat(textIndex).isGreaterThan(arcIndex)
        // The background sweeps as far as the measured text.
        assertThat(operations.any { it is TextAttribute }).isTrue()
    }

    @Test
    fun transparentBackground_isNotDrawn() {
        val operations = captureOperations {
            RemoteTimeText(
                modifier = RemoteModifier.fillMaxSize(),
                time = "10:09".rs,
                backgroundColor = RemoteColor.Transparent,
            )
        }

        assertThat(operations.any { it is DrawArc }).isFalse()
        assertThat(operations.any { it is TextAttribute }).isFalse()
        assertThat(operations.any { it is DrawTextOnCircle }).isTrue()
    }

    @Test
    fun backgroundColor_defaultsToBackgroundWithAlpha() {
        lateinit var backgroundColor: RemoteColor
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme = RemoteColorScheme().copy(background = RemoteColor.Blue)
            ) {
                backgroundColor = RemoteTimeTextDefaults.backgroundColor
            }
        }

        assertThat(backgroundColor.constantValue)
            .isEqualTo(RemoteColor.Blue.copy(alpha = 0.85f.rf).constantValue)
    }

    /** Captures [content] and returns its operations, including the operations of containers. */
    private fun captureOperations(
        content: @Composable @RemoteComposable () -> Unit
    ): List<Operation> {
        val document = runBlocking {
            captureRule.captureDocument(
                context = context,
                profile = TestProfiles.androidNativeProfile,
                content = content,
            )
        }
        return document.operations.flatten()
    }

    private fun List<Operation>.flatten(): List<Operation> = flatMap { operation ->
        if (operation is Container) listOf(operation) + operation.list.flatten()
        else listOf(operation)
    }

    private suspend fun runDocumentTest(content: @Composable @RemoteComposable () -> Unit) {
        val bytes =
            captureRule
                .captureRecordedDocument(
                    context = context,
                    profile = TestProfiles.androidNativeProfile,
                    content = content,
                )
                .bytes
        assertThat(bytes).isNotEmpty()
    }
}
