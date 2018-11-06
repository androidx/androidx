/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.services.text_input

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextInputTypeTest {
    @Test
    fun `numberWithOptions default`() {
        val defaultTextInputType = TextInputType.numberWithOptions()

        assertThat(2, equalTo(defaultTextInputType.index))
        assertThat(false, equalTo(defaultTextInputType.signed))
        assertThat(false, equalTo(defaultTextInputType.decimal))
    }

    @Test
    fun `numberWithOptions with signed`() {
        val textInputType = TextInputType.numberWithOptions(signed = true)

        assertThat(2, equalTo(textInputType.index))
        assertThat(true, equalTo(textInputType.signed))
        assertThat(false, equalTo(textInputType.decimal))
    }

    @Test
    fun `numberWithOptions with decimal`() {
        val textInputType = TextInputType.numberWithOptions(decimal = true)

        assertThat(2, equalTo(textInputType.index))
        assertThat(false, equalTo(textInputType.signed))
        assertThat(true, equalTo(textInputType.decimal))
    }
}