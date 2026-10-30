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
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Verifies the font axes that [RemoteText] writes into the document for a [TextStyle]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteTextFontSettingsTest {
    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun variableFontWithFontFeatureSettings_writesFontAxes() = runTest {
        // Repro from b/568283088.
        val style =
            TextStyle(
                fontFamily =
                    FontFamily(
                        Font(
                            DeviceFontFamilyName("google-sans-flex"),
                            variationSettings =
                                FontVariation.Settings(FontVariation.Setting("ROND", 100f)),
                        )
                    ),
                fontFeatureSettings = "pnum",
            )

        val axes = captureTextAxes(RemoteTextStyle.fromTextStyle(style))

        // The font axes are kept. The document format has no separate channel for OpenType
        // features, so RemoteText still writes them after the font axes.
        assertThat(axes).containsExactly("ROND", 100f, "pnum", 1f).inOrder()
    }

    @Test
    @OptIn(ExperimentalTextApi::class)
    fun resourceVariableFontWithFontFeatureSettings_writesFontAxes() = runTest {
        // Exact repro from b/568283088, which uses a resource font (not an AndroidFont).
        val style =
            TextStyle(
                fontFamily =
                    FontFamily(
                        Font(
                            resId = 1,
                            variationSettings =
                                FontVariation.Settings(FontVariation.Setting("ROND", 100f)),
                        )
                    ),
                fontFeatureSettings = "pnum",
            )

        val axes = captureTextAxes(RemoteTextStyle.fromTextStyle(style))

        assertThat(axes).containsExactly("ROND", 100f, "pnum", 1f).inOrder()
    }

    @Test
    fun multiWeightFamily_writesAxesOfMatchingWeight() = runTest {
        val style =
            TextStyle(
                fontFamily =
                    FontFamily(
                        Font(
                            DeviceFontFamilyName("roboto-flex"),
                            weight = FontWeight.Normal,
                            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
                        ),
                        Font(
                            DeviceFontFamilyName("roboto-flex"),
                            weight = FontWeight.Bold,
                            variationSettings = FontVariation.Settings(FontVariation.weight(700)),
                        ),
                    ),
                fontWeight = FontWeight.Bold,
            )

        val axes = captureTextAxes(RemoteTextStyle.fromTextStyle(style))

        assertThat(axes).containsEntry("wght", 700f)
    }

    @Test
    fun fontVariationSettingsParameter_writesFontAxesInOrder() = runTest {
        val axes = captureTextAxes {
            RemoteText(
                "Text".rs,
                fontVariationSettings =
                    FontVariation.Settings(FontVariation.weight(300), FontVariation.width(70f)),
            )
        }

        assertThat(axes).containsExactly("wght", 300f, "wdth", 70f).inOrder()
    }

    @Test
    fun fontVariationSettingsParameter_replacesStyleFontVariationSettings() = runTest {
        val axes = captureTextAxes {
            RemoteText(
                "Text".rs,
                style =
                    RemoteTextStyle(
                        fontVariationSettings =
                            FontVariation.Settings(FontVariation.Setting("ROND", 100f))
                    ),
                fontVariationSettings = FontVariation.Settings(FontVariation.weight(500)),
            )
        }

        assertThat(axes).containsExactly("wght", 500f)
    }

    @Test
    fun fontFeatureSettingsParameter_overridesStyleFontAxesInPlace() = runTest {
        val axes = captureTextAxes {
            RemoteText(
                "Text".rs,
                style =
                    RemoteTextStyle(
                        fontVariationSettings =
                            FontVariation.Settings(
                                FontVariation.width(100f),
                                FontVariation.weight(700),
                            )
                    ),
                fontFeatureSettings = "'wght' 300, tnum",
            )
        }

        assertThat(axes).containsExactly("wdth", 100f, "wght", 300f, "tnum", 1f).inOrder()
    }

    @Test
    fun negativeZeroFontAxisValue_isWrittenUnchanged() = runTest {
        val axes = captureTextAxes {
            RemoteText(
                "Text".rs,
                fontVariationSettings = FontVariation.Settings(FontVariation.Setting("slnt", -0f)),
            )
        }

        assertThat(axes.getValue("slnt").toRawBits()).isEqualTo((-0f).toRawBits())
    }

    @Test
    fun densityDependentFontVariationSetting_throws() = runTest {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                captureTextAxes {
                    RemoteText(
                        "Text".rs,
                        fontVariationSettings =
                            FontVariation.Settings(FontVariation.opticalSizing(16.sp)),
                    )
                }
            }

        assertThat(exception).hasMessageThat().isEqualTo("density must not be null")
    }

    @Test
    fun fontFeatureSettingsParameter_overridesDensityDependentFontAxis() = runTest {
        val axes = captureTextAxes {
            RemoteText(
                "Text".rs,
                fontFeatureSettings = "'opsz' 12",
                fontVariationSettings = FontVariation.Settings(FontVariation.opticalSizing(16.sp)),
            )
        }

        assertThat(axes).containsExactly("opsz", 12f)
    }

    private suspend fun captureTextAxes(style: RemoteTextStyle): Map<String, Float> =
        captureTextAxes {
            RemoteText("Text".rs, style = style)
        }

    private suspend fun captureTextAxes(
        content: @Composable @RemoteComposable () -> Unit
    ): Map<String, Float> {
        val document = captureRule.captureDocument(context = context, content = content)
        val remoteContext =
            AndroidRemoteContext().apply {
                useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
            }
        document.initializeContext(remoteContext)

        val text = document.findCoreText()
        val tags = text.fontAxis ?: return emptyMap()
        val values = text.fontAxisValues!!
        return tags.indices.associate { remoteContext.getText(tags[it])!! to values[it] }
    }

    private fun CoreDocument.findCoreText(): CoreText =
        checkNotNull(operations.firstCoreText()) { "No CoreText in document" }

    private fun List<Operation>.firstCoreText(): CoreText? {
        for (op in this) {
            if (op is CoreText) return op
            if (op is Component)
                op.list.firstCoreText()?.let {
                    return it
                }
        }
        return null
    }
}
