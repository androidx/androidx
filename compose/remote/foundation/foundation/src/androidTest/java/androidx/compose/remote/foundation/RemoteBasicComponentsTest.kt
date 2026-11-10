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

package androidx.compose.remote.foundation

import android.content.Context
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.path
import androidx.compose.remote.creation.compose.layout.RemoteContentScale
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.text.RemoteFontStyle
import androidx.compose.remote.creation.compose.text.RemoteFontVariation
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteTextAlign
import androidx.compose.remote.creation.compose.text.RemoteTextOverflow
import androidx.compose.remote.foundation.icon.RemoteBasicIcon
import androidx.compose.remote.foundation.image.RemoteBasicImage
import androidx.compose.remote.foundation.text.RemoteBasicText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RemoteBasicComponentsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun remoteBasicImage_captureCropAndFit() = runBlocking {
        val bitmap = RemoteImageBitmap("https://example.com/test.png")
        val capturedDefault =
            captureSingleRemoteDocument(context) {
                RemoteBasicImage(
                    bitmap = bitmap,
                    contentDescription = "Test Image".rs,
                )
            }
        val capturedFit =
            captureSingleRemoteDocument(context) {
                RemoteBasicImage(
                    bitmap = bitmap,
                    contentDescription = "Test Image".rs,
                    contentScale = RemoteContentScale.Fit,
                    alpha = 1f.rf,
                )
            }
        val capturedCrop =
            captureSingleRemoteDocument(context) {
                RemoteBasicImage(
                    bitmap = bitmap,
                    contentDescription = "Test Image".rs,
                    contentScale = RemoteContentScale.Crop,
                )
            }
        val capturedHalfAlpha =
            captureSingleRemoteDocument(context) {
                RemoteBasicImage(
                    bitmap = bitmap,
                    contentDescription = "Test Image".rs,
                    alpha = 0.5f.rf,
                )
            }

        assertThat(capturedDefault.bytes).isEqualTo(capturedFit.bytes)
        assertThat(capturedCrop.bytes).isNotEqualTo(capturedFit.bytes)
        assertThat(capturedHalfAlpha.bytes).isNotEqualTo(capturedDefault.bytes)
    }

    @Test
    fun remoteBasicText_captureStylingAndVariationSettings() = runBlocking {
        val capturedDefault =
            captureSingleRemoteDocument(context) {
                RemoteBasicText(text = "Styled Text".rs)
            }
        val capturedExplicitClip =
            captureSingleRemoteDocument(context) {
                RemoteBasicText(
                    text = "Styled Text".rs,
                    overflow = RemoteTextOverflow.Clip,
                )
            }
        val capturedStyled =
            captureSingleRemoteDocument(context) {
                RemoteBasicText(
                    text = "Styled Text".rs,
                    fontStyle = RemoteFontStyle.Italic,
                    fontWeight = RemoteFontWeight.Bold,
                    textAlign = RemoteTextAlign.Center,
                    overflow = RemoteTextOverflow.Ellipsis,
                )
            }
        val capturedVariation =
            captureSingleRemoteDocument(context) {
                RemoteBasicText(
                    text = "Styled Text".rs,
                    fontVariationSettings =
                        RemoteFontVariation.Settings(RemoteFontVariation.weight(700.rf)),
                )
            }

        assertThat(capturedExplicitClip.bytes).isEqualTo(capturedDefault.bytes)
        assertThat(capturedStyled.bytes).isNotEqualTo(capturedDefault.bytes)
        assertThat(capturedVariation.bytes).isNotEqualTo(capturedDefault.bytes)
    }

    @Test
    fun remoteBasicIcon_captureDefaultAndCustomTint() = runBlocking {
        val imageVector =
            RemoteImageVector.Builder(
                    viewportWidth = 24f.rf,
                    viewportHeight = 24f.rf,
                    tintColor = 0xFF000000.toInt().rc,
                )
                .path(fill = RemoteBrush.solidColor(0xFFFFFFFF.toInt().rc)) {
                    moveTo(0f.rf, 0f.rf)
                    lineTo(24f.rf, 24f.rf)
                    close()
                }
                .build()

        val capturedDefault =
            captureSingleRemoteDocument(context) {
                RemoteBasicIcon(
                    imageVector = imageVector,
                    contentDescription = "Icon".rs,
                )
            }
        val capturedExplicitWhite =
            captureSingleRemoteDocument(context) {
                RemoteBasicIcon(
                    imageVector = imageVector,
                    contentDescription = "Icon".rs,
                    tint = 0xFFFFFFFF.toInt().rc,
                )
            }
        val capturedCustomRed =
            captureSingleRemoteDocument(context) {
                RemoteBasicIcon(
                    imageVector = imageVector,
                    contentDescription = "Icon".rs,
                    tint = 0xFFFF0000.toInt().rc,
                )
            }

        assertThat(capturedDefault.bytes).isEqualTo(capturedExplicitWhite.bytes)
        assertThat(capturedDefault.bytes).isNotEqualTo(capturedCustomRed.bytes)
    }
}
