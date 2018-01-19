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
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.RestrictTo;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
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

import java.util.ArrayList;
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
public class GridRowView extends SliceChildView implements View.OnClickListener {

    private static final String TAG = "GridView";

    // TODO -- Should add notion to the builder so that apps could define the "see more" intent
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

    private int mRowIndex;
    private boolean mIsAllImages;
    private @SliceView.SliceMode int mSliceMode = 0;

    private int mIconSize;
    private int mLargeIconSize;
    private int mBigPictureHeight;
    private int mAllImagesHeight;
    private GridContent mGridContent;
    private LinearLayout mViewContainer;

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
        mViewContainer = new LinearLayout(getContext());
        mViewContainer.setOrientation(LinearLayout.HORIZONTAL);
        addView(mViewContainer, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
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

    @Override
    public int getMode() {
        return mSliceMode;
    }

    @Override
    public void setTint(@ColorInt int tintColor) {
        super.setTint(tintColor);
        if (mGridContent != null) {
            // TODO -- could be smarter about this
            resetView();
            populateViews(mGridContent);
        }
    }

    /**
     * This is called when GridView is being used as a small template.
     */
    @Override
    public void setSlice(Slice slice) {
        mRowIndex = 0;
        mSliceMode = SliceView.MODE_SMALL;
        Slice.Builder sb = new Slice.Builder(slice.getUri());
        sb.addSubSlice(slice);
        Slice parentSlice = sb.build();
        mGridContent = new GridContent(parentSlice.getItems().get(0));
        populateViews(mGridContent);
    }

    /**
     * This is called when GridView is being used as a component in a large template.
     */
    @Override
    public void setSliceItem(SliceItem slice, boolean isHeader, int index,
            SliceView.SliceObserver observer) {
        setSliceObserver(observer);
        mRowIndex = index;
        mSliceMode = SliceView.MODE_LARGE;
        mGridContent = new GridContent(slice);
        populateViews(mGridContent);
    }

    private void populateViews(GridContent gc) {
        mIsAllImages = gc.isAllImages();
        ArrayList<GridContent.CellContent> cells = gc.getGridContent();
        final int max = mIsAllImages ? MAX_IMAGES : MAX_ALL;
        for (int i = 0; i < cells.size(); i++) {
            if (isFull()) {
                break;
            }
            addCell(cells.get(i), i, Math.min(cells.size(), max));
        }
        if (ALLOW_SEE_MORE && mIsAllImages && cells.size() > getChildCount()) {
            addSeeMoreCount(cells.size() - getChildCount());
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

        mViewContainer.addView(frame);
    }

    private boolean isFull() {
        return getChildCount() >= (mIsAllImages ? MAX_IMAGES : MAX_ALL);
    }

    /**
     * Adds a cell to the grid view based on the provided {@link SliceItem}.
     */
    private void addCell(GridContent.CellContent cell, int index, int total) {
        final int maxCellText = mSliceMode == SliceView.MODE_SMALL
                ? MAX_CELL_TEXT_SMALL
                : MAX_CELL_TEXT;
        LinearLayout cellContainer = new LinearLayout(getContext());
        cellContainer.setOrientation(LinearLayout.VERTICAL);
        cellContainer.setGravity(Gravity.CENTER_HORIZONTAL);

        ArrayList<SliceItem> cellItems = cell.getCellItems();
        SliceItem contentIntentItem = cell.getContentIntent();

        int textCount = 0;
        int imageCount = 0;
        boolean added = false;
        boolean singleItem = cellItems.size() == 1;
        List<SliceItem> textItems = null;
        // In small format we display one text item and prefer titles
        if (!singleItem && mSliceMode == SliceView.MODE_SMALL) {
            // Get all our text items
            textItems = cellItems.stream().filter(new Predicate<SliceItem>() {
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
        for (int i = 0; i < cellItems.size(); i++) {
            SliceItem item = cellItems.get(i);
            final String itemFormat = item.getFormat();
            if (textCount < maxCellText && (FORMAT_TEXT.equals(itemFormat)
                    || FORMAT_TIMESTAMP.equals(itemFormat))) {
                if (textItems != null && !textItems.contains(item)) {
                    continue;
                }
                if (addItem(item, mTintColor, cellContainer, singleItem)) {
                    textCount++;
                    added = true;
                }
            } else if (imageCount < MAX_CELL_IMAGES && FORMAT_IMAGE.equals(item.getFormat())) {
                if (addItem(item, mTintColor, cellContainer, singleItem)) {
                    imageCount++;
                    added = true;
                }
            }
        }
        if (added) {
            mViewContainer.addView(cellContainer,
                    new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
            if (contentIntentItem != null) {
                EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_BUTTON,
                        EventInfo.ROW_TYPE_GRID, mRowIndex);
                info.setPosition(EventInfo.POSITION_CELL, index, total);
                Pair<SliceItem, EventInfo> tagItem = new Pair(contentIntentItem, info);
                cellContainer.setTag(tagItem);
                makeClickable(cellContainer);
            }
        }
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
            if (color != -1 && !item.hasHint(HINT_NO_TINT) && !item.hasHint(HINT_LARGE)) {
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
        Pair<SliceItem, EventInfo> tagItem = (Pair<SliceItem, EventInfo>) view.getTag();
        final SliceItem actionItem = tagItem.first;
        final EventInfo info = tagItem.second;
        if (actionItem != null && FORMAT_ACTION.equals(actionItem.getFormat())) {
            try {
                actionItem.getAction().send();
                if (mObserver != null) {
                    mObserver.onSliceAction(info, actionItem);
                }
            } catch (PendingIntent.CanceledException e) {
                Log.w(TAG, "PendingIntent for slice cannot be sent", e);
            }
        }
    }

    @Override
    public void resetView() {
        mIsAllImages = true;
        mGridContent = null;
        mViewContainer.removeAllViews();
    }
}
