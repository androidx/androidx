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
package androidx.emoji2.viewsintegration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class EmojiInputConnectionTest {

    private InputConnection mInputConnection;
    private Editable mEditable;
    private EmojiInputConnection mEmojiEmojiInputConnection;
    private EmojiInputConnection.EmojiCompatDeleteHelper mEmojiCompatDeleteHelper;

    @Before
    public void setup() {
        mEditable = mock(Editable.class);
        mInputConnection = mock(InputConnection.class);
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final TextView textView = spy(new TextView(context));
        mEmojiCompatDeleteHelper = mock(EmojiInputConnection.EmojiCompatDeleteHelper.class);
        doNothing().when(mEmojiCompatDeleteHelper).updateEditorInfoAttrs(any());

        doReturn(mEditable).when(textView).getEditableText();
        when(mInputConnection.deleteSurroundingText(anyInt(), anyInt())).thenReturn(false);
        setupDeleteSurroundingText();

        mEmojiEmojiInputConnection = new EmojiInputConnection(textView, mInputConnection,
                new EditorInfo(), mEmojiCompatDeleteHelper);
    }

    private void setupDeleteSurroundingText() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when(mInputConnection.deleteSurroundingTextInCodePoints(anyInt(), anyInt())).thenReturn(
                    false);
        }
    }

    @Test
    public void whenEmojiCompatDelete_doesntDelete_inputConnectionIsCalled() {
        when(mEmojiCompatDeleteHelper.handleDeleteSurroundingText(any(), any(), anyInt(),
                anyInt(), anyBoolean())).thenReturn(false);
        assertFalse(mEmojiEmojiInputConnection.deleteSurroundingText(1, 0));
        verify(mInputConnection, times(1)).deleteSurroundingText(1, 0);
    }

    @Test
    public void whenEmojiCompatDelete_doesDelete_inputConnectionIsNotCalled() {
        when(mEmojiCompatDeleteHelper.handleDeleteSurroundingText(any(), any(), anyInt(),
                anyInt(), anyBoolean())).thenReturn(true);
        assertTrue(mEmojiEmojiInputConnection.deleteSurroundingText(1, 0));
        verify(mInputConnection, never()).deleteSurroundingText(anyInt(), anyInt());
    }
}
