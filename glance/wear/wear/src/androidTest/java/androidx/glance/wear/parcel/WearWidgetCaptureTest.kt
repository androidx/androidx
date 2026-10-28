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

package androidx.glance.wear.parcel

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.core.os.BundleCompat
import androidx.glance.wear.GlanceWearProfiles
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.WearWidgetContentTooLargeException
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.RendererVersion
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WearWidgetRawContent
import androidx.glance.wear.core.WidgetInstanceId
import androidx.glance.wear.createNoiseBitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class WearWidgetCaptureTest {

    @get:Rule val composeTestRule = createComposeRule()

    companion object {
        val context: Context = ApplicationProvider.getApplicationContext()
        val requestCode0 = 1234
        val requestCode1 = 5678

        // Helper to create PendingIntents consistently for layout and assertions
        private fun createTestPendingIntent(
            localContext: Context,
            requestCode: Int,
        ): PendingIntent {
            return PendingIntent.getActivity(
                localContext,
                requestCode,
                Intent(),
                PendingIntent.FLAG_IMMUTABLE,
            )
        }

        @Composable
        @RemoteComposable
        fun TestLayout() {
            RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
                RemoteBox(modifier = RemoteModifier.size(100.rdp))
                RemoteText(
                    text = "text-0".rs,
                    modifier =
                        RemoteModifier.clickable(
                            pendingIntentAction { localContext ->
                                createTestPendingIntent(localContext, requestCode0)
                            }
                        ),
                )
                RemoteText(
                    text = "text-1".rs,
                    modifier =
                        RemoteModifier.clickable(
                            pendingIntentAction { localContext ->
                                createTestPendingIntent(localContext, requestCode1)
                            }
                        ),
                )
            }
        }

        @Composable
        internal fun CaptureWidgetContentData(content: @Composable @RemoteComposable () -> Unit) {
            val creationDisplayInfo =
                RemoteCreationDisplayInfo(400, 400, LocalConfiguration.current.densityDpi)
            val context = LocalContext.current
            val data = remember { mutableStateOf<WearWidgetRawContent?>(null) }
            LaunchedEffect(Unit) {
                data.value =
                    WearWidgetCapture.capture(
                        context,
                        creationDisplayInfo,
                        RendererVersion.SAFE_FALLBACK_SUPPORTED_OPERATIONS,
                        content,
                    )
            }

            Column {
                BasicText(
                    modifier =
                        androidx.compose.ui.Modifier.semantics { contentDescription = "Document" },
                    text =
                        data.value?.rcDocument?.let { rcDoc ->
                            RemoteDocument(rcDoc).document.displayHierarchy()
                        } ?: "",
                )
                BasicText(
                    modifier =
                        androidx.compose.ui.Modifier.semantics {
                            contentDescription = "PendingIntent 0"
                        },
                    text = data.value?.extras?.getPendingIntent(0)?.toString() ?: "",
                )
                BasicText(
                    modifier =
                        androidx.compose.ui.Modifier.semantics {
                            contentDescription = "PendingIntent 1"
                        },
                    text = data.value?.extras?.getPendingIntent(1)?.toString() ?: "",
                )
            }
        }

        private fun SemanticsNodeInteraction.assertTextMatches(expected: String) {
            val text =
                fetchSemanticsNode()
                    .config
                    .getOrNull(SemanticsProperties.Text)
                    ?.firstOrNull()
                    ?.toString()
            assertThat(text!!.normalizeWhiteSpace()).isEqualTo(expected.normalizeWhiteSpace())
        }

        /**
         * Asserts that the node's text matches the provided PendingIntent, ignoring the unstable
         * identity hash code but matching the system token.
         */
        private fun SemanticsNodeInteraction.assertPendingIntentMatch(
            expected: PendingIntent
        ): SemanticsNodeInteraction {
            // Extract "android.os.BinderProxy@..." from "PendingIntent{hash:
            // android.os.BinderProxy@...}"
            val stableToken = expected.toString().substringAfter(": ")
            val pattern = Regex("PendingIntent\\{.*: ${Regex.escape(stableToken)}")

            return this.assert(
                SemanticsMatcher("matches PendingIntent with token $stableToken") { node ->
                    node.config.getOrNull(SemanticsProperties.Text)?.any {
                        pattern.containsMatchIn(it.text)
                    } ?: false
                }
            )
        }

        // Replace all sequences of whitespace (including newlines, tabs) with a single space. Then
        // trim leading/trailing spaces from the whole string
        private fun String.normalizeWhiteSpace() = this.replace(Regex("``s+"), " ").trim()

        private fun Bundle.getPendingIntent(index: Int): PendingIntent? {
            val pendingIntents =
                BundleCompat.getParcelable(
                    this,
                    WearWidgetCapture.PENDING_INTENT_KEY,
                    Bundle::class.java,
                )
            return BundleCompat.getParcelable(
                pendingIntents!!,
                index.toString(),
                PendingIntent::class.java,
            )
        }
    }

    @Test
    fun pendingIntentCollection() = runTest {
        val creationDisplayInfo =
            RemoteCreationDisplayInfo(400, 400, context.resources.configuration.densityDpi)
        val profile = GlanceWearProfiles.createWearWidgetsProfile()
        val result =
            captureSingleRemoteDocument(
                creationDisplayInfo = creationDisplayInfo,
                context = context,
                profile = profile,
            ) {
                TestLayout()
            }

        val pendingIntents = result.pendingIntents

        assertThat(pendingIntents.size).isEqualTo(2)
        assertThat(pendingIntents[0]).isEqualTo(createTestPendingIntent(context, requestCode0))
        assertThat(pendingIntents[1]).isEqualTo(createTestPendingIntent(context, requestCode1))
    }

    @Test
    fun pendingIntentCollection_addToBundle() {
        val result =
            """
ROOT [-2:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE
  COLUMN [-3:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE
    DATA_TEXT<42> = "text-0"
    DATA_TEXT<45> = "text-1"
    MODIFIERS
    BOX [-5:-1] = [0.0, 0.0, 100.0, 100.0] VISIBLE
      MODIFIERS
        WIDTH = 100.0 dp
        HEIGHT = 100.0 dp
    TEXT_LAYOUT [-7:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE (42:"null")
      MODIFIERS
        CLICK_MODIFIER
          HOST_NAMED_ACTION = 44 : 43
        SEMANTICS = SEMANTICS BUTTON
    TEXT_LAYOUT [-9:-1] = [0.0, 0.0, 0.0, 0.0] VISIBLE (45:"null")
      MODIFIERS
        CLICK_MODIFIER
          HOST_NAMED_ACTION = 44 : 46
        SEMANTICS = SEMANTICS BUTTON
"""

        composeTestRule.setContent { CaptureWidgetContentData { TestLayout() } }

        composeTestRule.onNodeWithContentDescription("Document").assertTextMatches(result)
        composeTestRule
            .onNodeWithContentDescription("PendingIntent 0")
            .assertPendingIntentMatch(createTestPendingIntent(context, requestCode0))
        composeTestRule
            .onNodeWithContentDescription("PendingIntent 1")
            .assertPendingIntentMatch(createTestPendingIntent(context, requestCode1))
    }

    @Test
    fun capture_largeContent_doesNotCheckSize() = runTest {
        val creationDisplayInfo =
            RemoteCreationDisplayInfo(400, 400, context.resources.configuration.densityDpi)
        // Random noise doesn't compress, so this bitmap is embedded as a ~750 KB PNG.
        val noiseBitmap = createNoiseBitmap(width = 500, height = 500).asImageBitmap()

        // The size is checked by GlanceWearWidget.provideWidgetDataAsRawContent, so that it also
        // applies to custom WearWidgetData implementations.
        val rawContent =
            WearWidgetCapture.capture(
                context,
                creationDisplayInfo,
                RendererVersion.SAFE_FALLBACK_SUPPORTED_OPERATIONS,
            ) {
                RemoteImage(
                    bitmap = RemoteImageBitmap(noiseBitmap),
                    contentDescription = null,
                    modifier = RemoteModifier.size(60.rdp),
                )
            }

        assertThat(rawContent.rcDocument.size)
            .isGreaterThan(WearWidgetContentTooLargeException.MAX_CONTENT_SIZE_BYTES)
    }

    @Test
    fun provideWidgetDataAsRawContent_contentTooLarge_throws() = runTest {
        // Random noise doesn't compress, so this bitmap is embedded as a ~750 KB PNG.
        val noiseBitmap = createNoiseBitmap(width = 500, height = 500).asImageBitmap()
        val widget =
            object : GlanceWearWidget() {
                override suspend fun provideWidgetData(
                    context: Context,
                    params: WearWidgetParams,
                ): WearWidgetData =
                    WearWidgetDocument(background = WearWidgetBrush) {
                        RemoteImage(
                            bitmap = RemoteImageBitmap(noiseBitmap),
                            contentDescription = null,
                            modifier = RemoteModifier.size(60.rdp),
                        )
                    }
            }
        val params =
            WearWidgetParams(
                instanceId = WidgetInstanceId("namespace", 17),
                containerType = ContainerInfo.CONTAINER_TYPE_LARGE,
                widthDp = 200f,
                heightDp = 200f,
                horizontalPaddingDp = 0f,
                verticalPaddingDp = 0f,
                cornerRadiusDp = 0f,
            )

        val exception = runCatching {
            widget.provideWidgetDataAsRawContent(context, params)
        }
            .exceptionOrNull()

        assertThat(exception).isInstanceOf(WearWidgetContentTooLargeException::class.java)
        val tooLargeException = exception as WearWidgetContentTooLargeException
        assertThat(tooLargeException.contentSizeBytes)
            .isGreaterThan(WearWidgetContentTooLargeException.MAX_CONTENT_SIZE_BYTES)
    }
}
