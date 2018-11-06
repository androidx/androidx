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

package androidx.ui.port.engine.text.platform

import android.text.Layout
import android.text.TextPaint
import androidx.test.filters.SmallTest
import androidx.ui.engine.text.platform.TextLayout
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class TextLayoutTest {
    @Test
    fun constructor_default_values() {
        val textLayuout = TextLayout(charSequence = "", textPaint = TextPaint())
        val nativeLayout = textLayuout.layout

        assertThat(nativeLayout.width, equalTo(0))
        assertThat(nativeLayout.alignment, equalTo(Layout.Alignment.ALIGN_NORMAL))
        assertThat(nativeLayout.getParagraphDirection(0), equalTo(Layout.DIR_LEFT_TO_RIGHT))
        assertThat(nativeLayout.spacingMultiplier, equalTo(1.0f))
        assertThat(nativeLayout.spacingAdd, equalTo(0.0f))
        // TODO(Migration/haoyuchang): Need public API to test includePadding, maxLines,
        // breakStrategy and hyphenFrequency.
    }
}