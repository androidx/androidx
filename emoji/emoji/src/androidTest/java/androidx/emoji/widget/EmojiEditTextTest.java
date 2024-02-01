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

import static androidx.emoji.util.Emoji.EMOJI_SINGLE_CODEPOINT;
import static androidx.emoji.util.EmojiMatcher.hasEmojiCount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.Instrumentation;

import androidx.emoji.test.R;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.TestActivity;
import androidx.emoji.text.TestConfigBuilder;
import androidx.emoji.util.TestString;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 22) // there's a memory leak in API 21 that this triggers
public class EmojiEditTextTest {

    @SuppressWarnings("deprecation")
    @Rule
    public androidx.test.rule.ActivityTestRule<TestActivity> mActivityRule =
            new androidx.test.rule.ActivityTestRule<>(TestActivity.class);
    private Instrumentation mInstrumentation;

    @BeforeClass
    public static void setupEmojiCompat() {
        EmojiCompat.reset(TestConfigBuilder.config());
    }

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    @Test
    public void testInflateWithMaxEmojiCount() {
        final TestActivity activity = mActivityRule.getActivity();
        final EmojiEditText editText = activity.findViewById(R.id.editTextWithMaxCount);

        // value set in XML
        assertEquals(5, editText.getMaxEmojiCount());

        // set max emoji count
        mInstrumentation.runOnMainSync(() -> editText.setMaxEmojiCount(1));
        mInstrumentation.waitForIdleSync();

        assertEquals(1, editText.getMaxEmojiCount());
    }

    @Test
    @UiThreadTest
    public void testSetKeyListener_withNull() {
        final TestActivity activity = mActivityRule.getActivity();
        final EmojiEditText editText = activity.findViewById(R.id.editTextWithMaxCount);
        editText.setKeyListener(null);
        assertNull(editText.getKeyListener());
    }

    @Test
    public void testSetMaxCount() {
        final TestActivity activity = mActivityRule.getActivity();
        final EmojiEditText editText = activity.findViewById(R.id.editTextWithMaxCount);

        // set max emoji count to 1 and set text with 2 emojis
        mInstrumentation.runOnMainSync(() -> {
            editText.setMaxEmojiCount(1);
            final String string = new TestString(EMOJI_SINGLE_CODEPOINT).append(
                    EMOJI_SINGLE_CODEPOINT).toString();
            editText.setText(string);
        });
        mInstrumentation.waitForIdleSync();

        assertThat(editText.getText(), hasEmojiCount(1));
    }
}
