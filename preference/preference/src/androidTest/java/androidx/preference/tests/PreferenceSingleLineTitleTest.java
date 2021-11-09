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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for single line titles in {@link Preference}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class PreferenceSingleLineTitleTest {

    private PreferenceViewHolder mHolder;
    private Preference mPreference;
    private TextView mTitleView;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();

        // Create a spy of the title so we can verify setSingleLine() behaviour
        mTitleView = spy(new TextView(context));

        // Set the correct id so when findViewById() is called we return the spy
        mTitleView.setId(android.R.id.title);

        LinearLayout layout = new LinearLayout(context);
        layout.addView(mTitleView);

        mHolder = PreferenceViewHolder.createInstanceForTests(layout);

        mPreference = new Preference(context);
        mPreference.setTitle("Test Title");
    }

    @Test
    @UiThreadTest
    public void bindViewHolder_singleLineTitleNotSet_shouldNotSetSingleLine() {
        mPreference.onBindViewHolder(mHolder);

        verify(mTitleView, never()).setSingleLine(anyBoolean());
    }

    @Test
    @UiThreadTest
    public void bindViewHolder_singleLineTitleSetToTrue_shouldSetSingleLineToTrue() {
        mPreference.setSingleLineTitle(true);
        mPreference.onBindViewHolder(mHolder);

        verify(mTitleView).setSingleLine(true);
    }

    @Test
    @UiThreadTest
    public void bindViewHolder_singleLineTitleSetToFalse_shouldSetSingleLineToFalse() {
        mPreference.setSingleLineTitle(false);
        mPreference.onBindViewHolder(mHolder);

        verify(mTitleView).setSingleLine(false);
    }

}
