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

package android.support.wear.widget.drawer;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.annotation.StyleableRes;
import android.support.v4.widget.ViewDragHelper;
import android.support.wear.R;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * View that contains drawer content and a peeking view for use with {@link WearableDrawerLayout}.
 *
 * <p>This view provides the ability to set its main content as well as a view shown while peeking.
 * Specifying the peek view is entirely optional; a default is used if none are set. However, the
 * content must be provided.
 *
 * <p>There are two ways to specify the content and peek views: by invoking {@code setter} methods
 * on the {@code WearableDrawerView}, or by specifying the {@code app:drawerContent} and {@code
 * app:peekView} attributes. Examples:
 *
 * <pre>
 * // From Java:
 * drawerView.setDrawerContent(drawerContentView);
 * drawerView.setPeekContent(peekContentView);
 *
 * &lt;!-- From XML: --&gt;
 * &lt;android.support.wear.widget.drawer.WearableDrawerView
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     android:layout_gravity="bottom"
 *     android:background="@color/red"
 *     app:drawerContent="@+id/drawer_content"
 *     app:peekView="@+id/peek_view"&gt;
 *
 *     &lt;FrameLayout
 *         android:id="@id/drawer_content"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent" /&gt;
 *
 *     &lt;LinearLayout
 *         android:id="@id/peek_view"
 *         android:layout_width="wrap_content"
 *         android:layout_height="wrap_content"
 *         android:layout_gravity="center_horizontal"
 *         android:orientation="horizontal"&gt;
 *         &lt;ImageView
 *             android:layout_width="wrap_content"
 *             android:layout_height="wrap_content"
 *             android:src="@android:drawable/ic_media_play" /&gt;
 *         &lt;ImageView
 *             android:layout_width="wrap_content"
 *             android:layout_height="wrap_content"
 *             android:src="@android:drawable/ic_media_pause" /&gt;
 *     &lt;/LinearLayout&gt;
 * &lt;/android.support.wear.widget.drawer.WearableDrawerView&gt;</pre>
 */
@TargetApi(Build.VERSION_CODES.M)
public class WearableDrawerView extends FrameLayout {
    /**
     * Indicates that the drawer is in an idle, settled state. No animation is in progress.
     */
    public static final int STATE_IDLE = ViewDragHelper.STATE_IDLE;

    /**
     * Indicates that the drawer is currently being dragged by the user.
     */
    public static final int STATE_DRAGGING = ViewDragHelper.STATE_DRAGGING;

    /**
     * Indicates that the drawer is in the process of settling to a final position.
     */
    public static final int STATE_SETTLING = ViewDragHelper.STATE_SETTLING;

    /**
     * Enumeration of possible drawer states.
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(Scope.LIBRARY_GROUP)
    @IntDef({STATE_IDLE, STATE_DRAGGING, STATE_SETTLING})
    public @interface DrawerState {}

    private final ViewGroup mPeekContainer;
    private final ImageView mPeekIcon;
    private View mContent;
    private WearableDrawerController mController;
    /**
     * Vertical offset of the drawer. Ranges from 0 (closed) to 1 (opened)
     */
    private float mOpenedPercent;
    /**
     * True if the drawer's position cannot be modified by the user. This includes edge dragging,
     * view dragging, and scroll based auto-peeking.
     */
    private boolean mIsLocked = false;
    private boolean mCanAutoPeek = true;
    private boolean mLockWhenClosed = false;
    private boolean mOpenOnlyAtTop = false;
    private boolean mPeekOnScrollDown = false;
    private boolean mIsPeeking;
    @DrawerState private int mDrawerState;
    @IdRes private int mPeekResId = 0;
    @IdRes private int mContentResId = 0;
    public WearableDrawerView(Context context) {
        this(context, null);
    }

    public WearableDrawerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableDrawerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WearableDrawerView(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        LayoutInflater.from(context).inflate(R.layout.ws_wearable_drawer_view, this, true);

        setClickable(true);
        setElevation(context.getResources()
                .getDimension(R.dimen.ws_wearable_drawer_view_elevation));

        mPeekContainer = (ViewGroup) findViewById(R.id.ws_drawer_view_peek_container);
        mPeekIcon = (ImageView) findViewById(R.id.ws_drawer_view_peek_icon);

        mPeekContainer.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onPeekContainerClicked(v);
                    }
                });

        parseAttributes(context, attrs, defStyleAttr, defStyleRes);
    }

    private static Drawable getDrawable(
            Context context, TypedArray typedArray, @StyleableRes int index) {
        Drawable background;
        int backgroundResId =
                typedArray.getResourceId(index, 0);
        if (backgroundResId == 0) {
            background = typedArray.getDrawable(index);
        } else {
            background = context.getDrawable(backgroundResId);
        }
        return background;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Drawer content is added after the peek view, so we need to bring the peek view
        // to the front so it shows on top of the content.
        mPeekContainer.bringToFront();
    }

    /**
     * Called when anything within the peek container is clicked. However, if a custom peek view is
     * supplied and it handles the click, then this may not be called. The default behavior is to
     * open the drawer.
     */
    public void onPeekContainerClicked(View v) {
        mController.openDrawer();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // The peek view has a layout gravity of bottom for the top drawer, and a layout gravity
        // of top for the bottom drawer. This is required so that the peek view shows. On the top
        // drawer, the bottom peeks from the top, and on the bottom drawer, the top peeks.
        // LayoutParams are not guaranteed to return a non-null value until a child is attached to
        // the window.
        LayoutParams peekParams = (LayoutParams) mPeekContainer.getLayoutParams();
        if (!Gravity.isVertical(peekParams.gravity)) {
            final boolean isTopDrawer =
                    (((LayoutParams) getLayoutParams()).gravity & Gravity.VERTICAL_GRAVITY_MASK)
                            == Gravity.TOP;
            if (isTopDrawer) {
                peekParams.gravity = Gravity.BOTTOM;
                mPeekIcon.setImageResource(R.drawable.ws_ic_more_horiz_24dp_wht);
            } else {
                peekParams.gravity = Gravity.TOP;
                mPeekIcon.setImageResource(R.drawable.ws_ic_more_vert_24dp_wht);
            }
            mPeekContainer.setLayoutParams(peekParams);
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        @IdRes int childId = child.getId();
        if (childId != 0) {
            if (childId == mPeekResId) {
                setPeekContent(child, index, params);
                return;
            }
            if (childId == mContentResId && !setDrawerContentWithoutAdding(child)) {
                return;
            }
        }

        super.addView(child, index, params);
    }

    int preferGravity() {
        return Gravity.NO_GRAVITY;
    }

    ViewGroup getPeekContainer() {
        return mPeekContainer;
    }

    void setDrawerController(WearableDrawerController controller) {
        mController = controller;
    }

    /**
     * Returns the drawer content view.
     */
    @Nullable
    public View getDrawerContent() {
        return mContent;
    }

    /**
     * Set the drawer content view.
     *
     * @param content The view to show when the drawer is open, or {@code null} if it should not
     * open.
     */
    public void setDrawerContent(@Nullable View content) {
        if (setDrawerContentWithoutAdding(content)) {
            addView(content);
        }
    }

    /**
     * Set the peek content view.
     *
     * @param content The view to show when the drawer peeks.
     */
    public void setPeekContent(View content) {
        ViewGroup.LayoutParams layoutParams = content.getLayoutParams();
        setPeekContent(
                content,
                -1 /* index */,
                layoutParams != null ? layoutParams : generateDefaultLayoutParams());
    }

    /**
     * Called when the drawer has settled in a completely open state. The drawer is interactive at
     * this point. This is analogous to {@link
     * WearableDrawerLayout.DrawerStateCallback#onDrawerOpened}.
     */
    public void onDrawerOpened() {}

    /**
     * Called when the drawer has settled in a completely closed state. This is analogous to {@link
     * WearableDrawerLayout.DrawerStateCallback#onDrawerClosed}.
     */
    public void onDrawerClosed() {}

    /**
     * Called when the drawer state changes. This is analogous to {@link
     * WearableDrawerLayout.DrawerStateCallback#onDrawerStateChanged}.
     *
     * @param state one of {@link #STATE_DRAGGING}, {@link #STATE_SETTLING}, or {@link #STATE_IDLE}
     */
    public void onDrawerStateChanged(@DrawerState int state) {}

    /**
     * Only allow the user to open this drawer when at the top of the scrolling content. If there is
     * no scrolling content, then this has no effect. Defaults to {@code false}.
     */
    public void setOpenOnlyAtTopEnabled(boolean openOnlyAtTop) {
        mOpenOnlyAtTop = openOnlyAtTop;
    }

    /**
     * Returns whether this drawer may only be opened by the user when at the top of the scrolling
     * content. If there is no scrolling content, then this has no effect. Defaults to {@code
     * false}.
     */
    public boolean isOpenOnlyAtTopEnabled() {
        return mOpenOnlyAtTop;
    }

    /**
     * Sets whether or not this drawer should peek while scrolling down. This is currently only
     * supported for bottom drawers. Defaults to {@code false}.
     */
    public void setPeekOnScrollDownEnabled(boolean peekOnScrollDown) {
        mPeekOnScrollDown = peekOnScrollDown;
    }

    /**
     * Gets whether or not this drawer should peek while scrolling down. This is currently only
     * supported for bottom drawers. Defaults to {@code false}.
     */
    public boolean isPeekOnScrollDownEnabled() {
        return mPeekOnScrollDown;
    }

    /**
     * Sets whether this drawer should be locked when the user cannot see it.
     * @see #isLocked
     */
    public void setLockedWhenClosed(boolean locked) {
        mLockWhenClosed = locked;
    }

    /**
     * Returns true if this drawer should be locked when the user cannot see it.
     * @see #isLocked
     */
    public boolean isLockedWhenClosed() {
        return mLockWhenClosed;
    }

    /**
     * Returns the current drawer state, which will be one of {@link #STATE_DRAGGING}, {@link
     * #STATE_SETTLING}, or {@link #STATE_IDLE}
     */
    @DrawerState
    public int getDrawerState() {
        return mDrawerState;
    }

    /**
     * Sets the {@link DrawerState}.
     */
    void setDrawerState(@DrawerState int drawerState) {
        mDrawerState = drawerState;
    }

    /**
     * Returns whether the drawer is either peeking or the peek view is animating open.
     */
    public boolean isPeeking() {
        return mIsPeeking;
    }

    /**
     * Returns true if this drawer has auto-peeking enabled. This will always return {@code false}
     * for a locked drawer.
     */
    public boolean isAutoPeekEnabled() {
        return mCanAutoPeek && !mIsLocked;
    }

    /**
     * Sets whether or not the drawer can automatically adjust its peek state. Note that locked
     * drawers will never auto-peek, but their {@code isAutoPeekEnabled} state will be maintained
     * through a lock/unlock cycle.
     */
    public void setIsAutoPeekEnabled(boolean canAutoPeek) {
        mCanAutoPeek = canAutoPeek;
    }

    /**
     * Returns true if the position of the drawer cannot be modified by user interaction.
     * Specifically, a drawer cannot be opened, closed, or automatically peeked by {@link
     * WearableDrawerLayout}. However, it can be explicitly opened, closed, and peeked by the
     * developer. A drawer may be considered locked if the drawer is locked open, locked closed, or
     * is closed and {@link #isLockedWhenClosed} returns true.
     */
    public boolean isLocked() {
        return mIsLocked || (isLockedWhenClosed() && mOpenedPercent <= 0);
    }

    /**
     * Sets whether or not the position of the drawer can be modified by user interaction.
     * @see #isLocked
     */
    public void setIsLocked(boolean locked) {
        mIsLocked = locked;
    }

    /**
     * Returns true if the drawer is fully open.
     */
    public boolean isOpened() {
        return mOpenedPercent == 1;
    }

    /**
     * Returns true if the drawer is fully closed.
     */
    public boolean isClosed() {
        return mOpenedPercent == 0;
    }

    /**
     * Returns the {@link WearableDrawerController} associated with this {@link WearableDrawerView}.
     * This will only be valid after this {@code View} has been added to its parent.
     */
    public WearableDrawerController getController() {
        return mController;
    }

    /**
     * Sets whether the drawer is either peeking or the peek view is animating open.
     */
    void setIsPeeking(boolean isPeeking) {
        mIsPeeking = isPeeking;
    }

    /**
     * Returns the percent the drawer is open, from 0 (fully closed) to 1 (fully open).
     */
    float getOpenedPercent() {
        return mOpenedPercent;
    }

    /**
     * Sets the percent the drawer is open, from 0 (fully closed) to 1 (fully open).
     */
    void setOpenedPercent(float openedPercent) {
        mOpenedPercent = openedPercent;
    }

    private void parseAttributes(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (attrs == null) {
            return;
        }

        TypedArray typedArray =
                context.obtainStyledAttributes(
                        attrs, R.styleable.WearableDrawerView, defStyleAttr,
                        R.style.Widget_Wear_WearableDrawerView);

        Drawable background =
                getDrawable(context, typedArray, R.styleable.WearableDrawerView_android_background);
        int elevation = typedArray
                .getDimensionPixelSize(R.styleable.WearableDrawerView_android_elevation, 0);
        setBackground(background);
        setElevation(elevation);

        mContentResId = typedArray.getResourceId(R.styleable.WearableDrawerView_drawerContent, 0);
        mPeekResId = typedArray.getResourceId(R.styleable.WearableDrawerView_peekView, 0);
        mCanAutoPeek =
                typedArray.getBoolean(R.styleable.WearableDrawerView_enableAutoPeek, mCanAutoPeek);
        typedArray.recycle();
    }

    private void setPeekContent(View content, int index, ViewGroup.LayoutParams params) {
        if (content == null) {
            return;
        }
        if (mPeekContainer.getChildCount() > 0) {
            mPeekContainer.removeAllViews();
        }
        mPeekContainer.addView(content, index, params);
    }

    /**
     * @return {@code true} if this is a new and valid {@code content}.
     */
    private boolean setDrawerContentWithoutAdding(View content) {
        if (content == mContent) {
            return false;
        }
        if (mContent != null) {
            removeView(mContent);
        }

        mContent = content;
        return mContent != null;
    }
}
