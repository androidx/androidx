/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text.googlefonts

import androidx.compose.ui.text.googlefonts.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class XmlLoaderKtTest {

    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test(expected = IllegalArgumentException::class)
    fun whenMalformedQuery_throws() {
        val result = runCatching { GoogleFont(context, R.font.noname) }
        assertThat(result.exceptionOrNull()?.message).contains("noname")
        result.getOrThrow()
    }

    @Test
    fun nameFont_loadsName() {
        val result = GoogleFont(context, R.font.name)
        assertThat(result.name).isEqualTo("afont")
    }

    @Test
    fun nameFont_withBestEffort_loads() {
        val result = GoogleFont(context, R.font.name_besteffort)
        assertThat(result.name).isEqualTo("afont")
        assertThat(result.bestEffort).isEqualTo(false)
    }

    @Test
    fun nameFont_withSpaces_loads() {
        val result = GoogleFont(context, R.font.name_with_spaces)
        assertThat(result.name).isEqualTo("cat jack")
    }
}
