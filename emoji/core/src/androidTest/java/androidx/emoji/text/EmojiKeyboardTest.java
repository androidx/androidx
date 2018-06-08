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
package androidx.emoji.text;

import static androidx.emoji.util.Emoji.EMOJI_WITH_ZWJ;
import static androidx.emoji.util.EmojiMatcher.hasEmoji;
import static androidx.emoji.util.EmojiMatcher.hasEmojiAt;
import static androidx.emoji.util.KeyboardUtil.del;
import static androidx.emoji.util.KeyboardUtil.forwardDel;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.Suppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.Editable;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import androidx.emoji.test.R;
import androidx.emoji.util.KeyboardUtil;
import androidx.emoji.util.TestString;
import androidx.testutils.PollingCheck;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
@Suppress
public class EmojiKeyboardTest {

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(
            TestActivity.class);
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
    public void testAppendWithSoftKeyboard() throws Exception {
        TestActivity activity = mActivityRule.getActivity();
        final EditText editText = (EditText) activity.findViewById(R.id.editText);
        final TestString string = new TestString(EMOJI_WITH_ZWJ).withPrefix()
                .withSuffix();

        final InputConnection inputConnection = KeyboardUtil.initTextViewForSimulatedIme(
                mInstrumentation, editText);
        KeyboardUtil.setComposingTextInBatch(mInstrumentation, inputConnection,
                string.toString());
        Editable editable = editText.getEditableText();

        assertThat(editable, hasEmojiAt(EMOJI_WITH_ZWJ, string.emojiStartIndex(),
                string.emojiEndIndex()));
    }

    @Test
    public void testBackDeleteWithSoftKeyboard() throws Exception {
        TestActivity activity = mActivityRule.getActivity();
        final EditText editText = (EditText) activity.findViewById(R.id.editText);
        final TestString string = new TestString(EMOJI_WITH_ZWJ).withPrefix()
                .withSuffix();
        final InputConnection inputConnection = KeyboardUtil.initTextViewForSimulatedIme(
                mInstrumentation, editText);
        KeyboardUtil.setComposingTextInBatch(mInstrumentation, inputConnection, string.toString());

        // assert that emoji is there
        final Editable editable = editText.getEditableText();
        assertThat(editable, hasEmoji());

        // put selection at the end of emoji and back delete
        KeyboardUtil.setSelection(mInstrumentation, editText.getEditableText(),
                string.emojiEndIndex());
        KeyboardUtil.deleteSurroundingText(mInstrumentation, inputConnection, 1, 0);

        assertThat(editable, not(hasEmoji()));
    }

    @Test
    public void testForwardDeleteWithSoftKeyboard() throws Exception {
        TestActivity activity = mActivityRule.getActivity();
        final EditText editText = (EditText) activity.findViewById(R.id.editText);
        final TestString string = new TestString(EMOJI_WITH_ZWJ).withPrefix()
                .withSuffix();
        final InputConnection inputConnection = KeyboardUtil.initTextViewForSimulatedIme(
                mInstrumentation, editText);
        KeyboardUtil.setComposingTextInBatch(mInstrumentation, inputConnection, string.toString());

        // assert that emoji is there
        final Editable editable = editText.getEditableText();
        assertThat(editable, hasEmoji());

        // put selection at the begining of emoji and forward delete
        KeyboardUtil.setSelection(mInstrumentation, editText.getEditableText(),
                string.emojiStartIndex());
        KeyboardUtil.deleteSurroundingText(mInstrumentation, inputConnection, 0, 1);


        assertThat(editable, not(hasEmoji()));
    }

    @Test
    public void testBackDeleteWithHardwareKeyboard() throws Exception {
        TestActivity activity = mActivityRule.getActivity();
        final EditText editText = (EditText) activity.findViewById(R.id.editText);
        final TestString string = new TestString(EMOJI_WITH_ZWJ).withPrefix()
                .withSuffix();
        final InputConnection inputConnection = KeyboardUtil.initTextViewForSimulatedIme(
                mInstrumentation, editText);
        KeyboardUtil.setComposingTextInBatch(mInstrumentation, inputConnection, string.toString());

        // assert that emoji is there
        final Editable editable = editText.getEditableText();
        assertThat(editable, hasEmoji());

        // put selection at the end of emoji and back delete
        KeyboardUtil.setSelection(mInstrumentation, editText.getEditableText(),
                string.emojiEndIndex());
        mInstrumentation.sendKeySync(del());
        mInstrumentation.waitForIdleSync();

        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return not(hasEmoji()).matches(true);
            }
        });
        assertThat(editable, not(hasEmoji()));
    }

    @Test
    public void testForwardDeleteWithHardwareKeyboard() throws Exception {
        TestActivity activity = mActivityRule.getActivity();
        final EditText editText = (EditText) activity.findViewById(R.id.editText);
        final TestString string = new TestString(EMOJI_WITH_ZWJ).withPrefix()
                .withSuffix();
        final InputConnection inputConnection = KeyboardUtil.initTextViewForSimulatedIme(
                mInstrumentation, editText);
        KeyboardUtil.setComposingTextInBatch(mInstrumentation, inputConnection, string.toString());

        // assert that emoji is there
        final Editable editable = editText.getEditableText();
        assertThat(editable, hasEmoji());

        // put selection at the begining of emoji and forward delete
        KeyboardUtil.setSelection(mInstrumentation, editText.getEditableText(),
                string.emojiStartIndex());
        mInstrumentation.sendKeySync(forwardDel());
        mInstrumentation.waitForIdleSync();

        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return not(hasEmoji()).matches(true);
            }
        });
        assertThat(editable, not(hasEmoji()));
    }
}
