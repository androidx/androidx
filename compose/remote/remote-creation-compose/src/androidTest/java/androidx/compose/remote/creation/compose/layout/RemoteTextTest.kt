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

import android.content.Context
import android.content.res.Configuration
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.test.R.string.long_text_punctuation
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.remote.player.compose.test.utils.plus
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RemoteTextTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun remoteText_rtl_punctuation_pseudolocale() {
        val width = 300
        val height = 200

        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            creationComposableWrapper = { content ->
                val context = LocalContext.current
                val configuration = LocalConfiguration.current
                val localizedContext =
                    remember(context, configuration) {
                        val newConfig =
                            Configuration(configuration).apply {
                                setLocale(Locale.forLanguageTag("ar-XB"))
                                setLayoutDirection(Locale.forLanguageTag("ar-XB"))
                            }
                        context.createConfigurationContext(newConfig)
                    }
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                ) {
                    content()
                }
            },
            playComposableWrapper = ComposableWrappers.blackBackground + ComposableWrappers.rtl,
        ) {
            val text = stringResource(long_text_punctuation).rs
            RemoteColumn(modifier = RemoteModifier.size(width.rdp, height.rdp)) {
                RemoteText(
                    text = text,
                    color = Color.White.rc,
                    fontSize = 14.rsp,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
