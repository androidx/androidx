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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import androidx.appcompat.test.R;
import androidx.emoji2.text.EmojiCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppCompatTextViewEmojiTest {

    @Rule
    public final ActivityTestRule<AppCompatTextViewEmojiActivity> mActivityTestRule;

    private EmojiCompat mEmojiCompatMock;

    public AppCompatTextViewEmojiTest() {
        mActivityTestRule = new ActivityTestRule<>(AppCompatTextViewEmojiActivity.class, false,
                false);
    }

    @Before
    public void ensureEmojiInitialized() {
        resetEmojiCompatToNewMock();
        mActivityTestRule.launchActivity(null);
    }

    @After
    public void cleanupEmojiCompat() {
        EmojiCompat.reset((EmojiCompat) null);
    }

    private void resetEmojiCompatToNewMock() {
        mEmojiCompatMock = mock(EmojiCompat.class);
        when(mEmojiCompatMock.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_FAILED);
        EmojiCompat.reset(mEmojiCompatMock);
    }

    @Test
    public void byDefault_setText_callsEmojiCompat() {
        resetEmojiCompatToNewMock();
        AppCompatTextView subject = mActivityTestRule.getActivity()
                .findViewById(R.id.text_view_emoji_default);
        subject.setText("Test text");
        verify(mEmojiCompatMock, atLeastOnce()).getLoadState();
    }

    @Test
    public void whenEnabled_setText_callsProcess() {
        resetEmojiCompatToNewMock();
        AppCompatTextView subject = mActivityTestRule.getActivity()
                        .findViewById(R.id.text_view_emoji_enabled);
        String expected = "Test text";
        subject.setText(expected);
        verify(mEmojiCompatMock, atLeastOnce()).getLoadState();
    }

    @Test
    public void whenDisabled_noCalls() {
        resetEmojiCompatToNewMock();
        AppCompatTextView subject = mActivityTestRule.getActivity()
                        .findViewById(R.id.text_view_emoji_disabled);
        String expected = "Test text";
        subject.setText(expected);

        verifyNoMoreInteractions(mEmojiCompatMock);
    }

    @Test
    public void whenReEnabled_callsProcess() {
        resetEmojiCompatToNewMock();
        AppCompatTextView subject = mActivityTestRule.getActivity()
                        .findViewById(R.id.text_view_emoji_disabled);
        String expected = "Some text";
        subject.setText(expected);

        verifyNoMoreInteractions(mEmojiCompatMock);

        subject.setEmojiCompatEnabled(true);
        verify(mEmojiCompatMock, atLeastOnce()).getLoadState();
    }

    @Test
    public void whenNotConfigured_andDisabled_doesNotEnable_whenConfigured() {
        EmojiCompat.reset((EmojiCompat) null);
        mActivityTestRule.finishActivity();
        mActivityTestRule.launchActivity(null);

        AppCompatTextViewEmojiActivity activity = mActivityTestRule.getActivity();
        AppCompatTextView disabledInAdvance = activity.findViewById(R.id.text_view_emoji_disabled);
        AppCompatTextView enabledInAdvance = activity.findViewById(R.id.text_view_emoji_enabled);
        AppCompatTextView defaultEmoji = activity.findViewById(R.id.text_view_emoji_default);

        enabledInAdvance.setEmojiCompatEnabled(false);
        defaultEmoji.setEmojiCompatEnabled(false);

        // now: confirm no interactions with EmojiCompat with all text views disabled
        resetEmojiCompatToNewMock();
        // set the filters without calling enabled to avoid allowing re-set of disabled to update
        // the disabled state
        disabledInAdvance.setFilters(disabledInAdvance.getFilters());
        disabledInAdvance.setText("Some text");
        enabledInAdvance.setFilters(enabledInAdvance.getFilters());
        enabledInAdvance.setText("Some text");
        defaultEmoji.setFilters(defaultEmoji.getFilters());
        defaultEmoji.setText("Some text");
        verifyNoMoreInteractions(mEmojiCompatMock);
    }

    @Test
    public void whenNotConfigured_callingEnabled_afterConfigure_enablesEmoji() {
        EmojiCompat.reset((EmojiCompat) null);
        mActivityTestRule.finishActivity();
        mActivityTestRule.launchActivity(null);

        AppCompatTextViewEmojiActivity activity = mActivityTestRule.getActivity();
        AppCompatTextView enabledInAdvance = activity.findViewById(R.id.text_view_emoji_enabled);

        resetEmojiCompatToNewMock();
        enabledInAdvance.setEmojiCompatEnabled(true);
        enabledInAdvance.setText("Some text");

        verify(mEmojiCompatMock, atLeastOnce()).getLoadState();
    }

    @Test
    public void getEnabled() {
        AppCompatTextViewEmojiActivity activity = mActivityTestRule.getActivity();
        AppCompatTextView disabledInAdvance = activity.findViewById(R.id.text_view_emoji_disabled);
        AppCompatTextView enabledInAdvance = activity.findViewById(R.id.text_view_emoji_enabled);
        AppCompatTextView defaultEmoji = activity.findViewById(R.id.text_view_emoji_default);

        assertFalse(disabledInAdvance.isEmojiCompatEnabled());
        assertTrue(enabledInAdvance.isEmojiCompatEnabled());
        assertTrue(defaultEmoji.isEmojiCompatEnabled());

        defaultEmoji.setEmojiCompatEnabled(false);
        assertFalse(defaultEmoji.isEmojiCompatEnabled());
    }
}
