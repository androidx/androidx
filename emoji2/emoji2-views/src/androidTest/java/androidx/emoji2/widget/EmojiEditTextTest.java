/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.emoji2.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.widget.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class EmojiEditTextTest {

    @BeforeClass
    public static void setupEmojiCompat() {
        EmojiCompat.reset(mock(EmojiCompat.class));
    }

    @Test
    public void testInflateWithMaxEmojiCount() {
        Context context = ApplicationProvider.getApplicationContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            final EmojiEditText editText =
                    LayoutInflater.from(context)
                            .inflate(R.layout.edit_text_layout, null)
                            .findViewById(R.id.editTextWithMaxCount);

            // value set in XML
            assertEquals(5, editText.getMaxEmojiCount());

            editText.setMaxEmojiCount(1);
            assertEquals(1, editText.getMaxEmojiCount());
        });

    }

    @Test
    public void testSetKeyListener_withNull() {
        Context context = ApplicationProvider.getApplicationContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            final EmojiEditText editText =
                    LayoutInflater.from(context)
                            .inflate(R.layout.edit_text_layout, null)
                            .findViewById(R.id.editTextWithMaxCount);
            editText.setKeyListener(null);
            assertNull(editText.getKeyListener());

        });
    }
}
