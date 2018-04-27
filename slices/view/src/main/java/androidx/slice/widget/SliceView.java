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

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.TypedArray;
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
import androidx.lifecycle.Observer;
import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceMetadata;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceHints;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * A view for displaying a {@link Slice} which is a piece of app content and actions. SliceView is
 * able to present slice content in a templated format outside of the associated app. The way this
 * content is displayed depends on the structure of the slice, the hints associated with the
 * content, and the mode that SliceView is configured for. The modes that SliceView supports are:
 * <ul>
 * <li><b>Shortcut</b>: A shortcut is presented as an icon and a text label representing the main
 * content or action associated with the slice.</li>
 * <li><b>Small</b>: The small format has a restricted height and can present a single
 * {@link SliceItem} or a limited collection of items.</li>
 * <li><b>Large</b>: The large format displays multiple small templates in a list, if scrolling is
 * not enabled (see {@link #setScrollable(boolean)}) the view will show as many items as it can
 * comfortably fit.</li>
 * </ul>
 * <p>
 * When constructing a slice, the contents of it can be annotated with hints, these provide the OS
 * with some information on how the content should be displayed. For example, text annotated with
 * {@link android.app.slice.Slice#HINT_TITLE} would be placed in the title position of a template.
 * A slice annotated with {@link android.app.slice.Slice#HINT_LIST} would present the child items
 * of that slice in a list.
 * <p>
 * Example usage:
 *
 * <pre class="prettyprint">
 * SliceView v = new SliceView(getContext());
 * v.setMode(desiredMode);
 * LiveData<Slice> liveData = SliceLiveData.fromUri(sliceUri);
 * liveData.observe(lifecycleOwner, v);
 * </pre>
 * @see SliceLiveData
 */
public class SliceView extends ViewGroup implements Observer<Slice>, View.OnClickListener {

    private static final String TAG = "SliceView";

    /**
     * Implement this interface to be notified of interactions with the slice displayed
     * in this view.
     * @see EventInfo
     */
    public interface OnSliceActionListener {
        /**
         * Called when an interaction has occurred with an element in this view.
         * @param info the type of event that occurred.
         * @param item the specific item within the {@link Slice} that was interacted with.
         */
        void onSliceAction(@NonNull EventInfo info, @NonNull SliceItem item);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            MODE_SMALL, MODE_LARGE, MODE_SHORTCUT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SliceMode {}

    /**
     * Mode indicating this slice should be presented in small template format.
     */
    public static final int MODE_SMALL       = 1;
    /**
     * Mode indicating this slice should be presented in large template format.
     */
    public static final int MODE_LARGE       = 2;
    /**
     * Mode indicating this slice should be presented as an icon. A shortcut requires an intent,
     * icon, and label. This can be indicated by using {@link android.app.slice.Slice#HINT_TITLE}
     * on an action in a slice.
     */
    public static final int MODE_SHORTCUT    = 3;

    private int mMode = MODE_LARGE;
    private Slice mCurrentSlice;
    private ListContent mListContent;
    private SliceChildView mCurrentView;
    private List<SliceItem> mActions;
    private ActionRow mActionRow;

    private boolean mShowActions = false;
    private boolean mIsScrollable = true;
    private boolean mShowLastUpdated = true;

    private int mShortcutSize;
    private int mMinLargeHeight;
    private int mMaxLargeHeight;
    private int mActionRowHeight;

    private AttributeSet mAttrs;
    private int mDefStyleAttr;
    private int mDefStyleRes;
    private int mThemeTintColor = -1;

    private OnSliceActionListener mSliceObserver;
    private int mTouchSlopSquared;
    private View.OnLongClickListener mLongClickListener;
    private View.OnClickListener mOnClickListener;
    private int mDownX;
    private int mDownY;
    private boolean mPressing;
    private boolean mInLongpress;
    private Handler mHandler;
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

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mAttrs = attrs;
        mDefStyleAttr = defStyleAttr;
        mDefStyleRes = defStyleRes;
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SliceView,
                defStyleAttr, defStyleRes);

        try {
            mThemeTintColor = a.getColor(R.styleable.SliceView_tintColor, -1);
        } finally {
            a.recycle();
        }
        mShortcutSize = getContext().getResources()
                .getDimensionPixelSize(R.dimen.abc_slice_shortcut_size);
        mMinLargeHeight = getResources().getDimensionPixelSize(R.dimen.abc_slice_large_height);
        mMaxLargeHeight = getResources().getDimensionPixelSize(R.dimen.abc_slice_max_large_height);
        mActionRowHeight = getResources().getDimensionPixelSize(
                R.dimen.abc_slice_action_row_height);

        mCurrentView = new LargeTemplateView(getContext());
        mCurrentView.setMode(getMode());
        addView(mCurrentView, getChildLp(mCurrentView));

        // TODO: action row background should support light / dark / maybe presenter customization
        mActionRow = new ActionRow(getContext(), true);
        mActionRow.setBackground(new ColorDrawable(0xffeeeeee));
        addView(mActionRow, getChildLp(mActionRow));

        final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mTouchSlopSquared = slop * slop;
        mHandler = new Handler();

        super.setOnClickListener(this);
    }

    /**
     * Indicates whether this view reacts to click events or not.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean isSliceViewClickable() {
        return mOnClickListener != null
                || (mListContent != null && mListContent.getPrimaryAction() != null);
    }

    /**
     * Sets the event info for logging a click.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setClickInfo(int[] info) {
        mClickInfo = info;
    }

    @Override
    public void onClick(View v) {
        if (mListContent != null && mListContent.getPrimaryAction() != null) {
            try {
                SliceActionImpl sa = new SliceActionImpl(mListContent.getPrimaryAction());
                sa.getAction().send();
                if (mSliceObserver != null && mClickInfo != null && mClickInfo.length > 1) {
                    EventInfo eventInfo = new EventInfo(getMode(),
                            EventInfo.ACTION_TYPE_CONTENT, mClickInfo[0], mClickInfo[1]);
                    mSliceObserver.onSliceAction(eventInfo, mListContent.getPrimaryAction());
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
        boolean ret = super.onInterceptTouchEvent(ev);
        if (mLongClickListener != null) {
            return handleTouchForLongpress(ev);
        }
        return ret;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean ret = super.onTouchEvent(ev);
        if (mLongClickListener != null) {
            return handleTouchForLongpress(ev);
        }
        return ret;
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
                break;

            case MotionEvent.ACTION_MOVE:
                final int deltaX = (int) ev.getRawX() - mDownX;
                final int deltaY = (int) ev.getRawY() - mDownY;
                int distance = (deltaX * deltaX) + (deltaY * deltaY);
                if (distance > mTouchSlopSquared) {
                    mPressing = false;
                    mHandler.removeCallbacks(mLongpressCheck);
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mPressing = false;
                mInLongpress = false;
                mHandler.removeCallbacks(mLongpressCheck);
                break;
        }
        return mInLongpress;
    }

    private int getHeightForMode() {
        int mode = getMode();
        if (mode == MODE_SHORTCUT) {
            return mListContent != null && mListContent.isValid() ? mShortcutSize : 0;
        }
        return mode == MODE_LARGE
                ? mCurrentView.getActualHeight()
                : mCurrentView.getSmallHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int childWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (MODE_SHORTCUT == mMode) {
            // TODO: consider scaling the shortcut to fit if too small
            childWidth = mShortcutSize;
            width = mShortcutSize + getPaddingLeft() + getPaddingRight();
        }
        final int actionHeight = mActionRow.getVisibility() != View.GONE
                ? mActionRowHeight
                : 0;
        final int sliceHeight = getHeightForMode();
        final int heightAvailable = MeasureSpec.getSize(heightMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        // Remove the padding from our available height
        int height = heightAvailable - getPaddingTop() - getPaddingBottom();
        if (heightAvailable >= sliceHeight + actionHeight
                || heightMode == MeasureSpec.UNSPECIFIED) {
            // Available space is larger than the slice or we be what we want
            if (heightMode != MeasureSpec.EXACTLY) {
                if (!mIsScrollable) {
                    height = Math.min(mMaxLargeHeight, sliceHeight);
                } else {
                    // If we want to be bigger than max, then we can be a good scrollable at min
                    // large height, if it's not larger lets just use its desired height
                    height = sliceHeight > mMaxLargeHeight ? mMinLargeHeight : sliceHeight;
                }
            }
        } else {
            // Not enough space available for slice in current mode
            if (getMode() == MODE_LARGE && heightAvailable >= mMinLargeHeight + actionHeight) {
                // It's just a slice with scrolling content; cap it to height available.
                height = Math.min(mMinLargeHeight, heightAvailable);
            } else if (getMode() == MODE_SHORTCUT) {
                // TODO: consider scaling the shortcut to fit if too small
                height = mShortcutSize;
            }
        }

        int childHeight = height + getPaddingTop() + getPaddingBottom();
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
        measureChild(mCurrentView, childWidthMeasureSpec, childHeightMeasureSpec);

        int actionPaddedHeight = actionHeight + getPaddingTop() + getPaddingBottom();
        int actionHeightSpec = MeasureSpec.makeMeasureSpec(actionPaddedHeight, MeasureSpec.EXACTLY);
        measureChild(mActionRow, childWidthMeasureSpec, actionHeightSpec);

        // Total height should include action row and our padding
        height += actionHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View v = mCurrentView;
        final int left = getPaddingLeft();
        final int top = getPaddingTop();
        v.layout(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
        if (mActionRow.getVisibility() != View.GONE) {
            mActionRow.layout(left,
                    top + v.getMeasuredHeight(),
                    left + mActionRow.getMeasuredWidth(),
                    top + v.getMeasuredHeight() + mActionRow.getMeasuredHeight());
        }
    }

    @Override
    public void onChanged(@Nullable Slice slice) {
        setSlice(slice);
    }

    /**
     * Populates this view to the provided {@link Slice}.
     *
     * This will not update automatically if the slice content changes, for live
     * content see {@link SliceLiveData}.
     */
    public void setSlice(@Nullable Slice slice) {
        if (slice != null) {
            if (mCurrentSlice == null || !mCurrentSlice.getUri().equals(slice.getUri())) {
                mCurrentView.resetView();
            }
        } else {
            // No slice, no actions
            mActions = null;
        }
        mActions = SliceMetadata.getSliceActions(slice);
        mCurrentSlice = slice;
        reinflate();
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
    public List<SliceItem> getSliceActions() {
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
    public void setSliceActions(@Nullable List<SliceItem> newActions) {
        // Check that these actions are part of available set
        if (mCurrentSlice == null) {
            throw new IllegalStateException("Trying to set actions on a view without a slice");
        }
        List<SliceItem> availableActions = SliceMetadata.getSliceActions(mCurrentSlice);
        if (availableActions != null && newActions != null) {
            for (int i = 0; i < newActions.size(); i++) {
                if (!availableActions.contains(newActions.get(i))) {
                    throw new IllegalArgumentException(
                            "Trying to set an action that isn't available: " + newActions.get(i));
                }
            }
        }
        mActions = newActions;
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
        mIsScrollable = isScrollable;
        reinflate();
    }

    /**
     * Sets the listener to notify when an interaction events occur on the view.
     * @see EventInfo
     */
    public void setOnSliceActionListener(@Nullable OnSliceActionListener observer) {
        mSliceObserver = observer;
        mCurrentView.setSliceActionListener(mSliceObserver);
    }

    /**
     * @deprecated TO BE REMOVED; use {@link #setAccentColor(int)} instead.
     */
    @Deprecated
    public void setTint(int tintColor) {
        setAccentColor(tintColor);
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
        mCurrentView.setTint(accentColor);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setMode(@SliceMode int mode, boolean animate) {
        if (animate) {
            Log.e(TAG, "Animation not supported yet");
        }
        if (mMode == mode) {
            return;
        }
        mMode = mode;
        reinflate();
    }

    /**
     * @return the mode this view is presenting in.
     */
    public @SliceMode int getMode() {
        return mMode;
    }

    /**
     * @hide
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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean isShowingActionRow() {
        return mShowActions;
    }

    private void reinflate() {
        if (mCurrentSlice == null) {
            mCurrentView.resetView();
            updateActions();
            return;
        }
        mListContent = new ListContent(getContext(), mCurrentSlice, mAttrs, mDefStyleAttr,
                mDefStyleRes);
        if (!mListContent.isValid()) {
            mCurrentView.resetView();
            updateActions();
            return;
        }

        // TODO: Smarter mapping here from one state to the next.
        int mode = getMode();
        boolean isCurrentViewShortcut = mCurrentView instanceof ShortcutView;
        if (mode == MODE_SHORTCUT && !isCurrentViewShortcut) {
            removeAllViews();
            mCurrentView = new ShortcutView(getContext());
            addView(mCurrentView, getChildLp(mCurrentView));
        } else if (mode != MODE_SHORTCUT && isCurrentViewShortcut) {
            removeAllViews();
            mCurrentView = new LargeTemplateView(getContext());
            addView(mCurrentView, getChildLp(mCurrentView));
        }
        mCurrentView.setMode(mode);

        mCurrentView.setSliceActionListener(mSliceObserver);
        if (mCurrentView instanceof LargeTemplateView) {
            ((LargeTemplateView) mCurrentView).setScrollable(mIsScrollable);
        }
        mCurrentView.setStyle(mAttrs, mDefStyleAttr, mDefStyleRes);
        mCurrentView.setTint(getTintColor());

        // Check if the slice content is expired and show when it was last updated
        SliceMetadata sliceMetadata = SliceMetadata.from(getContext(), mCurrentSlice);
        long lastUpdated = sliceMetadata.getLastUpdatedTime();
        long expiry = sliceMetadata.getExpiry();
        long now = System.currentTimeMillis();
        mCurrentView.setLastUpdated(lastUpdated);
        boolean expired = expiry != 0 && expiry != SliceHints.INFINITY && now > expiry;
        mCurrentView.setShowLastUpdated(mShowLastUpdated && expired);

        // Set the slice
        mCurrentView.setSliceContent(mListContent);
        updateActions();
    }

    private void updateActions() {
        if (mActions == null || mActions.isEmpty()) {
            // No actions, hide the row, clear out the view
            mActionRow.setVisibility(View.GONE);
            mCurrentView.setSliceActions(null);
            return;
        }

        // TODO: take priority attached to actions into account
        if (mShowActions && mMode != MODE_SHORTCUT && mActions.size() >= 2) {
            // Show in action row if available
            mActionRow.setActions(mActions, getTintColor());
            mActionRow.setVisibility(View.VISIBLE);
            // Hide them on the template
            mCurrentView.setSliceActions(null);
        } else if (mActions.size() > 0) {
            // Otherwise set them on the template
            mCurrentView.setSliceActions(mActions);
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
     * @hide
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
}
