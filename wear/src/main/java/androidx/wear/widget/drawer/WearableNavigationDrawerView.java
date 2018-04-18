/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.wear.widget.drawer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.R;
import androidx.wear.internal.widget.drawer.MultiPagePresenter;
import androidx.wear.internal.widget.drawer.MultiPageUi;
import androidx.wear.internal.widget.drawer.SinglePagePresenter;
import androidx.wear.internal.widget.drawer.SinglePageUi;
import androidx.wear.internal.widget.drawer.WearableNavigationDrawerPresenter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Ease of use class for creating a Wearable navigation drawer. This can be used with {@link
 * WearableDrawerLayout} to create a drawer for users to easily navigate a wearable app.
 *
 * <p>There are two ways this information may be presented: as a single page and as multiple pages.
 * The single page navigation drawer will display 1-7 items to the user representing different
 * navigation verticals. If more than 7 items are provided to a single page navigation drawer, the
 * navigation drawer will be displayed as empty. The multiple page navigation drawer will display 1
 * or more pages to the user, each representing different navigation verticals.
 *
 * <p>The developer may specify which style to use with the {@code app:navigationStyle} custom
 * attribute. If not specified, {@link #SINGLE_PAGE singlePage} will be used as the default.
 */
public class WearableNavigationDrawerView extends WearableDrawerView {

    private static final String TAG = "WearableNavDrawer";

    /**
     * Listener which is notified when the user selects an item.
     */
    public interface OnItemSelectedListener {

        /**
         * Notified when the user has selected an item at position {@code pos}.
         */
        void onItemSelected(int pos);
    }

    /**
     * Enumeration of possible drawer styles.
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(Scope.LIBRARY)
    @IntDef({SINGLE_PAGE, MULTI_PAGE})
    public @interface NavigationStyle {}

    /**
     * Single page navigation drawer style. This is the default drawer style. It is ideal for 1-5
     * items, but works with up to 7 items. If more than 7 items exist, then the drawer will be
     * displayed as empty.
     */
    public static final int SINGLE_PAGE = 0;

    /**
     * Multi-page navigation drawer style. Each item is on its own page. Useful when more than 7
     * items exist.
     */
    public static final int MULTI_PAGE = 1;

    @NavigationStyle private static final int DEFAULT_STYLE = SINGLE_PAGE;
    private static final long AUTO_CLOSE_DRAWER_DELAY_MS = TimeUnit.SECONDS.toMillis(5);
    private final boolean mIsAccessibilityEnabled;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());
    private final Runnable mCloseDrawerRunnable =
            new Runnable() {
                @Override
                public void run() {
                    getController().closeDrawer();
                }
            };
    /**
     * Listens for single taps on the drawer.
     */
    @Nullable private final GestureDetector mGestureDetector;
    @NavigationStyle private final int mNavigationStyle;
    private final WearableNavigationDrawerPresenter mPresenter;
    private final SimpleOnGestureListener mOnGestureListener =
            new SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return mPresenter.onDrawerTapped();
                }
            };
    public WearableNavigationDrawerView(Context context) {
        this(context, (AttributeSet) null);
    }
    public WearableNavigationDrawerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableNavigationDrawerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WearableNavigationDrawerView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mGestureDetector = new GestureDetector(getContext(), mOnGestureListener);

        @NavigationStyle int navStyle = DEFAULT_STYLE;
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.WearableNavigationDrawerView,
                    defStyleAttr,
                    0 /* defStyleRes */);

            //noinspection WrongConstant
            navStyle = typedArray.getInt(
                    R.styleable.WearableNavigationDrawerView_navigationStyle, DEFAULT_STYLE);
            typedArray.recycle();
        }

        mNavigationStyle = navStyle;
        AccessibilityManager accessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        mIsAccessibilityEnabled = accessibilityManager.isEnabled();

        mPresenter =
                mNavigationStyle == SINGLE_PAGE
                        ? new SinglePagePresenter(new SinglePageUi(this), mIsAccessibilityEnabled)
                        : new MultiPagePresenter(this, new MultiPageUi(), mIsAccessibilityEnabled);

        getPeekContainer()
                .setContentDescription(
                        context.getString(R.string.ws_navigation_drawer_content_description));

        setOpenOnlyAtTopEnabled(true);
    }

    /**
     * Set a {@link WearableNavigationDrawerAdapter} that will supply data for this drawer.
     */
    public void setAdapter(final WearableNavigationDrawerAdapter adapter) {
        mPresenter.onNewAdapter(adapter);
    }

    /**
     * Add an {@link OnItemSelectedListener} that will be notified when the user selects an item.
     */
    public void addOnItemSelectedListener(OnItemSelectedListener listener) {
        mPresenter.onItemSelectedListenerAdded(listener);
    }

    /**
     * Remove an {@link OnItemSelectedListener}.
     */
    public void removeOnItemSelectedListener(OnItemSelectedListener listener) {
        mPresenter.onItemSelectedListenerRemoved(listener);
    }

    /**
     * Changes which index is selected. {@link OnItemSelectedListener#onItemSelected} will
     * be called when the specified {@code index} is reached, but it won't be called for items
     * between the current index and the destination index.
     */
    public void setCurrentItem(int index, boolean smoothScrollTo) {
        mPresenter.onSetCurrentItemRequested(index, smoothScrollTo);
    }

    /**
     * Returns the style this drawer is using, either {@link #SINGLE_PAGE} or {@link #MULTI_PAGE}.
     */
    @NavigationStyle
    public int getNavigationStyle() {
        return mNavigationStyle;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        autoCloseDrawerAfterDelay();
        return mGestureDetector != null && mGestureDetector.onTouchEvent(ev);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        // Prevent the window from being swiped closed while it is open by saying that it can scroll
        // horizontally.
        return isOpened();
    }

    @Override
    public void onDrawerOpened() {
        autoCloseDrawerAfterDelay();
    }

    @Override
    public void onDrawerClosed() {
        mMainThreadHandler.removeCallbacks(mCloseDrawerRunnable);
    }

    private void autoCloseDrawerAfterDelay() {
        if (!mIsAccessibilityEnabled) {
            mMainThreadHandler.removeCallbacks(mCloseDrawerRunnable);
            mMainThreadHandler.postDelayed(mCloseDrawerRunnable, AUTO_CLOSE_DRAWER_DELAY_MS);
        }
    }

    @Override
  /* package */ int preferGravity() {
        return Gravity.TOP;
    }

    /**
     * Adapter for specifying the contents of WearableNavigationDrawer.
     */
    public abstract static class WearableNavigationDrawerAdapter {

        @Nullable
        private WearableNavigationDrawerPresenter mPresenter;

        /**
         * Get the text associated with the item at {@code pos}.
         */
        public abstract CharSequence getItemText(int pos);

        /**
         * Get the drawable associated with the item at {@code pos}.
         */
        public abstract Drawable getItemDrawable(int pos);

        /**
         * Returns the number of items in this adapter.
         */
        public abstract int getCount();

        /**
         * This method should be called by the application if the data backing this adapter has
         * changed and associated views should update.
         */
        public void notifyDataSetChanged() {
            // If this method is called before drawer.setAdapter, then we will not yet have a
            // presenter.
            if (mPresenter != null) {
                mPresenter.onDataSetChanged();
            } else {
                Log.w(TAG,
                        "adapter.notifyDataSetChanged called before drawer.setAdapter; ignoring.");
            }
        }

        /**
         * @hide
         */
        @RestrictTo(Scope.LIBRARY)
        public void setPresenter(WearableNavigationDrawerPresenter presenter) {
            mPresenter = presenter;
        }
    }

}
