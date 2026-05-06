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

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.toFontFamily
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RemoteFontFamilyTest {

    @Test
    fun testFromComposeFontFamily_Default() {
        val remote = RemoteFontFamily.fromComposeFontFamily(FontFamily.Default)
        assertThat(remote).isEqualTo(RemoteFontFamily.Default)
        assertThat(remote?.name).isEqualTo("default")
    }

    @Test
    fun testFromComposeFontFamily_SansSerif() {
        val remote = RemoteFontFamily.fromComposeFontFamily(FontFamily.SansSerif)
        assertThat(remote).isEqualTo(RemoteFontFamily.SansSerif)
        assertThat(remote?.name).isEqualTo("sans-serif")
    }

    @Test
    fun testFromComposeFontFamily_Serif() {
        val remote = RemoteFontFamily.fromComposeFontFamily(FontFamily.Serif)
        assertThat(remote).isEqualTo(RemoteFontFamily.Serif)
        assertThat(remote?.name).isEqualTo("serif")
    }

    @Test
    fun testFromComposeFontFamily_Monospace() {
        val remote = RemoteFontFamily.fromComposeFontFamily(FontFamily.Monospace)
        assertThat(remote).isEqualTo(RemoteFontFamily.Monospace)
        assertThat(remote?.name).isEqualTo("monospace")
    }

    @Test
    fun testFromComposeFontFamily_Cursive() {
        val remote = RemoteFontFamily.fromComposeFontFamily(FontFamily.Cursive)
        assertThat(remote).isEqualTo(RemoteFontFamily.Cursive)
        assertThat(remote?.name).isEqualTo("cursive")
    }

    @Test
    fun testFromComposeFontFamily_UnknownFallback() {
        val customFamily = Font(123).toFontFamily()
        val remote = RemoteFontFamily.fromComposeFontFamily(customFamily)
        assertThat(remote).isNull()
    }

    @Test
    fun testNamedFontFamily() {
        val remote = RemoteFontFamily.Named("custom-font")
        assertThat(remote.name).isEqualTo("custom-font")
    }
}
