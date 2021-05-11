/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.wear.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.wear.utils.WearableNavigationHelper;

import java.util.ArrayList;

/**
 * Special FrameLayout that is dismissible by the Back button press, and by left to right swipe
 * when the SwipeToDismiss is been enabled on the device config or themes.
 */
@UiThread
public class DismissibleFrameLayout extends FrameLayout {
    private static final String TAG = "DismissibleFrameLayout";

    /**
     * Implement this callback to act on particular stage of the dismissal.
     */
    @UiThread
    public abstract static class Callback {

        /**
         * Notifies listeners the dismissal is started.
         * @param layout The layout associated with this callback.
         */
        public void onDismissStarted(@NonNull DismissibleFrameLayout layout) {
        }

        /**
         * Notifies listeners the dismissal is canceled.
         *
         * Note, only valid on swipe-to-dismiss gesture. Dismissal triggered by back button press
         * can not be canceled.
         * @param layout The layout associated with this callback.
         */
        public void onDismissCanceled(@NonNull DismissibleFrameLayout layout) {
        }

        /**
         * Notifies listeners the dismissal is complete and the view now off screen.
         *
         * @param layout The layout associated with this callback.
         */
        public void onDismissFinished(@NonNull DismissibleFrameLayout layout) {
        }
    }

    private final Context mContext;
    private SwipeDismissController mSwipeDismissController = null;
    private BackButtonDismissController mBackButtonDismissController = null;
    private final MyDismissListener mDismissListener = new MyDismissListener();
    final ArrayList<Callback> mCallbacks = new ArrayList<>();

    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The {@link Context} the view is running in, through which it can access the
     *                current theme, resources, etc.
     */
    public DismissibleFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called when a view is
     * being constructed from an XML file, supplying attributes that were specified in the XML file.
     * This version uses a default style of 0, so the only attribute values applied are those in the
     * Context's Theme and the given AttributeSet.
     *
     * <p>
     *
     * <p>The method onFinishInflate() will be called after all children have been added.
     *
     * @param context The {@link Context} the view is running in, through which it can access the
     *                current theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public DismissibleFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     * This constructor allows subclasses to use their own base style when they are inflating.
     *
     * @param context  The {@link Context} the view is running in, through which it can access the
     *                 current theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style
     *                 resource that supplies default values for the view. Can be 0 to not look for
     *                 defaults.
     */
    public DismissibleFrameLayout(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a theme attribute.
     * This constructor allows subclasses to use their own base style when they are inflating.
     *
     * @param context  The {@link Context} the view is running in, through which it can access the
     *                 current theme, resources, etc.
     * @param attrs    The attributes of the XML tag that is inflating the view.
     * @param defStyle An attribute in the current theme that contains a reference to a style
     *                 resource that supplies default values for the view. Can be 0 to not look for
     *                 defaults.
     * @param defStyleRes It allows a style resource to be specified when creating the view.
     */
    public DismissibleFrameLayout(@NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyle,
            int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);

        mContext = context;

        setSwipeDismissible(WearableNavigationHelper.isSwipeToDismissEnabled(context));
        setBackButtonDismissible(false);
    }

    /** Registers a callback for dismissal. */
    @UiThread
    public final void registerCallback(@NonNull Callback callback) {
        mCallbacks.add(callback);
    }

    /** Removes a callback that was added with {@link #registerCallback(Callback)}. */
    @UiThread
    public final void unregisterCallback(@NonNull Callback callback) {
        if (!mCallbacks.remove(callback)) {
            throw new IllegalStateException("removeCallback called with nonexistent callback");
        }
    }

    /**
     * Sets the frame layout to be swipe dismissible or not.
     *
     * @param swipeDismissible whether the layout should react to the swipe gesture
     */
    public final void setSwipeDismissible(boolean swipeDismissible) {
        if (swipeDismissible) {
            if (mSwipeDismissController == null) {
                mSwipeDismissController = new SwipeDismissController(mContext, this);
                mSwipeDismissController.setOnDismissListener(mDismissListener);
            }
        } else if (mSwipeDismissController != null) {
            mSwipeDismissController.setOnDismissListener(null);
            mSwipeDismissController = null;
        }
    }

    /** Returns true if the frame layout can be dismissed by swipe gestures. */
    public boolean isDismissableBySwipe() {
        return mSwipeDismissController != null;
    }

    /**
     * Sets the frame layout to be back button dismissible or not.
     * @param backButtonDismissible boolean value to enable/disable the back button dismiss
     */
    public final void setBackButtonDismissible(boolean backButtonDismissible) {
        if (backButtonDismissible) {
            if (mBackButtonDismissController == null) {
                mBackButtonDismissController = new BackButtonDismissController(mContext, this);
                mBackButtonDismissController.setOnDismissListener(mDismissListener);
            }
        } else if (mBackButtonDismissController != null) {
            mBackButtonDismissController.disable(this);
            mBackButtonDismissController = null;
        }
    }

    /** Returns true if the frame layout would be dismissed with back button click */
    public boolean isDismissableByBackButton()  {
        return mBackButtonDismissController != null;
    }

    @Nullable
    SwipeDismissController getSwipeDismissController() {
        return mSwipeDismissController;
    }

    protected void performDismissFinishedCallbacks() {
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            mCallbacks.get(i).onDismissFinished(this);
        }
    }

    protected void performDismissStartedCallbacks() {
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            mCallbacks.get(i).onDismissStarted(this);
        }
    }

    protected void performDismissCanceledCallbacks() {
        for (int i = mCallbacks.size() - 1; i >= 0; i--) {
            mCallbacks.get(i).onDismissCanceled(this);
        }
    }

    private final class MyDismissListener implements DismissController.OnDismissListener {
        MyDismissListener() {
        }

        @Override
        public void onDismissStarted() {
            performDismissStartedCallbacks();
        }

        @Override
        public void onDismissCanceled() {
            performDismissCanceledCallbacks();
        }

        @Override
        public void onDismissed() {
            performDismissFinishedCallbacks();
        }
    }

    /**
     * Following methods overriding are only required with swipe-to-dismiss
     * to handle touch event for detect swipe gesture.
     */

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (mSwipeDismissController != null) {
            mSwipeDismissController.requestDisallowInterceptTouchEvent(disallowIntercept);
        } else {
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        if (mSwipeDismissController != null) {
            return mSwipeDismissController.onInterceptTouchEvent(ev);
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        if (mSwipeDismissController != null) {
            return mSwipeDismissController.canScrollHorizontally(direction);
        }
        return super.canScrollHorizontally(direction);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        if (mSwipeDismissController != null
                && mSwipeDismissController.onTouchEvent(ev)) {
            return true;
        }
        return super.onTouchEvent(ev);
    }
}

