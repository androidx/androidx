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
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.SUBTYPE_MAX;
import static android.app.slice.Slice.SUBTYPE_MILLIS;
import static android.app.slice.Slice.SUBTYPE_TOGGLE;
import static android.app.slice.Slice.SUBTYPE_VALUE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.slice.Slice.EXTRA_SELECTION;
import static androidx.slice.Slice.SUBTYPE_RANGE_MODE;
import static androidx.slice.core.SliceHints.HINT_RAW;
import static androidx.slice.core.SliceHints.HINT_SELECTION_OPTION;
import static androidx.slice.core.SliceHints.INDETERMINATE_RANGE;
import static androidx.slice.core.SliceHints.STAR_RATING;
import static androidx.slice.core.SliceHints.SUBTYPE_DATE_PICKER;
import static androidx.slice.core.SliceHints.SUBTYPE_MIN;
import static androidx.slice.core.SliceHints.SUBTYPE_SELECTION_OPTION_KEY;
import static androidx.slice.core.SliceHints.SUBTYPE_SELECTION_OPTION_VALUE;
import static androidx.slice.core.SliceHints.SUBTYPE_TIME_PICKER;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_BUTTON;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_DATE_PICK;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_SELECTION;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_SLIDER;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_TIME_PICK;
import static androidx.slice.widget.EventInfo.ACTION_TYPE_TOGGLE;
import static androidx.slice.widget.EventInfo.ROW_TYPE_DATE_PICK;
import static androidx.slice.widget.EventInfo.ROW_TYPE_LIST;
import static androidx.slice.widget.EventInfo.ROW_TYPE_SELECTION;
import static androidx.slice.widget.EventInfo.ROW_TYPE_SLIDER;
import static androidx.slice.widget.EventInfo.ROW_TYPE_TIME_PICK;
import static androidx.slice.widget.EventInfo.ROW_TYPE_TOGGLE;
import static androidx.slice.widget.SliceView.MODE_SMALL;

import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Handler;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.view.ViewCompat;
import androidx.slice.CornerDrawable;
import androidx.slice.SliceItem;
import androidx.slice.SliceStructure;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Row item is in small template format and can be used to construct list items for use
 * with {@link TemplateView}.
 */
@RequiresApi(19)
public class RowView extends SliceChildView implements View.OnClickListener,
        AdapterView.OnItemSelectedListener {

    private static final String TAG = "RowView";

    private static final int HEIGHT_UNBOUND = -1;

    // The number of items that fit on the right hand side of a small slice
    // TODO: this should be based on available width
    private static final int MAX_END_ITEMS = 3;
    // How frequently (ms) intent can be sent in response to slider moving.
    private static final int SLIDER_INTERVAL = 200;

    // The index for star rating's layer drawable's foreground
    private static final int STAR_COLOR_INDEX = 2;

    // On versions before M, SeekBar won't render properly if stretched taller than the default
    // size.
    private static final boolean sCanSpecifyLargerRangeBarHeight =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

    private final LinearLayout mRootView;
    private final LinearLayout mStartContainer;
    private final LinearLayout mContent;
    private final LinearLayout mSubContent;
    private final TextView mPrimaryText;
    private final TextView mSecondaryText;
    private final TextView mLastUpdatedText;
    private final View mBottomDivider;
    private final View mActionDivider;
    private final ArrayMap<SliceActionImpl, SliceActionView> mToggles = new ArrayMap<>();
    private final ArrayMap<SliceActionImpl, SliceActionView> mActions = new ArrayMap<>();
    private final LinearLayout mEndContainer;
    private View mSeeMoreView;
    private View mRangeBar;
    private boolean mIsStarRating;
    private final ProgressBar mActionSpinner;
    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    protected Set<SliceItem> mLoadingActions = new HashSet<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mShowActionSpinner;
    private Spinner mSelectionSpinner;

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
    // TODO: mRangeValue is in 0..(mRangeMaxValue-mRangeMinValue) at initialization, and in
    //       mRangeMinValue..mRangeMaxValue after user interaction. As far as I know, this doesn't
    //       cause any incorrect behavior, but it is confusing and error-prone.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mRangeValue;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mRangeMinValue;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mRangeMaxValue;
    private SliceItem mRangeItem;
    private SliceItem mSelectionItem;
    private ArrayList<String> mSelectionOptionKeys;
    private ArrayList<CharSequence> mSelectionOptionValues;

    private int mImageSize;
    private int mIconSize;
    // How big mRangeBar wants to be.
    private int mMeasuredRangeHeight;

    public RowView(@NonNull Context context) {
        super(context);
        mIconSize = getContext().getResources().getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mImageSize = getContext().getResources().getDimensionPixelSize(
                R.dimen.abc_slice_small_image_size);
        mRootView = (LinearLayout) LayoutInflater.from(context).inflate(
                R.layout.abc_slice_small_template, this, false);
        addView(mRootView);

        mStartContainer = findViewById(R.id.icon_frame);
        mContent = findViewById(android.R.id.content);
        mSubContent = findViewById(R.id.subcontent);
        mPrimaryText = findViewById(android.R.id.title);
        mSecondaryText = findViewById(android.R.id.summary);
        mLastUpdatedText = findViewById(R.id.last_updated);
        mBottomDivider = findViewById(R.id.bottom_divider);
        mActionDivider = findViewById(R.id.action_divider);
        mActionSpinner = findViewById(R.id.action_sent_indicator);
        SliceViewUtil.tintIndeterminateProgressBar(getContext(), mActionSpinner);
        mEndContainer = findViewById(android.R.id.widget_frame);
        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
        ViewCompat.setImportantForAccessibility(
                mContent, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setStyle(SliceStyle styles, RowStyle rowStyle) {
        super.setStyle(styles, rowStyle);
        applyRowStyle();
    }

    private void applyRowStyle() {
        if (mSliceStyle == null || mRowStyle == null) {
            return;
        }

        setViewSidePaddings(mStartContainer,
                mRowStyle.getTitleItemStartPadding(), mRowStyle.getTitleItemEndPadding());
        setViewSidePaddings(mContent,
                mRowStyle.getContentStartPadding(), mRowStyle.getContentEndPadding());
        setViewSidePaddings(mPrimaryText,
                mRowStyle.getTitleStartPadding(), mRowStyle.getTitleEndPadding());
        setViewSidePaddings(mSubContent,
                mRowStyle.getSubContentStartPadding(), mRowStyle.getSubContentEndPadding());
        setViewSidePaddings(mEndContainer,
                mRowStyle.getEndItemStartPadding(), mRowStyle.getEndItemEndPadding());
        setViewSideMargins(mBottomDivider,
                mRowStyle.getBottomDividerStartPadding(), mRowStyle.getBottomDividerEndPadding());
        setViewHeight(mActionDivider, mRowStyle.getActionDividerHeight());
        if (mRowStyle.getTintColor() != -1) {
            setTint(mRowStyle.getTintColor());
        }
    }

    private void setViewSidePaddings(View v, int start, int end) {
        final boolean isNoPaddingSet = start < 0 && end < 0;
        if (v == null || isNoPaddingSet) {
            return;
        }

        v.setPaddingRelative(
                start >= 0 ? start : v.getPaddingStart(),
                v.getPaddingTop(),
                end >= 0 ? end : v.getPaddingEnd(),
                v.getPaddingBottom());
    }

    private void setViewSideMargins(View v, int start, int end) {
        final boolean isNoMarginSet = start < 0 && end < 0;
        if (v == null || isNoMarginSet) {
            return;
        }

        final MarginLayoutParams params = (MarginLayoutParams) v.getLayoutParams();
        if (start >= 0) {
            params.setMarginStart(start);
        }
        if (end >= 0) {
            params.setMarginEnd(end);
        }
        v.setLayoutParams(params);
    }

    private void setViewHeight(View v, int height) {
        if (v != null && height >= 0) {
            final ViewGroup.LayoutParams params = v.getLayoutParams();
            params.height = height;
            v.setLayoutParams(params);
        }
    }

    private void setViewWidth(View v, int width) {
        if (v != null && width >= 0) {
            final ViewGroup.LayoutParams params = v.getLayoutParams();
            params.width = width;
            v.setLayoutParams(params);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setInsets(int l, int t, int r, int b) {
        super.setInsets(l, t, r, b);
        setPadding(l, t, r, b);
    }

    /**
     * Allows subclasses to access the SliceItem that can be used to fire an action.
     */
    @Nullable
    protected SliceItem getPrimaryActionItem() {
        return (mRowContent != null) ? mRowContent.getPrimaryAction() : null;
    }

    /**
     * Allows subclasses to access the key associated with the primary action of the row.
     */
    @Nullable
    protected String getPrimaryActionKey() {
        if (mRowContent != null) {
            SliceItem primaryAction = mRowContent.getPrimaryAction();
            if (primaryAction != null && primaryAction != mStartItem) {
                mRowAction = new SliceActionImpl(primaryAction);
                return mRowAction.getKey();
            }
        }
        return null;
    }

    /**
     * A list of keys from the SliceAction end items that can be used by subclasses for custom
     * rendering.
     */
    @NonNull
    protected List<String> getEndItemKeys() {
        List<String> endItemKeys = new ArrayList<>();
        if (mRowContent != null) {
            // If we're here we can can show end items; check for top level actions first
            List<SliceItem> endItems = mRowContent.getEndItems();

            // If we're here we might be able to show end items
            int endItemCount = 0;
            for (int i = 0; i < endItems.size(); i++) {
                if (endItemCount < MAX_END_ITEMS) {
                    SliceActionImpl endItemAction = new SliceActionImpl(endItems.get(i));
                    if (endItemAction.getKey() != null) {
                        endItemKeys.add(endItemAction.getKey());
                    }
                }
            }
        }
        return endItemKeys;
    }

    /**
     * @return height row content (i.e. title, subtitle) without the height of the range element.
     */
    private int getRowContentHeight() {
        int rowHeight = mRowContent.getHeight(mSliceStyle, mViewPolicy);
        if (mRangeBar != null && mStartItem == null) {
            rowHeight -= mSliceStyle.getRowRangeHeight();
        }
        if (mSelectionSpinner != null) {
            rowHeight -= mSliceStyle.getRowSelectionHeight();
        }
        return rowHeight;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setSliceActions(List<SliceAction> actions) {
        mHeaderActions = actions;
        if (mRowContent != null) {
            updateEndItems();
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setShowLastUpdated(boolean showLastUpdated) {
        super.setShowLastUpdated(showLastUpdated);
        if (mRowContent != null) {
            populateViews(true);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setAllowTwoLines(boolean allowTwoLines) {
        mAllowTwoLines = allowTwoLines;
        if (mRowContent != null) {
            populateViews(true);
        }
    }

    private void measureChildWithExactHeight(View child, int widthMeasureSpec, int childHeight) {
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeight + mInsetTop + mInsetBottom,
                MeasureSpec.EXACTLY);
        measureChild(child, widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int childWidth = 0;

        int rowHeight = getRowContentHeight();
        if (rowHeight != 0) {
            // Might be gone if we have range / progress but nothing else
            mRootView.setVisibility(View.VISIBLE);
            measureChildWithExactHeight(mRootView, widthMeasureSpec, rowHeight);

            childWidth = mRootView.getMeasuredWidth();
        } else {
            mRootView.setVisibility(View.GONE);
        }
        if (mRangeBar != null && mStartItem == null) {
            // If we're on a platform where SeekBar can't be stretched vertically, find out the
            // exact size it would like to be so we can honor that in onLayout.
            if (sCanSpecifyLargerRangeBarHeight) {
                measureChildWithExactHeight(mRangeBar, widthMeasureSpec,
                        mSliceStyle.getRowRangeHeight());
            } else {
                measureChild(mRangeBar, widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            }
            // Remember the measured height later for onLayout, since super.onMeasure will overwrite
            // it.
            mMeasuredRangeHeight = mRangeBar.getMeasuredHeight();
            childWidth = Math.max(childWidth, mRangeBar.getMeasuredWidth());
        } else if (mSelectionSpinner != null) {
            measureChildWithExactHeight(mSelectionSpinner, widthMeasureSpec,
                    mSliceStyle.getRowSelectionHeight());
            childWidth = Math.max(childWidth, mSelectionSpinner.getMeasuredWidth());
        }

        childWidth = Math.max(childWidth + mInsetStart + mInsetEnd, getSuggestedMinimumWidth());
        int rowContentHeight = mRowContent != null ? mRowContent.getHeight(mSliceStyle,
                mViewPolicy) : 0;
        setMeasuredDimension(resolveSizeAndState(childWidth, widthMeasureSpec, 0),
                rowContentHeight + mInsetTop + mInsetBottom);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int leftPadding = getPaddingLeft();
        mRootView.layout(leftPadding, mInsetTop, mRootView.getMeasuredWidth() + leftPadding,
                getRowContentHeight() + mInsetTop);
        if (mRangeBar != null && mStartItem == null) {
            // If we're on a platform where SeekBar can't be stretched vertically, then
            // mMeasuredRangeHeight can (and probably will) be smaller than the ideal height, so we
            // need to add some padding to make mRangeBar look like it's the larger size.
            int verticalPadding = (mSliceStyle.getRowRangeHeight() - mMeasuredRangeHeight) / 2;
            int top = (getRowContentHeight() + verticalPadding + mInsetTop);
            int bottom = top + mMeasuredRangeHeight;
            mRangeBar.layout(leftPadding, top, mRangeBar.getMeasuredWidth() + leftPadding, bottom);
        } else if (mSelectionSpinner != null) {
            int top = getRowContentHeight() + mInsetTop;
            int bottom = top + mSelectionSpinner.getMeasuredHeight();
            mSelectionSpinner.layout(leftPadding, top,
                    mSelectionSpinner.getMeasuredWidth() + leftPadding, bottom);
        }
    }

    /**
     * This is called when RowView is being used as a component in a large template.
     */
    @Override
    public void setSliceItem(@Nullable SliceContent content, boolean isHeader, int index,
            int rowCount, @Nullable SliceView.OnSliceActionListener observer) {
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
        boolean showStart = mStartItem != null && (!mRowContent.getIsHeader()
                || mRowContent.hasTitleItems());
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
            mPrimaryText.setTextColor(mRowStyle.getTitleColor());
        }
        mPrimaryText.setVisibility(titleItem != null ? View.VISIBLE : View.GONE);

        addSubtitle(titleItem != null /* hasTitle */);

        mBottomDivider.setVisibility(mRowContent.hasBottomDivider() ? View.VISIBLE : View.GONE);

        SliceItem primaryAction = mRowContent.getPrimaryAction();
        if (primaryAction != null && primaryAction != mStartItem) {
            mRowAction = new SliceActionImpl(primaryAction);
            if (mRowAction.getSubtype() != null) {
                switch (mRowAction.getSubtype()) {
                    case SUBTYPE_TOGGLE:
                        // If primary action is a toggle, add it and we're done
                        addAction(mRowAction, mTintColor, mEndContainer, false /* isStart */);
                        // TODO: if start item is tappable, touch feedback should exclude it
                        setViewClickable(mRootView, true);
                        return;
                    case SUBTYPE_DATE_PICKER:
                        setViewClickable(mRootView, true);
                        return;
                    case SUBTYPE_TIME_PICKER:
                        setViewClickable(mRootView, true);
                        return;
                    default:
                }
            }
        }

        final SliceItem range = mRowContent.getRange();
        if (range != null) {
            if (mRowAction != null) {
                setViewClickable(mRootView, true);
            }
            mRangeItem = range;
            SliceItem mode = SliceQuery.findSubtype(mRangeItem, FORMAT_INT, SUBTYPE_RANGE_MODE);
            if (mode != null) {
                mIsStarRating = mode.getInt() == STAR_RATING;
            }
            if (!skipSliderUpdate) {
                initRangeBar();
                addRangeView();
            }
            // if mStartItem is not null, then RowView should update end items.
            if (mStartItem == null) {
                return;
            }
        }

        final SliceItem selection = mRowContent.getSelection();
        if (selection != null) {
            mSelectionItem = selection;
            addSelection(selection);
            return;
        }

        updateEndItems();
        updateActionSpinner();
    }

    @SuppressWarnings("unchecked")
    private void updateEndItems() {
        if (mRowContent == null || (mRowContent.getRange() != null && mStartItem == null)) {
            return;
        }
        mEndContainer.removeAllViews();

        // If we're here we can can show end items; check for top level actions first
        List endItems = mRowContent.getEndItems();
        if (mHeaderActions != null) {
            // Use these if we have them instead
            endItems = mHeaderActions;
        }
        // Add start item to end of row for the top row if end items are empty and presenter
        // doesn't show title items.
        if (mRowContent.getIsHeader() && mStartItem != null && endItems.isEmpty()
                && !mRowContent.hasTitleItems()) {
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
        mActionDivider.setVisibility(mRowAction != null && (firstItemIsADefaultToggle
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

        ViewCompat.setImportantForAccessibility(mRootView, mRootView.isClickable()
                ? ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                : ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        );
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Override
    public void setLastUpdated(long lastUpdated) {
        super.setLastUpdated(lastUpdated);
        if (mRowContent != null) {
            addSubtitle(mRowContent.getTitleItem() != null
                    && TextUtils.isEmpty(mRowContent.getTitleItem().getSanitizedText()));
        }
    }

    private void addSubtitle(boolean hasTitle) {
        if (mRowContent == null || (mRowContent.getRange() != null && mStartItem != null)) {
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
                mSecondaryText.setTextColor(mRowStyle.getSubtitleColor());
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
                mLastUpdatedText.setTextColor(mRowStyle.getSubtitleColor());
            }
        }
        mLastUpdatedText.setVisibility(TextUtils.isEmpty(subtitleTimeString) ? GONE : VISIBLE);
        mSecondaryText.setVisibility(subtitleExists ? VISIBLE : GONE);

        // If this is non-header or something that can have 2 lines in the header (e.g. permission
        // slice) then allow 2 lines if there's only a subtitle and now timestring.
        boolean canHaveMultiLines = !mRowContent.getIsHeader() || mAllowTwoLines;
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

    private void initRangeBar() {
        SliceItem min = SliceQuery.findSubtype(mRangeItem, FORMAT_INT, SUBTYPE_MIN);
        int minValue = 0;
        if (min != null) {
            minValue = min.getInt();
        }
        mRangeMinValue = minValue;

        SliceItem max = SliceQuery.findSubtype(mRangeItem, FORMAT_INT, SUBTYPE_MAX);
        int maxValue = mIsStarRating ? 5 : 100;
        if (max != null) {
            maxValue = max.getInt();
        }
        mRangeMaxValue = maxValue;

        SliceItem progress = SliceQuery.findSubtype(mRangeItem, FORMAT_INT, SUBTYPE_VALUE);
        int progressValue = 0;
        if (progress != null) {
            progressValue = progress.getInt() - minValue;
        }
        mRangeValue = progressValue;
    }

    @SuppressWarnings("deprecation")
    private void addRangeView() {
        if (mHandler == null) {
            mHandler = new Handler();
        }
        if (mIsStarRating) {
            addRatingBarView();
            return;
        }

        // add either input slider or progress bar view
        SliceItem style = SliceQuery.findSubtype(mRangeItem, FORMAT_INT, SUBTYPE_RANGE_MODE);
        final boolean isIndeterminate = style != null && style.getInt() == INDETERMINATE_RANGE;
        final boolean isSeekBar = FORMAT_ACTION.equals(mRangeItem.getFormat());
        final boolean renderInNewLine = mStartItem == null;
        ProgressBar progressBar;
        if (isSeekBar) {
            // If action and not star rating, must be input range.
            if (renderInNewLine) {
                progressBar = new SeekBar(getContext());
            } else {
                progressBar = (SeekBar) LayoutInflater.from(getContext()).inflate(
                        R.layout.abc_slice_seekbar_view, this, false);
                if (mRowStyle != null) {
                    setViewWidth(progressBar, mRowStyle.getSeekBarInlineWidth());
                }
            }
        } else {
            // non interactive progress bar
            if (renderInNewLine) {
                progressBar = new ProgressBar(getContext(), null,
                        android.R.attr.progressBarStyleHorizontal);
            } else {
                progressBar = (ProgressBar) LayoutInflater.from(getContext()).inflate(
                        R.layout.abc_slice_progress_inline_view, this, false);
                if (mRowStyle != null) {
                    setViewWidth(progressBar,
                            mRowStyle.getProgressBarInlineWidth());
                    setViewSidePaddings(progressBar,
                            mRowStyle.getProgressBarStartPadding(),
                            mRowStyle.getProgressBarEndPadding());
                }
            }
            if (isIndeterminate) {
                progressBar.setIndeterminate(true);
            }
        }
        Drawable progressDrawable = isIndeterminate ? DrawableCompat.wrap(
                progressBar.getIndeterminateDrawable()) :
                DrawableCompat.wrap(progressBar.getProgressDrawable());
        if (mTintColor != -1 && progressDrawable != null) {
            DrawableCompat.setTint(progressDrawable, mTintColor);
            if (isIndeterminate) {
                progressBar.setIndeterminateDrawable(progressDrawable);
            } else {
                progressBar.setProgressDrawable(progressDrawable);
            }
        }
        // N.B. We don't use progressBar.setMin because it doesn't work properly in backcompat
        //      and/or sliders.
        progressBar.setMax(mRangeMaxValue - mRangeMinValue);
        progressBar.setProgress(mRangeValue);
        progressBar.setVisibility(View.VISIBLE);
        if (mStartItem == null) {
            addView(progressBar,
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        } else {
            mSubContent.setVisibility(GONE);
            mContent.addView(progressBar, 1);
        }
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
    }

    private void addRatingBarView() {
        RatingBar ratingBar = new RatingBar(getContext());
        LayerDrawable starDrawable = (LayerDrawable) ratingBar.getProgressDrawable();
        starDrawable.getDrawable(STAR_COLOR_INDEX).setColorFilter(mTintColor,
                PorterDuff.Mode.SRC_IN);
        ratingBar.setStepSize(1.0f);
        ratingBar.setNumStars(mRangeMaxValue);
        ratingBar.setRating(mRangeValue);
        ratingBar.setVisibility(View.VISIBLE);
        LinearLayout ratingBarContainer = new LinearLayout(getContext());
        ratingBarContainer.setGravity(Gravity.CENTER);
        ratingBarContainer.setVisibility(View.VISIBLE);
        ratingBarContainer.addView(ratingBar,
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(ratingBarContainer, new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        ratingBar.setOnRatingBarChangeListener(mRatingBarChangeListener);
        mRangeBar = ratingBarContainer;
    }

    void sendSliderValue() {
        if (mRangeItem == null) {
            return;
        }

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

    @SuppressWarnings("deprecation")
    private void addSelection(final SliceItem selection) {
        if (mHandler == null) {
            mHandler = new Handler();
        }

        mSelectionOptionKeys = new ArrayList<String>();
        mSelectionOptionValues = new ArrayList<CharSequence>();

        final List<SliceItem> optionItems = selection.getSlice().getItems();

        for (int i = 0; i < optionItems.size(); i++) {
            final SliceItem optionItem = optionItems.get(i);
            if (!optionItem.hasHint(HINT_SELECTION_OPTION)) {
                continue;
            }

            final SliceItem optionKeyItem =
                    SliceQuery.findSubtype(optionItem, FORMAT_TEXT, SUBTYPE_SELECTION_OPTION_KEY);
            final SliceItem optionValueItem =
                    SliceQuery.findSubtype(optionItem, FORMAT_TEXT, SUBTYPE_SELECTION_OPTION_VALUE);
            if (optionKeyItem == null || optionValueItem == null) {
                continue;
            }

            mSelectionOptionKeys.add(optionKeyItem.getText().toString());
            mSelectionOptionValues.add(optionValueItem.getSanitizedText());
        }

        mSelectionSpinner = (Spinner) LayoutInflater.from(getContext()).inflate(
                R.layout.abc_slice_row_selection, this, false);

        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                getContext(), R.layout.abc_slice_row_selection_text, mSelectionOptionValues);
        adapter.setDropDownViewResource(R.layout.abc_slice_row_selection_dropdown_text);
        mSelectionSpinner.setAdapter(adapter);

        addView(mSelectionSpinner);
        // XXX: onItemSelected is called automatically.

        mSelectionSpinner.setOnItemSelectedListener(this);
    }
    /**
     * Add an action view to the container.
     */
    private void addAction(final SliceActionImpl actionContent, int color, ViewGroup container,
                           boolean isStart) {
        SliceActionView sav = new SliceActionView(getContext(), mSliceStyle, mRowStyle);
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
        } else if (FORMAT_LONG.equals(sliceItem.getFormat())) {
            timeStamp = sliceItem;
        }
        View addedView = null;
        if (icon != null) {
            boolean isIcon = !sliceItem.hasHint(HINT_NO_TINT);
            boolean useIntrinsicSize = sliceItem.hasHint(HINT_RAW);
            final float density = getResources().getDisplayMetrics().density;
            ImageView iv = new ImageView(getContext());
            Drawable d = icon.loadDrawable(getContext());
            final boolean hasRoundedImage =
                    mSliceStyle != null && mSliceStyle.getApplyCornerRadiusToLargeImages();
            if (hasRoundedImage && sliceItem.hasHint(HINT_LARGE)) {
                CornerDrawable cd = new CornerDrawable(d, mSliceStyle.getImageCornerRadius());
                iv.setImageDrawable(cd);
            } else {
                iv.setImageDrawable(d);
            }
            if (isIcon && color != -1) {
                iv.setColorFilter(color);
            }
            // Because of sliding, the title icon is added many times.
            if (mIsRangeSliding) {
                container.removeAllViews();
                container.addView(iv);
            } else {
                container.addView(iv);
            }
            if (mRowStyle != null) {
                int styleIconSize = mRowStyle.getIconSize();
                mIconSize = styleIconSize > 0 ? styleIconSize : mIconSize;
                int styleImageSize = mRowStyle.getImageSize();
                mImageSize = styleImageSize > 0 ? styleImageSize : mImageSize;
            }
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
            lp.width = useIntrinsicSize ? Math.round(d.getIntrinsicWidth() / density) : mImageSize;
            lp.height = useIntrinsicSize ? Math.round(d.getIntrinsicHeight() / density) :
                    mImageSize;
            iv.setLayoutParams(lp);
            int p = 0;
            if (isIcon) {
                p = mImageSize == HEIGHT_UNBOUND
                    ? mIconSize / 2 : (mImageSize - mIconSize) / 2;
            }
            iv.setPadding(p, p, p, p);
            addedView = iv;
        } else if (timeStamp != null) {
            TextView tv = new TextView(getContext());
            tv.setText(SliceViewUtil.getTimestampString(getContext(), sliceItem.getLong()));
            if (mSliceStyle != null) {
                tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSliceStyle.getSubtitleSize());
                tv.setTextColor(mRowStyle.getSubtitleColor());
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

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
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
    public void onClick(@NonNull View view) {
        if (mRowAction == null || mRowAction.getActionItem() == null) {
            return;
        }
        SliceActionView sav;
        if (mRowAction.getSubtype() != null) {
            switch (mRowAction.getSubtype()) {
                case SUBTYPE_TOGGLE:
                    sav = mToggles.get(mRowAction);
                    break;
                case SUBTYPE_DATE_PICKER:
                    onClickPicker(/*isDatePicker*/ true);
                    return;
                case SUBTYPE_TIME_PICKER:
                    onClickPicker(/*isDatePicker*/ false);
                    return;
                default:
                    sav = mActions.get(mRowAction);
            }
        } else {
            sav = mActions.get(mRowAction);
        }
        if (sav != null && !(view instanceof SliceActionView)) {
            // Row might have a single action item set on it, in that case we activate that item
            // and it will handle displaying any loading states / updating state for toggles
            sav.sendAction();
        } else {
            if (mRowContent.getIsHeader()) {
                // Header clicks are a little weird and SliceView needs to know about them to
                // maintain loading state; this is hooked up in SliceAdapter -- it will call
                // through to SliceView parent which has the info to perform the click.
                performClick();
            } else {
                try {
                    mShowActionSpinner =
                            mRowAction.getActionItem().fireActionInternal(getContext(), null);
                    if (mObserver != null) {
                        EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_CONTENT,
                                EventInfo.ROW_TYPE_LIST, mRowIndex);
                        mObserver.onSliceAction(info, mRowAction.getSliceItem());
                    }
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

    private void onClickPicker(boolean isDatePicker) {
        if (mRowAction == null) {
            return;
        }
        Log.d("ASDF", "ASDF" + isDatePicker + ":" + mRowAction.getSliceItem());
        SliceItem dateTimeItem = SliceQuery.findSubtype(mRowAction.getSliceItem(), FORMAT_LONG,
                SUBTYPE_MILLIS);
        if (dateTimeItem == null) {
            return;
        }
        int rowIndex = mRowIndex;
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(dateTimeItem.getLong()));
        if (isDatePicker) {
            DatePickerDialog dialog = new DatePickerDialog(
                    getContext(),
                    R.style.DialogTheme,
                    new DateSetListener(mRowAction.getSliceItem(), rowIndex),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        } else {
            TimePickerDialog dialog = new TimePickerDialog(
                    getContext(),
                    R.style.DialogTheme,
                    new TimeSetListener(mRowAction.getSliceItem(), rowIndex),
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false);
            dialog.show();
        }
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

    @Override
    public void onItemSelected(@NonNull AdapterView<?> parent, @NonNull View view, int position,
            long id) {
        if (mSelectionItem == null
                || parent != mSelectionSpinner
                || position < 0
                || position >= mSelectionOptionKeys.size()) {
            return;
        }

        if (mObserver != null) {
            EventInfo info = new EventInfo(getMode(), ACTION_TYPE_SELECTION, ROW_TYPE_SELECTION,
                    mRowIndex);
            // TODO: Record selected item somehow?
            mObserver.onSliceAction(info, mSelectionItem);
        }

        final String optionKey = mSelectionOptionKeys.get(position);

        try {
            final boolean loading = mSelectionItem.fireActionInternal(getContext(),
                    new Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                            .putExtra(EXTRA_SELECTION, optionKey));
            if (loading) {
                mShowActionSpinner = true;
                if (mLoadingListener != null) {
                    mLoadingListener.onSliceActionLoading(mRowAction.getSliceItem(), mRowIndex);
                    mLoadingActions.add(mRowAction.getSliceItem());
                }
                updateActionSpinner();
            }
        } catch (CanceledException e) {
            Log.e(TAG, "PendingIntent for slice cannot be sent", e);
        }
    }

    @Override
    public void onNothingSelected(@NonNull AdapterView<?> parent) {

    }

    private void setViewClickable(View layout, boolean isClickable) {
        layout.setOnClickListener(isClickable ? this : null);
        layout.setBackground(isClickable
                ? SliceViewUtil.getDrawable(getContext(), android.R.attr.selectableItemBackground)
                : null);
        layout.setClickable(isClickable);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
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
        mBottomDivider.setVisibility(GONE);
        mActionDivider.setVisibility(GONE);
        if (mSeeMoreView != null) {
            mRootView.removeView(mSeeMoreView);
            mSeeMoreView = null;
        }
        mIsRangeSliding = false;
        mRangeHasPendingUpdate = false;
        mRangeItem = null;
        mRangeMinValue = 0;
        mRangeMaxValue = 0;
        mRangeValue = 0;
        mLastSentRangeUpdate = 0;
        mHandler = null;
        if (mRangeBar != null) {
            if (mStartItem == null) {
                removeView(mRangeBar);
            } else {
                mContent.removeView(mRangeBar);
            }
            mRangeBar = null;
        }
        mSubContent.setVisibility(VISIBLE);
        mStartItem = null;
        mActionSpinner.setVisibility(GONE);
        if (mSelectionSpinner != null) {
            removeView(mSelectionSpinner);
            mSelectionSpinner = null;
        }
        mSelectionItem = null;
    }

    Runnable mRangeUpdater = new Runnable() {
        @Override
        public void run() {
            sendSliderValue();
            mRangeUpdaterRunning = false;
        }
    };

    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener =
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

    private final RatingBar.OnRatingBarChangeListener mRatingBarChangeListener =
            new RatingBar.OnRatingBarChangeListener() {
                @Override
                public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                    mRangeValue = Math.round(rating + mRangeMinValue);
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
            };
}
