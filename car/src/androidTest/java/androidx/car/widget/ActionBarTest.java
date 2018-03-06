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

package androidx.car.widget;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import androidx.core.util.Preconditions;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.ToggleButton;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.car.R;

/** Unit tests for {@link ActionBar}. */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class ActionBarTest {
    @Rule
    public ActivityTestRule<ActionBarTestActivity> mActivityRule =
            new ActivityTestRule<>(ActionBarTestActivity.class);
    private ActionBarTestActivity mActivity;
    private ActionBar mActionBar;
    private LinearLayout mRowsContainer;
    private View[] mItems;

    private static final int TOP_ROW_IDX = 0;
    private static final int BOTTOM_ROW_IDX = 1;
    private static final int NUM_COLS = 5;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mActionBar = mActivity.findViewById(androidx.car.test.R.id.action_bar);
        mRowsContainer = mActionBar.findViewById(R.id.rows_container);
    }

    private void setUpActionBarItems(int numItems) {
        mItems = new View[numItems];
        for (int i = 0; i < numItems; i++) {
            mItems[i] = createButton(mActivity);
        }
        mActivity.runOnUiThread(() ->  mActionBar.setViews(mItems));
    }

    private ImageButton createButton(Context context) {
        ImageButton button = new ImageButton(context, null, R.style.Widget_Car_Button_ActionBar);
        button.setImageDrawable(context.getDrawable(androidx.car.test.R.drawable.ic_overflow));
        return button;
    }

    /**
     * Asserts that only the first 'numItems' slots are used.
     */
    private void assertLeftItemsNotEmpty(int rowIdx, int numItems) {
        for (int colIdx = 0; colIdx < NUM_COLS - 1; colIdx++) {
            if (colIdx < numItems) {
                assertNotNull(String.format("Slot (%d, %d) should be taken", rowIdx, colIdx),
                        mActionBar.getViewAt(rowIdx, colIdx));
            } else {
                assertNull(String.format("Slot (%d, %d) should be empty", rowIdx, colIdx),
                        mActionBar.getViewAt(rowIdx, colIdx));
            }
        }
    }

    /**
     * Tests that the bar with no children views is displayed correctly
     */
    @Test
    public void testEmptyState() {
        setUpActionBarItems(0);
        onView(withId(androidx.car.test.R.id.action_bar)).check((view, noViewFoundException) -> {
            Preconditions.checkNotNull(view);
            // All slots should be empty.
            assertLeftItemsNotEmpty(TOP_ROW_IDX, 0);
            assertLeftItemsNotEmpty(BOTTOM_ROW_IDX, 0);
        });
    }

    /**
     * Tests that slots are used from left to right and from bottom to top
     */
    @Test
    public void testNormalSlotUsage() {
        for (int items = 1; items < NUM_COLS - 1; items++) {
            setUpActionBarItems(items);
            final int numItems = items;
            onView(withId(androidx.car.test.R.id.action_bar))
                    .check((view, noViewFoundException) -> {
                        Preconditions.checkNotNull(view);
                        // Top row should be empty
                        assertLeftItemsNotEmpty(TOP_ROW_IDX, 0);
                        // Expand/collapse slot should be empty
                        assertNull("Expand/collapse should be empty" ,
                                mActionBar.getViewAt(BOTTOM_ROW_IDX, NUM_COLS - 1));
                        // Slots on the bottom left should be taken while the rest should be empty.
                        assertLeftItemsNotEmpty(BOTTOM_ROW_IDX, numItems);
                    });
        }
    }

    private void assertRowVisibility(int rowIdx, int visibility) {
        assertEquals(visibility, mRowsContainer.getChildAt(rowIdx).getVisibility());
    }

    /**
     * Tests that the expand/collapse button is added if enough views are set
     */
    @Test
    public void testExpandCollapseEnabled() {
        setUpActionBarItems(NUM_COLS + 1);

        // Top row should have 2 slot taken (as expand/collapse takes one slot on the bottom row)
        onView(withContentDescription(R.string.action_bar_expand_collapse_button))
                .check((view, noViewFoundException) -> {
                    Preconditions.checkNotNull(view);
                    assertLeftItemsNotEmpty(TOP_ROW_IDX, 2);
                    assertLeftItemsNotEmpty(BOTTOM_ROW_IDX, NUM_COLS);
                    assertRowVisibility(TOP_ROW_IDX, View.GONE);
                })
                // Check that expand/collapse works
                .perform(click())
                .check((view, noViewFoundException) -> {
                    assertRowVisibility(TOP_ROW_IDX, View.VISIBLE);
                })
                .perform(click())
                .check((view, noViewFoundException) -> {
                    assertRowVisibility(TOP_ROW_IDX, View.GONE);
                });
    }

    private void setViewInPosition(View view, @ActionBar.SlotPosition int position) {
        mActivity.runOnUiThread(() -> {
            mActionBar.setView(view, position);
        });
    }

    /**
     * Tests that reserved slots are not used by normal views.
     */
    @Test
    public void testReservingNamedSlots() {
        View mainView = createButton(mActivity);
        setViewInPosition(mainView, ActionBar.SLOT_MAIN);
        View leftView = new Space(mActivity);
        setViewInPosition(leftView, ActionBar.SLOT_LEFT);
        setUpActionBarItems(NUM_COLS + 1);

        // Expand/collapse plus two other slots should be taken in the bottom row.
        onView(withContentDescription(R.string.action_bar_expand_collapse_button))
                .check((view, noViewFoundException) -> {
                    // Only 2 items fit in the bottom row. The remaining 4 should be on the top
                    Preconditions.checkNotNull(view);
                    assertLeftItemsNotEmpty(TOP_ROW_IDX, 4);
                    assertLeftItemsNotEmpty(BOTTOM_ROW_IDX, NUM_COLS);
                    assertRowVisibility(TOP_ROW_IDX, View.GONE);
                    assertEquals(mainView, mActionBar.getViewAt(BOTTOM_ROW_IDX, 2));
                    assertEquals(leftView, mActionBar.getViewAt(BOTTOM_ROW_IDX, 1));
                })
                .perform(click())
                .check((view, noViewFoundException) -> {
                    assertRowVisibility(TOP_ROW_IDX, View.VISIBLE);
                });
    }

    private void setExpandCollapseCustomView(View view) {
        mActivity.runOnUiThread(() -> {
            mActionBar.setExpandCollapseView(view);
        });
    }

    /**
     * Tests setting custom expand/collapse views.
     */
    @Test
    public void testCustomExpandCollapseView() {
        View customExpandCollapse = new ToggleButton(mActivity);
        customExpandCollapse.setContentDescription(mActivity.getString(
                R.string.action_bar_expand_collapse_button));
        setExpandCollapseCustomView(customExpandCollapse);
        setUpActionBarItems(NUM_COLS + 1);

        onView(withContentDescription(R.string.action_bar_expand_collapse_button))
                .check((view, noViewFoundException) -> {
                    Preconditions.checkNotNull(view);
                    assertEquals(customExpandCollapse, mActionBar.getViewAt(BOTTOM_ROW_IDX,
                            NUM_COLS - 1));
                });
    }
}
