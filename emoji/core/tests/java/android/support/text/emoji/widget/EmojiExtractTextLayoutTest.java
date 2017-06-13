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

package android.support.text.emoji.widget;

import static junit.framework.TestCase.assertFalse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.text.emoji.EmojiCompat;
import android.support.text.emoji.R;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class EmojiExtractTextLayoutTest {
    private InputMethodService mInputMethodService;
    private EmojiExtractTextLayout mLayout;

    @Before
    public void setup() {
        EmojiCompat.reset(mock(EmojiCompat.class));
        mInputMethodService = mock(InputMethodService.class);
    }

    @Test
    @UiThreadTest
    public void testInflate() {
        final Context context = InstrumentationRegistry.getTargetContext();
        mLayout = (EmojiExtractTextLayout) LayoutInflater.from(context)
                .inflate(android.support.text.emoji.test.R.layout.extract_view, null);

        final EmojiExtractEditText extractEditText = mLayout.findViewById(
                android.R.id.inputExtractEditText);
        assertNotNull(extractEditText);

        final ViewGroup inputExtractAccessories = mLayout.findViewById(
                R.id.inputExtractAccessories);
        assertNotNull(inputExtractAccessories);

        final ExtractButtonCompat extractButton = inputExtractAccessories.findViewById(
                R.id.inputExtractAction);
        assertNotNull(extractButton);
    }

    @Test
    @UiThreadTest
    public void testOnUpdateExtractingViews() {
        final Context context = InstrumentationRegistry.getTargetContext();
        mLayout = (EmojiExtractTextLayout) LayoutInflater.from(context)
                .inflate(android.support.text.emoji.test.R.layout.extract_view, null);

        final EditorInfo editorInfo = new EditorInfo();
        editorInfo.actionLabel = "My Action Label";
        editorInfo.imeOptions = EditorInfo.IME_ACTION_SEND;
        editorInfo.inputType = InputType.TYPE_CLASS_TEXT;

        when(mInputMethodService.isExtractViewShown()).thenReturn(true);

        final ViewGroup inputExtractAccessories = mLayout.findViewById(
                R.id.inputExtractAccessories);
        inputExtractAccessories.setVisibility(View.GONE);

        final ExtractButtonCompat extractButton = inputExtractAccessories.findViewById(
                R.id.inputExtractAction);

        mLayout.onUpdateExtractingViews(mInputMethodService, editorInfo);

        assertEquals(View.VISIBLE, inputExtractAccessories.getVisibility());
        assertEquals(editorInfo.actionLabel, extractButton.getText());
        assertTrue(extractButton.hasOnClickListeners());
    }

    @Test
    @UiThreadTest
    public void testOnUpdateExtractingViews_hidesAccessoriesIfNoAction() {
        final Context context = InstrumentationRegistry.getTargetContext();
        mLayout = (EmojiExtractTextLayout) LayoutInflater.from(context)
                .inflate(android.support.text.emoji.test.R.layout.extract_view, null);

        final EditorInfo editorInfo = new EditorInfo();
        editorInfo.imeOptions = EditorInfo.IME_ACTION_NONE;
        when(mInputMethodService.isExtractViewShown()).thenReturn(true);

        final ViewGroup inputExtractAccessories = mLayout.findViewById(
                R.id.inputExtractAccessories);
        final ExtractButtonCompat extractButton = inputExtractAccessories.findViewById(
                R.id.inputExtractAction);

        mLayout.onUpdateExtractingViews(mInputMethodService, editorInfo);

        assertEquals(View.GONE, inputExtractAccessories.getVisibility());
        assertFalse(extractButton.hasOnClickListeners());
    }
}
