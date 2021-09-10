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

import static android.app.slice.Slice.EXTRA_RANGE_VALUE;
import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_MILLIS;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.slice.core.SliceHints.LARGE_IMAGE;
import static androidx.slice.core.SliceHints.RAW_IMAGE_LARGE;
import static androidx.slice.core.SliceHints.SUBTYPE_DATE_PICKER;
import static androidx.slice.core.SliceHints.SUBTYPE_TIME_PICKER;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_DATE_PICK;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_TIME_PICK;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_TOGGLE;
import static androidx.slice.widget.EventInfo.ROW_TYPE_DATE_PICK;
import static androidx.slice.widget.EventInfo.ROW_TYPE_TIME_PICK;
import static androidx.slice.widget.EventInfo.ROW_TYPE_TOGGLE;
import static androidx.slice.widget.SliceView.MODE_SMALL;

import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.slice.CornerDrawable;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceHints;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@RequiresApi(19)
public class GridRowView extends SliceChildView implements View.OnClickListener,
        View.OnTouchListener {

    private static final String TAG = "GridRowView";

    private static final int TEXT_LAYOUT = R.layout.abc_slice_secondary_text;

    // Max number of text items that can show in a cell
    private static final int MAX_CELL_TEXT = 2;
    // Max number of text items that can show in a cell if the mode is small
    private static final int MAX_CELL_TEXT_SMALL = 1;
    // Max number of images that can show in a cell
    private static final int MAX_CELL_IMAGES = 1;
    private final int mGutter;
    private final int mTextPadding;

    private final int[] mLoc = new int[2];

    boolean mMaxCellUpdateScheduled;

    private int mHiddenItemCount;

    /**
     * @hide
     */
    protected final View mForeground;
    /**
     * @hide
     */
    protected int mRowIndex;
    /**
     * @hide
     */
    protected int mRowCount;
    /**
     * @hide
     */
    protected int mMaxCells = -1;
    /**
     * @hide
     */
    protected @Nullable GridContent mGridContent;
    /**
     * @hide
     */
    protected final int mLargeImageHeight;
    /**
     * @hide
     */
    protected final int mSmallImageSize;
    /**
     * @hide
     */
    protected final int mSmallImageMinWidth;
    /**
     * @hide
     */
    protected final int mIconSize;
    /**
     * @hide
     */
    protected final LinearLayout mViewContainer;

    public GridRowView(@NonNull Context context) {
        this(context, null);
    }

    public GridRowView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        final Resources res = getContext().getResources();
        mViewContainer = new LinearLayout(getContext());
        mViewContainer.setOrientation(LinearLayout.HORIZONTAL);
        addView(mViewContainer, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
        mViewContainer.setGravity(Gravity.CENTER_VERTICAL);
        mIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mSmallImageSize = res.getDimensionPixelSize(R.dimen.abc_slice_small_image_size);
        mLargeImageHeight = res.getDimensionPixelSize(R.dimen.abc_slice_grid_image_only_height);
        mSmallImageMinWidth = res.getDimensionPixelSize(R.dimen.abc_slice_grid_image_min_width);
        mGutter = res.getDimensionPixelSize(R.dimen.abc_slice_grid_gutter);
        mTextPadding = res.getDimensionPixelSize(R.dimen.abc_slice_grid_text_padding);

        mForeground = new View(getContext());
        addView(mForeground, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setInsets(int l, int t, int r, int b) {
        super.setInsets(l, t, r, b);
        mViewContainer.setPadding(l, t + getExtraTopPadding(), r, b + getExtraBottomPadding());
    }

    protected int getTitleTextLayout() {
        return R.layout.abc_slice_title;
    }

    protected int getExtraTopPadding() {
        if (mGridContent != null && mGridContent.isAllImages()) {
            // Might need to add padding if in first or last position
            if (mRowIndex == 0) {
                return mSliceStyle != null ? mSliceStyle.getGridTopPadding() : 0;
            }
        }
        return 0;
    }

    protected int getExtraBottomPadding() {
        if (mGridContent != null && mGridContent.isAllImages()) {
            if (mRowIndex == mRowCount - 1 || getMode() == MODE_SMALL) {
                return mSliceStyle != null ? mSliceStyle.getGridBottomPadding() : 0;
            }
        }
        return 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = mGridContent.getHeight(mSliceStyle, mViewPolicy)
                + mInsetTop + mInsetBottom;
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        mViewContainer.getLayoutParams().height = height;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setTint(@ColorInt int tintColor) {
        super.setTint(tintColor);
        if (mGridContent != null) {
            // TODO -- could be smarter about this
            resetView();
            populateViews();
        }
    }

    /**
     * This is called when GridView is being used as a component in a larger template.
     */
    @Override
    public void setSliceItem(@NonNull SliceContent slice, boolean isHeader, int rowIndex,
            int rowCount, @Nullable SliceView.OnSliceActionListener observer) {
        resetView();
        setSliceActionListener(observer);
        mRowIndex = rowIndex;
        mRowCount = rowCount;
        mGridContent = (GridContent) slice;

        if (!scheduleMaxCellsUpdate()) {
            populateViews();
        }
        mViewContainer.setPadding(mInsetStart, mInsetTop + getExtraTopPadding(), mInsetEnd,
                mInsetBottom + getExtraBottomPadding());
    }

    /**
     * Schedules update to determine the max number of cells that can be shown in this grid.
     *
     * @return true if update was scheduled, false if it wasn't needed
     */
    protected boolean scheduleMaxCellsUpdate() {
        if (mGridContent == null || !mGridContent.isValid()) {
            return true;
        }
        if (getWidth() == 0) {
            // Need to wait for layout pass so we know how much width we have for the grid items.
            mMaxCellUpdateScheduled = true;
            getViewTreeObserver().addOnPreDrawListener(mMaxCellsUpdater);
            return true;
        } else {
            mMaxCells = getMaxCells();
            return false;
        }
    }

    protected int getMaxCells() {
        if (mGridContent == null || !mGridContent.isValid() || getWidth() == 0) {
            return -1;
        }
        ArrayList<GridContent.CellContent> cells = mGridContent.getGridContent();
        if (cells.size() > 1) {
            int desiredCellWidth;
            switch (mGridContent.getLargestImageMode()) {
                case LARGE_IMAGE:
                    desiredCellWidth = mLargeImageHeight;
                    break;
                case RAW_IMAGE_LARGE:
                    desiredCellWidth = mGridContent.getFirstImageSize(getContext()).x;
                    break;
                default:
                    desiredCellWidth = mSmallImageMinWidth;
            }
            return getWidth() / (desiredCellWidth + mGutter);
        } else {
            return 1;
        }
    }

    protected void populateViews() {
        if (mGridContent == null || !mGridContent.isValid()) {
            resetView();
            return;
        }
        if (scheduleMaxCellsUpdate()) {
            return;
        }
        if (mGridContent.getLayoutDir() != -1) {
            setLayoutDirection(mGridContent.getLayoutDir());
        }
        if (mGridContent.getContentIntent() != null) {
            EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_CONTENT,
                    EventInfo.ROW_TYPE_GRID, mRowIndex);
            Pair<SliceItem, EventInfo> tagItem = new Pair<>(mGridContent.getContentIntent(), info);
            mViewContainer.setTag(tagItem);
            makeEntireGridClickable(true);
        }
        CharSequence contentDescr = mGridContent.getContentDescription();
        if (contentDescr != null) {
            mViewContainer.setContentDescription(contentDescr);
        }
        ArrayList<GridContent.CellContent> cells = mGridContent.getGridContent();
        if (mGridContent.getLargestImageMode() == LARGE_IMAGE
                || mGridContent.getLargestImageMode() == RAW_IMAGE_LARGE) {
            mViewContainer.setGravity(Gravity.TOP);
        } else {
            mViewContainer.setGravity(Gravity.CENTER_VERTICAL);
        }
        int maxCells = mMaxCells;
        boolean hasSeeMore = mGridContent.getSeeMoreItem() != null;
        mHiddenItemCount = 0;
        for (int i = 0; i < cells.size(); i++) {
            if (mViewContainer.getChildCount() >= maxCells) {
                mHiddenItemCount = cells.size() - maxCells;
                if (hasSeeMore) {
                    addSeeMoreCount(mHiddenItemCount);
                }
                break;
            }
            addCell(cells.get(i), i, Math.min(cells.size(), maxCells));
        }
    }

    private void addSeeMoreCount(int numExtra) {
        // Remove last element
        View last = mViewContainer.getChildAt(mViewContainer.getChildCount() - 1);
        mViewContainer.removeView(last);

        SliceItem seeMoreItem = mGridContent.getSeeMoreItem();
        int index = mViewContainer.getChildCount();
        int total = mMaxCells;
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
        View extraTint;
        ViewGroup seeMoreView;
        if (mGridContent.isAllImages()) {
            seeMoreView = (FrameLayout) inflater.inflate(R.layout.abc_slice_grid_see_more_overlay,
                    mViewContainer, false);
            seeMoreView.addView(last, 0, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
            extraText = seeMoreView.findViewById(R.id.text_see_more_count);
            extraTint = seeMoreView.findViewById(R.id.overlay_see_more);
            extraTint.setBackground(new CornerDrawable(SliceViewUtil.getDrawable(
                    getContext(), android.R.attr.colorForeground),
                    mSliceStyle.getImageCornerRadius()));
        } else {
            seeMoreView = (LinearLayout) inflater.inflate(
                    R.layout.abc_slice_grid_see_more, mViewContainer, false);
            extraText = seeMoreView.findViewById(R.id.text_see_more_count);

            // Update text appearance
            TextView moreText = seeMoreView.findViewById(R.id.text_see_more);
            if (mSliceStyle != null && mRowStyle != null) {
                moreText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSliceStyle.getGridTitleSize());
                moreText.setTextColor(mRowStyle.getTitleColor());
            }
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
        final int maxCellText = getMode() == MODE_SMALL && mGridContent.hasImage()
                ? MAX_CELL_TEXT_SMALL : MAX_CELL_TEXT;
        LinearLayout cellContainer = new LinearLayout(getContext());
        cellContainer.setOrientation(LinearLayout.VERTICAL);
        cellContainer.setGravity(Gravity.CENTER_HORIZONTAL);

        ArrayList<SliceItem> cellItems = cell.getCellItems();
        SliceItem contentIntentItem = cell.getContentIntent();
        SliceItem pickerItem = cell.getPicker();
        SliceItem toggleItem = cell.getToggleItem();

        int textCount = 0;
        int imageCount = 0;
        boolean added = false;
        boolean isSingleItem = cellItems.size() == 1;
        List<SliceItem> textItems = null;
        // In small format we display one text item and prefer titles
        if (!isSingleItem && getMode() == MODE_SMALL) {
            // Get all our text items
            textItems = new ArrayList<>();
            for (SliceItem cellItem : cellItems) {
                if (FORMAT_TEXT.equals(cellItem.getFormat())) {
                    textItems.add(cellItem);
                }
            }
            // If we have more than 1 remove non-titles
            Iterator<SliceItem> iterator = textItems.iterator();
            while (textItems.size() > maxCellText) {
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
                if (addTextItem(item, cellContainer, padding)) {
                    prevItem = item;
                    textCount++;
                    added = true;
                }
            } else if (imageCount < MAX_CELL_IMAGES && FORMAT_IMAGE.equals(item.getFormat())) {
                if (addImageItem(item, cell.getOverlayItem(), mTintColor, cellContainer,
                        isSingleItem)) {
                    prevItem = item;
                    imageCount++;
                    added = true;
                }
            }
        }
        if (pickerItem != null) {
            if (SUBTYPE_DATE_PICKER.equals(pickerItem.getSubType())) {
                added = addPickerItem(pickerItem, cellContainer, determinePadding(prevItem),
                        /*isDatePicker=*/ true);
            } else if (SUBTYPE_TIME_PICKER.equals(pickerItem.getSubType())) {
                added = addPickerItem(pickerItem, cellContainer, determinePadding(prevItem),
                        /*isDatePicker=*/ false);
            }
        }
        SliceActionView sav = null;
        if (toggleItem != null) {
            sav = new SliceActionView(getContext(), mSliceStyle, mRowStyle);
            cellContainer.addView(sav);
            added = true;
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
            if (toggleItem != null) {
                EventInfo info =
                        new EventInfo(getMode(), ACTION_TYPE_TOGGLE, ROW_TYPE_TOGGLE, mRowIndex);
                sav.setAction(
                        new SliceActionImpl(toggleItem),
                        info, mObserver, mTintColor, mLoadingListener);
                info.setPosition(EventInfo.POSITION_CELL, index, total);
            }
        }
    }

    /**
     * Adds simple text based items to a container. Simple text items include text and
     * timestamps.
     *
     * @param item      item to add to the container.
     * @param container the container to add to.
     * @param padding   the padding to apply to the item.
     * @return Whether an item was added.
     */
    private boolean addTextItem(SliceItem item, ViewGroup container, int padding) {
        final String format = item.getFormat();
        if (!FORMAT_TEXT.equals(format) && !FORMAT_LONG.equals(format)) {
            return false;
        }
        boolean isTitle = SliceQuery.hasAnyHints(item, HINT_LARGE, HINT_TITLE);
        TextView tv = (TextView) LayoutInflater.from(getContext()).inflate(isTitle
                ? getTitleTextLayout() : TEXT_LAYOUT, null);
        if (mSliceStyle != null && mRowStyle != null) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, isTitle
                    ? mSliceStyle.getGridTitleSize() : mSliceStyle.getGridSubtitleSize());
            tv.setTextColor(isTitle
                    ? mRowStyle.getTitleColor() : mRowStyle.getSubtitleColor());
        }
        CharSequence text = FORMAT_LONG.equals(format)
                ? SliceViewUtil.getTimestampString(getContext(), item.getLong())
                : item.getSanitizedText();
        tv.setText(text);
        container.addView(tv);
        tv.setPadding(0, padding, 0, 0);
        return true;
    }

    /**
     * Adds simple image based item to a container.
     *
     * @param item        item to add to the container.
     * @param overlayItem overlaid text to add to the image.
     * @param container   the container to add to.
     * @param isSingle    whether this is the only item in the cell or not.
     * @return Whether an item was added.
     */
    protected boolean addImageItem(@NonNull SliceItem item, @Nullable SliceItem overlayItem,
            int color,
            @NonNull ViewGroup container, boolean isSingle) {
        final String format = item.getFormat();
        final boolean hasRoundedImage =
                mSliceStyle != null && mSliceStyle.getApplyCornerRadiusToLargeImages();
        if (!FORMAT_IMAGE.equals(format) || item.getIcon() == null) {
            return false;
        }
        Drawable d = item.getIcon().loadDrawable(getContext());
        if (d == null) {
            return false;
        }
        ImageView iv = new ImageView(getContext());
        if (hasRoundedImage) {
            CornerDrawable cd = new CornerDrawable(d, mSliceStyle.getImageCornerRadius());
            iv.setImageDrawable(cd);
        } else {
            iv.setImageDrawable(d);
        }
        LinearLayout.LayoutParams lp;
        if (item.hasHint(SliceHints.HINT_RAW)) {
            iv.setScaleType(ScaleType.CENTER_INSIDE);
            lp = new LinearLayout.LayoutParams(mGridContent.getFirstImageSize(getContext()).x,
                    mGridContent.getFirstImageSize(getContext()).y);
        } else if (item.hasHint(HINT_LARGE)) {
            iv.setScaleType(hasRoundedImage ? ScaleType.FIT_XY : ScaleType.CENTER_CROP);
            int height = isSingle ? MATCH_PARENT : mLargeImageHeight;
            lp = new LinearLayout.LayoutParams(MATCH_PARENT, height);
        } else {
            boolean isIcon = !item.hasHint(HINT_NO_TINT);
            int size = !isIcon ? mSmallImageSize : mIconSize;
            iv.setScaleType(isIcon ? ScaleType.CENTER_INSIDE : ScaleType.CENTER_CROP);
            lp = new LinearLayout.LayoutParams(size, size);
        }
        if (color != -1 && !item.hasHint(HINT_NO_TINT)) {
            iv.setColorFilter(color);
        }
        // don't add an overlay on see more
        if (overlayItem == null || mViewContainer.getChildCount() == mMaxCells - 1) {
            container.addView(iv, lp);
            return true;
        }
        // add overlay on top of the ImageView
        LayoutInflater inflater = LayoutInflater.from(getContext());
        TextView overlayText;
        View overlayTint;
        ViewGroup overlayView;
        overlayView = (FrameLayout) inflater.inflate(R.layout.abc_slice_grid_text_overlay_image,
                container, false);
        overlayView.addView(iv, 0, new LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        overlayText = overlayView.findViewById(R.id.text_overlay);
        overlayText.setText(overlayItem.getText());
        overlayTint = overlayView.findViewById(R.id.tint_overlay);
        overlayTint.setBackground(new CornerDrawable(ContextCompat.getDrawable(getContext(),
                R.drawable.abc_slice_gradient), mSliceStyle.getImageCornerRadius()));
        container.addView(overlayView, lp);
        return true;
    }

    /**
     * Adds date or time picker to a container.
     *
     * @param pickerItem item to add to the container.
     * @param container      the container to add to.
     * @param padding        the padding to apply to the item.
     * @param isDatePicker   if true, it is a date picker, otherwise is a time picker.
     * @return Whether an item was added.
     */
    private boolean addPickerItem(SliceItem pickerItem, ViewGroup container, int padding,
            boolean isDatePicker) {
        SliceItem dateTimeItem = SliceQuery.findSubtype(pickerItem, FORMAT_LONG,
                SUBTYPE_MILLIS);
        if (dateTimeItem == null) {
            return false;
        }
        long dateTimeMillis = dateTimeItem.getLong();

        TextView tv = (TextView) LayoutInflater.from(getContext()).inflate(getTitleTextLayout(),
                null);
        if (mSliceStyle != null) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSliceStyle.getGridTitleSize());
            tv.setTextColor(mSliceStyle.getTitleColor());
        }

        Date date = new Date(dateTimeMillis);
        SliceItem titleItem = SliceQuery.find(pickerItem, FORMAT_TEXT, HINT_TITLE,
                /*nonHints=*/null);
        if (titleItem != null) {
            tv.setText(titleItem.getText());
        }

        int rowIndex = mRowIndex;

        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                if (isDatePicker) {
                    DatePickerDialog dialog = new DatePickerDialog(
                            getContext(),
                            R.style.DialogTheme,
                            new DateSetListener(pickerItem, rowIndex),
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH));
                    dialog.show();
                } else {
                    TimePickerDialog dialog = new TimePickerDialog(
                            getContext(),
                            R.style.DialogTheme,
                            new TimeSetListener(pickerItem, rowIndex),
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            false);
                    dialog.show();
                }
            }
        });
        container.setClickable(true);

        int backgroundAttr = android.R.attr.selectableItemBackground;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            backgroundAttr = android.R.attr.selectableItemBackgroundBorderless;
        }
        container.setBackground(SliceViewUtil.getDrawable(getContext(), backgroundAttr));
        container.addView(tv);
        tv.setPadding(0, padding, 0, 0);
        return true;
    }

    private class DateSetListener implements DatePickerDialog.OnDateSetListener {
        private final SliceItem mActionItem;
        private final int mRowIndex;

        DateSetListener(SliceItem datePickerItem, int mRowIndex) {
            this.mActionItem = datePickerItem;
            this.mRowIndex = mRowIndex;
        }

        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
            Calendar c = Calendar.getInstance();
            c.set(year, month, day);
            Date date = c.getTime();


            if (mActionItem != null) {
                try {
                    mActionItem.fireAction(getContext(),
                            new Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                    .putExtra(EXTRA_RANGE_VALUE, date.getTime()));
                    if (mObserver != null) {
                        EventInfo info = new EventInfo(getMode(), ACTION_TYPE_DATE_PICK,
                                ROW_TYPE_DATE_PICK,
                                mRowIndex);
                        mObserver.onSliceAction(info, mActionItem);
                    }
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "PendingIntent for slice cannot be sent", e);
                }
            }
        }
    }

    private class TimeSetListener implements TimePickerDialog.OnTimeSetListener {
        private final SliceItem mActionItem;
        private final int mRowIndex;

        TimeSetListener(SliceItem timePickerItem, int mRowIndex) {
            this.mActionItem = timePickerItem;
            this.mRowIndex = mRowIndex;
        }

        @Override
        public void onTimeSet(TimePicker timePicker, int hour, int minute) {
            Calendar c = Calendar.getInstance();
            Date time = c.getTime();
            time.setHours(hour);
            time.setMinutes(minute);

            if (mActionItem != null) {
                try {
                    mActionItem.fireAction(getContext(),
                            new Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                    .putExtra(EXTRA_RANGE_VALUE, time.getTime()));
                    if (mObserver != null) {
                        EventInfo info = new EventInfo(getMode(), ACTION_TYPE_TIME_PICK,
                                ROW_TYPE_TIME_PICK,
                                mRowIndex);
                        mObserver.onSliceAction(info, mActionItem);
                    }
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "PendingIntent for slice cannot be sent", e);
                }
            }
        }
    }

    private int determinePadding(SliceItem prevItem) {
        if (prevItem == null) {
            // No need for top padding
            return 0;
        } else if (FORMAT_IMAGE.equals(prevItem.getFormat())) {
            return mTextPadding;
        } else if (FORMAT_TEXT.equals(prevItem.getFormat())
                || FORMAT_LONG.equals(prevItem.getFormat())) {
            return mSliceStyle != null ? mSliceStyle.getVerticalGridTextPadding() : 0;
        }
        return 0;
    }

    private void makeEntireGridClickable(boolean isClickable) {
        mViewContainer.setOnTouchListener(isClickable ? this : null);
        mViewContainer.setOnClickListener((isClickable ? this : null));
        mForeground.setBackground(isClickable
                ? SliceViewUtil.getDrawable(
                        getContext(), android.R.attr.selectableItemBackground)
                : null);
        mViewContainer.setClickable(isClickable);
        setClickable(isClickable);
    }

    private void makeClickable(View layout, boolean isClickable) {
        layout.setOnClickListener(isClickable ? this : null);
        int backgroundAttr = android.R.attr.selectableItemBackground;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            backgroundAttr = android.R.attr.selectableItemBackgroundBorderless;
        }
        layout.setBackground(isClickable
                ? SliceViewUtil.getDrawable(getContext(), backgroundAttr)
                : null);
        layout.setClickable(isClickable);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    @SuppressWarnings("unchecked")
    public void onClick(@NonNull View view) {
        Pair<SliceItem, EventInfo> tagItem = (Pair<SliceItem, EventInfo>) view.getTag();
        final SliceItem sliceItem = tagItem.first;
        final EventInfo info = tagItem.second;
        if (sliceItem != null) {
            final SliceItem actionItem = SliceQuery.find(sliceItem,
                    FORMAT_ACTION, (String) null, null);
            if (actionItem != null) {
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
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public boolean onTouch(@NonNull View view, @NonNull MotionEvent event) {
        onForegroundActivated(event);
        return false;
    }

    private void onForegroundActivated(MotionEvent event) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mForeground.getLocationOnScreen(mLoc);
            final int x = (int) (event.getRawX() - mLoc[0]);
            final int y = (int) (event.getRawY() - mLoc[1]);
            mForeground.getBackground().setHotspot(x, y);
        }
        int action = event.getActionMasked();
        if (action == android.view.MotionEvent.ACTION_DOWN) {
            mForeground.setPressed(true);
        } else if (action == android.view.MotionEvent.ACTION_CANCEL
                || action == android.view.MotionEvent.ACTION_UP
                || action == android.view.MotionEvent.ACTION_MOVE) {
            mForeground.setPressed(false);
        }
    }

    @Override
    public void resetView() {
        if (mMaxCellUpdateScheduled) {
            mMaxCellUpdateScheduled = false;
            getViewTreeObserver().removeOnPreDrawListener(mMaxCellsUpdater);
        }
        mViewContainer.removeAllViews();
        setLayoutDirection(View.LAYOUT_DIRECTION_INHERIT);
        makeEntireGridClickable(false);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public int getHiddenItemCount() {
        return mHiddenItemCount;
    }

    private final ViewTreeObserver.OnPreDrawListener mMaxCellsUpdater =
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mMaxCells = getMaxCells();
                    populateViews();
                    getViewTreeObserver().removeOnPreDrawListener(this);
                    mMaxCellUpdateScheduled = false;
                    return true;
                }
            };
}
