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
package androidx.ui.text

import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.TextDirection
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextBoxTest {

    @Test
    fun `toRect`() {
        val textBox = TextBox(
            1.0f,
            2.0f,
            3.0f,
            4.0f,
            TextDirection.Ltr
        )
        assertThat(textBox.toRect(), `is`(equalTo(Rect.fromLTRB(1.0f, 2.0f, 3.0f, 4.0f))))
    }

    @Test
    fun `start for LTR`() {
        val textBox = TextBox(
            1.0f,
            2.0f,
            3.0f,
            4.0f,
            TextDirection.Ltr
        )
        assertThat(textBox.start(), `is`(equalTo(1.0f)))
    }

    @Test
    fun `start for RTL`() {
        val textBox = TextBox(
            1.0f,
            2.0f,
            3.0f,
            4.0f,
            TextDirection.Rtl
        )
        assertThat(textBox.start(), `is`(equalTo(3.0f)))
    }

    @Test
    fun `end for LTR`() {
        val textBox = TextBox(
            1.0f,
            2.0f,
            3.0f,
            4.0f,
            TextDirection.Ltr
        )
        assertThat(textBox.end(), `is`(equalTo(3.0f)))
    }

    @Test
    fun `end for RTL`() {
        val textBox = TextBox(
            1.0f,
            2.0f,
            3.0f,
            4.0f,
            TextDirection.Rtl
        )
        assertThat(textBox.end(), `is`(equalTo(1.0f)))
    }

    @Test
    fun `fromLTRBD`() {
        val textBox = TextBox.fromLTRBD(
            1.0f,
            2.0f,
            3.0f,
            4.0f,
            TextDirection.Ltr
        )
        assertThat(textBox.toRect(), `is`(equalTo(Rect.fromLTRB(1.0f, 2.0f, 3.0f, 4.0f))))
        assertThat(textBox.direction, `is`(equalTo(TextDirection.Ltr)))
    }

    @Test
    fun `toString `() {
        val textBox = TextBox(
            1.0f,
            2.0f,
            3.0f,
            4.0f,
            TextDirection.Ltr
        )
        assertThat(
            textBox.toString(),
            `is`(equalTo("TextBox.fromLTRBD(1.0, 2.0, 3.0, 4.0, ${TextDirection.Ltr})"))
        )
    }
}