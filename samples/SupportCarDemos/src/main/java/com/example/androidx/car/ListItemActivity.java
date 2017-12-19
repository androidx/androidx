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
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;

/**
 * Demo activity for {@link ListItem}.
 */
public class ListItemActivity extends Activity {

    private static int pixelToDip(Context context, int pixels) {
        return (int) (pixels / context.getResources().getDisplayMetrics().density);
    }

    PagedListView mPagedListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paged_list_view);

        mPagedListView = findViewById(R.id.paged_list_view);

        ListItemAdapter adapter = new ListItemAdapter(this,
                new SampleProvider(this), ListItemAdapter.BackgroundStyle.PANEL);
        mPagedListView.setAdapter(adapter);
        mPagedListView.setMaxPages(PagedListView.UNLIMITED_PAGES);
    }

    private static class SampleProvider extends ListItemProvider {
        private Context mContext;
        private List<ListItem> mItems;

        private View.OnClickListener mGetParentHeight = (v) -> {
            int parentHeight = ((FrameLayout) v.getParent().getParent().getParent()).getHeight();
            Toast.makeText(v.getContext(),
                    "card height is " + pixelToDip(mContext, parentHeight) + " dp",
                    Toast.LENGTH_SHORT).show();
        };

        private ListItemProvider.ListProvider mListProvider;

        SampleProvider(Context context) {
            mContext = context;
            mItems = new ArrayList<>();

            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true)
                    .withTitle("single line with full icon and one action")
                    .withAction("card height", true, mGetParentHeight)
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withTitle("primary action set by drawable")
                    .withPrimaryActionIcon(mContext.getDrawable(R.drawable.pressed_icon), true)
                    .withViewBinder(vh -> vh.getPrimaryIcon().setClickable(true))
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                    .withTitle("single line with small icon and clickable end icon")
                    .withSupplementalIcon(android.R.drawable.sym_def_app_icon, true,
                            mGetParentHeight)
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionEmptyIcon()
                    .withTitle("single line with empty icon and end icon no divider")
                    .withSupplementalIcon(android.R.drawable.sym_def_app_icon, false)
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withTitle("title is single line and ellipsizes. "
                            + mContext.getString(R.string.long_text))
                    .withSupplementalIcon(android.R.drawable.sym_def_app_icon, true)
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionNoIcon()
                    .withTitle("single line with two actions and no divider")
                    .withActions("action 1", false,
                            (v) -> Toast.makeText(
                                    v.getContext(), "action 1", Toast.LENGTH_SHORT).show(),
                            "action 2", false,
                            (v) -> Toast.makeText(
                                    v.getContext(), "action 2", Toast.LENGTH_SHORT).show())
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionNoIcon()
                    .withTitle("single line with two actions and action 2 divider")
                    .withActions("action 1", false,
                            (v) -> Toast.makeText(
                                    v.getContext(), "action 1", Toast.LENGTH_SHORT).show(),
                            "action 2", true,
                            (v) -> Toast.makeText(
                                    v.getContext(), "action 2", Toast.LENGTH_SHORT).show())
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionNoIcon()
                    .withTitle("single line with divider between actions. "
                            + mContext.getString(R.string.long_text))
                    .withActions("action 1", true,
                            (v) -> Toast.makeText(
                                    v.getContext(), "action 1", Toast.LENGTH_SHORT).show(),
                            "action 2", false,
                            (v) -> Toast.makeText(
                                    v.getContext(), "action 2", Toast.LENGTH_SHORT).show())
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true)
                    .withTitle("double line with full icon and no end icon divider")
                    .withBody("one line text")
                    .withSupplementalIcon(android.R.drawable.sym_def_app_icon, false,
                            mGetParentHeight)
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                    .withTitle("double line with small icon and one action")
                    .withBody("one line text")
                    .withAction("card height", true, mGetParentHeight)
                    .build());

            String tenChars = "Ten Chars.";
            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                    .withTitle("Card with small icon and text longer than limit")
                    .withBody("some chars")
                    .withBody(TextUtils.join("", Collections.nCopies(20, tenChars)))
                    .withSupplementalIcon(android.R.drawable.sym_def_app_icon, true,
                            mGetParentHeight)
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionEmptyIcon()
                    .withTitle("double line with empty primary icon."
                            + mContext.getString(R.string.long_text))
                    .withBody("one line text as primary", true)
                    .withActions("screen size", false, (v) -> {
                        Context c = v.getContext();
                        Point size = new Point();
                        c.getSystemService(WindowManager.class).getDefaultDisplay().getSize(size);

                        Toast.makeText(v.getContext(),
                                String.format("%s x %s dp", pixelToDip(c, size.x),
                                        pixelToDip(c, size.y)), Toast.LENGTH_SHORT).show();
                    }, "card height", true, mGetParentHeight)
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withTitle("double line with no primary action and one divider")
                    .withBody("one line text as primary", true)
                    .withActions("screen size", false, (v) -> {
                        Context c = v.getContext();
                        Point size = new Point();
                        c.getSystemService(WindowManager.class).getDefaultDisplay().getSize(size);

                        Toast.makeText(v.getContext(),
                                String.format("%s x %s dp", pixelToDip(c, size.x),
                                        pixelToDip(c, size.y)), Toast.LENGTH_SHORT).show();
                    }, "card height", true, mGetParentHeight)
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true)
                    .withBody("Only body - no title is set")
                    .withAction("card height", true, mGetParentHeight)
                    .build());

            mItems.add(new ListItem.Builder(mContext)
                    .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                    .withBody("Only body - no title. " + mContext.getString(R.string.long_text))
                    .build());

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
