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

package androidx.app.slice.widget;

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_COLOR;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.slice.Slice;
import android.app.slice.SliceItem;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.app.slice.core.SliceHints;
import androidx.app.slice.core.SliceQuery;
import androidx.app.slice.view.R;

/**
 * Small template is also used to construct list items for use with {@link LargeTemplateView}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SmallTemplateView extends SliceView.SliceModeView implements
        LargeSliceAdapter.SliceListView, View.OnClickListener {

    private static final String TAG = "SmallTemplateView";

    // The number of items that fit on the right hand side of a small slice
    private static final int MAX_END_ITEMS = 3;

    private SliceItem mColorItem;

    private int mIconSize;
    private int mPadding;

    private LinearLayout mStartContainer;
    private LinearLayout mContent;
    private TextView mPrimaryText;
    private TextView mSecondaryText;
    private LinearLayout mEndContainer;

    private SliceItem mRowAction;
    private View mDivider;
    private Switch mToggle;

    public SmallTemplateView(Context context) {
        super(context);
        mIconSize = getContext().getResources().getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mPadding = getContext().getResources().getDimensionPixelSize(R.dimen.abc_slice_padding);
        inflate(context, R.layout.abc_slice_small_template, this);

        mStartContainer = (LinearLayout) findViewById(android.R.id.icon_frame);
        mContent = (LinearLayout) findViewById(android.R.id.content);
        mPrimaryText = (TextView) findViewById(android.R.id.title);
        mSecondaryText = (TextView) findViewById(android.R.id.summary);
        mDivider = findViewById(R.id.divider);
        mEndContainer = (LinearLayout) findViewById(android.R.id.widget_frame);
    }

    @Override
    public @SliceView.SliceMode int getMode() {
        return SliceView.MODE_SMALL;
    }

    @Override
    public void setColor(SliceItem color) {
        mColorItem = color;
    }


    @Override
    public void setSliceItem(SliceItem slice) {
        populateViews(slice, slice);
    }
    @Override
    public void setSlice(Slice slice) {
        Slice.Builder sb = new Slice.Builder(slice.getUri());
        sb.addSubSlice(slice);
        Slice parentSlice = sb.build();
        populateViews(parentSlice.getItems().get(0), getFirstSlice(slice));
    }

    private SliceItem getFirstSlice(Slice slice) {
        List<SliceItem> items = slice.getItems();
        if (items.size() > 0 && FORMAT_SLICE.equals(items.get(0).getFormat())) {
            // Check if this slice is appropriate to use to populate small template
            SliceItem firstSlice = items.get(0);
            if (firstSlice.hasHint(Slice.HINT_LIST)) {
                // Check for header, use that if it exists
                SliceItem header = SliceQuery.find(firstSlice, FORMAT_SLICE,
                        null,
                        new String[] {
                                Slice.HINT_LIST_ITEM, Slice.HINT_LIST
                        });
                if (header != null) {
                    return SliceQuery.findFirstSlice(header);
                } else {
                    // Otherwise use the first list item
                    SliceItem newFirst = firstSlice.getSlice().getItems().get(0);
                    return SliceQuery.findFirstSlice(newFirst);
                }
            } else {
                // Not a list, find first slice with non-slice children
                return SliceQuery.findFirstSlice(firstSlice);
            }
        }
        // Get it as a SliceItem type slice
        Slice.Builder sb = new Slice.Builder(slice.getUri());
        Slice s = sb.addSubSlice(slice).build();
        return s.getItems().get(0);
    }

    private void populateViews(SliceItem fullSlice, SliceItem sliceItem) {
        resetViews();
        ArrayList<SliceItem> items = new ArrayList<>();
        if (FORMAT_SLICE.equals(sliceItem.getFormat())) {
            items = new ArrayList<>(sliceItem.getSlice().getItems());
        } else {
            items.add(sliceItem);
        }

        // These are the things that can go in our small template
        SliceItem startItem = null;
        SliceItem titleItem = null;
        SliceItem subTitle = null;
        ArrayList<SliceItem> endItems = new ArrayList<>();

        // If the first item is an action let's check if it should be used to populate the content
        // or if it should be in the start position.
        SliceItem firstSlice = items.size() > 0 ? items.get(0) : null;
        if (firstSlice != null && FORMAT_ACTION.equals(firstSlice.getFormat())) {
            if (!SliceQuery.isSimpleAction(firstSlice)) {
                mRowAction = firstSlice;
                items.remove(0);
                // Populating with first action, bias to use slice associated with this action
                items.addAll(0, mRowAction.getSlice().getItems());
            } else {
                // It's simple so maybe it's a start item
                startItem = items.get(0);
            }
        }

        // Look through our items and try to figure out main content
        for (int i = 0; i < items.size(); i++) {
            SliceItem item = items.get(i);
            List<String> hints = item.getHints();
            String itemType = item.getFormat();
            if (hints.contains(Slice.HINT_TITLE)) {
                // Things with these hints could go in the title / start position
                if ((startItem == null || !startItem.hasHint(Slice.HINT_TITLE))
                        && SliceQuery.isStartType(item)) {
                    startItem = item;
                } else if ((titleItem == null || !titleItem.hasHint(Slice.HINT_TITLE))
                        && FORMAT_TEXT.equals(itemType)) {
                    titleItem = item;
                } else {
                    endItems.add(item);
                }
            } else if (FORMAT_TEXT.equals(item.getFormat())) {
                if (titleItem == null) {
                    titleItem = item;
                } else if (subTitle == null) {
                    subTitle = item;
                } else {
                    endItems.add(item);
                }
            } else if (FORMAT_SLICE.equals(item.getFormat())) {
                List<SliceItem> subItems = item.getSlice().getItems();
                for (int j = 0; j < subItems.size(); j++) {
                    endItems.add(subItems.get(j));
                }
            } else {
                endItems.add(item);
            }
        }

        // Populate main part of the template
        if (startItem != null) {
            // TODO - check for icon, timestamp, action with icon
        }
        if (titleItem != null) {
            mPrimaryText.setText(titleItem.getText());
        }
        mPrimaryText.setVisibility(titleItem != null ? View.VISIBLE : View.GONE);
        if (subTitle != null) {
            mSecondaryText.setText(subTitle.getText());
        }
        mSecondaryText.setVisibility(subTitle != null ? View.VISIBLE : View.GONE);

        // Figure out what end items we're showing
        // If we're showing an action in this row check if it's a toggle
        if (mRowAction != null && SliceQuery.hasHints(mRowAction.getSlice(), SliceHints.HINT_TOGGLE)
                && addToggle(mRowAction)) {
            // Can't show more end actions if we have a toggle so we're done
            makeClickable(this);
            return;
        }
        // Check if we have a toggle somewhere in our end items
        SliceItem toggleItem = endItems.stream()
                .filter(item -> FORMAT_ACTION.equals(item.getFormat())
                        && SliceQuery.hasHints(item.getSlice(), SliceHints.HINT_TOGGLE))
                .findFirst().orElse(null);
        if (toggleItem != null) {
            if (addToggle(toggleItem)) {
                mDivider.setVisibility(mRowAction != null ? View.VISIBLE : View.GONE);
                makeClickable(mRowAction != null ? mContent : this);
                // Can't show more end actions if we have a toggle so we're done
                return;
            }
        }
        // If we're here we can still show end items
        SliceItem colorItem = SliceQuery.find(fullSlice, FORMAT_COLOR);
        int color = colorItem != null
                ? colorItem.getColor()
                : (mColorItem != null)
                        ? mColorItem.getColor()
                        : -1;
        boolean clickableEndItem = false;
        int itemCount = 0;
        for (int i = 0; i < items.size(); i++) {
            SliceItem item = items.get(i);
            if (itemCount <= MAX_END_ITEMS) {
                if (FORMAT_ACTION.equals(item.getFormat())) {
                    if (SliceQuery.hasHints(item.getSlice(), SliceHints.HINT_TOGGLE)) {
                        if (addToggle(item)) {
                            break;
                        }
                    }
                    if (addIcon(item, color, mEndContainer)) {
                        clickableEndItem = true;
                        itemCount++;
                    }
                } else if (FORMAT_IMAGE.equals(item.getFormat())) {
                    addIcon(item, color, mEndContainer);
                    itemCount++;
                } else if (FORMAT_TIMESTAMP.equals(item.getFormat())) {
                    TextView tv = new TextView(getContext());
                    tv.setText(SliceViewUtil.getRelativeTimeString(item.getTimestamp()));
                    mEndContainer.addView(tv);
                    itemCount++;
                }
            }
        }
        if (mRowAction != null) {
            makeClickable(clickableEndItem ? mContent : this);
        }
    }

    /**
     * @return Whether a toggle was added.
     */
    private boolean addToggle(SliceItem toggleItem) {
        if (!FORMAT_ACTION.equals(toggleItem.getFormat())
                || !SliceQuery.hasHints(toggleItem.getSlice(), SliceHints.HINT_TOGGLE)) {
            return false;
        }
        mToggle = new Switch(getContext());
        mEndContainer.addView(mToggle);
        mToggle.setChecked(SliceQuery.hasHints(toggleItem.getSlice(), Slice.HINT_SELECTED));
        mToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            try {
                PendingIntent pi = toggleItem.getAction();
                Intent i = new Intent().putExtra(SliceHints.EXTRA_TOGGLE_STATE, isChecked);
                pi.send(getContext(), 0, i, null, null);
            } catch (CanceledException e) {
                mToggle.setSelected(!isChecked);
            }
        });
        return true;
    }

    /**
     * @return Whether an icon was added.
     */
    private boolean addIcon(SliceItem sliceItem, int color, LinearLayout container) {
        SliceItem image = null;
        SliceItem action = null;
        if (FORMAT_ACTION.equals(sliceItem.getFormat())) {
            image = SliceQuery.find(sliceItem.getSlice(), FORMAT_IMAGE);
            action = sliceItem;
        } else if (FORMAT_IMAGE.equals(sliceItem.getFormat())) {
            image = sliceItem;
        }
        if (image != null) {
            ImageView iv = new ImageView(getContext());
            iv.setImageIcon(image.getIcon());
            if (action != null) {
                final SliceItem sliceAction = action;
                iv.setOnClickListener(v -> AsyncTask.execute(
                        () -> {
                            try {
                                sliceAction.getAction().send();
                            } catch (CanceledException e) {
                                e.printStackTrace();
                            }
                        }));
                iv.setBackground(SliceViewUtil.getDrawable(getContext(),
                        android.R.attr.selectableItemBackground));
            }
            if (color != -1 && !sliceItem.hasHint(Slice.HINT_NO_TINT)) {
                iv.setColorFilter(color);
            }
            container.addView(iv);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
            lp.width = mIconSize;
            lp.height = mIconSize;
            lp.setMarginStart(mPadding);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (mRowAction != null && FORMAT_ACTION.equals(mRowAction.getFormat())) {
            if (mToggle != null
                    && SliceQuery.hasHints(mRowAction.getSlice(), SliceHints.HINT_TOGGLE)) {
                mToggle.toggle();
                return;
            }
            AsyncTask.execute(() -> {
                try {
                    mRowAction.getAction().send();
                } catch (CanceledException e) {
                    Log.w(TAG, "PendingIntent for slice cannot be sent", e);
                }
            });
        }
    }

    private void makeClickable(View layout) {
        layout.setOnClickListener(this);
        layout.setBackground(SliceViewUtil.getDrawable(getContext(),
                android.R.attr.selectableItemBackground));
    }

    private void resetViews() {
        mStartContainer.removeAllViews();
        mEndContainer.removeAllViews();
        mPrimaryText.setText(null);
        mSecondaryText.setText(null);
    }
}
