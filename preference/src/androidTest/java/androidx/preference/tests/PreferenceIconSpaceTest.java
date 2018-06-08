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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.preference.AndroidResources;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SdkSuppress(maxSdkVersion = 27) // This test only works pre-P due to mocking final methods.
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PreferenceIconSpaceTest {

    private Preference mPreference;

    @Mock
    private ViewGroup mViewGroup;
    @Mock
    private ImageView mIconView;
    @Mock
    private View mImageFrame;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mViewGroup.findViewById(AndroidResources.ANDROID_R_ICON_FRAME))
                .thenReturn(mImageFrame);
        when(mViewGroup.findViewById(android.R.id.icon))
                .thenReturn(mIconView);

        mPreference = new Preference(InstrumentationRegistry.getTargetContext());
    }

    @Test
    @UiThreadTest
    public void bindViewHolder_iconSpaceReserved_shouldReserveIconSpace() {
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(mViewGroup);
        mPreference.setIconSpaceReserved(true);
        mPreference.onBindViewHolder(holder);

        verify(mIconView).setVisibility(View.INVISIBLE);
        verify(mImageFrame).setVisibility(View.INVISIBLE);
    }

    @LargeTest
    @Test
    @UiThreadTest
    public void bindViewHolder_iconSpaceNotReserved_shouldNotReserveIconSpace() {
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(mViewGroup);
        mPreference.setIconSpaceReserved(false);
        mPreference.onBindViewHolder(holder);

        verify(mIconView).setVisibility(View.GONE);
        verify(mImageFrame).setVisibility(View.GONE);
    }

    @Test
    @UiThreadTest
    public void bindViewHolder_hasIcon_shouldDisplayIcon() {
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(mViewGroup);
        mPreference.setIcon(new ColorDrawable(Color.BLACK));
        mPreference.onBindViewHolder(holder);

        verify(mIconView).setVisibility(View.VISIBLE);
        verify(mImageFrame).setVisibility(View.VISIBLE);
    }
}
