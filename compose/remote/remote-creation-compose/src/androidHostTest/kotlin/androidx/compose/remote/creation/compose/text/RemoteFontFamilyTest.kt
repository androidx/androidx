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

import androidx.compose.remote.creation.compose.layout.toRemoteFontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.toFontFamily
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RemoteFontFamilyTest {

    @Test
    fun testToRemoteFontFamily_Default() {
        val remote = FontFamily.Default.toRemoteFontFamily()
        assertThat(remote).isEqualTo(RemoteFontFamily.Default)
        assertThat(remote?.name).isEqualTo("default")
    }

    @Test
    fun testToRemoteFontFamily_SansSerif() {
        val remote = FontFamily.SansSerif.toRemoteFontFamily()
        assertThat(remote).isEqualTo(RemoteFontFamily.SansSerif)
        assertThat(remote?.name).isEqualTo("sans-serif")
    }

    @Test
    fun testToRemoteFontFamily_Serif() {
        val remote = FontFamily.Serif.toRemoteFontFamily()
        assertThat(remote).isEqualTo(RemoteFontFamily.Serif)
        assertThat(remote?.name).isEqualTo("serif")
    }

    @Test
    fun testToRemoteFontFamily_Monospace() {
        val remote = FontFamily.Monospace.toRemoteFontFamily()
        assertThat(remote).isEqualTo(RemoteFontFamily.Monospace)
        assertThat(remote?.name).isEqualTo("monospace")
    }

    @Test
    fun testToRemoteFontFamily_Cursive() {
        val remote = FontFamily.Cursive.toRemoteFontFamily()
        assertThat(remote).isEqualTo(RemoteFontFamily.Cursive)
        assertThat(remote?.name).isEqualTo("cursive")
    }

    @Test
    fun testToRemoteFontFamily_UnknownFallback() {
        val customFamily = Font(123).toFontFamily()
        val remote = customFamily.toRemoteFontFamily()
        assertThat(remote).isNull()
    }

    @Test
    fun testNamedFontFamily() {
        val remote = RemoteFontFamily.Named("custom-font")
        assertThat(remote.name).isEqualTo("custom-font")
    }
}
