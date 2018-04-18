/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.emoji.widget;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import android.annotation.SuppressLint;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;

import androidx.emoji.text.EmojiMetadata;
import androidx.emoji.text.EmojiSpan;
import androidx.emoji.text.TypefaceEmojiSpan;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class EmojiEditableFactoryTest {

    @Test
    public void testGetInstance() {
        final Editable.Factory instance = EmojiEditableFactory.getInstance();
        assertNotNull(instance);

        final Editable.Factory instance2 = EmojiEditableFactory.getInstance();
        assertSame(instance, instance2);
    }

    @Test
    public void testNewEditable_returnsEditable() {
        final Editable editable = EmojiEditableFactory.getInstance().newEditable("abc");
        assertNotNull(editable);
        assertThat(editable, instanceOf(Editable.class));
    }

    @Test
    public void testNewEditable_preservesCharSequenceData() {
        final String string = "abc";
        final SpannableString str = new SpannableString(string);
        final EmojiMetadata metadata = mock(EmojiMetadata.class);
        final EmojiSpan span = new TypefaceEmojiSpan(metadata);
        str.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        final Editable editable = EmojiEditableFactory.getInstance().newEditable(str);
        assertNotNull(editable);
        assertEquals(string, editable.toString());
        final EmojiSpan[] spans = editable.getSpans(0, 1, EmojiSpan.class);
        assertThat(spans, arrayWithSize(1));
        assertSame(spans[0], span);
    }

    @SuppressLint("PrivateApi")
    @Test
    public void testNewEditable_returnsEmojiSpannableIfWatcherClassExists() {
        Class clazz = null;
        try {
            String className = "android.text.DynamicLayout$ChangeWatcher";
            clazz = getClass().getClassLoader().loadClass(className);
        } catch (Throwable t) {
            // ignore
        }

        if (clazz == null) {
            final Editable editable = EmojiEditableFactory.getInstance().newEditable("");
            assertThat(editable, instanceOf(Editable.class));
        } else {
            final Editable editable = EmojiEditableFactory.getInstance().newEditable("");
            assertThat(editable, instanceOf(SpannableBuilder.class));
        }
    }
}
