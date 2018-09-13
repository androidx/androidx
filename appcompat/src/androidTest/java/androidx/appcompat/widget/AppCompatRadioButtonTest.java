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
package androidx.appcompat.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Typeface;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import androidx.appcompat.graphics.drawable.AnimatedStateListDrawableCompat;
import androidx.appcompat.test.R;
import androidx.core.content.res.ResourcesCompat;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Provides tests specific to {@link AppCompatRadioButton} class.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppCompatRadioButtonTest {

    @Rule
    public final ActivityTestRule<AppCompatRadioButtonActivity> mActivityTestRule =
            new ActivityTestRule(AppCompatRadioButtonActivity.class);
    private AppCompatRadioButtonActivity mActivity;
    private ViewGroup mContainer;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mContainer = mActivity.findViewById(R.id.container);
    }

    @Test
    public void testFontResources() {
        AppCompatRadioButton radioButton = mContainer.findViewById(R.id.radiobutton_fontresource);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplefont);

        assertEquals(expected, radioButton.getTypeface());
    }

    @Test
    public void testDefaultButton_isAnimated() {
        // Given an ACRB with the theme's button drawable
        final AppCompatRadioButtonSpy radio = mContainer.findViewById(
                R.id.radiobutton_button_compat);
        final Drawable button = radio.mButton;

        // Then this drawable should be an animated-selector
        assertTrue(button instanceof AnimatedStateListDrawableCompat
                || button instanceof AnimatedStateListDrawable);
    }
}
