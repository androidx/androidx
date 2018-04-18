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
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

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

        mPagedListView = findViewById(R.id.paged_list_view);

        SampleProvider provider = new SampleProvider(this);
        ListItemAdapter adapter = new ListItemAdapter(this, provider,
                ListItemAdapter.BackgroundStyle.SOLID);

        final boolean[] hideDivider = {true};
        // Demonstrate how to update list item post construction.
        TextListItem toBeUpdated = new TextListItem(this);
        toBeUpdated.setPrimaryActionEmptyIcon();
        toBeUpdated.setTitle("tap next item to update my icon");
        toBeUpdated.setHideDivider(hideDivider[0]);
        provider.mItems.add(0, toBeUpdated);

        boolean[] useEmptyIcon = new boolean[]{false};
        TextListItem update = new TextListItem(this);
        update.setTitle("tap me to update the icon of item above");
        update.setOnClickListener(v -> {
            // Change icon.
            if (useEmptyIcon[0]) {
                toBeUpdated.setPrimaryActionEmptyIcon();
            } else {
                toBeUpdated.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
            }
            useEmptyIcon[0] = !useEmptyIcon[0];

            // Show/hide item divider.
            toBeUpdated.setHideDivider(hideDivider[0]);
            hideDivider[0] = !hideDivider[0];

            // Make sure to notify adapter about the change.
            adapter.notifyItemChanged(0);
        });
        provider.mItems.add(1, update);

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
                    "card height is " + pixelToDip(mContext, parentHeight) + " dp",
                    Toast.LENGTH_SHORT).show();
        };

        private ListItemProvider.ListProvider mListProvider;

        SampleProvider(Context context) {
            mContext = context;
            mItems = new ArrayList<>();

            TextListItem item;

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true);
            item.setTitle("clickable single line with full icon and one action");
            item.setAction("card height", true, mGetParentHeight);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(mContext.getDrawable(R.drawable.pressed_icon), true);
            item.setTitle("primary action set by drawable");
            item.addViewBinder(vh -> vh.getPrimaryIcon().setClickable(true));
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
            item.setTitle("clickable single line with small icon and clickable end icon");
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true, mGetParentHeight);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
            item.setTitle("single line without a list divider");
            item.setHideDivider(true);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setOnClickListener(mOnClickListener);
            item.setPrimaryActionEmptyIcon();
            item.setTitle("clickable single line with empty icon and end icon no divider");
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setTitle("title is single line and ellipsizes. "
                            + mContext.getString(R.string.long_text));
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setTitle("Subtitle-like line without a list divider");
            item.setHideDivider(true);
            item.addViewBinder(viewHolder ->
                            viewHolder.getTitle().setTextAppearance(R.style.CarListSubtitle));
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionNoIcon();
            item.setTitle("single line with two actions and no divider");
            item.setActions("action 1", false,
                    v -> Toast.makeText(
                            v.getContext(), "action 1", Toast.LENGTH_SHORT).show(),
                    "action 2", false,
                    v -> Toast.makeText(
                            v.getContext(), "action 2", Toast.LENGTH_SHORT).show());
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionNoIcon();
            item.setTitle("single line with two actions and action 2 divider");
            item.setActions("action 1", false,
                    v -> Toast.makeText(
                            v.getContext(), "action 1", Toast.LENGTH_SHORT).show(),
                    "action 2", true,
                    v -> Toast.makeText(
                            v.getContext(), "action 2", Toast.LENGTH_SHORT).show());
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionNoIcon();
            item.setTitle("single line with divider between actions. "
                    + mContext.getString(R.string.long_text));
            item.setActions("action 1", true,
                    v -> Toast.makeText(
                            v.getContext(), "action 1", Toast.LENGTH_SHORT).show(),
                    "action 2", false,
                    v -> Toast.makeText(
                            v.getContext(), "action 2", Toast.LENGTH_SHORT).show());
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setTitle("item longer than containing View size");
            item.setBody(mContext.getResources().getString(R.string.super_long_text));
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true);
            item.setTitle("double line with full icon and no end icon divider");
            item.setBody("one line text");
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false, mGetParentHeight);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
            item.setTitle("double line with small icon and one action");
            item.setBody("one line text");
            item.setAction("card height", true, mGetParentHeight);
            mItems.add(item);

            String tenChars = "Ten Chars.";
            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
            item.setTitle("Card with small icon and text longer than limit");
            item.setBody(TextUtils.join("", Collections.nCopies(20, tenChars)));
            item.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true, mGetParentHeight);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionEmptyIcon();
            item.setTitle("double line with empty primary icon."
                    + mContext.getString(R.string.long_text));
            item.setBody("one line text as primary", true);
            item.setActions("screen size", false, v -> {
                Context c = v.getContext();
                Point size = new Point();
                c.getSystemService(WindowManager.class).getDefaultDisplay().getSize(size);

                Toast.makeText(v.getContext(), String.format("%s x %s dp", pixelToDip(c, size.x),
                        pixelToDip(c, size.y)),
                        Toast.LENGTH_SHORT).show();
            }, "card height", true, mGetParentHeight);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setTitle("double line with no primary action and one divider");
            item.setBody("one line text as primary", true);
            item.setActions("screen size", false, v -> {
                Context c = v.getContext();
                Point size = new Point();
                c.getSystemService(WindowManager.class).getDefaultDisplay().getSize(size);

                Toast.makeText(v.getContext(),
                        String.format("%s x %s dp", pixelToDip(c, size.x),
                                pixelToDip(c, size.y)), Toast.LENGTH_SHORT).show();
            }, "card height", true, mGetParentHeight);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true);
            item.setBody("Only body - no title is set");
            item.setAction("card height", true, mGetParentHeight);
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
            item.setBody("Only body - no title. " + mContext.getString(R.string.long_text));
            mItems.add(item);

            item = new TextListItem(mContext);
            item.setTitle("Switch - initially unchecked");
            item.setSwitch(false, true, (button, isChecked) -> {
                Toast.makeText(mContext,
                        isChecked ? "checked" : "unchecked", Toast.LENGTH_SHORT).show();
            });
            mItems.add(item);

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
