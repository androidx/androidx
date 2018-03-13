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

import static androidx.emoji.util.EmojiMatcher.sameCharSequence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.emoji.R;
import androidx.emoji.text.EmojiCompat;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class EmojiExtractTextLayoutTest {

    private InputMethodService mInputMethodService;

    @BeforeClass
    public static void setupEmojiCompat() {
        EmojiCompat.reset(mock(EmojiCompat.class));
    }

    @Before
    public void setup() {
        mInputMethodService = mock(InputMethodService.class);
    }

    @Test
    @UiThreadTest
    public void testInflate() {
        final Context context = InstrumentationRegistry.getTargetContext();
        final EmojiExtractTextLayout layout = (EmojiExtractTextLayout) LayoutInflater.from(context)
                .inflate(androidx.emoji.test.R.layout.extract_view, null);

        final EmojiExtractEditText extractEditText = layout.findViewById(
                android.R.id.inputExtractEditText);
        assertNotNull(extractEditText);

        final ViewGroup inputExtractAccessories = layout.findViewById(
                R.id.inputExtractAccessories);
        assertNotNull(inputExtractAccessories);

        final ExtractButtonCompat extractButton = inputExtractAccessories.findViewById(
                R.id.inputExtractAction);
        assertNotNull(extractButton);
    }

    @Test
    @UiThreadTest
    public void testSetKeyListener_withNull() {
        final Context context = InstrumentationRegistry.getTargetContext();
        final EmojiExtractTextLayout layout = (EmojiExtractTextLayout) LayoutInflater.from(context)
                .inflate(androidx.emoji.test.R.layout.extract_view, null);

        final EmojiExtractEditText extractEditText = layout.findViewById(
                android.R.id.inputExtractEditText);
        assertNotNull(extractEditText);

        extractEditText.setKeyListener(null);
        assertNull(extractEditText.getKeyListener());
    }

    @Test
    @UiThreadTest
    public void testSetEmojiReplaceStrategy() {
        final Context context = InstrumentationRegistry.getTargetContext();

        final EmojiExtractTextLayout layout = (EmojiExtractTextLayout) LayoutInflater.from(context)
                .inflate(androidx.emoji.test.R.layout.extract_view_with_attrs, null);

        assertEquals(EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT, layout.getEmojiReplaceStrategy());

        final EmojiExtractEditText extractEditText = layout.findViewById(
                android.R.id.inputExtractEditText);
        assertNotNull(extractEditText);
        assertEquals(EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT,
                extractEditText.getEmojiReplaceStrategy());

        layout.setEmojiReplaceStrategy(EmojiCompat.REPLACE_STRATEGY_ALL);
        assertEquals(EmojiCompat.REPLACE_STRATEGY_ALL, layout.getEmojiReplaceStrategy());
        assertEquals(EmojiCompat.REPLACE_STRATEGY_ALL, extractEditText.getEmojiReplaceStrategy());
    }

    @Test
    @UiThreadTest
    @SdkSuppress(minSdkVersion = 19)
    public void testSetEmojiReplaceStrategyCallEmojiCompatWithCorrectStrategy() {
        final Context context = InstrumentationRegistry.getTargetContext();

        final EmojiExtractTextLayout layout = (EmojiExtractTextLayout) LayoutInflater.from(context)
                .inflate(androidx.emoji.test.R.layout.extract_view_with_attrs, null);

        final EmojiExtractEditText extractEditText = layout.findViewById(
                android.R.id.inputExtractEditText);
        assertNotNull(layout);
        assertNotNull(extractEditText);
        assertEquals(EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT, layout.getEmojiReplaceStrategy());

        final EmojiCompat emojiCompat = mock(EmojiCompat.class);
        when(emojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);
        EmojiCompat.reset(emojiCompat);

        final String testString = "anytext";
        extractEditText.setText(testString);

        verify(emojiCompat, times(1)).process(sameCharSequence(testString), anyInt(), anyInt(),
                anyInt(), eq(EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT));
    }

    @Test
    @UiThreadTest
    public void testOnUpdateExtractingViews() {
        final Context context = InstrumentationRegistry.getTargetContext();
        final EmojiExtractTextLayout layout = (EmojiExtractTextLayout) LayoutInflater.from(context)
                .inflate(androidx.emoji.test.R.layout.extract_view, null);

        final EditorInfo editorInfo = new EditorInfo();
        editorInfo.actionLabel = "My Action Label";
        editorInfo.imeOptions = EditorInfo.IME_ACTION_SEND;
        editorInfo.inputType = InputType.TYPE_CLASS_TEXT;

        when(mInputMethodService.isExtractViewShown()).thenReturn(true);

        final ViewGroup inputExtractAccessories = layout.findViewById(
                R.id.inputExtractAccessories);
        inputExtractAccessories.setVisibility(View.GONE);

        final ExtractButtonCompat extractButton = inputExtractAccessories.findViewById(
                R.id.inputExtractAction);

        layout.onUpdateExtractingViews(mInputMethodService, editorInfo);

        assertEquals(View.VISIBLE, inputExtractAccessories.getVisibility());
        assertEquals(editorInfo.actionLabel, extractButton.getText());
        assertTrue(extractButton.hasOnClickListeners());
    }

    @Test
    @UiThreadTest
    public void testOnUpdateExtractingViews_hidesAccessoriesIfNoAction() {
        final Context context = InstrumentationRegistry.getTargetContext();
        final EmojiExtractTextLayout layout = (EmojiExtractTextLayout) LayoutInflater.from(context)
                .inflate(androidx.emoji.test.R.layout.extract_view, null);

        final EditorInfo editorInfo = new EditorInfo();
        editorInfo.imeOptions = EditorInfo.IME_ACTION_NONE;
        when(mInputMethodService.isExtractViewShown()).thenReturn(true);

        final ViewGroup inputExtractAccessories = layout.findViewById(
                R.id.inputExtractAccessories);
        final ExtractButtonCompat extractButton = inputExtractAccessories.findViewById(
                R.id.inputExtractAction);

        layout.onUpdateExtractingViews(mInputMethodService, editorInfo);

        assertEquals(View.GONE, inputExtractAccessories.getVisibility());
        assertFalse(extractButton.hasOnClickListeners());
    }
}
