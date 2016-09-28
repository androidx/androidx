/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.v17.leanback.R;
import android.support.v17.leanback.graphics.CompositeDrawable;
import android.support.v17.leanback.graphics.FitWidthBitmapDrawable;
import android.support.v17.leanback.testutils.PollingCheck;
import android.view.View;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Unit tests for {@link DetailsTestFragment}.
 */
@RunWith(JUnit4.class)
@MediumTest
public class DetailsFragmentTest {

    @Rule
    public ActivityTestRule<DetailsFragmentTestActivity> activityTestRule =
            new ActivityTestRule<>(DetailsFragmentTestActivity.class, false, false);
    private DetailsFragmentTestActivity mActivity;

    @Test
    public void parallaxTest() throws InterruptedException {
        final int mDefaultVerticalOffset = -300;
        Intent intent = new Intent();
        intent.putExtra(DetailsTestFragment.VERTICAL_OFFSET, mDefaultVerticalOffset);
        mActivity = activityTestRule.launchActivity(intent);

        final DetailsTestFragment detailsFragment = mActivity.getDetailsFragment();
        DetailsBackgroundParallaxHelper parallaxHelper = detailsFragment.getParallaxHelper();
        final CompositeDrawable drawable = (CompositeDrawable) parallaxHelper.getDrawable();
        final FitWidthBitmapDrawable bitmapDrawable = (FitWidthBitmapDrawable)
                (drawable.getChildAt(0).getDrawable());

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mActivity.getDetailsFragment().getRowsFragment().getAdapter().size() > 1;
            }
        });

        int windowHeight = mActivity.getWindow().getDecorView().getHeight();
        int windowWidth = mActivity.getWindow().getDecorView().getWidth();

        Rect bounds = drawable.getChildAt(0).getDrawable().getBounds();
        assertEquals(windowWidth, bounds.width());
        assertEquals(mActivity.getResources().getDimensionPixelSize(
                R.dimen.lb_details_v2_align_pos_for_actions), bounds.height());
        assertEquals(0, bitmapDrawable.getVerticalOffset());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                detailsFragment.getRowsFragment().getVerticalGridView().scrollToPosition(1);
            }
        });

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return bitmapDrawable.getVerticalOffset() == mDefaultVerticalOffset;
            }
        });

        bounds = drawable.getChildAt(0).getDrawable().getBounds();
        assertEquals(mActivity.getResources().getDimensionPixelSize(
                R.dimen.lb_details_v2_align_pos_for_description), bounds.height());
        assertEquals(windowWidth, bounds.width());

        View detailsFrame = mActivity.findViewById(R.id.details_frame);
        int [] loc = new int[2];
        detailsFrame.getLocationOnScreen(loc);
        ColorDrawable colorDrawable = (ColorDrawable) (drawable.getChildAt(1).getDrawable());

        assertEquals(windowWidth, colorDrawable.getBounds().width());
        // Since bottom is using float mapping, using float compare with delta
        assertEquals(windowHeight - (loc[1] + detailsFrame.getHeight()),
                (float) colorDrawable.getBounds().height(), 2 /*delta*/);
    }
}
