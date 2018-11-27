/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.appcompat.testutils.TestUtilsMatchers.thumbColor;
import static androidx.appcompat.testutils.TestUtilsMatchers.trackColor;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;

import android.graphics.Typeface;
import android.view.ViewGroup;

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
 * Provides tests specific to {@link SwitchCompat} class.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class SwitchCompatTest {

    @Rule
    public final ActivityTestRule<SwitchCompatActivity> mActivityTestRule =
            new ActivityTestRule(SwitchCompatActivity.class);
    private SwitchCompatActivity mActivity;
    private ViewGroup mContainer;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mContainer = mActivity.findViewById(androidx.appcompat.test.R.id.container);
    }

    @Test
    public void testFontResources() {
        SwitchCompat switchButton = mContainer.findViewById(R.id.switch_fontresource);
        Typeface expected = ResourcesCompat.getFont(mActivity, R.font.samplefont);

        assertEquals(expected, switchButton.getTypeface());
    }

    @Test
    public void testTint() {
        // Given a switch with tints set for the track and thumb
        final int expectedThumbTint = 0xffff00ff;
        final int expectedTrackTint = 0xff00ffff;

        // Then the tints should be applied
        onView(withId(R.id.switch_tint)).check(matches(thumbColor(expectedThumbTint)));
        onView(withId(R.id.switch_tint)).check(matches(trackColor(expectedTrackTint)));
    }
}
