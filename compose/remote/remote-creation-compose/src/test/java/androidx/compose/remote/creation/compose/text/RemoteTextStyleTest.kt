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

import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class RemoteTextStyleTest {
    val density by lazy { RemoteDensity(1f.rf, 1f.rf) }

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

        Truth.assertThat(newStyle.color?.constantValueOrNull)
            .isEqualTo(Color.Blue.rc.constantValueOrNull)
        Truth.assertThat(newStyle.fontSize?.constantValueOrNull)
            .isEqualTo(24.rsp.constantValueOrNull)
        Truth.assertThat(newStyle.fontWeight).isEqualTo(FontWeight.Bold)
        Truth.assertThat(newStyle.lineHeight?.constantValueOrNull)
            .isEqualTo(20.rsp.constantValueOrNull)
        Truth.assertThat(newStyle.textDecoration).isEqualTo(TextDecoration.LineThrough)
    }

    @Test
    fun noarg_copy_does_not_overrides_properties() {
        val style = RemoteTextStyle(color = Color.Red.rc, fontSize = 12.rsp)

        val newStyle = style.copy()

        Truth.assertThat(newStyle.color).isEqualTo(style.color)
        Truth.assertThat(newStyle.fontSize).isEqualTo(style.fontSize)
    }

    @Test
    fun merge_overrides_properties() {
        val style =
            RemoteTextStyle(color = Color.Red.rc, fontSize = 12.rsp, fontWeight = FontWeight.Normal)

        val newStyle = style.merge(color = Color.Blue.rc, fontWeight = FontWeight.Bold)

        Truth.assertThat(newStyle.color?.constantValueOrNull)
            .isEqualTo(Color.Blue.rc.constantValueOrNull)
        Truth.assertThat(newStyle.fontSize?.constantValueOrNull)
            .isEqualTo(12.rsp.constantValueOrNull)
        Truth.assertThat(newStyle.fontWeight).isEqualTo(FontWeight.Bold)
    }
}
