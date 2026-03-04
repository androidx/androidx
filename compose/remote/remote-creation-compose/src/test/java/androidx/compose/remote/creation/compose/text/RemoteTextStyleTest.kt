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

package androidx.compose.remote.creation.compose.text

import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class RemoteTextStyleTest {

    @Test
    fun fromTextStyle_createsCorrectly() {
        // Unspecified values map to null
        val defaultRemoteStyle = RemoteTextStyle.fromTextStyle(TextStyle.Default)

        assertThat(defaultRemoteStyle.fontSize).isNull()
        assertThat(defaultRemoteStyle.color).isNull()
        assertThat(defaultRemoteStyle.letterSpacing).isNull()
        assertThat(defaultRemoteStyle.lineHeight).isNull()
        assertThat(defaultRemoteStyle.background).isNull()
        assertThat(defaultRemoteStyle.fontWeight).isNull()
        assertThat(defaultRemoteStyle.fontStyle).isNull()
        assertThat(defaultRemoteStyle.fontFamily).isNull()
        assertThat(defaultRemoteStyle.textAlign).isEqualTo(TextAlign.Unspecified)
        assertThat(defaultRemoteStyle.textDecoration).isNull()

        // Explicit values
        val remoteStyle =
            RemoteTextStyle.fromTextStyle(
                TextStyle.Default.copy(
                    fontSize = 16.sp,
                    color = Color.Red,
                    letterSpacing = 1.2f.sp,
                    lineHeight = 20.sp,
                    background = Color.Blue,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    textDecoration = TextDecoration.Underline,
                )
            )

        assertThat(remoteStyle.fontSize?.type).isEqualTo(TextUnitType.Sp)
        assertThat(remoteStyle.fontSize?.constantValueOrNull).isEqualTo(16.sp)
        assertThat(remoteStyle.color?.constantValueOrNull).isEqualTo(Color.Red)
        assertThat(remoteStyle.letterSpacing?.constantValueOrNull).isEqualTo(1.2f.sp)
        assertThat(remoteStyle.lineHeight?.constantValueOrNull).isEqualTo(20.sp)
        assertThat(remoteStyle.background?.constantValueOrNull).isEqualTo(Color.Blue)
        assertThat(remoteStyle.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(remoteStyle.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(remoteStyle.fontFamily).isEqualTo(FontFamily.Serif)
        assertThat(remoteStyle.textAlign).isEqualTo(TextAlign.Center)
        assertThat(remoteStyle.textDecoration).isEqualTo(TextDecoration.Underline)
    }

    @Test
    fun copy_overrides_properties() {
        val style =
            RemoteTextStyle(
                color = Color.Red.rc,
                fontSize = 12.rsp,
                fontWeight = FontWeight.Bold,
                lineHeight = 10.rsp,
                textDecoration = TextDecoration.Underline,
            )

        val newStyle =
            style.copy(
                color = Color.Blue.rc,
                fontSize = 24.rsp,
                lineHeight = 20.rsp,
                textDecoration = TextDecoration.LineThrough,
            )

        assertThat(newStyle.color?.constantValueOrNull).isEqualTo(Color.Blue.rc.constantValueOrNull)
        assertThat(newStyle.fontSize?.constantValueOrNull).isEqualTo(24.rsp.constantValueOrNull)
        assertThat(newStyle.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(newStyle.lineHeight?.constantValueOrNull).isEqualTo(20.rsp.constantValueOrNull)
        assertThat(newStyle.textDecoration).isEqualTo(TextDecoration.LineThrough)
    }

    @Test
    fun noarg_copy_does_not_overrides_properties() {
        val style = RemoteTextStyle(color = Color.Red.rc, fontSize = 12.rsp)

        val newStyle = style.copy()

        assertThat(newStyle.color).isEqualTo(style.color)
        assertThat(newStyle.fontSize).isEqualTo(style.fontSize)
    }

    @Test
    fun merge_overrides_properties() {
        val style =
            RemoteTextStyle(color = Color.Red.rc, fontSize = 12.rsp, fontWeight = FontWeight.Normal)

        val newStyle = style.merge(color = Color.Blue.rc, fontWeight = FontWeight.Bold)

        assertThat(newStyle.color?.constantValueOrNull).isEqualTo(Color.Blue.rc.constantValueOrNull)
        assertThat(newStyle.fontSize?.constantValueOrNull).isEqualTo(12.rsp.constantValueOrNull)
        assertThat(newStyle.fontWeight).isEqualTo(FontWeight.Bold)
    }
}
