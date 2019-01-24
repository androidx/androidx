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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.FrameLayout;

import androidx.car.test.R;
import androidx.car.uxrestrictions.CarUxRestrictions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

/** Unit tests for {@link ListItemAdapter}. */
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class ListItemAdapterTest {
    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);

    private Context mContext;

    /** Returns {@code true} if the testing device has the automotive feature flag. */
    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());
        mContext = mActivityRule.getActivity();
    }

    @Test
    public void testCreateTextListItemType_returnsCorrectViewHolder() {
        ListItemAdapter adapter = new ListItemAdapter(mContext, /* itemProvider= */ null);

        // Note that the type of parent in this case does not matter.
        ListItem.ViewHolder viewHolder = adapter.onCreateViewHolder(
                /* parent= */ new FrameLayout(mContext),
                ListItemAdapter.LIST_ITEM_TYPE_TEXT);

        assertTrue(viewHolder.getClass() == TextListItem.ViewHolder.class);
    }

    @Test
    public void testCreateSeekbarListItemType_returnsCorrectViewHolder() {
        ListItemAdapter adapter = new ListItemAdapter(mContext, /* itemProvider= */ null);

        // Note that the type of parent in this case does not matter.
        ListItem.ViewHolder viewHolder = adapter.onCreateViewHolder(
                /* parent= */ new FrameLayout(mContext),
                ListItemAdapter.LIST_ITEM_TYPE_SEEKBAR);

        assertTrue(viewHolder.getClass() == SeekbarListItem.ViewHolder.class);
    }

    @Test
    public void testCreateSubheaderListItemType_returnsCorrectViewHolder() {
        ListItemAdapter adapter = new ListItemAdapter(mContext, /* itemProvider= */ null);

        // Note that the type of parent in this case does not matter.
        ListItem.ViewHolder viewHolder = adapter.onCreateViewHolder(
                /* parent= */ new FrameLayout(mContext),
                ListItemAdapter.LIST_ITEM_TYPE_SUBHEADER);

        assertTrue(viewHolder.getClass() == SubheaderListItem.ViewHolder.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterListItemType_positiveViewTypeThrowsException() {
        ListItemAdapter adapter = new ListItemAdapter(mContext, /* itemProvider= */ null);

        // Using a positive value for the view type, which should throw an error.
        int viewType = 1;

        // Using a random layout. The actual values does not matter.
        int layoutRes = R.layout.activity_column_card_view;

        adapter.registerListItemViewType(
                viewType, layoutRes, CustomListItem::createViewHolder);
    }

    @Test
    public void testRegisterListItemType_createsCorrectCustomListItem() {
        ListItemAdapter adapter = new ListItemAdapter(mContext, /* itemProvider= */ null);

        // Using a random layout. The actual values does not matter.
        int layoutRes = R.layout.activity_column_card_view;

        adapter.registerListItemViewType(
                CustomListItem.LIST_ITEM_ID, layoutRes, CustomListItem::createViewHolder);

        // Note that the type of parent in this case does not matter.
        ListItem.ViewHolder viewHolder = adapter.onCreateViewHolder(
                /* parent= */ new FrameLayout(mContext),
                CustomListItem.LIST_ITEM_ID);

        assertTrue(viewHolder.getClass() == CustomListItem.ViewHolder.class);
    }

    @Test
    public void testGetItemCount_returnsCorrectItemCountWhenUnlimited() {
        // A fixed number of items. The contents do not matter.
        List<ListItem> items = Arrays.asList(
                new TextListItem(mContext),
                new TextListItem(mContext),
                new TextListItem(mContext));

        ListItemAdapter adapter = new ListItemAdapter(mContext,
                new ListItemProvider.ListProvider(items));
        adapter.setMaxItems(PagedListView.ItemCap.UNLIMITED);

        // Verify that item count matches
        assertEquals(items.size(), adapter.getItemCount());
    }

    @Test
    public void testGetItemCount_respectsMaxItems() {
        // A fixed number of items. The contents do not matter.
        List<ListItem> items = Arrays.asList(
                new TextListItem(mContext),
                new TextListItem(mContext),
                new TextListItem(mContext));

        ListItemAdapter adapter = new ListItemAdapter(mContext,
                new ListItemProvider.ListProvider(items));

        // Set the max items as "2", which is less than the size of "items".
        int maxItems = 2;
        adapter.setMaxItems(maxItems);

        // Verify that item count matches
        assertEquals(maxItems, adapter.getItemCount());
    }

    @Test
    public void testShowDivider_respectsListItemsHideDivider() {
        TextListItem hiddenDividerItem = new TextListItem(mContext);
        hiddenDividerItem.setShowDivider(false);

        TextListItem dividerItem = new TextListItem(mContext);
        dividerItem.setShowDivider(true);

        List<ListItem> items = Arrays.asList(
                hiddenDividerItem,
                dividerItem);

        ListItemAdapter adapter = new ListItemAdapter(mContext,
                new ListItemProvider.ListProvider(items));

        assertFalse(adapter.getShowDivider(0));
        assertTrue(adapter.getShowDivider(1));
    }

    /** An extension of {@link ListItem} to be used for testing custom extensions. */
    private static class CustomListItem extends ListItem<CustomListItem.ViewHolder> {
        static final int LIST_ITEM_ID = -1;

        @Override
        protected void resolveDirtyState() {
            // No-op.
        }

        @Override
        protected void onBind(ViewHolder viewHolder) {
            // No-op.
        }

        @Override
        public void setEnabled(boolean enabled) {
            // No-op.
        }

        @Override
        public int getViewType() {
            return LIST_ITEM_ID;
        }

        /** Creates a {@link CustomListItem.ViewHolder}. */
        static ViewHolder createViewHolder(View itemView) {
            return new ViewHolder(itemView);
        }

        public static class ViewHolder extends ListItem.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }

            @Override
            public void onUxRestrictionsChanged(CarUxRestrictions restrictionInfo) {
                // No-op
            }
        }
    }
}
