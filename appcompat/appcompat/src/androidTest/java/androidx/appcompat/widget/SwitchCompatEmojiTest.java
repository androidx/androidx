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

package androidx.appcompat.widget;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import androidx.appcompat.test.R;
import androidx.emoji2.text.EmojiCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SwitchCompatEmojiTest extends
        AppCompatBaseTextViewEmojiTest<SwitchCompatEmojiActivity, SwitchCompat> {

    public SwitchCompatEmojiTest() {
        super(SwitchCompatEmojiActivity.class);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void setupCallbacks_whenLoading() throws Throwable {
        SwitchCompat emojiEnabled = mActivityTestRule.getActivity()
                .findViewById(R.id.emoji_enabled);

        mInstrumentation.runOnMainSync(() -> {
            resetEmojiCompatToNewMock();
            when(mEmojiCompatMock.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_LOADING);
            emojiEnabled.setTextOn("text on");
            emojiEnabled.setShowText(true);
        });
        verify(mEmojiCompatMock).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void dontSetupCallbacks_whenDisabled() throws Throwable {
        SwitchCompat emojiDisabled =
                mActivityTestRule.getActivity().findViewById(R.id.emoji_disabled);

        mInstrumentation.runOnMainSync(() -> {
            resetEmojiCompatToNewMock();
            when(mEmojiCompatMock.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_LOADING);
            emojiDisabled.setTextOn("text on");
            emojiDisabled.setShowText(true);
        });
        verify(mEmojiCompatMock, never()).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void noInteractions_whenDisnabled_andRepeatedShowText() {
        SwitchCompat emojiDisabled =
                mActivityTestRule.getActivity().findViewById(R.id.emoji_disabled);

        mInstrumentation.runOnMainSync(() -> {
            resetEmojiCompatToNewMock();
            when(mEmojiCompatMock.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_LOADING);
            emojiDisabled.setTextOn("text on");
            emojiDisabled.setShowText(true);
            emojiDisabled.setShowText(true);
            emojiDisabled.setShowText(true);
            emojiDisabled.setShowText(true);
        });
        verifyNoMoreInteractions(mEmojiCompatMock);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void setupCallbacks_whenLoading_andEnabledLate() throws Throwable {
        SwitchCompat emojiDisabled =
                mActivityTestRule.getActivity().findViewById(R.id.emoji_disabled);

        mInstrumentation.runOnMainSync(() -> {
            resetEmojiCompatToNewMock();
            when(mEmojiCompatMock.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_LOADING);
            emojiDisabled.setTextOn("text on");
            emojiDisabled.setShowText(true);
            verifyNoMoreInteractions(mEmojiCompatMock);
            // and enable it
            emojiDisabled.setEmojiCompatEnabled(true);
        });
        verify(mEmojiCompatMock).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void whenSetShowText_doesTransform() {
        SwitchCompat emojiEnabled =
                mActivityTestRule.getActivity().findViewById(R.id.emoji_enabled);
        mInstrumentation.runOnMainSync(() -> {
            resetEmojiCompatToNewMock();
            emojiEnabled.setShowText(true);
        });
        verify(mEmojiCompatMock, atLeastOnce()).getLoadState();
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void whenSetTextOn_doesTransform() {
        SwitchCompat emojiEnabled =
                mActivityTestRule.getActivity().findViewById(R.id.emoji_enabled);
        mInstrumentation.runOnMainSync(() -> {
            emojiEnabled.setShowText(true);
            resetEmojiCompatToNewMock();
            emojiEnabled.setTextOn("Hi");
        });
        verify(mEmojiCompatMock, atLeastOnce()).getLoadState();
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void whenSetTextOff_doesTransform() {
        SwitchCompat emojiEnabled =
                mActivityTestRule.getActivity().findViewById(R.id.emoji_enabled);
        mInstrumentation.runOnMainSync(() -> {
            emojiEnabled.setShowText(true);
            resetEmojiCompatToNewMock();
            emojiEnabled.setTextOff("Hi");
        });
        verify(mEmojiCompatMock, atLeastOnce()).getLoadState();
    }
}
