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

package androidx.slice.widget;

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.slice.widget.SliceView.MODE_SMALL;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
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

import androidx.annotation.ColorInt;
import androidx.annotation.RestrictTo;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class GridRowView extends SliceChildView implements View.OnClickListener {

    private static final String TAG = "GridView";

    private static final int TITLE_TEXT_LAYOUT = R.layout.abc_slice_title;
    private static final int TEXT_LAYOUT = R.layout.abc_slice_secondary_text;

    // Max number of normal cell items that can be shown in a row
    private static final int MAX_CELLS = 5;

    // Max number of text items that can show in a cell
    private static final int MAX_CELL_TEXT = 2;
    // Max number of text items that can show in a cell if the mode is small
    private static final int MAX_CELL_TEXT_SMALL = 1;
    // Max number of images that can show in a cell
    private static final int MAX_CELL_IMAGES = 1;

    private int mRowIndex;
    private int mRowCount;

    private int mSmallImageSize;
    private int mIconSize;
    private int mGutter;
    private int mTextPadding;

    private GridContent mGridContent;
    private LinearLayout mViewContainer;

    public GridRowView(Context context) {
        this(context, null);
    }

    public GridRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final Resources res = getContext().getResources();
        mViewContainer = new LinearLayout(getContext());
        mViewContainer.setOrientation(LinearLayout.HORIZONTAL);
        addView(mViewContainer, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
        mViewContainer.setGravity(Gravity.CENTER_VERTICAL);
        mIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mSmallImageSize = res.getDimensionPixelSize(R.dimen.abc_slice_small_image_size);
        mGutter = res.getDimensionPixelSize(R.dimen.abc_slice_grid_gutter);
        mTextPadding = res.getDimensionPixelSize(R.dimen.abc_slice_grid_text_padding);
    }

    @Override
    public int getSmallHeight() {
        // GridRow is small if its the first element in a list without a header presented in small
        if (mGridContent == null) {
            return 0;
        }
        return mGridContent.getSmallHeight() + getExtraTopPadding() + getExtraBottomPadding();
    }

    @Override
    public int getActualHeight() {
        if (mGridContent == null) {
            return 0;
        }
        return mGridContent.getActualHeight() + getExtraTopPadding() + getExtraBottomPadding();
    }

    private int getExtraTopPadding() {
        if (mGridContent != null && mGridContent.isAllImages()) {
            // Might need to add padding if in first or last position
            if (mRowIndex == 0) {
                return mGridTopPadding;
            }
        }
        return 0;
    }

    private int getExtraBottomPadding() {
        if (mGridContent != null && mGridContent.isAllImages()) {
            if (mRowIndex == mRowCount - 1 || getMode() == MODE_SMALL) {
                return mGridBottomPadding;
            }
        }
        return 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = getMode() == MODE_SMALL ? getSmallHeight() : getActualHeight();
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        mViewContainer.getLayoutParams().height = height;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setTint(@ColorInt int tintColor) {
        super.setTint(tintColor);
        if (mGridContent != null) {
            GridContent gc = mGridContent;
            // TODO -- could be smarter about this
            resetView();
            populateViews(gc);
        }
    }

    /**
     * This is called when GridView is being used as a component in a larger template.
     */
    @Override
    public void setSliceItem(SliceItem slice, boolean isHeader, int rowIndex,
            int rowCount, SliceView.OnSliceActionListener observer) {
        resetView();
        setSliceActionListener(observer);
        mRowIndex = rowIndex;
        mRowCount = rowCount;
        mGridContent = new GridContent(getContext(), slice);
        populateViews(mGridContent);
        mViewContainer.setPadding(0, getExtraTopPadding(), 0, getExtraBottomPadding());
    }

    private void populateViews(GridContent gc) {
        if (gc.getContentIntent() != null) {
            EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_CONTENT,
                    EventInfo.ROW_TYPE_GRID, mRowIndex);
            Pair<SliceItem, EventInfo> tagItem = new Pair<>(gc.getContentIntent(), info);
            mViewContainer.setTag(tagItem);
            makeClickable(mViewContainer, true);
        }
        CharSequence contentDescr = gc.getContentDescription();
        if (contentDescr != null) {
            mViewContainer.setContentDescription(contentDescr);
        }
        ArrayList<GridContent.CellContent> cells = gc.getGridContent();
        boolean hasSeeMore = gc.getSeeMoreItem() != null;
        for (int i = 0; i < cells.size(); i++) {
            if (mViewContainer.getChildCount() >= MAX_CELLS) {
                if (hasSeeMore) {
                    addSeeMoreCount(cells.size() - MAX_CELLS);
                }
                break;
            }
            addCell(cells.get(i), i, Math.min(cells.size(), MAX_CELLS));
        }
    }

    private void addSeeMoreCount(int numExtra) {
        // Remove last element
        View last = mViewContainer.getChildAt(mViewContainer.getChildCount() - 1);
        mViewContainer.removeView(last);

        SliceItem seeMoreItem = mGridContent.getSeeMoreItem();
        int index = mViewContainer.getChildCount();
        int total = MAX_CELLS;
        if ((FORMAT_SLICE.equals(seeMoreItem.getFormat())
                || FORMAT_ACTION.equals(seeMoreItem.getFormat()))
                && seeMoreItem.getSlice().getItems().size() > 0) {
            // It's a custom see more cell, add it
            addCell(new GridContent.CellContent(seeMoreItem), index, total);
            return;
        }

        // Default see more, create it
        LayoutInflater inflater = LayoutInflater.from(getContext());
        TextView extraText;
        ViewGroup seeMoreView;
        if (mGridContent.isAllImages()) {
            seeMoreView = (FrameLayout) inflater.inflate(R.layout.abc_slice_grid_see_more_overlay,
                    mViewContainer, false);
            seeMoreView.addView(last, 0, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
            extraText = seeMoreView.findViewById(R.id.text_see_more_count);
        } else {
            seeMoreView = (LinearLayout) inflater.inflate(
                    R.layout.abc_slice_grid_see_more, mViewContainer, false);
            extraText = seeMoreView.findViewById(R.id.text_see_more_count);

            // Update text appearance
            TextView moreText = seeMoreView.findViewById(R.id.text_see_more);
            moreText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mGridTitleSize);
            moreText.setTextColor(mTitleColor);
        }
        mViewContainer.addView(seeMoreView, new LinearLayout.LayoutParams(0, MATCH_PARENT, 1));
        extraText.setText(getResources().getString(R.string.abc_slice_more_content, numExtra));

        // Make it clickable
        EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_SEE_MORE,
                EventInfo.ROW_TYPE_GRID, mRowIndex);
        info.setPosition(EventInfo.POSITION_CELL, index, total);
        Pair<SliceItem, EventInfo> tagItem = new Pair<>(seeMoreItem, info);
        seeMoreView.setTag(tagItem);
        makeClickable(seeMoreView, true);
    }

    /**
     * Adds a cell to the grid view based on the provided {@link SliceItem}.
     */
    private void addCell(GridContent.CellContent cell, int index, int total) {
        final int maxCellText = getMode() == MODE_SMALL
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
        if (!singleItem && getMode() == MODE_SMALL) {
            // Get all our text items
            textItems = new ArrayList<>();
            for (SliceItem cellItem : cellItems) {
                if (FORMAT_TEXT.equals(cellItem.getFormat())) {
                    textItems.add(cellItem);
                }
            }
            // If we have more than 1 remove non-titles
            Iterator<SliceItem> iterator = textItems.iterator();
            while (textItems.size() > 1) {
                SliceItem item = iterator.next();
                if (!item.hasAnyHints(HINT_TITLE, HINT_LARGE)) {
                    iterator.remove();
                }
            }
        }
        SliceItem prevItem = null;
        for (int i = 0; i < cellItems.size(); i++) {
            SliceItem item = cellItems.get(i);
            final String itemFormat = item.getFormat();
            int padding = determinePadding(prevItem);
            if (textCount < maxCellText && (FORMAT_TEXT.equals(itemFormat)
                    || FORMAT_LONG.equals(itemFormat))) {
                if (textItems != null && !textItems.contains(item)) {
                    continue;
                }
                if (addItem(item, mTintColor, cellContainer, padding)) {
                    prevItem = item;
                    textCount++;
                    added = true;
                }
            } else if (imageCount < MAX_CELL_IMAGES && FORMAT_IMAGE.equals(item.getFormat())) {
                if (addItem(item, mTintColor, cellContainer, 0)) {
                    prevItem = item;
                    imageCount++;
                    added = true;
                }
            }
        }
        if (added) {
            CharSequence contentDescr = cell.getContentDescription();
            if (contentDescr != null) {
                cellContainer.setContentDescription(contentDescr);
            }
            mViewContainer.addView(cellContainer,
                    new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
            if (index != total - 1) {
                // If we're not the last or only element add space between items
                MarginLayoutParams lp =
                        (LinearLayout.MarginLayoutParams) cellContainer.getLayoutParams();
                lp.setMarginEnd(mGutter);
                cellContainer.setLayoutParams(lp);
            }
            if (contentIntentItem != null) {
                EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_BUTTON,
                        EventInfo.ROW_TYPE_GRID, mRowIndex);
                info.setPosition(EventInfo.POSITION_CELL, index, total);
                Pair<SliceItem, EventInfo> tagItem = new Pair<>(contentIntentItem, info);
                cellContainer.setTag(tagItem);
                makeClickable(cellContainer, true);
            }
        }
    }

    /**
     * Adds simple items to a container. Simple items include icons, text, and timestamps.
     *
     * @param item item to add to the container.
     * @param container the container to add to.
     * @param padding the padding to apply to the item.
     *
     * @return Whether an item was added.
     */
    private boolean addItem(SliceItem item, int color, ViewGroup container, int padding) {
        final String format = item.getFormat();
        View addedView = null;
        if (FORMAT_TEXT.equals(format) || FORMAT_LONG.equals(format)) {
            boolean title = SliceQuery.hasAnyHints(item, HINT_LARGE, HINT_TITLE);
            TextView tv = (TextView) LayoutInflater.from(getContext()).inflate(title
                    ? TITLE_TEXT_LAYOUT : TEXT_LAYOUT, null);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, title ? mGridTitleSize : mGridSubtitleSize);
            tv.setTextColor(title ? mTitleColor : mSubtitleColor);
            CharSequence text = FORMAT_LONG.equals(format)
                    ? SliceViewUtil.getRelativeTimeString(item.getTimestamp())
                    : item.getText();
            tv.setText(text);
            container.addView(tv);
            tv.setPadding(0, padding, 0, 0);
            addedView = tv;
        } else if (FORMAT_IMAGE.equals(format)) {
            ImageView iv = new ImageView(getContext());
            iv.setImageDrawable(item.getIcon().loadDrawable(getContext()));
            LinearLayout.LayoutParams lp;
            if (item.hasHint(HINT_LARGE)) {
                iv.setScaleType(ScaleType.CENTER_CROP);
                lp = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
            } else {
                boolean isIcon = !item.hasHint(HINT_NO_TINT);
                int size = isIcon ? mIconSize : mSmallImageSize;
                iv.setScaleType(isIcon ? ScaleType.CENTER_INSIDE : ScaleType.CENTER_CROP);
                lp = new LinearLayout.LayoutParams(size, size);
            }
            if (color != -1 && !item.hasHint(HINT_NO_TINT)) {
                iv.setColorFilter(color);
            }
            container.addView(iv, lp);
            addedView = iv;
        }
        return addedView != null;
    }

    private int determinePadding(SliceItem prevItem) {
        if (prevItem == null) {
            // No need for top padding
            return 0;
        } else if (FORMAT_IMAGE.equals(prevItem.getFormat())) {
            return mTextPadding;
        } else if (FORMAT_TEXT.equals(prevItem.getFormat())
                || FORMAT_LONG.equals(prevItem.getFormat())) {
            return mVerticalGridTextPadding;
        }
        return 0;
    }

    private void makeClickable(View layout, boolean isClickable) {
        layout.setOnClickListener(isClickable ? this : null);
        layout.setBackground(isClickable
                ? SliceViewUtil.getDrawable(getContext(), android.R.attr.selectableItemBackground)
                : null);
        layout.setClickable(isClickable);
    }

    @Override
    public void onClick(View view) {
        Pair<SliceItem, EventInfo> tagItem = (Pair<SliceItem, EventInfo>) view.getTag();
        final SliceItem actionItem = tagItem.first;
        final EventInfo info = tagItem.second;
        if (actionItem != null && FORMAT_ACTION.equals(actionItem.getFormat())) {
            try {
                actionItem.fireAction(null, null);
                if (mObserver != null) {
                    mObserver.onSliceAction(info, actionItem);
                }
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "PendingIntent for slice cannot be sent", e);
            }
        }
    }

    @Override
    public void resetView() {
        mViewContainer.removeAllViews();
        makeClickable(mViewContainer, false);
    }
}
