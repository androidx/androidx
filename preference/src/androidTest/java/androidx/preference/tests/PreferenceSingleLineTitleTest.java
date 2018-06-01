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

package androidx.preference.tests;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SdkSuppress(maxSdkVersion = 27) // This test only works pre-P due to mocking final methods.
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PreferenceSingleLineTitleTest {

    private Preference mPreference;

    @Mock
    private ViewGroup mViewGroup;
    @Mock
    private TextView mTitleView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mViewGroup.findViewById(android.R.id.title)).thenReturn(mTitleView);

        mPreference = new Preference(InstrumentationRegistry.getTargetContext());
        mPreference.setTitle("Test Title");
    }

    @Test
    public void bindViewHolder_singleLineTitleNotSet_shouldNotSetSingleLine() {
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(mViewGroup);
        mPreference.onBindViewHolder(holder);

        verify(mTitleView, never()).setSingleLine(anyBoolean());
    }

    @Test
    public void bindViewHolder_singleLineTitleSetToTrue_shouldSetSingleLineToTrue() {
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(mViewGroup);
        mPreference.setSingleLineTitle(true);
        mPreference.onBindViewHolder(holder);

        verify(mTitleView).setSingleLine(true);
    }

    @Test
    public void bindViewHolder_singleLineTitleSetToFalse_shouldSetSingleLineToFalse() {
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(mViewGroup);
        mPreference.setSingleLineTitle(false);
        mPreference.onBindViewHolder(holder);

        verify(mTitleView).setSingleLine(false);
    }

}
