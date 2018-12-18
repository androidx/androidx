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

package androidx.slice;

import static android.graphics.Typeface.BOLD;

import static org.junit.Assert.assertEquals;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = 19)
public class SliceItemTest {

    @Test
    public void testNormalText() {
        String text = "Some normal text";
        SliceItem item = new SliceItem(text, android.app.slice.SliceItem.FORMAT_TEXT, null,
                new String[0]);

        assertEquals(text, item.getText());
        assertEquals(text, item.getRedactedText());
    }

    @Test
    public void testSpannedText() {
        // "Some [normal] text" where [] denotes bold.
        SpannableStringBuilder text = new SpannableStringBuilder();
        text.append("Some ");
        int spanStart = text.length();
        text.append("normal");
        int spanEnd = text.length();
        text.append(" text");
        text.setSpan(new StyleSpan(BOLD), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SliceItem item = new SliceItem(text, android.app.slice.SliceItem.FORMAT_TEXT, null,
                new String[0]);

        assertEquals(text, item.getText());
        assertEquals(text, item.getRedactedText());
    }

    @Test
    public void testRedactedText() {
        // "Some [normal] text" where [] denotes sensitive
        SpannableStringBuilder text = new SpannableStringBuilder();
        text.append("Some ");
        int spanStart = text.length();
        text.append("normal");
        int spanEnd = text.length();
        text.append(" text");
        text.setSpan(SliceItem.createSensitiveSpan(), spanStart, spanEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        final String redactedText = "Some ****** text";

        SliceItem item = new SliceItem(text, android.app.slice.SliceItem.FORMAT_TEXT, null,
                new String[0]);

        assertEquals(text, item.getText());
        assertEquals(redactedText, item.getRedactedText().toString());
    }

}
