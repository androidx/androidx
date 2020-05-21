/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.layout.test

import androidx.test.filters.SmallTest
import androidx.ui.core.LayoutDirection
import androidx.ui.core.Modifier
import androidx.ui.input.EditorValue
import androidx.ui.layout.rtl
import androidx.ui.text.AnnotatedString
import androidx.ui.text.CoreText
import androidx.ui.text.CoreTextField
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextOverflow
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(JUnit4::class)
@SmallTest
class TextLayoutDirectionModifierTest : LayoutTest() {

    @Test
    fun test_CoreTextField_RtlModifier_changesDirectionTo_Rtl() {
        val latch = CountDownLatch(1)
        var layoutDirection: LayoutDirection? = null

        show {
            CoreTextField(
                value = EditorValue("..."),
                modifier = Modifier.rtl,
                onValueChange = {},
                onTextLayout = { result ->
                    layoutDirection = result.layoutInput.layoutDirection
                    latch.countDown()
                }
            )
        }

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(layoutDirection).isNotNull()
        assertThat(layoutDirection!!).isEqualTo(LayoutDirection.Rtl)
    }

    @Test
    fun test_CoreText_RtlModifier_changesDirectionTo_Rtl() {
        val latch = CountDownLatch(1)
        var layoutDirection: LayoutDirection? = null
        show {
            CoreText(
                text = AnnotatedString("..."),
                modifier = Modifier.rtl,
                style = TextStyle.Default,
                softWrap = true,
                overflow = TextOverflow.Clip,
                maxLines = 1,
                inlineContent = mapOf()
            ) { result ->
                layoutDirection = result.layoutInput.layoutDirection
                latch.countDown()
            }
        }

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(layoutDirection).isNotNull()
        assertThat(layoutDirection!!).isEqualTo(LayoutDirection.Rtl)
    }
}
