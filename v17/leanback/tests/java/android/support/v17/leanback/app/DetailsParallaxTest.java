/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.app;

import static org.junit.Assert.assertEquals;

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.v17.leanback.widget.DetailsParallax;
import android.support.v17.leanback.widget.RecyclerViewParallax;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link DetailsParallax}.
 */
@RunWith(JUnit4.class)
@SmallTest
public class DetailsParallaxTest {

    @Rule
    public ActivityTestRule<DetailsFragmentTestActivity> activityTestRule =
            new ActivityTestRule<>(DetailsFragmentTestActivity.class);
    private DetailsFragmentTestActivity mActivity;

    @Before
    public void setUp() {
        mActivity = activityTestRule.getActivity();
    }

    @Test
    public void setupTest() {
        double delta = 0.0002;
        DetailsParallax dpm = new DetailsParallax();
        dpm.setRecyclerView(mActivity.getDetailsFragment().getRowsFragment().getVerticalGridView());

        RecyclerViewParallax.ChildPositionProperty frameTop =
                (RecyclerViewParallax.ChildPositionProperty) dpm.getOverviewRowTop();
        assertEquals(0f, frameTop.getFraction(), delta);
        assertEquals(0f, frameTop.getAdapterPosition(), delta);


        RecyclerViewParallax.ChildPositionProperty frameBottom =
                (RecyclerViewParallax.ChildPositionProperty) dpm.getOverviewRowBottom();
        assertEquals(1f, frameBottom.getFraction(), delta);
        assertEquals(0f, frameBottom.getAdapterPosition(), delta);
    }
}
