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

package androidx.car.widget;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.car.test.R;

import org.hamcrest.Matcher;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the layout configuration in {@link SeekbarListItem}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SeekbarListItemTest {

    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);

    private PagedListViewTestActivity mActivity;
    private PagedListView mPagedListView;

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());
        mActivity = mActivityRule.getActivity();
        mPagedListView = mActivity.findViewById(R.id.paged_list_view);
    }

    private SeekbarListItem initSeekbarListItem() {
        SeekbarListItem item = new SeekbarListItem(mActivity);
        item.setMax(0);
        item.setProgress(0);
        item.setOnSeekBarChangeListener(null);
        item.setText(null);
        return item;
    }

    @Test
    public void testOnlySliderIsVisibleInEmptyItem() {
        SeekbarListItem item = initSeekbarListItem();

        setupPagedListView(Arrays.asList(item));
        verifyViewDefaultVisibility(mPagedListView.getRecyclerView().getLayoutManager()
                .getChildAt(0));
    }

    @Test
    public void testPrimaryActionVisible() {
        SeekbarListItem item0 = initSeekbarListItem();
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon);

        SeekbarListItem item1 = initSeekbarListItem();
        item1.setPrimaryActionIcon(new ColorDrawable(Color.BLACK));

        setupPagedListView(Arrays.asList(item0, item1));

        assertThat(getViewHolderAtPosition(0).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(1).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSliderTextVisible() {
        SeekbarListItem item = initSeekbarListItem();
        item.setText("Text");

        setupPagedListView(Arrays.asList(item));

        assertThat(getViewHolderAtPosition(0).getText().getVisibility(), is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSupplementalIconVisible() {
        SeekbarListItem item = initSeekbarListItem();
        item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);

        setupPagedListView(Arrays.asList(item));

        assertThat(getViewHolderAtPosition(0).getSupplementalIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(0).getSupplementalIconDivider().getVisibility(),
                is(equalTo(View.GONE)));
    }

    @Test
    public void testSupplementalIconDividerVisible() {
        SeekbarListItem item = initSeekbarListItem();
        item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);

        setupPagedListView(Arrays.asList(item));

        assertThat(getViewHolderAtPosition(0).getSupplementalIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(0).getSupplementalIconDivider().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSettingMax() {
        final int max = 50;
        SeekbarListItem item = new SeekbarListItem(mActivity);
        item.setMax(max);

        setupPagedListView(Arrays.asList(item));

        assertThat(getViewHolderAtPosition(0).getSeekBar().getMax(), is(equalTo(max)));
    }

    @Test
    public void testSettingProgress() {
        final int progress = 100;
        SeekbarListItem item = new SeekbarListItem(mActivity);
        item.setMax(progress);
        item.setProgress(progress);

        setupPagedListView(Arrays.asList(item));

        assertThat(getViewHolderAtPosition(0).getSeekBar().getProgress(), is(equalTo(progress)));
    }

    @Test
    public void testSettingSecondaryProgress() {
        final int secondaryProgress = 50;
        SeekbarListItem item = new SeekbarListItem(mActivity);
        item.setMax(secondaryProgress);
        item.setSecondaryProgress(secondaryProgress);

        setupPagedListView(Arrays.asList(item));

        assertThat(getViewHolderAtPosition(0).getSeekBar().getSecondaryProgress(),
                is(equalTo(secondaryProgress)));
    }

    @Test
    public void testSettingOnSeekBarChangeListener() throws Throwable {
        boolean[] changed = new boolean[]{false};

        SeekbarListItem item = new SeekbarListItem(mActivity);
        item.setMax(100);
        item.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                changed[0] = true;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        setupPagedListView(Arrays.asList(item));

        mActivityRule.runOnUiThread(() -> getViewHolderAtPosition(0).getSeekBar().setProgress(50));
        assertTrue(changed[0]);
    }

    @Test
    public void testUpdatingProgress() {
        final int progress = 50;
        final int newProgress = 100;

        SeekbarListItem item = new SeekbarListItem(mActivity);
        item.setMax(newProgress);
        item.setProgress(progress);
        setupPagedListView(Arrays.asList(item));

        item.setProgress(newProgress);
        refreshUi();

        assertThat(getViewHolderAtPosition(0).getSeekBar().getProgress(), is(equalTo(newProgress)));
    }

    @Test
    public void testPrimaryIconIsNotClickableWithoutListener() {
        SeekbarListItem item0 = initSeekbarListItem();
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon);

        SeekbarListItem item1 = initSeekbarListItem();
        item1.setPrimaryActionIcon(new ColorDrawable(Color.BLACK));

        setupPagedListView(Arrays.asList(item0, item1));

        assertFalse(getViewHolderAtPosition(0).getPrimaryIcon().isClickable());
        assertFalse(getViewHolderAtPosition(1).getPrimaryIcon().isClickable());
    }

    @Test
    public void testClickingPrimaryActionIcon() {
        boolean[] clicked = {false};
        SeekbarListItem item = initSeekbarListItem();
        item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon);
        item.setPrimaryActionIconListener(v -> clicked[0] = true);

        setupPagedListView(Arrays.asList(item));

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.primary_icon)));

        assertTrue(clicked[0]);
        assertTrue(getViewHolderAtPosition(0).getPrimaryIcon().isClickable());
    }

    @Test
    public void testSupplementalIconNotClickableWithoutListener() {
        SeekbarListItem item = initSeekbarListItem();
        item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);

        setupPagedListView(Arrays.asList(item));

        assertFalse(getViewHolderAtPosition(0).getSupplementalIcon().isClickable());
    }

    @Test
    public void testClickingSupplementalIcon() {
        boolean[] clicked = {false};
        SeekbarListItem item = initSeekbarListItem();
        item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);
        item.setSupplementalIconListener(v -> clicked[0] = true);

        setupPagedListView(Arrays.asList(item));

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.supplemental_icon)));

        assertTrue(clicked[0]);
    }

    @Test
    public void testPrimaryActionEmptyIconSpacing() {
        SeekbarListItem item0 = initSeekbarListItem();
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon);

        SeekbarListItem item1 = initSeekbarListItem();
        item1.setPrimaryActionIcon(new ColorDrawable(Color.BLACK));

        SeekbarListItem item2 = initSeekbarListItem();
        item2.setPrimaryActionEmptyIcon();

        List<ListItem> items = Arrays.asList(item0, item1, item2);
        setupPagedListView(items);

        for (int i = 1; i < items.size(); i++) {
            assertThat(getViewHolderAtPosition(i - 1).getSeekBar().getLeft(),
                    is(equalTo(getViewHolderAtPosition(i).getSeekBar().getLeft())));
        }
    }

    @Test
    public void testSupplementalIconSpacingWithoutDivider() {
        final boolean showDivider = false;
        SeekbarListItem item0 = initSeekbarListItem();
        item0.setSupplementalIcon(android.R.drawable.sym_def_app_icon, showDivider);

        SeekbarListItem item1 = initSeekbarListItem();
        item1.setSupplementalEmptyIcon(showDivider);

        List<ListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        for (int i = 1; i < items.size(); i++) {
            assertThat(getViewHolderAtPosition(i - 1).getSeekBar().getRight(),
                    is(equalTo(getViewHolderAtPosition(i).getSeekBar().getRight())));
        }
    }

    @Test
    public void testSupplementalIconSpacingWithDivider() {
        final boolean showDivider = true;
        SeekbarListItem item0 = initSeekbarListItem();
        item0.setSupplementalIcon(android.R.drawable.sym_def_app_icon, showDivider);

        SeekbarListItem item1 = initSeekbarListItem();
        item1.setSupplementalEmptyIcon(showDivider);

        List<ListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        for (int i = 1; i < items.size(); i++) {
            assertThat(getViewHolderAtPosition(i - 1).getSeekBar().getRight(),
                    is(equalTo(getViewHolderAtPosition(i).getSeekBar().getRight())));
        }
    }

    @Test
    public void testSettingSupplementalIconWithDrawable() {
        Drawable drawable = mActivity.getDrawable(android.R.drawable.sym_def_app_icon);
        SeekbarListItem item = initSeekbarListItem();
        item.setSupplementalIcon(drawable, false);

        setupPagedListView(Arrays.asList(item));

        assertThat(getViewHolderAtPosition(0).getSupplementalIcon().getDrawable(),
                is(equalTo(drawable)));
    }

    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    private void setupPagedListView(List<? extends ListItem> items) {
        ListItemProvider provider = new ListItemProvider.ListProvider(
                new ArrayList<>(items));
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.setAdapter(new ListItemAdapter(mActivity, provider));
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        // Wait for paged list view to layout by using espresso to scroll to a position.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(0));
    }

    private void verifyViewDefaultVisibility(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            final int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                verifyViewDefaultVisibility(viewGroup.getChildAt(i));
            }
        } else if (view instanceof SeekBar) {
            assertThat(view.getVisibility(), is(equalTo(View.VISIBLE)));
        } else {
            assertThat("Visibility of view "
                            + mActivity.getResources().getResourceEntryName(view.getId())
                            + " by default should be GONE.",
                    view.getVisibility(), is(equalTo(View.GONE)));
        }
    }

    private SeekbarListItem.ViewHolder getViewHolderAtPosition(int position) {
        return (SeekbarListItem.ViewHolder) mPagedListView.getRecyclerView()
                .findViewHolderForAdapterPosition(
                        position);
    }

    private void refreshUi() {
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.getAdapter().notifyDataSetChanged();
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        // Wait for paged list view to layout by using espresso to scroll to a position.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(0));
    }

    private static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specific id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                v.performClick();
            }
        };
    }
}
