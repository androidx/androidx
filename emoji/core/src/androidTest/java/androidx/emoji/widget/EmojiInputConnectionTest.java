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

import static androidx.emoji.util.EmojiMatcher.hasEmoji;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.TestConfigBuilder;
import androidx.emoji.util.Emoji;
import androidx.emoji.util.TestString;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class EmojiInputConnectionTest {

    private InputConnection mInputConnection;
    private TestString mTestString;
    private Editable mEditable;
    private EmojiInputConnection mEmojiEmojiInputConnection;

    @BeforeClass
    public static void setupEmojiCompat() {
        EmojiCompat.reset(TestConfigBuilder.config());
    }

    @Before
    public void setup() {
        mTestString = new TestString(Emoji.EMOJI_WITH_ZWJ).withPrefix().withSuffix();
        mEditable = new SpannableStringBuilder(mTestString.toString());
        mInputConnection = mock(InputConnection.class);
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final TextView textView = spy(new TextView(context));
        EmojiCompat.get().process(mEditable);
        assertThat(mEditable, hasEmoji());

        doReturn(mEditable).when(textView).getEditableText();
        when(mInputConnection.deleteSurroundingText(anyInt(), anyInt())).thenReturn(false);
        setupDeleteSurroundingText();

        mEmojiEmojiInputConnection = new EmojiInputConnection(textView, mInputConnection,
                new EditorInfo());
    }

    private void setupDeleteSurroundingText() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when(mInputConnection.deleteSurroundingTextInCodePoints(anyInt(), anyInt())).thenReturn(
                    false);
        }
    }

    @Test
    public void testDeleteSurroundingText_doesNotDelete() {
        Selection.setSelection(mEditable, 0, mEditable.length());
        assertFalse(mEmojiEmojiInputConnection.deleteSurroundingText(1, 0));
        verify(mInputConnection, times(1)).deleteSurroundingText(1, 0);
    }

    @Test
    public void testDeleteSurroundingText_deletesEmojiBackward() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        assertTrue(mEmojiEmojiInputConnection.deleteSurroundingText(1, 0));
        verify(mInputConnection, never()).deleteSurroundingText(anyInt(), anyInt());
    }

    @Test
    public void testDeleteSurroundingText_doesNotDeleteEmojiIfSelectionAtStartIndex() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());
        assertFalse(mEmojiEmojiInputConnection.deleteSurroundingText(1, 0));
        verify(mInputConnection, times(1)).deleteSurroundingText(1, 0);
    }

    @Test
    public void testDeleteSurroundingText_deletesEmojiForward() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());
        assertTrue(mEmojiEmojiInputConnection.deleteSurroundingText(0, 1));
        verify(mInputConnection, never()).deleteSurroundingText(anyInt(), anyInt());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    public void testDeleteSurroundingTextInCodePoints_doesNotDelete() {
        Selection.setSelection(mEditable, 0, mEditable.length());
        assertFalse(mEmojiEmojiInputConnection.deleteSurroundingTextInCodePoints(1, 0));
        verify(mInputConnection, times(1)).deleteSurroundingTextInCodePoints(1, 0);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    public void testDeleteSurroundingTextInCodePoints_deletesEmojiBackward() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        assertTrue(mEmojiEmojiInputConnection.deleteSurroundingTextInCodePoints(1, 0));
        verify(mInputConnection, never()).deleteSurroundingTextInCodePoints(anyInt(), anyInt());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    public void testDeleteSurroundingTextInCodePoints_deletesEmojiForward() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());
        assertTrue(mEmojiEmojiInputConnection.deleteSurroundingTextInCodePoints(0, 1));
        verify(mInputConnection, never()).deleteSurroundingTextInCodePoints(anyInt(), anyInt());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    public void testDeleteSurroundingTextInCodePoints_doesNotDeleteEmojiIfSelectionAtStartIndex() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());
        assertFalse(mEmojiEmojiInputConnection.deleteSurroundingTextInCodePoints(1, 0));
        verify(mInputConnection, times(1)).deleteSurroundingTextInCodePoints(1, 0);
    }
}
