/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.app;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.mediarouter.R;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.Toast;

/**
 * The media route button allows the user to select routes and to control the
 * currently selected route.
 * <p>
 * The application must specify the kinds of routes that the user should be allowed
 * to select by specifying a {@link MediaRouteSelector selector} with the
 * {@link #setRouteSelector} method.
 * </p><p>
 * When the default route is selected or when the currently selected route does not
 * match the {@link #getRouteSelector() selector}, the button will appear in
 * an inactive state indicating that the application is not connected to a
 * route of the kind that it wants to use.  Clicking on the button opens
 * a {@link MediaRouteChooserDialog} to allow the user to select a route.
 * If no non-default routes match the selector and it is not possible for an active
 * scan to discover any matching routes, then the button is disabled and cannot
 * be clicked.
 * </p><p>
 * When a non-default route is selected that matches the selector, the button will
 * appear in an active state indicating that the application is connected
 * to a route of the kind that it wants to use.  The button may also appear
 * in an intermediary connecting state if the route is in the process of connecting
 * to the destination but has not yet completed doing so.  In either case, clicking
 * on the button opens a {@link MediaRouteControllerDialog} to allow the user
 * to control or disconnect from the current route.
 * </p>
 *
 * <h3>Prerequisites</h3>
 * <p>
 * To use the media route button, the activity must be a subclass of
 * {@link FragmentActivity} from the <code>android.support.v4</code>
 * support library.  Refer to support library documentation for details.
 * </p>
 *
 * @see MediaRouteActionProvider
 * @see #setRouteSelector
 */
public class MediaRouteButton extends View {
    private static final String TAG = "MediaRouteButton";

    private static final String CHOOSER_FRAGMENT_TAG =
            "android.support.v7.mediarouter:MediaRouteChooserDialogFragment";
    private static final String CONTROLLER_FRAGMENT_TAG =
            "android.support.v7.mediarouter:MediaRouteControllerDialogFragment";

    private final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;

    private MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;
    private MediaRouteDialogFactory mDialogFactory = MediaRouteDialogFactory.getDefault();

    private boolean mAttachedToWindow;

    private Drawable mRemoteIndicator;
    private boolean mRemoteActive;
    private boolean mCheatSheetEnabled;
    private boolean mIsConnecting;

    private int mMinWidth;
    private int mMinHeight;

    // The checked state is used when connected to a remote route.
    private static final int[] CHECKED_STATE_SET = {
        android.R.attr.state_checked
    };

    // The checkable state is used while connecting to a remote route.
    private static final int[] CHECKABLE_STATE_SET = {
        android.R.attr.state_checkable
    };

    public MediaRouteButton(Context context) {
        this(context, null);
    }

    public MediaRouteButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.mediaRouteButtonStyle);
    }

    public MediaRouteButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(MediaRouterThemeHelper.createThemedContext(context), attrs, defStyleAttr);
        context = getContext();

        mRouter = MediaRouter.getInstance(context);
        mCallback = new MediaRouterCallback();

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MediaRouteButton, defStyleAttr, 0);
        setRemoteIndicatorDrawable(a.getDrawable(
                R.styleable.MediaRouteButton_externalRouteEnabledDrawable));
        mMinWidth = a.getDimensionPixelSize(
                R.styleable.MediaRouteButton_android_minWidth, 0);
        mMinHeight = a.getDimensionPixelSize(
                R.styleable.MediaRouteButton_android_minHeight, 0);
        a.recycle();

        setClickable(true);
        setLongClickable(true);
    }

    /**
     * Gets the media route selector for filtering the routes that the user can
     * select using the media route chooser dialog.
     *
     * @return The selector, never null.
     */
    @NonNull
    public MediaRouteSelector getRouteSelector() {
        return mSelector;
    }

    /**
     * Sets the media route selector for filtering the routes that the user can
     * select using the media route chooser dialog.
     *
     * @param selector The selector, must not be null.
     */
    public void setRouteSelector(MediaRouteSelector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }

        if (!mSelector.equals(selector)) {
            if (mAttachedToWindow) {
                if (!mSelector.isEmpty()) {
                    mRouter.removeCallback(mCallback);
                }
                if (!selector.isEmpty()) {
                    mRouter.addCallback(selector, mCallback);
                }
            }
            mSelector = selector;
            refreshRoute();
        }
    }

    /**
     * Gets the media route dialog factory to use when showing the route chooser
     * or controller dialog.
     *
     * @return The dialog factory, never null.
     */
    @NonNull
    public MediaRouteDialogFactory getDialogFactory() {
        return mDialogFactory;
    }

    /**
     * Sets the media route dialog factory to use when showing the route chooser
     * or controller dialog.
     *
     * @param factory The dialog factory, must not be null.
     */
    public void setDialogFactory(@NonNull MediaRouteDialogFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("factory must not be null");
        }

        mDialogFactory = factory;
    }

    /**
     * Show the route chooser or controller dialog.
     * <p>
     * If the default route is selected or if the currently selected route does
     * not match the {@link #getRouteSelector selector}, then shows the route chooser dialog.
     * Otherwise, shows the route controller dialog to offer the user
     * a choice to disconnect from the route or perform other control actions
     * such as setting the route's volume.
     * </p><p>
     * The application can customize the dialogs by calling {@link #setDialogFactory}
     * to provide a customized dialog factory.
     * </p>
     *
     * @return True if the dialog was actually shown.
     *
     * @throws IllegalStateException if the activity is not a subclass of
     * {@link FragmentActivity}.
     */
    public boolean showDialog() {
        if (!mAttachedToWindow) {
            return false;
        }

        final FragmentManager fm = getFragmentManager();
        if (fm == null) {
            throw new IllegalStateException("The activity must be a subclass of FragmentActivity");
        }

        MediaRouter.RouteInfo route = mRouter.getSelectedRoute();
        if (route.isDefault() || !route.matchesSelector(mSelector)) {
            if (fm.findFragmentByTag(CHOOSER_FRAGMENT_TAG) != null) {
                Log.w(TAG, "showDialog(): Route chooser dialog already showing!");
                return false;
            }
            MediaRouteChooserDialogFragment f =
                    mDialogFactory.onCreateChooserDialogFragment();
            f.setRouteSelector(mSelector);
            f.show(fm, CHOOSER_FRAGMENT_TAG);
        } else {
            if (fm.findFragmentByTag(CONTROLLER_FRAGMENT_TAG) != null) {
                Log.w(TAG, "showDialog(): Route controller dialog already showing!");
                return false;
            }
            MediaRouteControllerDialogFragment f =
                    mDialogFactory.onCreateControllerDialogFragment();
            f.show(fm, CONTROLLER_FRAGMENT_TAG);
        }
        return true;
    }

    private FragmentManager getFragmentManager() {
        Activity activity = getActivity();
        if (activity instanceof FragmentActivity) {
            return ((FragmentActivity)activity).getSupportFragmentManager();
        }
        return null;
    }

    private Activity getActivity() {
        // Gross way of unwrapping the Activity so we can get the FragmentManager
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }

    /**
     * Sets whether to enable showing a toast with the content descriptor of the
     * button when the button is long pressed.
     */
    void setCheatSheetEnabled(boolean enable) {
        mCheatSheetEnabled = enable;
    }

    @Override
    public boolean performClick() {
        // Send the appropriate accessibility events and call listeners
        boolean handled = super.performClick();
        if (!handled) {
            playSoundEffect(SoundEffectConstants.CLICK);
        }
        return showDialog() || handled;
    }

    @Override
    public boolean performLongClick() {
        if (super.performLongClick()) {
            return true;
        }

        if (!mCheatSheetEnabled) {
            return false;
        }

        final CharSequence contentDesc = getContentDescription();
        if (TextUtils.isEmpty(contentDesc)) {
            // Don't show the cheat sheet if we have no description
            return false;
        }

        final int[] screenPos = new int[2];
        final Rect displayFrame = new Rect();
        getLocationOnScreen(screenPos);
        getWindowVisibleDisplayFrame(displayFrame);

        final Context context = getContext();
        final int width = getWidth();
        final int height = getHeight();
        final int midy = screenPos[1] + height / 2;
        final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

        Toast cheatSheet = Toast.makeText(context, contentDesc, Toast.LENGTH_SHORT);
        if (midy < displayFrame.height()) {
            // Show along the top; follow action buttons
            cheatSheet.setGravity(Gravity.TOP | GravityCompat.END,
                    screenWidth - screenPos[0] - width / 2, height);
        } else {
            // Show along the bottom center
            cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
        }
        cheatSheet.show();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        return true;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);

        // Technically we should be handling this more completely, but these
        // are implementation details here. Checkable is used to express the connecting
        // drawable state and it's mutually exclusive with check for the purposes
        // of state selection here.
        if (mIsConnecting) {
            mergeDrawableStates(drawableState, CHECKABLE_STATE_SET);
        } else if (mRemoteActive) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (mRemoteIndicator != null) {
            int[] myDrawableState = getDrawableState();
            mRemoteIndicator.setState(myDrawableState);
            invalidate();
        }
    }

    /**
     * Sets a drawable to use as the remote route indicator.
     */
    public void setRemoteIndicatorDrawable(Drawable d) {
        if (mRemoteIndicator != null) {
            mRemoteIndicator.setCallback(null);
            unscheduleDrawable(mRemoteIndicator);
        }
        mRemoteIndicator = d;
        if (d != null) {
            d.setCallback(this);
            d.setState(getDrawableState());
            d.setVisible(getVisibility() == VISIBLE, false);
        }

        refreshDrawableState();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mRemoteIndicator;
    }

    //@Override defined in v11
    public void jumpDrawablesToCurrentState() {
        // We can't call super to handle the background so we do it ourselves.
        //super.jumpDrawablesToCurrentState();
        if (getBackground() != null) {
            DrawableCompat.jumpToCurrentState(getBackground());
        }

        // Handle our own remote indicator.
        if (mRemoteIndicator != null) {
            DrawableCompat.jumpToCurrentState(mRemoteIndicator);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        if (mRemoteIndicator != null) {
            mRemoteIndicator.setVisible(getVisibility() == VISIBLE, false);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        mAttachedToWindow = true;
        if (!mSelector.isEmpty()) {
            mRouter.addCallback(mSelector, mCallback);
        }
        refreshRoute();
    }

    @Override
    public void onDetachedFromWindow() {
        mAttachedToWindow = false;
        if (!mSelector.isEmpty()) {
            mRouter.removeCallback(mCallback);
        }

        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        final int minWidth = Math.max(mMinWidth,
                mRemoteIndicator != null ? mRemoteIndicator.getIntrinsicWidth() : 0);
        final int minHeight = Math.max(mMinHeight,
                mRemoteIndicator != null ? mRemoteIndicator.getIntrinsicHeight() : 0);

        int width;
        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                width = widthSize;
                break;
            case MeasureSpec.AT_MOST:
                width = Math.min(widthSize, minWidth + getPaddingLeft() + getPaddingRight());
                break;
            default:
            case MeasureSpec.UNSPECIFIED:
                width = minWidth + getPaddingLeft() + getPaddingRight();
                break;
        }

        int height;
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                height = heightSize;
                break;
            case MeasureSpec.AT_MOST:
                height = Math.min(heightSize, minHeight + getPaddingTop() + getPaddingBottom());
                break;
            default:
            case MeasureSpec.UNSPECIFIED:
                height = minHeight + getPaddingTop() + getPaddingBottom();
                break;
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mRemoteIndicator != null) {
            final int left = getPaddingLeft();
            final int right = getWidth() - getPaddingRight();
            final int top = getPaddingTop();
            final int bottom = getHeight() - getPaddingBottom();

            final int drawWidth = mRemoteIndicator.getIntrinsicWidth();
            final int drawHeight = mRemoteIndicator.getIntrinsicHeight();
            final int drawLeft = left + (right - left - drawWidth) / 2;
            final int drawTop = top + (bottom - top - drawHeight) / 2;

            mRemoteIndicator.setBounds(drawLeft, drawTop,
                    drawLeft + drawWidth, drawTop + drawHeight);
            mRemoteIndicator.draw(canvas);
        }
    }

    private void refreshRoute() {
        if (mAttachedToWindow) {
            final MediaRouter.RouteInfo route = mRouter.getSelectedRoute();
            final boolean isRemote = !route.isDefault() && route.matchesSelector(mSelector);
            final boolean isConnecting = isRemote && route.isConnecting();

            boolean needsRefresh = false;
            if (mRemoteActive != isRemote) {
                mRemoteActive = isRemote;
                needsRefresh = true;
            }
            if (mIsConnecting != isConnecting) {
                mIsConnecting = isConnecting;
                needsRefresh = true;
            }

            if (needsRefresh) {
                refreshDrawableState();
                if (mIsConnecting && mRemoteIndicator.getCurrent() instanceof AnimationDrawable) {
                    AnimationDrawable curDrawable =
                            (AnimationDrawable) mRemoteIndicator.getCurrent();
                    if (!curDrawable.isRunning()) {
                        curDrawable.start();
                    }
                }
            }

            setEnabled(mRouter.isRouteAvailable(mSelector,
                    MediaRouter.AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE));
        }
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoute();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoute();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoute();
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoute();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoute();
        }

        @Override
        public void onProviderAdded(MediaRouter router, MediaRouter.ProviderInfo provider) {
            refreshRoute();
        }

        @Override
        public void onProviderRemoved(MediaRouter router, MediaRouter.ProviderInfo provider) {
            refreshRoute();
        }

        @Override
        public void onProviderChanged(MediaRouter router, MediaRouter.ProviderInfo provider) {
            refreshRoute();
        }
    }
}
