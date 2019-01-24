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

package androidx.car.drawer;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assume.assumeThat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.car.test.R;
import androidx.car.util.CarUxRestrictionsTestUtils;
import androidx.car.widget.PagedListView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for classes under {@link androidx.car.drawer}.
 *
 * <p>{@code mActivity} sets up a drawer using the common car drawer components. To set content of
 * drawer, use {@link CarDrawerTestActivity#getDrawerController()}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class CarDrawerTest {
    // Note that launchActivity is passed "false" here because we only want to create the
    // Activity after we checked that the test is being run on an auto device. Otherwise, this will
    // cause an error due to classes not being found.
    @Rule
    public ActivityTestRule<CarDrawerTestActivity> mActivityRule = new ActivityTestRule<>(
            CarDrawerTestActivity.class,
            false /* initialTouchMode */,
            false /* launchActivity */);

    private CarDrawerTestActivity mActivity;
    private PagedListView mDrawerList;

    /** Returns {@code true} if the testing device has the automotive feature flag. */
    private boolean isAutoDevice() {
        PackageManager packageManager =
                ApplicationProvider.getApplicationContext().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());

        // Retrieve the activity after the isAutoDevice() check because this class depends on
        // car-related classes (android.car.Car). These classes will not be available on non-auto
        // devices.
        mActivityRule.launchActivity(new Intent());
        mActivity = mActivityRule.getActivity();

        mDrawerList = mActivity.findViewById(R.id.car_drawer_list);
    }

    @After
    public void tearDown() {
        // The Activity is only launched if the test was run on an auto device. If it's been
        // launched, then explicitly finish it here since it was also explicitly launched.
        if (isAutoDevice()) {
            mActivityRule.finishActivity();
        }
    }

    private DrawerItemViewHolder getViewHolderAtPositionInDrawer(int position) {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        return (DrawerItemViewHolder) mDrawerList.getRecyclerView()
                .findViewHolderForAdapterPosition(position);
    }

    private void refreshUi() {
        try {
            mActivityRule.runOnUiThread(() -> mDrawerList.getAdapter().notifyDataSetChanged());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        // Wait for PagedListView to layout by using espresso to scroll to a position.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(0));
    }

    @Test
    public void testUxRestrictionsChange() throws Throwable {
        String longText = mActivity.getResources().getString(R.string.over_uxr_text_length_limit);
        CarDrawerAdapter adapter = new TextDrawerAdapter(mActivity, 5, longText);
        mActivityRule.runOnUiThread(() -> mActivity.getDrawerController().setRootAdapter(adapter));

        DrawerItemViewHolder vh = getViewHolderAtPositionInDrawer(0);
        final String originalText = (String) vh.getBodyView().getText();

        vh.onUxRestrictionsChanged(CarUxRestrictionsTestUtils.getFullyRestricted());
        refreshUi();

        assumeThat(vh.getBodyView().getText().length(), is(lessThan(originalText.length())));
    }

    /**
     * Drawer adapter that populates {@code itemCount} items, each with text set to {@code text}.
     */
    private static class TextDrawerAdapter extends CarDrawerAdapter {
        private int mItemCount;
        private String mText;

        TextDrawerAdapter(Context context, int itemCount, String text) {
            super(context, true);
            mItemCount = itemCount;
            mText = text;

            setTitle("title");
        }

        @Override
        protected int getActualItemCount() {
            return mItemCount;
        }

        @Override
        protected void populateViewHolder(DrawerItemViewHolder holder, int position) {
            holder.getBodyView().setText(mText);
        }

        @Override
        protected boolean usesSmallLayout(int position) {
            return false;
        }
    }

}
