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
import static org.junit.Assert.assertThat;

import android.app.Instrumentation;

import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.util.Emoji;
import androidx.emoji2.util.EmojiMatcher;
import androidx.emoji2.util.TestString;
import androidx.emoji2.widget.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class EmojiEditTextTest {

    @SuppressWarnings("deprecation")
    @Rule
    public androidx.test.rule.ActivityTestRule<ViewsTestActivity> mActivityRule =
            new androidx.test.rule.ActivityTestRule<>(ViewsTestActivity.class);
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
        final ViewsTestActivity activity = mActivityRule.getActivity();
        final EmojiEditText editText = activity.findViewById(R.id.editTextWithMaxCount);

        // value set in XML
        assertEquals(5, editText.getMaxEmojiCount());

        // set max emoji count
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setMaxEmojiCount(1);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertEquals(1, editText.getMaxEmojiCount());
    }

    @Test
    @UiThreadTest
    public void testSetKeyListener_withNull() {
        final ViewsTestActivity activity = mActivityRule.getActivity();
        final EmojiEditText editText = activity.findViewById(R.id.editTextWithMaxCount);
        editText.setKeyListener(null);
        assertNull(editText.getKeyListener());
    }

    //TODO(seanmcq): re-enable without dependency on font
    @Test
    @SdkSuppress(minSdkVersion = 19)
    @Ignore("Disabled to avoid adding dependency on emoji font to this artifact")
    public void testSetMaxCount() {
        final ViewsTestActivity activity = mActivityRule.getActivity();
        final EmojiEditText editText = activity.findViewById(R.id.editTextWithMaxCount);

        // set max emoji count to 1 and set text with 2 emojis
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setMaxEmojiCount(1);
                final String string = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).append(
                        Emoji.EMOJI_SINGLE_CODEPOINT).toString();
                editText.setText(string);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertThat(editText.getText(), EmojiMatcher.hasEmojiCount(1));
    }

    //TODO(seanmcq): re-enable without dependency on font
    @Test
    @SdkSuppress(minSdkVersion = 19)
    @Ignore("Disabled to avoid adding dependency on emoji font to this artifact")
    public void testDoesReplaceEmoji() {
        final ViewsTestActivity activity = mActivityRule.getActivity();
        final EmojiEditText editText = activity.findViewById(R.id.editText);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final String string = new TestString(Emoji.EMOJI_FLAG).append(
                        Emoji.EMOJI_FLAG).toString();
                editText.setText(string);
            }
        });
        mInstrumentation.waitForIdleSync();

        assertThat(editText.getText(), EmojiMatcher.hasEmojiCount(2));
    }
}
