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
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.v17.leanback.R;
import android.support.v17.leanback.graphics.FitWidthBitmapDrawable;
import android.support.v17.leanback.testutils.PollingCheck;
import android.support.v17.leanback.widget.DetailsParallaxDrawable;
import android.support.v17.leanback.widget.VerticalGridView;
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
    public void parallaxTest() throws Throwable {
        final int mDefaultVerticalOffset = -300;
        Intent intent = new Intent();
        intent.putExtra(DetailsTestFragment.VERTICAL_OFFSET, mDefaultVerticalOffset);
        mActivity = activityTestRule.launchActivity(intent);

        final DetailsTestFragment detailsFragment = mActivity.getDetailsFragment();
        DetailsParallaxDrawable drawable =
                detailsFragment.getParallaxDrawable();
        final FitWidthBitmapDrawable bitmapDrawable = (FitWidthBitmapDrawable)
                drawable.getCoverDrawable();

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mActivity.getDetailsFragment().getRowsFragment().getAdapter().size() > 1;
            }
        });

        final VerticalGridView verticalGridView = mActivity.getDetailsFragment()
                .getRowsFragment().getVerticalGridView();
        final int windowHeight = verticalGridView.getHeight();
        final int windowWidth = verticalGridView.getWidth();
        // make sure background manager attached to window is same size as VerticalGridView
        // i.e. no status bar.
        assertEquals(windowHeight, mActivity.getWindow().getDecorView().getHeight());
        assertEquals(windowWidth, mActivity.getWindow().getDecorView().getWidth());

        final View detailsFrame = verticalGridView.findViewById(R.id.details_frame);

        assertEquals(windowWidth, bitmapDrawable.getBounds().width());

        final Rect detailsFrameRect = new Rect();
        detailsFrameRect.set(0, 0, detailsFrame.getWidth(), detailsFrame.getHeight());
        verticalGridView.offsetDescendantRectToMyCoords(detailsFrame, detailsFrameRect);

        assertEquals(Math.min(windowHeight, detailsFrameRect.top),
                bitmapDrawable.getBounds().height());
        assertEquals(0, bitmapDrawable.getVerticalOffset());

        assertTrue("TitleView is visible", detailsFragment.getView()
                .findViewById(R.id.browse_title_group).getVisibility() == View.VISIBLE);

        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                verticalGridView.scrollToPosition(1);
            }
        });

        PollingCheck.waitFor(4000, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return bitmapDrawable.getVerticalOffset() == mDefaultVerticalOffset
                        && detailsFragment.getView()
                        .findViewById(R.id.browse_title_group).getVisibility() != View.VISIBLE;
            }
        });

        detailsFrameRect.set(0, 0, detailsFrame.getWidth(), detailsFrame.getHeight());
        verticalGridView.offsetDescendantRectToMyCoords(detailsFrame, detailsFrameRect);

        assertEquals(0, bitmapDrawable.getBounds().top);
        assertEquals(Math.max(detailsFrameRect.top, 0), bitmapDrawable.getBounds().bottom);
        assertEquals(windowWidth, bitmapDrawable.getBounds().width());

        ColorDrawable colorDrawable = (ColorDrawable) (drawable.getChildAt(1).getDrawable());
        assertEquals(windowWidth, colorDrawable.getBounds().width());
        assertEquals(detailsFrameRect.bottom, colorDrawable.getBounds().top);
        assertEquals(windowHeight, colorDrawable.getBounds().bottom);
    }
}
