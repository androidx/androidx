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

package androidx.emoji2.bundled.viewstests;

import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Instrumentation;

import androidx.emoji2.bundled.TestActivity;
import androidx.emoji2.bundled.TestConfigBuilder;
import androidx.emoji2.bundled.test.R;
import androidx.emoji2.bundled.util.Emoji;
import androidx.emoji2.bundled.util.EmojiMatcher;
import androidx.emoji2.bundled.util.TestString;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.widget.EmojiEditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class EmojiEditTextParameters {

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
    public void testSetMaxCount() {
        final TestActivity activity = mActivityRule.getActivity();
        final EmojiEditText editText = activity.findViewById(R.id.emojiEditTextWithMaxCount);

        // set max emoji count to 1 and set text with 2 emojis
        mInstrumentation.runOnMainSync(() -> {
            editText.setMaxEmojiCount(1);
            final String string = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).append(
                    Emoji.EMOJI_SINGLE_CODEPOINT).toString();
            editText.setText(string);
        });
        mInstrumentation.waitForIdleSync();

        assertThat(editText.getText(), EmojiMatcher.hasEmojiCount(1));
    }
}
