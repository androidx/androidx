/*
 * Copyright 2017 The Android Open Source Project
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

package com.example.androidx.car;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Toast;

import androidx.car.widget.ActionListItem;
import androidx.car.widget.CarToolbar;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.TextListItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Demo activity for {@link ListItem}.
 */
public class TextListItemActivity extends Activity {

    private static int pixelToDip(Context context, int pixels) {
        return (int) (pixels / context.getResources().getDisplayMetrics().density);
    }

    PagedListView mPagedListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paged_list_view);

        CarToolbar toolbar = findViewById(R.id.car_toolbar);
        toolbar.setTitle(R.string.text_list_item_title);
        toolbar.setNavigationIconOnClickListener(v -> finish());

        mPagedListView = findViewById(R.id.paged_list_view);

        SampleProvider provider = new SampleProvider(this);
        ListItemAdapter adapter = new ListItemAdapter(this, provider,
                ListItemAdapter.BACKGROUND_STYLE_NONE);

        final boolean[] showDivider = {false};
        // Demonstrate how to update list item post construction.
        TextListItem toBeUpdated = new TextListItem(this);
        toBeUpdated.setPrimaryActionEmptyIcon();
        toBeUpdated.setTitle("tap next item to update my icon");
        toBeUpdated.setShowDivider(showDivider[0]);
        provider.mItems.add(0, toBeUpdated);

        boolean[] useEmptyIcon = new boolean[]{false};
        TextListItem update = new TextListItem(this);
        update.setTitle("tap me to update the icon of item above");
        update.setOnClickListener(v -> {
            // Change icon.
            if (useEmptyIcon[0]) {
                toBeUpdated.setPrimaryActionEmptyIcon();
            } else {
                toBeUpdated.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                        TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            }
            useEmptyIcon[0] = !useEmptyIcon[0];

            // Show/hide item divider.
            toBeUpdated.setShowDivider(showDivider[0]);
            showDivider[0] = !showDivider[0];

            // Make sure to notify adapter about the change.
            adapter.notifyItemChanged(0);
        });
        provider.mItems.add(1, update);

        TextListItem testItem = new TextListItem(this);
        testItem.setTitle("Switch - refresh self");
        testItem.setSwitch(false, true, (button, isChecked) -> {
            testItem.setBody(isChecked ? "checked" : "unchecked");
            adapter.notifyItemChanged(provider.mItems.indexOf(testItem));
        });
        provider.mItems.add(5, testItem);

        mPagedListView.setAdapter(adapter);
        mPagedListView.setMaxPages(PagedListView.UNLIMITED_PAGES);
        mPagedListView.setDividerVisibilityManager(adapter);
    }

    private static class SampleProvider extends ListItemProvider {
        private Context mContext;
        List<ListItem> mItems;

        private View.OnClickListener mOnClickListener = v ->
                Toast.makeText(mContext, "Clicked!", Toast.LENGTH_SHORT).show();

        private View.OnClickListener mGetParentHeight = v -> {
            int parentHeight = ((View) v.getParent().getParent()).getHeight();
            Toast.makeText(v.getContext(),
                    "Card Height is " + pixelToDip(mContext, parentHeight) + " dp",
                    Toast.LENGTH_SHORT).show();
        };

        private ListItemProvider.ListProvider mListProvider;

        SampleProvider(Context context) {
            mContext = context;
            mItems = new ArrayList<>();

            TextListItem item;
            ActionListItem actionItem;

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(mContext.getDrawable(R.drawable.pressed_icon),
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);
            item.setTitle("single line with clickable primary icon");
            item.addViewBinder(vh -> vh.getPrimaryIcon().setClickable(true));
            mItems.add(item);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    ActionListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);
            actionItem.setTitle("single line with large icon and one action");
            actionItem.setPrimaryAction("Card Height", false, mGetParentHeight);
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    ActionListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);
            actionItem.setTitle("single line with large icon and one secondary action");
            actionItem.setSecondaryAction("Card Height", true, mGetParentHeight);
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    ActionListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);
            actionItem.setTitle("single line with large icon and one raised action");
            actionItem.setPrimaryAction("Card Height", false, mGetParentHeight);
            actionItem.setActionBorderless(false);
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    ActionListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);
            actionItem.setTitle("single line with large icon, divider, one raised action");
            actionItem.setPrimaryAction("Card Height", true, mGetParentHeight);
            actionItem.setActionBorderless(false);
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    ActionListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);
            actionItem.setTitle("single line with large icon, and one raised secondary action");
            actionItem.setSecondaryAction("Card Height", false, mGetParentHeight);
            actionItem.setActionBorderless(false);
            mItems.add(actionItem);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);
            item.setTitle("single line with medium icon");
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setOnClickListener(mOnClickListener);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            item.setTitle("clickable single line with small icon and clickable end icon");
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true, mGetParentHeight);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            item.setTitle("single line without a list divider");
            item.setShowDivider(false);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setOnClickListener(mOnClickListener);
            item.setPrimaryActionEmptyIcon();
            item.setTitle("clickable single line with empty icon and end icon no divider");
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionEmptyIcon();
            item.setTitle("body with clickable link");
            item.setBody(mContext.getText(R.string.test_link));
            item.addViewBinder(
                    vh -> vh.getBody().setMovementMethod(LinkMovementMethod.getInstance()));
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setTitle("title is single line and ellipsizes. "
                    + mContext.getString(R.string.long_text));
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setTitle("Subtitle-like line without a list divider");
            item.setShowDivider(false);
            item.addViewBinder(viewHolder ->
                            viewHolder.getTitle().setTextAppearance(R.style.CarListSubtitle));
            mItems.add(item);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionNoIcon();
            actionItem.setTitle("single line with two actions and no divider");
            actionItem.setPrimaryAction("Action 1", false,
                    v -> Toast.makeText(v.getContext(), "Action 1", Toast.LENGTH_SHORT).show());
            actionItem.setSecondaryAction("Action 2", false,
                    v -> Toast.makeText(v.getContext(), "Action 2", Toast.LENGTH_SHORT).show());
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionNoIcon();
            actionItem.setTitle("single line with two raised actions and no divider");
            actionItem.setPrimaryAction("Action 1", false,
                    v -> Toast.makeText(v.getContext(), "Action 1", Toast.LENGTH_SHORT).show());
            actionItem.setSecondaryAction("Action 2", false,
                    v -> Toast.makeText(v.getContext(), "Action 2", Toast.LENGTH_SHORT).show());
            actionItem.setActionBorderless(false);
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionNoIcon();
            actionItem.setTitle("single line with two actions and Action 2 divider");
            actionItem.setPrimaryAction("Action 1", false,
                    v -> Toast.makeText(v.getContext(), "Action 1", Toast.LENGTH_SHORT).show());
            actionItem.setSecondaryAction("Action 2", true,
                    v -> Toast.makeText(v.getContext(), "Action 2", Toast.LENGTH_SHORT).show());
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionNoIcon();
            actionItem.setTitle("single line with two raised actions and Action 2 divider");
            actionItem.setPrimaryAction("Action 1", false,
                    v -> Toast.makeText(v.getContext(), "Action 1", Toast.LENGTH_SHORT).show());
            actionItem.setSecondaryAction("Action 2", true,
                    v -> Toast.makeText(v.getContext(), "Action 2", Toast.LENGTH_SHORT).show());
            actionItem.setActionBorderless(false);
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionNoIcon();
            actionItem.setTitle("single line with divider between actions. "
                    + mContext.getString(R.string.long_text));
            actionItem.setPrimaryAction("Action 1", true,
                    v -> Toast.makeText(v.getContext(), "Action 1", Toast.LENGTH_SHORT).show());
            actionItem.setSecondaryAction("Action 2", false,
                    v -> Toast.makeText(v.getContext(), "Action 2", Toast.LENGTH_SHORT).show());
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionNoIcon();
            actionItem.setTitle("single line with divider between raised actions. "
                    + mContext.getString(R.string.long_text));
            actionItem.setPrimaryAction("Action 1", true,
                    v -> Toast.makeText(v.getContext(), "Action 1", Toast.LENGTH_SHORT).show());
            actionItem.setSecondaryAction("Action 2", false,
                    v -> Toast.makeText(v.getContext(), "Action 2", Toast.LENGTH_SHORT).show());
            actionItem.setActionBorderless(false);
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionNoIcon();
            actionItem.setTitle("single line with both dividers for actions. "
                    + mContext.getString(R.string.long_text));
            actionItem.setPrimaryAction("Action 1 with really long text", true,
                    v -> Toast.makeText(v.getContext(), "Action 1", Toast.LENGTH_SHORT).show());
            actionItem.setSecondaryAction("Action 2 with really long text", true,
                    v -> Toast.makeText(v.getContext(), "Action 2", Toast.LENGTH_SHORT).show());
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionNoIcon();
            actionItem.setTitle("single line with both dividers for raised actions. "
                    + mContext.getString(R.string.long_text));
            actionItem.setPrimaryAction("Action 1 with really long text", true,
                    v -> Toast.makeText(v.getContext(), "Action 1", Toast.LENGTH_SHORT).show());
            actionItem.setSecondaryAction("Action 2 with really long text", true,
                    v -> Toast.makeText(v.getContext(), "Action 2", Toast.LENGTH_SHORT).show());
            actionItem.setActionBorderless(false);
            mItems.add(actionItem);

            item = new TextListItem(mContext);
            item.setTitle("item longer than containing View size");
            item.setBody(mContext.getResources().getString(R.string.super_long_text));
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);
            item.setTitle("double line with full icon and no end icon divider");
            item.setBody("one line text");
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false, mGetParentHeight);
            mItems.add(item);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            actionItem.setTitle("double line with small icon and one action");
            actionItem.setBody(mContext.getString(R.string.long_text));
            actionItem.setPrimaryAction("Card Height", true, mGetParentHeight);
            mItems.add(actionItem);

            String tenChars = "Ten Chars.";
            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            item.setTitle("Card with small icon and text longer than limit");
            item.setBody(TextUtils.join("", Collections.nCopies(20, tenChars)));
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true, mGetParentHeight);
            mItems.add(item);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    ActionListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);
            actionItem.setBody("Only body - no title is set");
            actionItem.setPrimaryAction("Card Height", true, mGetParentHeight);
            mItems.add(actionItem);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            item.setBody("Only body - no title. " + mContext.getString(R.string.long_text));
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            item.setTitle("Switch - initially unchecked");
            item.setSwitch(false, true, (button, isChecked) -> Toast.makeText(mContext,
                    isChecked ? "checked" : "unchecked", Toast.LENGTH_SHORT).show());
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            item.setTitle("Switch");
            item.setBody("with body " + mContext.getString(R.string.long_text));
            item.setSwitch(false, true, (button, isChecked) -> {
                Toast.makeText(mContext,
                        isChecked ? "checked" : "unchecked", Toast.LENGTH_SHORT).show();
            });
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            item.setBody("with only body " + mContext.getString(R.string.long_text));
            item.setSwitch(false, true, (button, isChecked) -> {
                Toast.makeText(mContext,
                        isChecked ? "checked" : "unchecked", Toast.LENGTH_SHORT).show();
            });
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setBody("Body with custom text appearance");
            item.addViewBinder(
                    vh -> vh.getBody().setTextAppearance(R.style.TextAppearance_Car_Body1_Light));
            mItems.add(item);

            actionItem = new ActionListItem(mContext);
            actionItem.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
            actionItem.setBody("Body with custom text appearance");
            actionItem.addViewBinder(
                    vh -> vh.getBody().setTextAppearance(R.style.TextAppearance_Car_Body1_Light));
            actionItem.setAction("Card Height", true, mGetParentHeight);
            mItems.add(actionItem);

            actionItem = new ActionListItem(mContext);
            actionItem.setOnClickListener(v -> {
                throw new RuntimeException("This item should not be clickable");
            });
            actionItem.setTitle("Disabled item");
            actionItem.setPrimaryAction("action", false, v -> {
                throw new RuntimeException("This button should not be clickable");
            });
            actionItem.setEnabled(false);
            mItems.add(actionItem);

            mListProvider = new ListItemProvider.ListProvider(mItems);
        }

        @Override
        public ListItem get(int position) {
            return mListProvider.get(position);
        }

        @Override
        public int size() {
            return mListProvider.size();
        }
    }
}
