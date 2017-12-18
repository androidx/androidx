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

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.RestrictTo;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;
import androidx.app.slice.core.SliceQuery;
import androidx.app.slice.view.R;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@TargetApi(24)
public class GridRowView extends LinearLayout implements LargeSliceAdapter.SliceListView,
        View.OnClickListener, SliceView.SliceModeView {

    private static final String TAG = "GridView";

    // TODO -- Should addRow notion to the builder so that apps could define the "see more" intent
    private static final boolean ALLOW_SEE_MORE = false;

    private static final int TITLE_TEXT_LAYOUT = R.layout.abc_slice_title;
    private static final int TEXT_LAYOUT = R.layout.abc_slice_secondary_text;

    // Max number of *just* images that can be shown in a row
    private static final int MAX_IMAGES = 3;
    // Max number of normal cell items that can be shown in a row
    private static final int MAX_ALL = 5;

    // Max number of text items that can show in a cell
    private static final int MAX_CELL_TEXT = 2;
    // Max number of text items that can show in a cell if the mode is small
    private static final int MAX_CELL_TEXT_SMALL = 1;
    // Max number of images that can show in a cell
    private static final int MAX_CELL_IMAGES = 1;

    private SliceItem mColorItem;
    private boolean mIsAllImages;
    private @SliceView.SliceMode int mSliceMode = 0;

    private int mIconSize;
    private int mLargeIconSize;
    private int mBigPictureHeight;
    private int mAllImagesHeight;

    public GridRowView(Context context) {
        this(context, null);
    }

    public GridRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final Resources res = getContext().getResources();
        mIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mLargeIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_large_icon_size);
        mBigPictureHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_big_picture_height);
        mAllImagesHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_image_only_height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mIsAllImages) {
            int count = getChildCount();
            int height = (count == 1) ? mBigPictureHeight : mAllImagesHeight;
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            getLayoutParams().height = height;
            for (int i = 0; i < count; i++) {
                getChildAt(i).getLayoutParams().height = height;
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Set the color for the items in this view.
     */
    @Override
    public void setColor(SliceItem colorItem) {
        mColorItem = colorItem;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public int getMode() {
        return mSliceMode;
    }

    /**
     * This is called when GridView is being used as a small template.
     */
    @Override
    public void setSlice(Slice slice) {
        mSliceMode = SliceView.MODE_SMALL;
        Slice.Builder sb = new Slice.Builder(slice.getUri());
        sb.addSubSlice(slice);
        Slice parentSlice = sb.build();
        populateViews(parentSlice.getItems().get(0));
    }

    /**
     * This is called when GridView is being used as a component in a large template.
     */
    @Override
    public void setSliceItem(SliceItem slice, boolean isHeader) {
        mSliceMode = SliceView.MODE_LARGE;
        populateViews(slice);
    }

    private void populateViews(SliceItem slice) {
        mIsAllImages = true;
        removeAllViews();
        int total = 1;
        if (FORMAT_SLICE.equals(slice.getFormat())) {
            List<SliceItem> items = slice.getSlice().getItems();
            // Check if it it's only one item that is a slice
            if (items.size() == 1 && items.get(0).getFormat().equals(FORMAT_SLICE)) {
                items = items.get(0).getSlice().getItems();
            }
            total = items.size();
            for (int i = 0; i < total; i++) {
                SliceItem item = items.get(i);
                if (isFull()) {
                    continue;
                }
                if (!addCell(item)) {
                    mIsAllImages = false;
                }
            }
        } else if (!isFull()) {
            if (!addCell(slice)) {
                mIsAllImages = false;
            }
        }
        if (ALLOW_SEE_MORE && mIsAllImages && total > getChildCount()) {
            addSeeMoreCount(total - getChildCount());
        }
    }

    private void addSeeMoreCount(int numExtra) {
        View last = getChildAt(getChildCount() - 1);
        FrameLayout frame = new FrameLayout(getContext());
        frame.setLayoutParams(last.getLayoutParams());

        removeView(last);
        frame.addView(last, new LayoutParams(MATCH_PARENT, MATCH_PARENT));

        TextView v = new TextView(getContext());
        v.setTextColor(Color.WHITE);
        v.setBackgroundColor(0x4d000000);
        v.setText(getResources().getString(R.string.abc_slice_more_content, numExtra));
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        v.setGravity(Gravity.CENTER);
        frame.addView(v, new LayoutParams(MATCH_PARENT, MATCH_PARENT));

        addView(frame);
    }

    private boolean isFull() {
        return getChildCount() >= (mIsAllImages ? MAX_IMAGES : MAX_ALL);
    }

    /**
     * Adds a cell to the grid view based on the provided {@link SliceItem}.
     * @return true if this item is just an image.
     */
    private boolean addCell(SliceItem sliceItem) {
        final int maxCellText = mSliceMode == SliceView.MODE_SMALL
                ? MAX_CELL_TEXT_SMALL
                : MAX_CELL_TEXT;
        LinearLayout cellContainer = new LinearLayout(getContext());
        cellContainer.setOrientation(LinearLayout.VERTICAL);
        cellContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        final int color = mColorItem != null ? mColorItem.getInt() : -1;
        final String format = sliceItem.getFormat();
        if (FORMAT_SLICE.equals(format) || FORMAT_ACTION.equals(format)) {
            // It's a slice -- try to add all the items we can to a cell.
            List<SliceItem> items = sliceItem.getSlice().getItems();
            SliceItem actionItem = null;
            if (FORMAT_ACTION.equals(format)) {
                actionItem = sliceItem;
            }
            if (items.size() == 1 && FORMAT_ACTION.equals(items.get(0).getFormat())) {
                actionItem = items.get(0);
                items = items.get(0).getSlice().getItems();
            }
            boolean imagesOnly = true;
            int textCount = 0;
            int imageCount = 0;
            boolean added = false;
            boolean singleItem = items.size() == 1;
            List<SliceItem> textItems = null;
            // In small format we display one text item and prefer titles
            if (!singleItem && mSliceMode == SliceView.MODE_SMALL) {
                // Get all our text items
                textItems = items.stream().filter(new Predicate<SliceItem>() {
                    @Override
                    public boolean test(SliceItem s) {
                        return FORMAT_TEXT.equals(s.getFormat());
                    }
                }).collect(Collectors.<SliceItem>toList());
                // If we have more than 1 remove non-titles
                Iterator<SliceItem> iterator = textItems.iterator();
                while (textItems.size() > 1) {
                    SliceItem item = iterator.next();
                    if (!item.hasHint(HINT_TITLE)) {
                        iterator.remove();
                    }
                }
            }
            for (int i = 0; i < items.size(); i++) {
                SliceItem item = items.get(i);
                final String itemFormat = item.getFormat();
                if (textCount < maxCellText && (FORMAT_TEXT.equals(itemFormat)
                        || FORMAT_TIMESTAMP.equals(itemFormat))) {
                    if (textItems != null && !textItems.contains(item)) {
                        continue;
                    }
                    if (addItem(item, color, cellContainer, singleItem)) {
                        textCount++;
                        imagesOnly = false;
                        added = true;
                    }
                } else if (imageCount < MAX_CELL_IMAGES && FORMAT_IMAGE.equals(item.getFormat())) {
                    if (addItem(item, color, cellContainer, singleItem)) {
                        imageCount++;
                        added = true;
                    }
                }
            }
            if (added) {
                addView(cellContainer, new LayoutParams(0, WRAP_CONTENT, 1));
                if (actionItem != null) {
                    cellContainer.setTag(actionItem);
                    makeClickable(cellContainer);
                }
            }
            return imagesOnly;
        } else if (addItem(sliceItem, color, this, true)) {
            return FORMAT_IMAGE.equals(sliceItem.getFormat());
        }
        return false;
    }

    /**
     * Adds simple items to a container. Simple items include icons, text, and timestamps.
     * @return Whether an item was added.
     */
    private boolean addItem(SliceItem item, int color, ViewGroup container, boolean singleItem) {
        final String format = item.getFormat();
        View addedView = null;
        if (FORMAT_TEXT.equals(format) || FORMAT_TIMESTAMP.equals(format)) {
            boolean title = SliceQuery.hasAnyHints(item, HINT_LARGE, HINT_TITLE);
            TextView tv = (TextView) LayoutInflater.from(getContext()).inflate(title
                            ? TITLE_TEXT_LAYOUT : TEXT_LAYOUT, null);
            CharSequence text = FORMAT_TIMESTAMP.equals(format)
                    ? SliceViewUtil.getRelativeTimeString(item.getTimestamp())
                    : item.getText();
            tv.setText(text);
            container.addView(tv);
            addedView = tv;
        } else if (FORMAT_IMAGE.equals(format)) {
            ImageView iv = new ImageView(getContext());
            iv.setImageIcon(item.getIcon());
            if (color != -1 && !item.hasHint(HINT_NO_TINT)) {
                iv.setColorFilter(color);
            }
            int size = mIconSize;
            if (item.hasHint(HINT_LARGE)) {
                iv.setScaleType(ScaleType.CENTER_CROP);
                size = singleItem ? MATCH_PARENT : mLargeIconSize;
            }
            container.addView(iv, new LayoutParams(size, size));
            addedView = iv;
        }
        return addedView != null;
    }

    private void makeClickable(View layout) {
        layout.setOnClickListener(this);
        layout.setBackground(SliceViewUtil.getDrawable(getContext(),
                android.R.attr.selectableItemBackground));
    }

    @Override
    public void onClick(View view) {
        final SliceItem actionTag = (SliceItem) view.getTag();
        if (actionTag != null && FORMAT_ACTION.equals(actionTag.getFormat())) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        actionTag.getAction().send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.w(TAG, "PendingIntent for slice cannot be sent", e);
                    }
                }
            });
        }
    }
}
