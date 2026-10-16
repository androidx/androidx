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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.remote.material3.util.EnableEmbeddedPlayerRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteTypographyDefaultFontFamilyTest {
    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun fromTypography_defaultFontFamily_appliedToAllStyles() {
        val remoteTypography =
            RemoteTypography(typography = Typography(), defaultFontFamily = GoogleSansFlex)

        for ((name, style) in remoteTypography.styles()) {
            assertWithMessage("$name fontFamily").that(style.fontFamily).isEqualTo(GoogleSansFlex)
        }
    }

    @Test
    fun fromTypography_defaultFontFamily_overridesConvertibleFontFamily() {
        val typography = Typography(bodyLarge = TextStyle(fontFamily = FontFamily.Serif))

        val remoteTypography =
            RemoteTypography(typography = typography, defaultFontFamily = GoogleSansFlex)

        assertThat(remoteTypography.bodyLarge.fontFamily).isEqualTo(GoogleSansFlex)
    }

    @Test
    fun fromTypography_withoutDefaultFontFamily_keepsConvertibleFontFamily() {
        val typography = Typography(bodyLarge = TextStyle(fontFamily = FontFamily.Serif))

        val remoteTypography =
            RemoteTypography(typography, defaultFontFamily = RemoteFontFamily.Default)

        assertThat(remoteTypography.bodyLarge.fontFamily).isEqualTo(RemoteFontFamily.Serif)
        // Wear Material3 resource fonts can't be converted, so they fall back to Default.
        assertThat(remoteTypography.bodyMedium.fontFamily).isEqualTo(RemoteFontFamily.Default)
    }

    @Test
    fun fromTypography_defaultFontFamily_survivesToPlayerTextLayout() {
        val remoteTypography =
            RemoteTypography(typography = Typography(), defaultFontFamily = GoogleSansFlex)

        renderInPlayer {
            RemoteMaterialTheme(typography = remoteTypography) {
                RemoteText("bodyLarge".rs, style = RemoteMaterialTheme.typography.bodyLarge)
                RemoteText("labelMedium".rs, style = RemoteMaterialTheme.typography.labelMedium)
            }
        }

        for (text in listOf("bodyLarge", "labelMedium")) {
            val fontFamily = layoutFontFamily(text)
            assertWithMessage("$text fontFamily $fontFamily")
                .that(fontFamily.isDeviceFont("google-sans-flex"))
                .isTrue()
        }
    }

    @Test
    fun fromTypography_withoutDefaultFontFamily_playerTextLayoutUsesSystemFont() {
        val remoteTypography =
            RemoteTypography(Typography(), defaultFontFamily = RemoteFontFamily.Default)

        renderInPlayer {
            RemoteMaterialTheme(typography = remoteTypography) {
                RemoteText("bodyLarge".rs, style = RemoteMaterialTheme.typography.bodyLarge)
            }
        }

        val fontFamily = layoutFontFamily("bodyLarge")
        assertWithMessage("fontFamily $fontFamily")
            .that(fontFamily.isDeviceFont("sans-serif"))
            .isTrue()
    }

    private fun renderInPlayer(content: @Composable @RemoteComposable () -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val document = runBlocking {
            val bytes = captureSingleRemoteDocument(context = context, content = content).bytes
            CoreDocument().apply {
                ByteArrayInputStream(bytes).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        }
        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(document = document) } }
        rule.waitForIdle()
    }

    /** Returns the [FontFamily] the player's text layout used for the node showing [text]. */
    private fun layoutFontFamily(text: String): FontFamily? {
        val node = rule.onNodeWithText(text).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.single().layoutInput.style.fontFamily
    }

    /** Whether this is a single [DeviceFontFamilyName] font family named [name]. */
    private fun FontFamily?.isDeviceFont(name: String): Boolean {
        val font = (this as? FontListFontFamily)?.fonts?.singleOrNull() as? AndroidFont
        return font != null &&
            font ==
                Font(
                    DeviceFontFamilyName(name),
                    weight = font.weight,
                    style = font.style,
                    variationSettings = font.variationSettings,
                )
    }

    private fun RemoteTypography.styles(): List<Pair<String, RemoteTextStyle>> =
        listOf(
            "displayLarge" to displayLarge,
            "displayMedium" to displayMedium,
            "displaySmall" to displaySmall,
            "titleLarge" to titleLarge,
            "titleMedium" to titleMedium,
            "titleSmall" to titleSmall,
            "labelLarge" to labelLarge,
            "labelMedium" to labelMedium,
            "labelSmall" to labelSmall,
            "bodyLarge" to bodyLarge,
            "bodyMedium" to bodyMedium,
            "bodySmall" to bodySmall,
            "bodyExtraSmall" to bodyExtraSmall,
            "numeralExtraLarge" to numeralExtraLarge,
            "numeralLarge" to numeralLarge,
            "numeralMedium" to numeralMedium,
            "numeralSmall" to numeralSmall,
            "numeralExtraSmall" to numeralExtraSmall,
        )

    private companion object {
        val GoogleSansFlex = RemoteFontFamily.Named("device:google-sans-flex")
    }
}
