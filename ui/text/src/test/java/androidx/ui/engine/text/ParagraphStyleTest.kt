/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.engine.text

import androidx.ui.engine.text.font.FontFamily
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParagraphStyleTest {
    @Test
    fun `hasFontAttributes default`() {
        val paragraphStyle = ParagraphStyle()
        assertThat(paragraphStyle.hasFontAttributes(), equalTo(false))
    }

    @Test
    fun `hasFontAttributes with fontFamily returns true`() {
        val paragraphStyle = ParagraphStyle(fontFamily = FontFamily("sans"))
        assertThat(paragraphStyle.hasFontAttributes(), equalTo(true))
    }

    @Test
    fun `hasFontAttributes with fontStyle returns true`() {
        val paragraphStyle = ParagraphStyle(fontStyle = FontStyle.Italic)
        assertThat(paragraphStyle.hasFontAttributes(), equalTo(true))
    }

    @Test
    fun `hasFontAttributes with fontWeight returns true`() {
        val paragraphStyle = ParagraphStyle(fontWeight = FontWeight.normal)
        assertThat(paragraphStyle.hasFontAttributes(), equalTo(true))
    }
}