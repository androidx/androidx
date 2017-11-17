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

import android.app.slice.Slice;
import android.app.slice.SliceItem;
import android.arch.lifecycle.Observer;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.List;

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
 * {@link Slice#HINT_TITLE} would be placed in the title position of a template. A slice annotated
 * with {@link Slice#HINT_LIST} would present the child items of that slice in a list.
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

    private static final String TAG = "SliceView";

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract static class SliceModeView extends FrameLayout {

        public SliceModeView(Context context) {
            super(context);
        }

        /**
         * @return the mode of the slice being presented.
         */
        public abstract int getMode();

        /**
         * @param slice the slice to show in this view.
         */
        public abstract void setSlice(Slice slice);
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
     * icon, and label. This can be indicated by using {@link Slice#HINT_TITLE} on an action in a
     * slice.
     */
    public static final int MODE_SHORTCUT    = 3;

    /**
     * Will select the type of slice binding based on size of the View. TODO: Put in some info about
     * that selection.
     */
    private static final int MODE_AUTO = 0;

    private int mMode = MODE_AUTO;
    private SliceModeView mCurrentView;
    private final ActionRow mActions;
    private Slice mCurrentSlice;
    private boolean mShowActions = true;
    private boolean mIsScrollable;
    private final int mShortcutSize;

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
        mActions = new ActionRow(getContext(), true);
        mActions.setBackground(new ColorDrawable(0xffeeeeee));
        mCurrentView = new LargeTemplateView(getContext());
        addView(mCurrentView, getChildLp(mCurrentView));
        addView(mActions, getChildLp(mActions));
        mShortcutSize = getContext().getResources()
                .getDimensionPixelSize(R.dimen.abc_slice_shortcut_size);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (MODE_SHORTCUT == mMode) {
            width = mShortcutSize;
        }
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.UNSPECIFIED) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        }
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int actionHeight = mActions.getVisibility() != View.GONE
                ? mActions.getMeasuredHeight()
                : 0;
        int newHeightSpec = MeasureSpec.makeMeasureSpec(
                mCurrentView.getMeasuredHeight() + actionHeight, MeasureSpec.EXACTLY);
        setMeasuredDimension(width, newHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mCurrentView.layout(0, 0, mCurrentView.getMeasuredWidth(),
                mCurrentView.getMeasuredHeight());
        if (mActions.getVisibility() != View.GONE) {
            mActions.layout(0, mCurrentView.getMeasuredHeight(), mActions.getMeasuredWidth(),
                    mCurrentView.getMeasuredHeight() + mActions.getMeasuredHeight());
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

    private SliceModeView createView(int mode) {
        switch (mode) {
            case MODE_SHORTCUT:
                return new ShortcutView(getContext());
            case MODE_SMALL:
                return new SmallTemplateView(getContext());
        }
        return new LargeTemplateView(getContext());
    }

    private void reinflate() {
        if (mCurrentSlice == null) {
            return;
        }
        // TODO: Smarter mapping here from one state to the next.
        SliceItem color = SliceQuery.find(mCurrentSlice, SliceItem.TYPE_COLOR);
        List<SliceItem> items = mCurrentSlice.getItems();
        SliceItem actionRow = SliceQuery.find(mCurrentSlice, SliceItem.TYPE_SLICE,
                Slice.HINT_ACTIONS,
                null);
        int mode = getMode();
        if (mMode == mCurrentView.getMode()) {
            mCurrentView.setSlice(mCurrentSlice);
        } else {
            removeAllViews();
            mCurrentView = createView(mode);
            addView(mCurrentView, getChildLp(mCurrentView));
            addView(mActions, getChildLp(mActions));
        }
        if (mode == MODE_LARGE) {
            ((LargeTemplateView) mCurrentView).setScrollable(mIsScrollable);
        }
        if (items.size() > 1 || (items.size() != 0 && items.get(0) != actionRow)) {
            mCurrentView.setVisibility(View.VISIBLE);
            mCurrentView.setSlice(mCurrentSlice);
        } else {
            mCurrentView.setVisibility(View.GONE);
        }

        boolean showActions = mShowActions && actionRow != null
                && mode != MODE_SHORTCUT;
        if (showActions) {
            mActions.setActions(actionRow, color);
            mActions.setVisibility(View.VISIBLE);
        } else {
            mActions.setVisibility(View.GONE);
        }
    }

    private LayoutParams getChildLp(View child) {
        if (child instanceof ShortcutView) {
            return new LayoutParams(mShortcutSize, mShortcutSize);
        } else {
            return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
    }

    private static void validate(Uri sliceUri) {
        if (!ContentResolver.SCHEME_CONTENT.equals(sliceUri.getScheme())) {
            throw new RuntimeException("Invalid uri " + sliceUri);
        }
        if (sliceUri.getPathSegments().size() == 0) {
            throw new RuntimeException("Invalid uri " + sliceUri);
        }
    }
}
