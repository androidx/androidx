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

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.preference.AndroidResources;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for reserving icon space in {@link Preference}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class PreferenceIconSpaceTest {

    private PreferenceViewHolder mHolder;
    private Preference mPreference;
    private ImageView mImageView;
    private View mImageFrame;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();

        mImageView = new ImageView(context);
        mImageFrame = new View(context);

        // Set the correct id so when findViewById() is called we return the relevant view
        mImageView.setId(android.R.id.icon);
        mImageFrame.setId(AndroidResources.ANDROID_R_ICON_FRAME);

        LinearLayout layout = new LinearLayout(context);
        layout.addView(mImageView);
        layout.addView(mImageFrame);

        mHolder = PreferenceViewHolder.createInstanceForTests(layout);

        mPreference = new Preference(context);
    }

    @Test
    @UiThreadTest
    public void bindViewHolder_iconSpaceReserved_shouldReserveIconSpace() {
        mPreference.setIconSpaceReserved(true);
        mPreference.onBindViewHolder(mHolder);

        assertEquals(View.INVISIBLE, mImageView.getVisibility());
        assertEquals(View.INVISIBLE, mImageFrame.getVisibility());
    }

    @Test
    @UiThreadTest
    public void bindViewHolder_iconSpaceNotReserved_shouldNotReserveIconSpace() {
        mPreference.setIconSpaceReserved(false);
        mPreference.onBindViewHolder(mHolder);

        assertEquals(View.GONE, mImageView.getVisibility());
        assertEquals(View.GONE, mImageFrame.getVisibility());
    }

    @Test
    @UiThreadTest
    public void bindViewHolder_hasIcon_shouldDisplayIcon() {
        mPreference.setIcon(new ColorDrawable(Color.BLACK));
        mPreference.onBindViewHolder(mHolder);

        assertEquals(View.VISIBLE, mImageView.getVisibility());
        assertEquals(View.VISIBLE, mImageFrame.getVisibility());
    }
}

