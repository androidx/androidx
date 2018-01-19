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

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_HORIZONTAL;
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;
import java.util.List;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;
import androidx.app.slice.SliceSpec;
import androidx.app.slice.core.SliceQuery;
import androidx.app.slice.view.R;

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
public class SliceView extends ViewGroup implements Observer<Slice> {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static List<SliceSpec> SUPPORTED_SPECS = Arrays.asList(
    );

    private static final String TAG = "SliceView";

    /**
     * Implement this interface to be notified of interactions with the slice displayed
     * in this view.
     * @hide
     * @see EventInfo
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public interface SliceObserver {
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

    /**
     * Will select the type of slice binding based on size of the View. TODO: Put in some info about
     * that selection.
     */
    private static final int MODE_AUTO = 0;
    private int mMode = MODE_AUTO;
    private SliceChildView mCurrentView;
    private Slice mCurrentSlice;
    private final ActionRow mActions;
    private final int mShortcutSize;
    private SliceObserver mSliceObserver;

    private boolean mShowActions = true;
    private boolean mIsScrollable = true;

    private int mThemeTintColor = -1;
    private AttributeSet mAttrs;

    public SliceView(Context context) {
        this(context, null);
    }

    public SliceView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SliceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SliceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mAttrs = attrs;
        mActions = new ActionRow(getContext(), true);
        mActions.setBackground(new ColorDrawable(0xffeeeeee));
        mCurrentView = new LargeTemplateView(getContext());
        addView(mCurrentView.getView(), getChildLp(mCurrentView.getView()));
        addView(mActions, getChildLp(mActions));
        mShortcutSize = getContext().getResources()
                .getDimensionPixelSize(R.dimen.abc_slice_shortcut_size);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int childWidth = MeasureSpec.getSize(widthMeasureSpec);
        int childHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (MODE_SHORTCUT == mMode) {
            // TODO: consider scaling the shortcut to fit
            childWidth = mShortcutSize;
            width = mShortcutSize;
        }
        final int left = getPaddingLeft();
        final int top = getPaddingTop();
        final int right = getPaddingRight();
        final int bot = getPaddingBottom();

        // Measure the children without the padding
        childWidth -= left + right;
        childHeight -= top + bot;
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
        measureChildren(childWidthMeasureSpec, childHeightMeasureSpec);

        // Figure out parent height
        int actionHeight = mActions.getVisibility() != View.GONE
                ? mActions.getMeasuredHeight()
                : 0;
        int currViewHeight = mCurrentView.getView().getMeasuredHeight() + top + bot;
        int newHeightSpec = MeasureSpec.makeMeasureSpec(currViewHeight + actionHeight,
                MeasureSpec.EXACTLY);
        // Figure out parent width
        width += left + right;
        setMeasuredDimension(width, newHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View v = mCurrentView.getView();
        final int left = getPaddingLeft();
        final int top = getPaddingTop();
        final int right = getPaddingRight();
        final int bottom = getPaddingBottom();
        v.layout(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
        if (mActions.getVisibility() != View.GONE) {
            mActions.layout(left,
                    top + v.getMeasuredHeight() + bottom,
                    left + mActions.getMeasuredWidth() + right,
                    top + v.getMeasuredHeight() + bottom + mActions.getMeasuredHeight());
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
        mCurrentSlice = slice;
        reinflate();
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
     * Sets the observer to notify when an interaction events occur on the view.
     * @hide
     * @see EventInfo
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setSliceObserver(@Nullable SliceObserver observer) {
        mSliceObserver = observer;
        mCurrentView.setSliceObserver(mSliceObserver);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setMode(@SliceMode int mode, boolean animate) {
        if (animate) {
            Log.e(TAG, "Animation not supported yet");
        }
        mMode = mode;
        reinflate();
    }

    /**
     * @return the mode this view is presenting in.
     */
    public @SliceMode int getMode() {
        if (mMode == MODE_AUTO) {
            return MODE_LARGE;
        }
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
        reinflate();
    }

    private SliceChildView createView(int mode) {
        switch (mode) {
            case MODE_SHORTCUT:
                return new ShortcutView(getContext());
            case MODE_SMALL:
                // Check if it's horizontal and use a grid instead
                if (SliceQuery.hasHints(mCurrentSlice, HINT_HORIZONTAL)) {
                    return new GridRowView(getContext());
                } else {
                    return new RowView(getContext());
                }
        }
        return new LargeTemplateView(getContext());
    }

    private void reinflate() {
        if (mCurrentSlice == null) {
            mCurrentView.resetView();
            return;
        }
        // TODO: Smarter mapping here from one state to the next.
        int mode = getMode();
        if (mMode == mCurrentView.getMode()) {
            mCurrentView.setSlice(mCurrentSlice);
        } else {
            removeAllViews();
            mCurrentView = createView(mode);
            if (mSliceObserver != null) {
                mCurrentView.setSliceObserver(mSliceObserver);
            }
            addView(mCurrentView.getView(), getChildLp(mCurrentView.getView()));
            addView(mActions, getChildLp(mActions));
        }
        // Scrolling
        if (mode == MODE_LARGE) {
            ((LargeTemplateView) mCurrentView).setScrollable(mIsScrollable);
        }
        // Styles
        mCurrentView.setStyle(mAttrs);
        // Set the slice
        SliceItem actionRow = SliceQuery.find(mCurrentSlice, FORMAT_SLICE,
                HINT_ACTIONS,
                null);
        List<SliceItem> items = mCurrentSlice.getItems();
        if (items.size() > 1 || (items.size() != 0 && items.get(0) != actionRow)) {
            mCurrentView.getView().setVisibility(View.VISIBLE);
            mCurrentView.setSlice(mCurrentSlice);
        } else {
            mCurrentView.getView().setVisibility(View.GONE);
        }
        // Deal with actions
        boolean showActions = mShowActions && actionRow != null
                && mode != MODE_SHORTCUT;
        if (showActions) {
            mActions.setActions(actionRow, getTintColor());
            mActions.setVisibility(View.VISIBLE);
        } else {
            mActions.setVisibility(View.GONE);
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
            return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * @return String representation of the provided mode.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static String modeToString(@SliceMode int mode) {
        switch(mode) {
            case MODE_AUTO:
                return "MODE AUTO";
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
}
