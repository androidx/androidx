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
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.SUBTYPE_MAX;
import static android.app.slice.Slice.SUBTYPE_VALUE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import static androidx.slice.core.SliceHints.ICON_IMAGE;
import static androidx.slice.core.SliceHints.SMALL_IMAGE;
import static androidx.slice.core.SliceHints.SUBTYPE_MIN;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_BUTTON;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_SLIDER;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_TOGGLE;
import static androidx.slice.widget.EventInfo.ROW_TYPE_LIST;
import static androidx.slice.widget.EventInfo.ROW_TYPE_SLIDER;
import static androidx.slice.widget.EventInfo.ROW_TYPE_TOGGLE;
import static androidx.slice.widget.SliceView.MODE_SMALL;

import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.SliceItem;
import androidx.slice.SliceStructure;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Row item is in small template format and can be used to construct list items for use
 * with {@link LargeTemplateView}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
public class RowView extends SliceChildView implements View.OnClickListener {

    private static final String TAG = "RowView";

    // The number of items that fit on the right hand side of a small slice
    // TODO: this should be based on available width
    private static final int MAX_END_ITEMS = 3;
    // How frequently (ms) intent can be sent in response to slider moving.
    private static final int SLIDER_INTERVAL = 200;

    // On versions before M, SeekBar won't render properly if stretched taller than the default
    // size.
    private static final boolean sCanSpecifyLargerRangeBarHeight =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

    private LinearLayout mRootView;
    private LinearLayout mStartContainer;
    private LinearLayout mContent;
    private TextView mPrimaryText;
    private TextView mSecondaryText;
    private TextView mLastUpdatedText;
    private View mDivider;
    private ArrayMap<SliceActionImpl, SliceActionView> mToggles = new ArrayMap<>();
    private ArrayMap<SliceActionImpl, SliceActionView> mActions = new ArrayMap<>();
    private LinearLayout mEndContainer;
    private View mSeeMoreView;
    private ProgressBar mRangeBar;
    private ProgressBar mActionSpinner;
    protected Set<SliceItem> mLoadingActions = new HashSet<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mShowActionSpinner;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mRowIndex;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    RowContent mRowContent;
    private SliceActionImpl mRowAction;
    private SliceItem mStartItem;
    private boolean mIsHeader;
    private List<SliceAction> mHeaderActions;
    // Indicates whether header rows can have 2 lines of subtitle text
    private boolean mAllowTwoLines;

    // Indicates if there's a slider in this row that is currently being interacted with.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mIsRangeSliding;
    // Indicates that there was an update to the row but we skipped it while the slice was
    // being interacted with.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mRangeHasPendingUpdate;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mRangeUpdaterRunning;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Handler mHandler;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mLastSentRangeUpdate;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mRangeValue;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mRangeMinValue;
    private SliceItem mRangeItem;

    private int mImageSize;
    private int mIconSize;
    // How big the RowView wants mRangeBar to be.
    private int mIdealRangeHeight;
    // How big mRangeBar wants to be.
    private int mMeasuredRangeHeight;
    private int mMaxSmallHeight;

    public RowView(Context context) {
        super(context);
        mIconSize = getContext().getResources().getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mImageSize = getContext().getResources().getDimensionPixelSize(
                R.dimen.abc_slice_small_image_size);
        mRootView = (LinearLayout) LayoutInflater.from(context).inflate(
                R.layout.abc_slice_small_template, this, false);
        addView(mRootView);

        mStartContainer = (LinearLayout) findViewById(R.id.icon_frame);
        mContent = (LinearLayout) findViewById(android.R.id.content);
        mPrimaryText = (TextView) findViewById(android.R.id.title);
        mSecondaryText = (TextView) findViewById(android.R.id.summary);
        mLastUpdatedText = (TextView) findViewById(R.id.last_updated);
        mDivider = findViewById(R.id.divider);
        mActionSpinner = findViewById(R.id.action_sent_indicator);
        SliceViewUtil.tintIndeterminateProgressBar(getContext(), mActionSpinner);
        mEndContainer = (LinearLayout) findViewById(android.R.id.widget_frame);

        mIdealRangeHeight = context.getResources().getDimensionPixelSize(
                R.dimen.abc_slice_row_range_height);
    }

    @Override
    public void setInsets(int l, int t, int r, int b) {
        super.setInsets(l, t, r, b);
        mRootView.setPadding(l, t, r, b);
        if (mRangeBar != null) {
            updateRangePadding();
        }
    }

    /**
     * @return height row content (i.e. title, subtitle) without the height of the range element.
     */
    private int getRowContentHeight() {
        int rowHeight = mRowContent.getHeight(mSliceStyle, mViewPolicy);
        if (mRangeBar != null) {
            rowHeight -= mIdealRangeHeight;
        }
        return rowHeight;
    }

    @Override
    public void setTint(@ColorInt int tintColor) {
        super.setTint(tintColor);
        if (mRowContent != null) {
            // TODO -- can be smarter about this
            populateViews(true);
        }
    }

    /**
     * @param actions if the actions are null then there are no header actions for this row.
     * If the actions are an empty list, then something has explicitly set that no header
     * actions should appear.
     */
    @Override
    public void setSliceActions(List<SliceAction> actions) {
        mHeaderActions = actions;
        if (mRowContent != null) {
            updateEndItems();
        }
    }

    @Override
    public void setShowLastUpdated(boolean showLastUpdated) {
        super.setShowLastUpdated(showLastUpdated);
        if (mRowContent != null) {
            populateViews(true);
        }
    }

    @Override
    public void setAllowTwoLines(boolean allowTwoLines) {
        mAllowTwoLines = allowTwoLines;
        if (mRowContent != null) {
            populateViews(true);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int totalHeight = mRowContent != null
                ? mRowContent.getHeight(mSliceStyle, mViewPolicy) : 0;
        int rowHeight = getRowContentHeight();
        if (rowHeight != 0) {
            // Might be gone if we have range / progress but nothing else
            mRootView.setVisibility(View.VISIBLE);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(rowHeight, MeasureSpec.EXACTLY);
            measureChild(mRootView, widthMeasureSpec, heightMeasureSpec);
        } else {
            mRootView.setVisibility(View.GONE);
        }
        if (mRangeBar != null) {
            // If we're on a platform where SeekBar can't be stretched vertically, find out the
            // exact size it would like to be so we can honor that in onLayout.
            int rangeMeasureSpec = sCanSpecifyLargerRangeBarHeight
                    ? MeasureSpec.makeMeasureSpec(mIdealRangeHeight, MeasureSpec.EXACTLY)
                    : MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            measureChild(mRangeBar, widthMeasureSpec, rangeMeasureSpec);
            // Remember the measured height later for onLayout, since super.onMeasure will overwrite
            // it.
            mMeasuredRangeHeight = mRangeBar.getMeasuredHeight();
        }

        int totalHeightSpec = MeasureSpec.makeMeasureSpec(totalHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, totalHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int insets = mInsetStart + mInsetEnd;
        mRootView.layout(0, 0, mRootView.getMeasuredWidth() + insets, getRowContentHeight());
        if (mRangeBar != null) {
            // If we're on aa platform where SeekBar can't be stretched vertically, then
            // mMeasuredRangeHeight can (and probably will) be smaller than mIdealRangeHeight, so we
            // need to add some padding to make mRangeBar look like it's the larger size.
            int verticalPadding = (mIdealRangeHeight - mMeasuredRangeHeight) / 2;
            int top = getRowContentHeight() + verticalPadding;
            int bottom = top + mMeasuredRangeHeight;
            mRangeBar.layout(0, top, mRangeBar.getMeasuredWidth(), bottom);
        }
    }

    /**
     * This is called when RowView is being used as a component in a large template.
     */
    @Override
    public void setSliceItem(SliceContent content, boolean isHeader, int index,
            int rowCount, SliceView.OnSliceActionListener observer) {
        setSliceActionListener(observer);

        boolean isUpdate = false;
        if (content != null && mRowContent != null && mRowContent.isValid()) {
            // Might be same slice
            SliceStructure prevSs = mRowContent != null
                    ? new SliceStructure(mRowContent.getSliceItem()) : null;
            SliceStructure newSs = new SliceStructure(content.getSliceItem().getSlice());
            boolean sameStructure = prevSs != null && prevSs.equals(newSs);
            boolean sameSliceId = prevSs != null
                    && prevSs.getUri() != null && prevSs.getUri().equals(newSs.getUri());
            isUpdate = sameSliceId && sameStructure;
        }
        mShowActionSpinner = false;
        mIsHeader = isHeader;
        mRowContent = (RowContent) content;
        mRowIndex = index;
        populateViews(isUpdate);
    }

    private void populateViews(boolean isUpdate) {
        boolean skipSliderUpdate = isUpdate && mIsRangeSliding;
        if (!skipSliderUpdate) {
            resetViewState();
        }

        if (mRowContent.getLayoutDir() != -1) {
            setLayoutDirection(mRowContent.getLayoutDir());
        }
        if (mRowContent.isDefaultSeeMore()) {
            showSeeMore();
            return;
        }
        CharSequence contentDescr = mRowContent.getContentDescription();
        if (contentDescr != null) {
            mContent.setContentDescription(contentDescr);
        }
        mStartItem = mRowContent.getStartItem();
        boolean showStart = mStartItem != null && mRowIndex > 0;
        if (showStart) {
            showStart = addItem(mStartItem, mTintColor, true /* isStart */);
        }
        mStartContainer.setVisibility(showStart ? View.VISIBLE : View.GONE);

        final SliceItem titleItem = mRowContent.getTitleItem();
        if (titleItem != null) {
            mPrimaryText.setText(titleItem.getSanitizedText());
        }
        if (mSliceStyle != null) {
            mPrimaryText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mIsHeader
                    ? mSliceStyle.getHeaderTitleSize()
                    : mSliceStyle.getTitleSize());
            mPrimaryText.setTextColor(mSliceStyle.getTitleColor());
        }
        mPrimaryText.setVisibility(titleItem != null ? View.VISIBLE : View.GONE);

        addSubtitle(titleItem != null /* hasTitle */);

        SliceItem primaryAction = mRowContent.getPrimaryAction();
        if (primaryAction != null && primaryAction != mStartItem) {
            mRowAction = new SliceActionImpl(primaryAction);
            if (mRowAction.isToggle()) {
                // If primary action is a toggle, add it and we're done
                addAction(mRowAction, mTintColor, mEndContainer, false /* isStart */);
                // TODO: if start item is tappable, touch feedback should exclude it
                setViewClickable(mRootView, true);
                return;
            }
        }

        final SliceItem range = mRowContent.getRange();
        if (range != null) {
            if (mRowAction != null) {
                setViewClickable(mRootView, true);
            }
            if (!skipSliderUpdate) {
                determineRangeValues(range);
                addRange(range);
            } else {
                // Even if we're skipping the update, we should still update the range item
                mRangeItem = range;
            }
            return;
        }
        updateEndItems();
        updateActionSpinner();
    }

    private void updateEndItems() {
        if (mRowContent == null) {
            return;
        }
        mEndContainer.removeAllViews();

        // If we're here we can can show end items; check for top level actions first
        List endItems = mRowContent.getEndItems();
        if (mHeaderActions != null) {
            // Use these if we have them instead
            endItems = mHeaderActions;
        }
        // Add start item to end of row for the top row if end items are empty.
        if (mRowIndex == 0 && mStartItem != null && endItems.isEmpty()) {
            endItems.add(mStartItem);
        }

        // If we're here we might be able to show end items
        int endItemCount = 0;
        boolean firstItemIsADefaultToggle = false;
        boolean singleActionAtTheEnd = false;
        SliceItem endAction = null;
        for (int i = 0; i < endItems.size(); i++) {
            final SliceItem endItem = (endItems.get(i) instanceof SliceItem)
                    ? (SliceItem) endItems.get(i)
                    : ((SliceActionImpl) endItems.get(i)).getSliceItem();
            if (endItemCount < MAX_END_ITEMS) {
                if (addItem(endItem, mTintColor, false /* isStart */)) {
                    if (endAction == null && SliceQuery.find(endItem, FORMAT_ACTION) != null) {
                        endAction = endItem;
                    }
                    endItemCount++;
                    if (endItemCount == 1) {
                        firstItemIsADefaultToggle = !mToggles.isEmpty()
                                && SliceQuery.find(endItem.getSlice(), FORMAT_IMAGE) == null;
                        singleActionAtTheEnd = endItems.size() == 1
                                && SliceQuery.find(endItem, FORMAT_ACTION) != null;
                    }
                }
            }
        }
        mEndContainer.setVisibility(endItemCount > 0 ? VISIBLE : GONE);

        // If there is a row action and the first end item is a default toggle, or action divider
        // is set by presenter and a single action is at the end of the row, show the divider.
        mDivider.setVisibility(mRowAction != null && (firstItemIsADefaultToggle
                || (mRowContent.hasActionDivider() && singleActionAtTheEnd))
                ? View.VISIBLE : View.GONE);
        boolean hasStartAction = mStartItem != null
                && SliceQuery.find(mStartItem, FORMAT_ACTION) != null;
        boolean hasEndItemAction = endAction != null;

        boolean endAndRowActionTheSame = false;
        if (mRowAction != null) {
            setViewClickable(mRootView, true);
        } else if (hasEndItemAction != hasStartAction && (endItemCount == 1 || hasStartAction)) {
            // This row only has 1 action in start or end position; make whole row clickable for it
            endAndRowActionTheSame = true;
            if (!mToggles.isEmpty()) {
                mRowAction = mToggles.keySet().iterator().next();
            } else if (!mActions.isEmpty() && mActions.size() == 1) {
                mRowAction = mActions.valueAt(0).getAction();
            }
            setViewClickable(mRootView, true);
        }

        if (mRowAction != null && !endAndRowActionTheSame
                && mLoadingActions.contains(mRowAction.getSliceItem())) {
            mShowActionSpinner = true;
        }
    }

    @Override
    public void setLastUpdated(long lastUpdated) {
        super.setLastUpdated(lastUpdated);
        if (mRowContent != null) {
            addSubtitle(mRowContent.getTitleItem() != null
                    && TextUtils.isEmpty(mRowContent.getTitleItem().getSanitizedText()));
        }
    }

    private void addSubtitle(boolean hasTitle) {
        if (mRowContent == null) {
            return;
        }
        final SliceItem subtitleItem = getMode() == MODE_SMALL
                ? mRowContent.getSummaryItem()
                : mRowContent.getSubtitleItem();
        CharSequence subtitleTimeString = null;
        if (mShowLastUpdated && mLastUpdated != -1) {
            CharSequence relativeTime = getRelativeTimeString(mLastUpdated);
            if (relativeTime != null) {
                subtitleTimeString =
                        getResources().getString(R.string.abc_slice_updated, relativeTime);
            }
        }
        CharSequence subtitle = subtitleItem != null ? subtitleItem.getSanitizedText() : null;
        boolean subtitleExists = !TextUtils.isEmpty(subtitle)
                        || (subtitleItem != null && subtitleItem.hasHint(HINT_PARTIAL));
        if (subtitleExists) {
            mSecondaryText.setText(subtitle);
            if (mSliceStyle != null) {
                mSecondaryText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mIsHeader
                        ? mSliceStyle.getHeaderSubtitleSize()
                        : mSliceStyle.getSubtitleSize());
                mSecondaryText.setTextColor(mSliceStyle.getSubtitleColor());
                int verticalPadding = mIsHeader
                        ? mSliceStyle.getVerticalHeaderTextPadding()
                        : mSliceStyle.getVerticalTextPadding();
                mSecondaryText.setPadding(0, verticalPadding, 0, 0);
            }
        }
        if (subtitleTimeString != null) {
            if (!TextUtils.isEmpty(subtitle)) {
                subtitleTimeString = " \u00B7 " + subtitleTimeString;
            }
            SpannableString sp = new SpannableString(subtitleTimeString);
            sp.setSpan(new StyleSpan(Typeface.ITALIC), 0, subtitleTimeString.length(), 0);
            mLastUpdatedText.setText(sp);
            if (mSliceStyle != null) {
                mLastUpdatedText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mIsHeader
                        ? mSliceStyle.getHeaderSubtitleSize() : mSliceStyle.getSubtitleSize());
                mLastUpdatedText.setTextColor(mSliceStyle.getSubtitleColor());
            }
        }
        mLastUpdatedText.setVisibility(TextUtils.isEmpty(subtitleTimeString) ? GONE : VISIBLE);
        mSecondaryText.setVisibility(subtitleExists ? VISIBLE : GONE);

        // If this is non-header or something that can have 2 lines in the header (e.g. permission
        // slice) then allow 2 lines if there's only a subtitle and now timestring.
        boolean canHaveMultiLines = mRowIndex > 0 || mAllowTwoLines;
        int maxLines = canHaveMultiLines && !hasTitle && subtitleExists
                && TextUtils.isEmpty(subtitleTimeString)
                ? 2 : 1;
        mSecondaryText.setSingleLine(maxLines == 1);
        mSecondaryText.setMaxLines(maxLines);

        // TODO: Consider refactoring layout structure to avoid this
        // Need to request a layout to update the weights for these views when RV recycles them
        mSecondaryText.requestLayout();
        mLastUpdatedText.requestLayout();
    }

    private CharSequence getRelativeTimeString(long time) {
        long difference = System.currentTimeMillis() - time;
        if (difference > DateUtils.YEAR_IN_MILLIS) {
            int years = (int) (difference / DateUtils.YEAR_IN_MILLIS);
            return getResources().getQuantityString(
                    R.plurals.abc_slice_duration_years, years, years);
        } else if (difference > DateUtils.DAY_IN_MILLIS) {
            int days = (int) (difference / DateUtils.DAY_IN_MILLIS);
            return getResources().getQuantityString(R.plurals.abc_slice_duration_days, days, days);
        } else if (difference > DateUtils.MINUTE_IN_MILLIS) {
            int mins = (int) (difference / DateUtils.MINUTE_IN_MILLIS);
            return getResources().getQuantityString(R.plurals.abc_slice_duration_min, mins, mins);
        } else {
            return null;
        }
    }

    private void determineRangeValues(SliceItem rangeItem) {
        if (rangeItem == null) {
            mRangeMinValue = 0;
            mRangeValue = 0;
            return;
        }
        mRangeItem = rangeItem;

        SliceItem min = SliceQuery.findSubtype(mRangeItem, FORMAT_INT, SUBTYPE_MIN);
        int minValue = 0;
        if (min != null) {
            minValue = min.getInt();
        }
        mRangeMinValue = minValue;

        SliceItem progress = SliceQuery.findSubtype(mRangeItem, FORMAT_INT, SUBTYPE_VALUE);
        if (progress != null) {
            mRangeValue = progress.getInt() - minValue;
        }
    }

    private void addRange(final SliceItem range) {
        if (mHandler == null) {
            mHandler = new Handler();
        }

        final boolean isSeekBar = FORMAT_ACTION.equals(range.getFormat());
        final ProgressBar progressBar = isSeekBar
                ? new SeekBar(getContext())
                : new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        Drawable progressDrawable = DrawableCompat.wrap(progressBar.getProgressDrawable());
        if (mTintColor != -1 && progressDrawable != null) {
            DrawableCompat.setTint(progressDrawable, mTintColor);
            progressBar.setProgressDrawable(progressDrawable);
        }
        SliceItem max = SliceQuery.findSubtype(range, FORMAT_INT, SUBTYPE_MAX);
        if (max != null) {
            progressBar.setMax(max.getInt() - mRangeMinValue);
        }
        progressBar.setProgress(mRangeValue);
        progressBar.setVisibility(View.VISIBLE);
        addView(progressBar);
        mRangeBar = progressBar;
        if (isSeekBar) {
            SliceItem thumb = mRowContent.getInputRangeThumb();
            SeekBar seekBar = (SeekBar) mRangeBar;
            if (thumb != null && thumb.getIcon() != null) {
                Drawable d = thumb.getIcon().loadDrawable(getContext());
                if (d != null) {
                    seekBar.setThumb(d);
                }
            }
            Drawable thumbDrawable = DrawableCompat.wrap(seekBar.getThumb());
            if (mTintColor != -1 && thumbDrawable != null) {
                DrawableCompat.setTint(thumbDrawable, mTintColor);
                seekBar.setThumb(thumbDrawable);
            }
            seekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        }
        updateRangePadding();
    }

    private void updateRangePadding() {
        if (mRangeBar != null) {
            int thumbSize = mRangeBar instanceof SeekBar
                    ? ((SeekBar) mRangeBar).getThumb().getIntrinsicWidth() : 0;
            int topInsetPadding = mRowContent != null
                    ? mRowContent.getLineCount() > 0 ? 0 : mInsetTop
                    : mInsetTop;
            // Check if the app defined inset is large enough for the drawable
            if (thumbSize != 0 && mInsetStart >= thumbSize / 2 && mInsetEnd >= thumbSize / 2) {
                // If row content has text then the top inset gets applied to mContent layout
                // not the range layout.
                mRangeBar.setPadding(mInsetStart, topInsetPadding, mInsetEnd, mInsetBottom);
            } else {
                // App defined inset not bug enough; we need to apply one
                int thumbPadding = thumbSize != 0 ? thumbSize / 2 : 0;
                mRangeBar.setPadding(mInsetStart + thumbPadding, topInsetPadding,
                        mInsetEnd + thumbPadding, mInsetBottom);
            }
        }
    }

    void sendSliderValue() {
        if (mRangeItem != null) {
            try {
                mLastSentRangeUpdate = System.currentTimeMillis();
                mRangeItem.fireAction(getContext(),
                        new Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                                .putExtra(EXTRA_RANGE_VALUE, mRangeValue));
                if (mObserver != null) {
                    EventInfo info = new EventInfo(getMode(), ACTION_TYPE_SLIDER, ROW_TYPE_SLIDER,
                            mRowIndex);
                    info.state = mRangeValue;
                    mObserver.onSliceAction(info, mRangeItem);
                }
            } catch (CanceledException e) {
                Log.e(TAG, "PendingIntent for slice cannot be sent", e);
            }
        }
    }

    /**
     * Add an action view to the container.
     */
    private void addAction(final SliceActionImpl actionContent, int color, ViewGroup container,
                           boolean isStart) {
        SliceActionView sav = new SliceActionView(getContext());
        container.addView(sav);
        if (container.getVisibility() == GONE) {
            container.setVisibility(VISIBLE);
        }

        boolean isToggle = actionContent.isToggle();
        int actionType = isToggle ? ACTION_TYPE_TOGGLE : ACTION_TYPE_BUTTON;
        int rowType = isToggle ? ROW_TYPE_TOGGLE : ROW_TYPE_LIST;
        EventInfo info = new EventInfo(getMode(), actionType, rowType, mRowIndex);
        if (isStart) {
            info.setPosition(EventInfo.POSITION_START, 0, 1);
        }
        sav.setAction(actionContent, info, mObserver, color, mLoadingListener);
        if (mLoadingActions.contains(actionContent.getSliceItem())) {
            sav.setLoading(true);
        }
        if (isToggle) {
            mToggles.put(actionContent, sav);
        } else {
            mActions.put(actionContent, sav);
        }
    }

    /**
     * Adds simple items to a container. Simple items include actions with icons, images, or
     * timestamps.
     */
    private boolean addItem(SliceItem sliceItem, int color, boolean isStart) {
        IconCompat icon = null;
        int imageMode = 0;
        SliceItem timeStamp = null;
        ViewGroup container = isStart ? mStartContainer : mEndContainer;
        if (FORMAT_SLICE.equals(sliceItem.getFormat())
                || FORMAT_ACTION.equals(sliceItem.getFormat())) {
            if (sliceItem.hasHint(HINT_SHORTCUT)) {
                addAction(new SliceActionImpl(sliceItem), color, container, isStart);
                return true;
            } else {
                if (sliceItem.getSlice().getItems().size() == 0) {
                    return false;
                }
                sliceItem = sliceItem.getSlice().getItems().get(0);
            }
        }

        if (FORMAT_IMAGE.equals(sliceItem.getFormat())) {
            icon = sliceItem.getIcon();
            imageMode = sliceItem.hasHint(HINT_NO_TINT) ? SMALL_IMAGE : ICON_IMAGE;
        } else if (FORMAT_LONG.equals(sliceItem.getFormat())) {
            timeStamp = sliceItem;
        }
        View addedView = null;
        if (icon != null) {
            boolean isIcon = imageMode == ICON_IMAGE;
            ImageView iv = new ImageView(getContext());
            iv.setImageDrawable(icon.loadDrawable(getContext()));
            if (isIcon && color != -1) {
                iv.setColorFilter(color);
            }
            container.addView(iv);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
            lp.width = mImageSize;
            lp.height = mImageSize;
            iv.setLayoutParams(lp);
            int p = isIcon ? mIconSize / 2 : 0;
            iv.setPadding(p, p, p, p);
            addedView = iv;
        } else if (timeStamp != null) {
            TextView tv = new TextView(getContext());
            tv.setText(SliceViewUtil.getTimestampString(getContext(), sliceItem.getLong()));
            if (mSliceStyle != null) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSliceStyle.getSubtitleSize());
                tv.setTextColor(mSliceStyle.getSubtitleColor());
            }
            container.addView(tv);
            addedView = tv;
        }
        return addedView != null;
    }

    private void showSeeMore() {
        final Button b = (Button) LayoutInflater.from(getContext()).inflate(
                R.layout.abc_slice_row_show_more, this, false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (mObserver != null) {
                        EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_SEE_MORE,
                                EventInfo.ROW_TYPE_LIST, mRowIndex);
                        mObserver.onSliceAction(info, mRowContent.getSliceItem());
                    }
                    mShowActionSpinner =
                            mRowContent.getSliceItem().fireActionInternal(getContext(), null);
                    if (mShowActionSpinner) {
                        if (mLoadingListener != null) {
                            mLoadingListener.onSliceActionLoading(mRowContent.getSliceItem(),
                                    mRowIndex);
                        }
                        mLoadingActions.add(mRowContent.getSliceItem());
                        b.setVisibility(GONE);
                    }
                    updateActionSpinner();
                } catch (CanceledException e) {
                    Log.e(TAG, "PendingIntent for slice cannot be sent", e);
                }
            }
        });
        if (mTintColor != -1) {
            b.setTextColor(mTintColor);
        }
        mSeeMoreView = b;
        mRootView.addView(mSeeMoreView);
        if (mLoadingActions.contains(mRowContent.getSliceItem())) {
            mShowActionSpinner = true;
            b.setVisibility(GONE);
            updateActionSpinner();
        }
    }

    void updateActionSpinner() {
        mActionSpinner.setVisibility(mShowActionSpinner ? VISIBLE : GONE);
    }

    @Override
    public void setLoadingActions(Set<SliceItem> actions) {
        if (actions == null) {
            mLoadingActions.clear();
            mShowActionSpinner = false;
        } else {
            mLoadingActions = actions;
        }
        updateEndItems();
        updateActionSpinner();
    }

    @Override
    public void onClick(View view) {
        if (mRowAction == null || mRowAction.getActionItem() == null) {
            return;
        }
        SliceActionView sav = mRowAction.isToggle()
                ? mToggles.get(mRowAction)
                : mActions.get(mRowAction);
        if (sav != null && !(view instanceof SliceActionView)) {
            // Row might have a single action item set on it, in that case we activate that item
            // and it will handle displaying any loading states / updating state for toggles
            sav.sendAction();
        } else {
            if (mRowIndex == 0) {
                // Header clicks are a little weird and SliceView needs to know about them to
                // maintain loading state; this is hooked up in LargeSliceAdapter -- it will call
                // through to SliceView parent which has the info to perform the click.
                performClick();
            } else {
                try {
                    mShowActionSpinner =
                            mRowAction.getActionItem().fireActionInternal(getContext(), null);
                    if (mShowActionSpinner && mLoadingListener != null) {
                        mLoadingListener.onSliceActionLoading(mRowAction.getSliceItem(), mRowIndex);
                        mLoadingActions.add(mRowAction.getSliceItem());
                    }
                    updateActionSpinner();
                } catch (CanceledException e) {
                    Log.e(TAG, "PendingIntent for slice cannot be sent", e);
                }
            }
        }
    }

    private void setViewClickable(View layout, boolean isClickable) {
        layout.setOnClickListener(isClickable ? this : null);
        layout.setBackground(isClickable
                ? SliceViewUtil.getDrawable(getContext(), android.R.attr.selectableItemBackground)
                : null);
        layout.setClickable(isClickable);
    }

    @Override
    public void resetView() {
        mRowContent = null;
        mLoadingActions.clear();
        resetViewState();
    }

    private void resetViewState() {
        mRootView.setVisibility(VISIBLE);
        setLayoutDirection(View.LAYOUT_DIRECTION_INHERIT);
        setViewClickable(mRootView, false);
        setViewClickable(mContent, false);
        mStartContainer.removeAllViews();
        mEndContainer.removeAllViews();
        mEndContainer.setVisibility(GONE);
        mPrimaryText.setText(null);
        mSecondaryText.setText(null);
        mLastUpdatedText.setText(null);
        mLastUpdatedText.setVisibility(GONE);
        mToggles.clear();
        mActions.clear();
        mRowAction = null;
        mStartItem = null;
        mDivider.setVisibility(GONE);
        if (mSeeMoreView != null) {
            mRootView.removeView(mSeeMoreView);
            mSeeMoreView = null;
        }
        mIsRangeSliding = false;
        mRangeHasPendingUpdate = false;
        mRangeItem = null;
        mRangeMinValue = 0;
        mRangeValue = 0;
        mLastSentRangeUpdate = 0;
        mHandler = null;
        if (mRangeBar != null) {
            removeView(mRangeBar);
            mRangeBar = null;
        }
        mActionSpinner.setVisibility(GONE);
    }

    Runnable mRangeUpdater = new Runnable() {
        @Override
        public void run() {
            sendSliderValue();
            mRangeUpdaterRunning = false;
        }
    };

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mRangeValue = progress + mRangeMinValue;
                    final long now = System.currentTimeMillis();
                    if (mLastSentRangeUpdate != 0 && now - mLastSentRangeUpdate > SLIDER_INTERVAL) {
                        mRangeUpdaterRunning = false;
                        mHandler.removeCallbacks(mRangeUpdater);
                        sendSliderValue();
                    } else if (!mRangeUpdaterRunning) {
                        mRangeUpdaterRunning = true;
                        mHandler.postDelayed(mRangeUpdater, SLIDER_INTERVAL);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mIsRangeSliding = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mIsRangeSliding = false;
                    if (mRangeUpdaterRunning || mRangeHasPendingUpdate) {
                        mRangeUpdaterRunning = false;
                        mRangeHasPendingUpdate = false;
                        mHandler.removeCallbacks(mRangeUpdater);
                        mRangeValue = seekBar.getProgress() + mRangeMinValue;
                        sendSliderValue();
                    }
                }
            };
}
