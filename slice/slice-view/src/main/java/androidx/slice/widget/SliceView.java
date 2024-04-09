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

import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Observer;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * A view for displaying {@link Slice}s.
 * <p>
 * A slice is a piece of app content and actions that can be surfaced outside of the app it
 * originates from. SliceView is able to interpret the structure and contents of a slice and display
 * it. This structure is defined by the app providing the slice when the slice is constructed with a
 * {@link androidx.slice.builders.TemplateSliceBuilder}.
 * </p>
 * <p>
 * SliceView is able to display slices in a couple of different modes via {@see #setMode}.
 * <ul>
 * <li><b>Small</b>: The small format has a restricted height and display top-level information
 * and actions from the slice.</li>
 * <li><b>Large</b>: The large format displays as much of the slice as it can based on the space
 * provided for SliceView, if the slice overflows the space SliceView will scroll the content if
 * scrolling has been enabled on SliceView, {@see #setScrollable}.</li>
 * <li><b>Shortcut</b>: A shortcut shows minimal information and is presented as a tappable icon
 * representing the main content or action associated with the slice.</li>
 * </ul>
 * </p>
 * <p>
 * Slices can contain dynamic content that may update due to user interaction or a change in the
 * data being displayed in the slice. SliceView can be configured to listen for these updates easily
 * using {@link SliceLiveData}. Example usage:
 * <pre class="prettyprint">
 * SliceView v = new SliceView(getContext());
 * v.setMode(desiredMode);
 * LiveData<Slice> liveData = SliceLiveData.fromUri(sliceUri);
 * liveData.observe(lifecycleOwner, v);
 * </pre>
 * </p>
 * <p>
 * SliceView supports various style options, see {@link R.styleable#SliceView SliceView Attributes}.
 *
 * @see Slice
 * @see SliceLiveData
 *
 * @deprecated Slice framework has been deprecated, it will not receive any updates moving
 * forward. If you are looking for a framework that handles communication across apps,
 * consider using {@link android.app.appsearch.AppSearchManager}.
 */
@Deprecated
public class SliceView extends ViewGroup implements Observer<Slice>, View.OnClickListener {

    private static final String TAG = "SliceView";

    /**
     * Implement this interface to be notified of interactions with the slice displayed
     * in this view.
     * @see EventInfo
     *
     * @deprecated Slice framework has been deprecated, it will not receive any updates moving
     * forward. If you are looking for a framework that handles communication across apps,
     * consider using {@link android.app.appsearch.AppSearchManager}.
     */
    @Deprecated
    public interface OnSliceActionListener {
        /**
         * Called when an interaction has occurred with an element in this view.
         * @param info the type of event that occurred.
         * @param item the specific item within the {@link Slice} that was interacted with.
         */
        void onSliceAction(@NonNull EventInfo info, @NonNull SliceItem item);
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            MODE_SMALL, MODE_LARGE, MODE_SHORTCUT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceMode {}

    /**
     * Mode indicating this slice should be presented in small format, only top-level information
     * and actions from the slice are shown.
     */
    public static final int MODE_SMALL       = 1;
    /**
     * Mode indicating this slice should be presented in large format, as much or all of the slice
     * contents are shown.
     */
    public static final int MODE_LARGE       = 2;
    /**
     * Mode indicating this slice should be presented as a tappable icon.
     */
    public static final int MODE_SHORTCUT    = 3;

    /**
     * Refresh last updated label every 60 seconds when the slice is visible.
     */
    private static final int REFRESH_LAST_UPDATED_IN_MILLIS = 60000;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    ListContent mListContent;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    SliceChildView mCurrentView;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    View.OnLongClickListener mLongClickListener;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Handler mHandler;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    SliceMetadata mSliceMetadata;

    private Slice mCurrentSlice;
    private SliceMetrics mCurrentSliceMetrics;
    private List<SliceAction> mActions;
    private ActionRow mActionRow;

    private boolean mShowActions = false;
    private boolean mShowLastUpdated = true;
    private boolean mCurrentSliceLoggedVisible = false;
    private boolean mShowTitleItems = false;
    private boolean mShowHeaderDivider = false;
    private boolean mShowActionDividers = false;

    private int mShortcutSize;
    private int mMinTemplateHeight;
    private int mLargeHeight;
    private int mActionRowHeight;

    private SliceViewPolicy mViewPolicy;
    private SliceStyle mSliceStyle;
    private int mThemeTintColor = -1;

    private OnSliceActionListener mSliceObserver;
    private int mTouchSlopSquared;
    private View.OnClickListener mOnClickListener;
    private int mDownX;
    private int mDownY;
    boolean mPressing;
    boolean mInLongpress;
    int[] mClickInfo;

    public SliceView(Context context) {
        this(context, null);
    }

    public SliceView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.sliceViewStyle);
    }

    public SliceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.Widget_SliceView);
    }

    @RequiresApi(21)
    public SliceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    @SuppressWarnings("deprecation")
    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mSliceStyle = new SliceStyle(context, attrs, defStyleAttr, defStyleRes);
        mThemeTintColor = mSliceStyle.getTintColor();
        mShortcutSize = getContext().getResources()
                .getDimensionPixelSize(R.dimen.abc_slice_shortcut_size);
        mMinTemplateHeight = getContext().getResources()
                .getDimensionPixelSize(R.dimen.abc_slice_row_min_height);
        mLargeHeight = getResources().getDimensionPixelSize(R.dimen.abc_slice_large_height);
        mActionRowHeight = getResources().getDimensionPixelSize(
                R.dimen.abc_slice_action_row_height);
        mViewPolicy = new SliceViewPolicy();
        mCurrentView = new TemplateView(getContext());
        mCurrentView.setPolicy(mViewPolicy);
        addView(mCurrentView, getChildLp(mCurrentView));
        applyConfigurations();

        // TODO: action row background should support light / dark / maybe presenter customization
        mActionRow = new ActionRow(getContext(), true);
        mActionRow.setBackground(new ColorDrawable(0xffeeeeee));
        addView(mActionRow, getChildLp(mActionRow));
        updateActions();

        final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mTouchSlopSquared = slop * slop;
        mHandler = new Handler();

        setClipToPadding(false);
        super.setOnClickListener(this);
    }

    /**
     * Allows subclasses to set the current view to a custom view.
     */
    public void setCurrentView(@NonNull SliceChildView currentView) {
        removeView(mCurrentView);
        mCurrentView = currentView;
        mCurrentView.setPolicy(mViewPolicy);
        addView(mCurrentView, getChildLp(mCurrentView));
        applyConfigurations();
    }

    @VisibleForTesting
    void setSliceViewPolicy(SliceViewPolicy policy) {
        mViewPolicy = policy;
    }

    /**
     * Indicates whether this view reacts to click events or not.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean isSliceViewClickable() {
        return mOnClickListener != null
                || (mListContent != null && mListContent.getShortcut(getContext()) != null);
    }

    /**
     * Sets the event info for logging a click.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setClickInfo(int[] info) {
        mClickInfo = info;
    }

    @Override
    public void onClick(View v) {
        if (mListContent != null && mListContent.getShortcut(getContext()) != null) {
            try {
                SliceActionImpl sa = (SliceActionImpl) mListContent.getShortcut(getContext());
                SliceItem actionItem = sa.getActionItem();
                boolean loading = actionItem != null
                        && actionItem.fireActionInternal(getContext(), null);
                if (loading) {
                    mCurrentView.setActionLoading(sa.getSliceItem());
                }
                if (actionItem != null && mSliceObserver != null && mClickInfo != null
                        && mClickInfo.length > 1) {
                    EventInfo eventInfo = new EventInfo(getMode(),
                            EventInfo.ACTION_TYPE_CONTENT, mClickInfo[0], mClickInfo[1]);
                    mSliceObserver.onSliceAction(eventInfo, sa.getSliceItem());
                    logSliceMetricsOnTouch(sa.getSliceItem(), eventInfo);
                }
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "PendingIntent for slice cannot be sent", e);
            }
        } else if (mOnClickListener != null) {
            mOnClickListener.onClick(this);
        }
    }

    @Override
    public void setOnClickListener(View.OnClickListener listener) {
        mOnClickListener = listener;
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener listener) {
        super.setOnLongClickListener(listener);
        mLongClickListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return (mLongClickListener != null && handleTouchForLongpress(ev))
                || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return (mLongClickListener != null && handleTouchForLongpress(ev))
                || super.onTouchEvent(ev);
    }

    private boolean handleTouchForLongpress(MotionEvent ev) {
        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mHandler.removeCallbacks(mLongpressCheck);
                mDownX = (int) ev.getRawX();
                mDownY = (int) ev.getRawY();
                mPressing = true;
                mInLongpress = false;
                mHandler.postDelayed(mLongpressCheck, ViewConfiguration.getLongPressTimeout());
                return false;

            case MotionEvent.ACTION_MOVE:
                final int deltaX = (int) ev.getRawX() - mDownX;
                final int deltaY = (int) ev.getRawY() - mDownY;
                int distance = (deltaX * deltaX) + (deltaY * deltaY);
                if (distance > mTouchSlopSquared) {
                    mPressing = false;
                    mHandler.removeCallbacks(mLongpressCheck);
                }
                // If a long press has already happened, consume further movement.
                return mInLongpress;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                boolean wasInLongpress = mInLongpress;
                mPressing = false;
                mInLongpress = false;
                mHandler.removeCallbacks(mLongpressCheck);
                // If a long press just happened, consume up event to avoid a duplicate short click.
                return wasInLongpress;

            default:
                return false;
        }
    }

    /**
     * Sets the maximum height for a slice through the view policy.
     */
    protected void configureViewPolicy(int maxHeight) {
        if (mListContent != null && mListContent.isValid() && getMode() != MODE_SHORTCUT) {
            if (maxHeight > 0 && maxHeight < mSliceStyle.getRowMaxHeight()) {
                if (maxHeight <= mMinTemplateHeight) {
                    maxHeight = mMinTemplateHeight;
                }
                mViewPolicy.setMaxSmallHeight(maxHeight);
            } else {
                mViewPolicy.setMaxSmallHeight(0);
            }
            mViewPolicy.setMaxHeight(maxHeight);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (MODE_SHORTCUT == getMode()) {
            // TODO: consider scaling the shortcut to fit if too small
            width = mShortcutSize + getPaddingLeft() + getPaddingRight();
        }
        final int actionHeight = mActionRow.getVisibility() != View.GONE
                ? mActionRowHeight
                : 0;
        final int heightAvailable = MeasureSpec.getSize(heightMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        LayoutParams lp = getLayoutParams();
        final int maxHeight = (lp != null && lp.height == LayoutParams.WRAP_CONTENT)
                || heightMode == UNSPECIFIED
                ? -1 // no max, be default sizes
                : heightAvailable;
        configureViewPolicy(maxHeight);
        // Remove the padding from our available height
        int childrenHeight = heightAvailable - getPaddingTop() - getPaddingBottom();

        // never change the height if set to exactly
        if (heightMode != EXACTLY) {
            if (mListContent == null || !mListContent.isValid()) {
                childrenHeight = actionHeight;
            } else if (getMode() == MODE_SHORTCUT) {
                // No compromise in case of shortcut
                childrenHeight = mShortcutSize + actionHeight;
            } else {
                int requiredHeight =
                        mListContent.getHeight(mSliceStyle, mViewPolicy) + actionHeight;
                if (childrenHeight > requiredHeight || heightMode == UNSPECIFIED) {
                    // Available space is larger than what the slice wants
                    childrenHeight = requiredHeight;
                } else {
                    // Not enough space available for slice in current mode
                    if (mSliceStyle.getExpandToAvailableHeight()) {
                        // Don't request more space than we're allowed to have.
                        requiredHeight = childrenHeight;
                    } else if (getMode() == MODE_LARGE
                            && childrenHeight >= mLargeHeight + actionHeight) {
                        childrenHeight = mLargeHeight + actionHeight;
                    } else if (childrenHeight <= mMinTemplateHeight) {
                        childrenHeight = mMinTemplateHeight;
                    }
                }
            }
        }

        // Measure directly instead of calling measureChild as the later substracts padding
        // from the provided size
        int childWidthSpec = makeMeasureSpec(width, EXACTLY);
        int actionRowHeight = actionHeight > 0 ? (actionHeight + getPaddingBottom()) : 0;
        mActionRow.measure(childWidthSpec, makeMeasureSpec(actionRowHeight, EXACTLY));

        // Include the bottom padding for currentView only if action row is invisible
        int currentViewHeight = childrenHeight + getPaddingTop()
                + (actionHeight > 0 ? 0 : getPaddingBottom());
        mCurrentView.measure(childWidthSpec, makeMeasureSpec(currentViewHeight, EXACTLY));
        setMeasuredDimension(width,
                mCurrentView.getMeasuredHeight() + mActionRow.getMeasuredHeight());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View v = mCurrentView;
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        if (mActionRow.getVisibility() != View.GONE) {
            int top = v.getMeasuredHeight();
            mActionRow.layout(0, top, mActionRow.getMeasuredWidth(),
                    mActionRow.getMeasuredHeight() + top);
        }
    }

    @Override
    public void onChanged(@Nullable Slice t) {
        setSlice(t);
    }

    /**
     * Populates this view to the provided {@link Slice}.
     *
     * This will not update automatically if the slice content changes, for live
     * content see {@link SliceLiveData}.
     */
    public void setSlice(@Nullable Slice slice) {
        LocationBasedViewTracker.trackInputFocused(this);
        LocationBasedViewTracker.trackA11yFocus(this);
        initSliceMetrics(slice);
        boolean isUpdate = slice != null && mCurrentSlice != null
                && slice.getUri().equals(mCurrentSlice.getUri());
        SliceMetadata oldSliceData = mSliceMetadata;
        mCurrentSlice = slice;
        mSliceMetadata = mCurrentSlice != null ? SliceMetadata.from(getContext(), mCurrentSlice)
                : null;
        if (isUpdate) {
            // If its an update check the loading state
            SliceMetadata newSliceData = mSliceMetadata;
            if (oldSliceData.getLoadingState() == SliceMetadata.LOADED_ALL
                    && newSliceData.getLoadingState() == SliceMetadata.LOADED_NONE) {
                // If it's the same slice going from "loaded all" to "loaded none"... let's
                // ignore the update.
                return;
            }
        } else {
            mCurrentView.resetView();
        }
        mListContent = mSliceMetadata != null ? mSliceMetadata.getListContent() : null;
        if (mShowTitleItems) {
            showTitleItems(true);
        }
        if (mShowHeaderDivider) {
            showHeaderDivider(true);
        }
        if (mShowActionDividers) {
            showActionDividers(true);
        }
        if (mListContent == null || !mListContent.isValid()) {
            mActions = null;
            mCurrentView.resetView();
            updateActions();
            return;
        }
        // New slice means we shouldn't have any actions loading
        mCurrentView.setLoadingActions(null);

        // Check if the slice content is expired and show when it was last updated
        mActions = mSliceMetadata.getSliceActions();
        mCurrentView.setLastUpdated(mSliceMetadata.getLastUpdatedTime());
        mCurrentView.setShowLastUpdated(mShowLastUpdated && mSliceMetadata.isExpired());
        mCurrentView.setAllowTwoLines(mSliceMetadata.isPermissionSlice());

        // Tint color can come with the slice, so may need to update it
        mCurrentView.setTint(getTintColor());

        if (mListContent.getLayoutDir() != -1) {
            mCurrentView.setLayoutDirection(mListContent.getLayoutDir());
        } else {
            mCurrentView.setLayoutDirection(View.LAYOUT_DIRECTION_INHERIT);
        }

        // Set the slice
        mCurrentView.setSliceContent(mListContent);
        updateActions();

        // Log slice metrics visible.
        logSliceMetricsVisibilityChange(true /* visible */);

        // Automatically refresh the last updated label when the slice TTL isn't infinity.
        refreshLastUpdatedLabel(true /* visible */);
    }

    /**
     * @return the slice being used to populate this view.
     */
    @Nullable
    public Slice getSlice() {
        return mCurrentSlice;
    }

    /**
     * Returns the slice actions presented in this view.
     * <p>
     * Note that these may be different from {@link SliceMetadata#getSliceActions()} if the actions
     * set on the view have been adjusted using {@link #setSliceActions(List)}.
     */
    @Nullable
    public List<SliceAction> getSliceActions() {
        if (mActions != null && mActions.isEmpty()) {
            // They're empty because presenter set null slice actions, return null to be consistent.
            return null;
        }
        return mActions;
    }

    /**
     * Sets the slice actions to display for the slice contained in this view. Normally SliceView
     * will automatically show actions, however, it is possible to reorder or omit actions on the
     * view using this method. This is generally discouraged.
     * <p>
     * It is required that the slice be set on this view before actions can be set, otherwise
     * this will throw {@link IllegalStateException}. If any of the actions supplied are not
     * available for the slice set on this view (i.e. the action is not returned by
     * {@link SliceMetadata#getSliceActions()} this will throw {@link IllegalArgumentException}.
     */
    public void setSliceActions(@Nullable List<SliceAction> newActions) {
        // Check that these actions are part of available set
        if (mCurrentSlice == null || mSliceMetadata == null) {
            throw new IllegalStateException("Trying to set actions on a view without a slice");
        }
        List<SliceAction> availableActions = mSliceMetadata.getSliceActions();
        if (availableActions != null && newActions != null) {
            for (int i = 0; i < newActions.size(); i++) {
                if (!availableActions.contains(newActions.get(i))) {
                    throw new IllegalArgumentException(
                            "Trying to set an action that isn't available: " + newActions.get(i));
                }
            }
        }
        mActions = newActions == null ? new ArrayList<SliceAction>() : newActions;
        updateActions();
    }

    /**
     * Set the mode this view should present in.
     */
    public void setMode(@SliceMode int mode) {
        setMode(mode, false /* animate */);
    }

    /**
     * Set whether this view should allow scrollable content when presenting in {@link #MODE_LARGE}.
     */
    public void setScrollable(boolean isScrollable) {
        if (isScrollable != mViewPolicy.isScrollable()) {
            mViewPolicy.setScrollable(isScrollable);
        }
    }

    /**
     * Whether this view allow scrollable content when presenting in {@link #MODE_LARGE}.
     */
    public boolean isScrollable() {
        return mViewPolicy.isScrollable();
    }

    /**
     * Sets the listener to notify when an interaction event occurs on the view.
     * @see EventInfo
     */
    public void setOnSliceActionListener(@Nullable OnSliceActionListener observer) {
        mSliceObserver = observer;
        mCurrentView.setSliceActionListener(mSliceObserver);
    }

    /**
     * Contents of a slice such as icons, text, and controls (e.g. toggle) can be tinted. Normally
     * a color for tinting will be provided by the slice. Using this method will override
     * the slice-provided color information and instead tint elements with the color set here.
     *
     * @param accentColor the color to use for tinting contents of this view.
     */
    public void setAccentColor(@ColorInt int accentColor) {
        mThemeTintColor = accentColor;
        mSliceStyle.setTintColor(mThemeTintColor);
        mCurrentView.setTint(getTintColor());
    }

    /**
     * Sets the {@link RowStyleFactory} which allows multiple children to have different styles.
     */
    public void setRowStyleFactory(@Nullable RowStyleFactory rowStyleFactory) {
        mSliceStyle.setRowStyleFactory(rowStyleFactory);
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setMode(@SliceMode int mode, boolean animate) {
        if (animate) {
            Log.e(TAG, "Animation not supported yet");
        }
        if (mViewPolicy.getMode() == mode) {
            return;
        }
        if (mode != MODE_SMALL && mode != MODE_LARGE && mode != MODE_SHORTCUT) {
            Log.w(TAG, "Unknown mode: " + mode
                    + " please use one of MODE_SHORTCUT, MODE_SMALL, MODE_LARGE");
            mode = MODE_LARGE;
        }
        mViewPolicy.setMode(mode);
        updateViewConfig();
    }

    /**
     * @return the mode this view is presenting in.
     */
    public @SliceMode int getMode() {
        return mViewPolicy.getMode();
    }

    /**
     * Whether this view should show title items on the first row of the slice.
     * Title items appear at the start of the row.
     */
    public void setShowTitleItems(boolean enabled) {
        mShowTitleItems = enabled;
        if (mListContent != null) {
            mListContent.showTitleItems(enabled);
        }
    }

    /**
     * @deprecated TO BE REMOVED
     */
    @Deprecated
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void showTitleItems(boolean enabled) {
        setShowTitleItems(enabled);
    }

    /**
     * Whether this view should show the header divider.
     */
    public void setShowHeaderDivider(boolean enabled) {
        mShowHeaderDivider = enabled;
        if (mListContent != null) {
            mListContent.showHeaderDivider(enabled);
        }
    }

    /**
     * @deprecated TO BE REMOVED
     */
    @Deprecated
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void showHeaderDivider(boolean enabled) {
        setShowHeaderDivider(enabled);
    }

    /**
     * Whether this view should show action dividers for rows.
     */
    public void setShowActionDividers(boolean enabled) {
        mShowActionDividers = enabled;
        if (mListContent != null) {
            mListContent.showActionDividers(enabled);
        }
    }

    /**
     * @deprecated TO BE REMOVED
     */
    @Deprecated
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public void showActionDividers(boolean enabled) {
        setShowActionDividers(enabled);
    }

    /**
     *
     * Whether this view should show a row of actions with it.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setShowActionRow(boolean show) {
        mShowActions = show;
        updateActions();
    }

    /**
     * @return whether this view is showing a row of actions.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean isShowingActionRow() {
        return mShowActions;
    }

    /**
     * Returns the number of slice items not displayed in this view.
     *
     * <ul>
     * <li> In {@link #MODE_LARGE}: If the slice is not scrollable, this is the number
     * of slice items that don't fit into the available height. If it is scrollable, this method
     * returns 0.</li>
     * <li> In {@link #MODE_SMALL}: Returns the number of items not presented to the user.</li>
     * <li> In {@link #MODE_SHORTCUT}: Returns 0.</li>
     * </ul>
     */
    public int getHiddenItemCount() {
        return mCurrentView.getHiddenItemCount();
    }

    /**
     * Updates the current view to represent the correct type of view for the current mode.
     * If the view is changed for the mode any configurations are also applied to it.
     */
    private void updateViewConfig() {
        boolean newView = false;

        // Check if our view is right for the current mode
        int mode = getMode();
        boolean isCurrentViewShortcut = mCurrentView instanceof ShortcutView;
        Set<SliceItem> loadingActions = mCurrentView.getLoadingActions();
        if (mode == MODE_SHORTCUT && !isCurrentViewShortcut) {
            removeView(mCurrentView);
            mCurrentView = new ShortcutView(getContext());
            addView(mCurrentView, getChildLp(mCurrentView));
            newView = true;
        } else if (mode != MODE_SHORTCUT && isCurrentViewShortcut) {
            removeView(mCurrentView);
            mCurrentView = new TemplateView(getContext());
            addView(mCurrentView, getChildLp(mCurrentView));
            newView = true;
        }

        // If the view changes we should apply any configurations to it
        if (newView) {
            mCurrentView.setPolicy(mViewPolicy);
            applyConfigurations();
            if (mListContent != null && mListContent.isValid()) {
                mCurrentView.setSliceContent(mListContent);
            }
            mCurrentView.setLoadingActions(loadingActions);
        }
        updateActions();
    }

    private void applyConfigurations() {
        mCurrentView.setSliceActionListener(mSliceObserver);
        mCurrentView.setStyle(mSliceStyle, mSliceStyle.getRowStyle(/* sliceItem= */ null));
        mCurrentView.setTint(getTintColor());

        if (mListContent != null && mListContent.getLayoutDir() != -1) {
            mCurrentView.setLayoutDirection(mListContent.getLayoutDir());
        } else {
            mCurrentView.setLayoutDirection(View.LAYOUT_DIRECTION_INHERIT);
        }
    }

    private void updateActions() {
        if (mActions == null) {
            // No actions, hide the row, clear out the view
            mActionRow.setVisibility(View.GONE);
            mCurrentView.setSliceActions(null);
            mCurrentView.setInsets(getPaddingStart(), getPaddingTop(), getPaddingEnd(),
                    getPaddingBottom());
            return;
        }
        // Sort actions based on priority and set them in action rows.
        List<SliceAction> sortedActions = new ArrayList<>(mActions);
        Collections.sort(sortedActions, SLICE_ACTION_PRIORITY_COMPARATOR);
        if (mShowActions && getMode() != MODE_SHORTCUT && mActions.size() >= 2) {
            // Show in action row if available
            mActionRow.setActions(sortedActions, getTintColor());
            mActionRow.setVisibility(View.VISIBLE);

            // Hide them on the template
            mCurrentView.setSliceActions(null);

            mCurrentView.setInsets(getPaddingStart(), getPaddingTop(), getPaddingEnd(), 0);
            mActionRow.setPaddingRelative(getPaddingStart(), 0, getPaddingEnd(),
                    getPaddingBottom());

        } else {
            // Otherwise set them on the template
            mCurrentView.setSliceActions(sortedActions);
            mCurrentView.setInsets(getPaddingStart(), getPaddingTop(), getPaddingEnd(),
                    getPaddingBottom());

            mActionRow.setVisibility(View.GONE);
        }
    }

    private int getTintColor() {
        if (mThemeTintColor != -1) {
            // Theme has specified a color, use that
            return mThemeTintColor;
        } else {
            final SliceItem colorItem = SliceQuery.findSubtype(
                    mCurrentSlice, FORMAT_INT, SUBTYPE_COLOR);
            return colorItem != null
                    ? colorItem.getInt()
                    : SliceViewUtil.getColorAccent(getContext());
        }
    }

    private LayoutParams getChildLp(View child) {
        if (child instanceof ShortcutView) {
            return new LayoutParams(mShortcutSize, mShortcutSize);
        } else {
            return new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);
        }
    }

    /**
     * @return String representation of the provided mode.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static String modeToString(@SliceMode int mode) {
        switch(mode) {
            case MODE_SHORTCUT:
                return "MODE SHORTCUT";
            case MODE_SMALL:
                return "MODE SMALL";
            case MODE_LARGE:
                return "MODE LARGE";
            default:
                return "unknown mode: " + mode;
        }
    }

    Runnable mLongpressCheck = new Runnable() {
        @Override
        public void run() {
            if (mPressing && mLongClickListener != null) {
                mInLongpress = true;
                mLongClickListener.onLongClick(SliceView.this);
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isShown()) {
            logSliceMetricsVisibilityChange(true /* visible */);
            refreshLastUpdatedLabel(true /* visible */);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        logSliceMetricsVisibilityChange(false /* not visible */);
        refreshLastUpdatedLabel(false /* not visible */);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (isAttachedToWindow()) {
            logSliceMetricsVisibilityChange(visibility == VISIBLE);
            refreshLastUpdatedLabel(visibility == VISIBLE);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        logSliceMetricsVisibilityChange(visibility == VISIBLE);
        refreshLastUpdatedLabel(visibility == VISIBLE);
    }

    private void initSliceMetrics(@Nullable Slice slice) {
        if (slice == null || slice.getUri() == null) {
            logSliceMetricsVisibilityChange(false /* not visible */);
            mCurrentSliceMetrics = null;
        } else if (mCurrentSlice == null || !mCurrentSlice.getUri().equals(slice.getUri())) {
            logSliceMetricsVisibilityChange(false /* not visible */);
            mCurrentSliceMetrics =
                    SliceMetrics.getInstance(getContext(), slice.getUri());
        }
    }

    private void logSliceMetricsVisibilityChange(boolean visibility) {
        if (mCurrentSliceMetrics != null) {
            if (visibility && !mCurrentSliceLoggedVisible) {
                mCurrentSliceMetrics.logVisible();
                mCurrentSliceLoggedVisible = true;
            }
            if (!visibility && mCurrentSliceLoggedVisible) {
                mCurrentSliceMetrics.logHidden();
                mCurrentSliceLoggedVisible = false;
            }
        }
    }

    private void logSliceMetricsOnTouch(SliceItem item, EventInfo info) {
        if (mCurrentSliceMetrics != null) {
            if (item.getSlice() != null && item.getSlice().getUri() != null) {
                mCurrentSliceMetrics.logTouch(
                        info.actionType,
                        item.getSlice().getUri());
            }
        }
    }

    private void refreshLastUpdatedLabel(boolean visibility) {
        if (mShowLastUpdated && mSliceMetadata != null && !mSliceMetadata.neverExpires()) {
            if (visibility) {
                mHandler.postDelayed(mRefreshLastUpdated, mSliceMetadata.isExpired()
                        ? REFRESH_LAST_UPDATED_IN_MILLIS
                        : mSliceMetadata.getTimeToExpiry() + REFRESH_LAST_UPDATED_IN_MILLIS);
            } else {
                mHandler.removeCallbacks(mRefreshLastUpdated);
            }
        }
    }

    Runnable mRefreshLastUpdated = new Runnable() {
        @Override
        public void run() {
            if (mSliceMetadata != null && mSliceMetadata.isExpired()) {
                mCurrentView.setShowLastUpdated(true);
                mCurrentView.setSliceContent(mListContent);
            }
            mHandler.postDelayed(this, REFRESH_LAST_UPDATED_IN_MILLIS);
        }
    };

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final Comparator<SliceAction> SLICE_ACTION_PRIORITY_COMPARATOR =
            new Comparator<SliceAction>() {
                @Override
                public int compare(SliceAction action1, SliceAction action2) {
                    // Priority 0 is the highest and -1 meaning no priority.
                    int priority1 = action1.getPriority();
                    int priority2 = action2.getPriority();
                    if (priority1 < 0 && priority2 < 0) {
                        return 0;
                    } else if (priority1 < 0) {
                        return 1;
                    } else if (priority2 < 0) {
                        return -1;
                    } else if (priority2 < priority1) {
                        return 1;
                    } else if (priority2 > priority1) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            };
}
