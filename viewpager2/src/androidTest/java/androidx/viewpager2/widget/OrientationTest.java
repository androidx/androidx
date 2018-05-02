/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget;

import static androidx.core.util.Preconditions.checkNotNull;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.LayoutInflater;

import androidx.viewpager2.test.R;
import androidx.viewpager2.widget.ViewPager2.Orientation;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class OrientationTest {
    @Test
    public void test_orientation_noAttrs() {
        ViewPager2 viewPager = new ViewPager2(InstrumentationRegistry.getContext());
        assertThat(viewPager.getOrientation(), equalTo(Orientation.HORIZONTAL));
    }

    @Test
    public void test_orientation_nullAttrs() {
        ViewPager2 viewPager = new ViewPager2(InstrumentationRegistry.getContext(), null);
        assertThat(viewPager.getOrientation(), equalTo(Orientation.HORIZONTAL));
    }

    @Test
    public void test_orientation_default() {
        assertOrientationCorrect(R.layout.orientation_default, Orientation.HORIZONTAL);
    }

    @Test
    public void test_orientation_horizontal() {
        assertOrientationCorrect(R.layout.orientation_horizontal, Orientation.HORIZONTAL);
    }

    @Test
    public void test_orientation_vertical() {
        assertOrientationCorrect(R.layout.orientation_vertical, Orientation.VERTICAL);
    }

    private void assertOrientationCorrect(int layoutId, int expectedOrientation) {
        LayoutInflater layoutInflater = (LayoutInflater) checkNotNull(InstrumentationRegistry
                .getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE));
        ViewPager2 viewPager = (ViewPager2) layoutInflater.inflate(layoutId, null);
        assertThat(viewPager.getOrientation(), equalTo(expectedOrientation));
    }
}
