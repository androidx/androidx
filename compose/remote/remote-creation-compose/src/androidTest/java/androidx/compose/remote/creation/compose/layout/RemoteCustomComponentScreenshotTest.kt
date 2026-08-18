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

package androidx.compose.remote.creation.compose.layout

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.test.base.GridScreenshotUI
import androidx.compose.remote.creation.compose.util.TestProfiles
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.remote.player.core.platform.AndroidComponentSupport
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RemoteCustomComponentScreenshotTest {
    @get:Rule
    val composeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val gridScreenshotUI = GridScreenshotUI()

    private fun getTests(): List<Pair<String, @RemoteComposable @Composable () -> Unit>> =
        listOf(
            "int_prop" to @Composable @RemoteComposable { IntProp() },
            "color_prop" to @Composable @RemoteComposable { ColorProp() },
            "float_prop" to @Composable @RemoteComposable { FloatProp() },
            "dp_prop" to @Composable @RemoteComposable { DpProp() },
            "string_prop" to @Composable @RemoteComposable { StringProp() },
        )

    @Test
    fun grid() {
        val customSupport =
            AndroidCustomContextImpl(
                initialDelegates = mapOf("SupportAllProperties" to SupportAllProperties())
            )
        composeTestRule.runScreenshotTest(
            profile = TestProfiles.androidXExperimental,
            customSupport = customSupport,
        ) {
            gridScreenshotUI.GridContent(getTests())
        }
    }

    @Test
    fun grid_captureSingleRemoteDocument() = runTest {
        val customSupport =
            AndroidCustomContextImpl(
                initialDelegates = mapOf("SupportAllProperties" to SupportAllProperties())
            )
        val context = ApplicationProvider.getApplicationContext<Context>()
        val creationDisplayInfo = composeTestRule.remoteCreationDisplayInfo
        val profile = TestProfiles.androidXExperimental

        val captured =
            captureSingleRemoteDocument(
                context = context,
                creationDisplayInfo = creationDisplayInfo,
                profile = profile,
            ) {
                gridScreenshotUI.GridContent(getTests())
            }

        val doc =
            CoreDocument().apply {
                ByteArrayInputStream(captured.bytes).use {
                    val buffer = RemoteComposeBuffer.fromInputStream(it)
                    buffer.setVersion(
                        profile.apiLevel,
                        profile.operationsProfiles,
                        profile.supportedOperations,
                    )
                    initFromBuffer(buffer)
                }
            }

        composeTestRule.composeTestRule.setContent {
            Box(
                Modifier.requiredSize(creationDisplayInfo.widthDp, creationDisplayInfo.heightDp)
                    .testTag(RemoteScreenshotTestRule.ROOT_TEST_TAG)
            ) {
                RemoteDocumentPlayer(
                    document = doc,
                    documentWidth = creationDisplayInfo.size.width.toInt(),
                    documentHeight = creationDisplayInfo.size.height.toInt(),
                    customSupport = customSupport,
                )
            }
        }

        composeTestRule.verifyScreenshot()
    }

    @Composable
    @RemoteComposable
    private fun IntProp() {
        RemoteCustomComponent(
            name = "SupportAllProperties",
            modifier = RemoteModifier.size(80.rdp, 30.rdp),
        ) {
            property(SupportAllProperties.PROP_INT.toInt(), 42)
        }
    }

    @Composable
    @RemoteComposable
    private fun ColorProp() {
        RemoteCustomComponent(
            name = "SupportAllProperties",
            modifier = RemoteModifier.size(80.rdp, 30.rdp),
        ) {
            property(SupportAllProperties.PROP_COLOR.toInt(), Color(0xFFFF0000))
        }
    }

    @Composable
    @RemoteComposable
    private fun FloatProp() {
        RemoteCustomComponent(
            name = "SupportAllProperties",
            modifier = RemoteModifier.size(80.rdp, 30.rdp),
        ) {
            property(SupportAllProperties.PROP_FLOAT.toInt(), 3.14f.rf)
        }
    }

    @Composable
    @RemoteComposable
    private fun DpProp() {
        RemoteCustomComponent(
            name = "SupportAllProperties",
            modifier = RemoteModifier.size(80.rdp, 30.rdp),
        ) {
            property(SupportAllProperties.PROP_FLOAT.toInt(), 16.rdp)
        }
    }

    @Composable
    @RemoteComposable
    private fun StringProp() {
        RemoteCustomComponent(
            name = "SupportAllProperties",
            modifier = RemoteModifier.size(80.rdp, 30.rdp),
        ) {
            property(SupportAllProperties.PROP_STRING.toInt(), "Hello".rs)
        }
    }

    @SuppressLint("RestrictedApiAndroidX")
    private class SupportAllProperties : AndroidComponentSupport {
        companion object {
            const val PROP_STRING: Short = 1
            const val PROP_INT: Short = 2
            const val PROP_FLOAT: Short = 3
            const val PROP_COLOR: Short = 4
        }

        override fun createView(context: Context): View {
            val textView =
                TextView(context).apply {
                    isSingleLine = false
                    layoutParams = FrameLayout.LayoutParams(0, 0)
                }
            return textView
        }

        override fun configure(view: View, type: Int, value: String) {
            if (view is TextView && type == PROP_STRING.toInt()) {
                view.text = value
            }
        }

        override fun configure(view: View, type: Int, value: Int) {
            if (view is TextView) {
                when (type) {
                    PROP_COLOR.toInt() -> {
                        view.text = Integer.toHexString(value).uppercase()
                    }
                    PROP_INT.toInt() -> {
                        view.text = value.toString()
                    }
                }
            }
        }

        override fun configure(view: View, type: Int, value: Float) {
            if (view is TextView && type == PROP_FLOAT.toInt()) {
                view.text = value.toString()
            }
        }
    }
}
