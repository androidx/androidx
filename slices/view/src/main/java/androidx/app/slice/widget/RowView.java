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

import static android.app.slice.Slice.HINT_LIST;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_SELECTED;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_COLOR;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;

import static androidx.app.slice.core.SliceHints.EXTRA_TOGGLE_STATE;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.support.annotation.RestrictTo;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;
import androidx.app.slice.core.SliceHints;
import androidx.app.slice.core.SliceQuery;
import androidx.app.slice.view.R;

/**
 * Row item is in small template format and can be used to construct list items for use
 * with {@link LargeTemplateView}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@TargetApi(23)
public class RowView extends SliceView.SliceModeView implements
        LargeSliceAdapter.SliceListView, View.OnClickListener {

    private static final String TAG = "RowView";

    // The number of items that fit on the right hand side of a small slice
    private static final int MAX_END_ITEMS = 3;

    private int mIconSize;
    private int mPadding;

    // If this is being used as a small template we don't allow a start item, for list items we do.
    private boolean mAllowStartItem;

    private LinearLayout mStartContainer;
    private LinearLayout mContent;
    private TextView mPrimaryText;
    private TextView mSecondaryText;
    private View mDivider;
    private CompoundButton mToggle;
    private LinearLayout mEndContainer;

    private SliceItem mColorItem;
    private SliceItem mRowAction;

    public RowView(Context context) {
        super(context);
        mIconSize = getContext().getResources().getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mPadding = getContext().getResources().getDimensionPixelSize(R.dimen.abc_slice_padding);
        inflate(context, R.layout.abc_slice_small_template, this);

        mStartContainer = (LinearLayout) findViewById(R.id.icon_frame);
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

    /**
     * This is called when RowView is being used as a component in a large template.
     */
    @Override
    public void setSliceItem(SliceItem slice, boolean isHeader) {
        mAllowStartItem = !isHeader; // Headers don't show start items
        populateViews(slice, slice);
    }

    /**
     * This is called when RowView is being used as a small template.
     */
    @Override
    public void setSlice(Slice slice) {
        mAllowStartItem = false;
        Slice.Builder sb = new Slice.Builder(slice.getUri());
        sb.addSubSlice(slice);
        Slice parentSlice = sb.build();
        populateViews(parentSlice.getItems().get(0), getHeaderItem(slice));
    }

    private SliceItem getHeaderItem(Slice slice) {
        List<SliceItem> items = slice.getItems();
        // See if a header is specified
        SliceItem header = SliceQuery.find(slice, FORMAT_SLICE, null, HINT_LIST_ITEM);
        if (header != null) {
            return header;
        }
        // Otherwise use the first non-color item and use it if it's a slice
        SliceItem firstSlice = null;
        for (int i = 0; i < items.size(); i++) {
            if (!FORMAT_COLOR.equals(items.get(i).getFormat())) {
                firstSlice = items.get(i);
                break;
            }
        }
        if (firstSlice != null && FORMAT_SLICE.equals(firstSlice.getFormat())) {
            // Check if this slice is appropriate to use to populate small template
            if (firstSlice.hasHint(HINT_LIST)) {
                // Check for header, use that if it exists
                SliceItem listHeader = SliceQuery.find(firstSlice, FORMAT_SLICE,
                        null,
                        new String[] {
                                HINT_LIST_ITEM, HINT_LIST
                        });
                if (listHeader != null) {
                    return SliceQuery.findFirstSlice(listHeader);
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
        // Fallback, just use this and convert to SliceItem type slice
        Slice.Builder sb = new Slice.Builder(slice.getUri());
        Slice s = sb.addSubSlice(slice).build();
        return s.getItems().get(0);
    }

    @TargetApi(24)
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
            }
        }

        // Look through our items and try to figure out main content
        for (int i = 0; i < items.size(); i++) {
            SliceItem item = items.get(i);
            List<String> hints = item.getHints();
            String itemType = item.getFormat();
            if (i == 0 && SliceQuery.isStartType((item))) {
                startItem = item;
            } else if (hints.contains(HINT_TITLE)) {
                // Things with these hints could go in the title / start position
                if ((startItem == null || !startItem.hasHint(HINT_TITLE))
                        && SliceQuery.isStartType(item)) {
                    startItem = item;
                } else if ((titleItem == null || !titleItem.hasHint(HINT_TITLE))
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

        SliceItem colorItem = SliceQuery.find(fullSlice, FORMAT_COLOR);
        int color = colorItem != null
                ? colorItem.getColor()
                : (mColorItem != null)
                        ? mColorItem.getColor()
                        : -1;
        // Populate main part of the template
        if (startItem != null) {
            if (mAllowStartItem) {
                startItem = addItem(startItem, color, mStartContainer, 0 /* padding */)
                        ? startItem
                        : null;
                if (startItem != null) {
                    endItems.remove(startItem);
                }
            } else {
                startItem = null;
                endItems.add(0, startItem);
            }
        }
        mStartContainer.setVisibility(startItem != null ? View.VISIBLE : View.GONE);
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
        if (mRowAction != null && SliceQuery.hasHints(mRowAction.getSlice(),
                SliceHints.SUBTYPE_TOGGLE) && addToggle(mRowAction, color)) {
            // Can't show more end actions if we have a toggle so we're done
            makeClickable(this);
            return;
        }
        // Check if we have a toggle somewhere in our end items
        SliceItem toggleItem = endItems.stream()
                .filter(new Predicate<SliceItem>() {
                    @Override
                    public boolean test(SliceItem item) {
                        if (item == null) {
                            return false;
                        }
                        return FORMAT_ACTION.equals(item.getFormat())
                                && SliceQuery.hasHints(item.getSlice(), SliceHints.SUBTYPE_TOGGLE);
                    }
                })
                .findFirst().orElse(null);
        if (toggleItem != null) {
            if (addToggle(toggleItem, color)) {
                mDivider.setVisibility(mRowAction != null ? View.VISIBLE : View.GONE);
                makeClickable(mRowAction != null ? mContent : this);
                // Can't show more end actions if we have a toggle so we're done
                return;
            }
        }
        boolean clickableEndItem = false;
        int itemCount = 0;
        for (int i = 0; i < endItems.size(); i++) {
            SliceItem item = endItems.get(i);
            if (itemCount <= MAX_END_ITEMS) {
                if (FORMAT_ACTION.equals(item.getFormat())
                        && itemCount == 0
                        && SliceQuery.hasHints(item.getSlice(), SliceHints.SUBTYPE_TOGGLE)
                        && addToggle(item, color)) {
                    // If a toggle is added we're done
                    break;
                } else if (addItem(item, color, mEndContainer, mPadding)) {
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
    private boolean addToggle(final SliceItem toggleItem, int color) {
        if (!FORMAT_ACTION.equals(toggleItem.getFormat())
                || !SliceQuery.hasHints(toggleItem.getSlice(), SliceHints.SUBTYPE_TOGGLE)) {
            return false;
        }

        // Check if this is a custom toggle
        Icon checkedIcon = null;
        List<SliceItem> sliceItems = toggleItem.getSlice().getItems();
        if (sliceItems.size() > 0) {
            checkedIcon = FORMAT_IMAGE.equals(sliceItems.get(0).getFormat())
                    ? sliceItems.get(0).getIcon()
                    : null;
        }
        if (checkedIcon != null) {
            if (color != -1) {
                // TODO - Should these be tinted? What if the app wants diff colors per state?
                checkedIcon.setTint(color);
            }
            mToggle = new ToggleButton(getContext());
            ((ToggleButton) mToggle).setTextOff("");
            ((ToggleButton) mToggle).setTextOn("");
            mToggle.setBackground(checkedIcon.loadDrawable(getContext()));
            mEndContainer.addView(mToggle);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mToggle.getLayoutParams();
            lp.width = mIconSize;
            lp.height = mIconSize;
        } else {
            mToggle = new Switch(getContext());
            mEndContainer.addView(mToggle);
        }
        mToggle.setChecked(SliceQuery.hasHints(toggleItem.getSlice(), HINT_SELECTED));
        mToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    PendingIntent pi = toggleItem.getAction();
                    Intent i = new Intent().putExtra(EXTRA_TOGGLE_STATE, isChecked);
                    pi.send(getContext(), 0, i, null, null);
                } catch (CanceledException e) {
                    mToggle.setSelected(!isChecked);
                }
            }
        });
        return true;
    }

    /**
     * Adds simple items to a container. Simple items include actions with icons, images, or
     * timestamps.
     *
     * @return Whether an item was added to the view.
     */
    private boolean addItem(SliceItem sliceItem, int color, LinearLayout container, int padding) {
        SliceItem image = null;
        SliceItem action = null;
        SliceItem timeStamp = null;
        if (FORMAT_ACTION.equals(sliceItem.getFormat())
                && !sliceItem.hasHint(SliceHints.SUBTYPE_TOGGLE)) {
            image = SliceQuery.find(sliceItem.getSlice(), FORMAT_IMAGE);
            timeStamp = SliceQuery.find(sliceItem.getSlice(), FORMAT_TIMESTAMP);
            action = sliceItem;
        } else if (FORMAT_IMAGE.equals(sliceItem.getFormat())) {
            image = sliceItem;
        } else if (FORMAT_TIMESTAMP.equals(sliceItem.getFormat())) {
            timeStamp = sliceItem;
        }
        View addedView = null;
        if (image != null) {
            ImageView iv = new ImageView(getContext());
            iv.setImageIcon(image.getIcon());
            if (color != -1 && !sliceItem.hasHint(HINT_NO_TINT)) {
                iv.setColorFilter(color);
            }
            container.addView(iv);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
            lp.width = mIconSize;
            lp.height = mIconSize;
            lp.setMarginStart(padding);
            addedView = iv;
        } else if (timeStamp != null) {
            TextView tv = new TextView(getContext());
            tv.setText(SliceViewUtil.getRelativeTimeString(sliceItem.getTimestamp()));
            container.addView(tv);
            addedView = tv;
        }
        if (action != null && addedView != null) {
            final SliceItem sliceAction = action;
            addedView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                sliceAction.getAction().send();
                            } catch (CanceledException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            addedView.setBackground(SliceViewUtil.getDrawable(getContext(),
                    android.R.attr.selectableItemBackground));
        }
        return addedView != null;
    }

    @Override
    public void onClick(View view) {
        if (mRowAction != null && FORMAT_ACTION.equals(mRowAction.getFormat())) {
            // Check for a row action
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        mRowAction.getAction().send();
                    } catch (CanceledException e) {
                        Log.w(TAG, "PendingIntent for slice cannot be sent", e);
                    }
                }
            });
        } else if (mToggle != null) {
            // Or no row action so let's just toggle if we've got one
            mToggle.toggle();
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
        mDivider.setVisibility(View.GONE);
    }
}
