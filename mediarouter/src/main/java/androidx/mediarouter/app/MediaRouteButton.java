/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.mediarouter.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.SoundEffectConstants;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.mediarouter.R;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import java.util.ArrayList;
import java.util.List;

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
    // Used to check connectivity and hide the button
    private static ConnectivityReceiver sConnectivityReceiver;

    private final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;

    private MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;
    private MediaRouteDialogFactory mDialogFactory = MediaRouteDialogFactory.getDefault();

    private boolean mAttachedToWindow;

    private int mVisibility = VISIBLE;

    static final SparseArray<Drawable.ConstantState> sRemoteIndicatorCache =
            new SparseArray<>(2);
    RemoteIndicatorLoader mRemoteIndicatorLoader;
    private Drawable mRemoteIndicator;
    // The resource id to be lazily loaded, 0 if it doesn't need to be loaded.
    private int mRemoteIndicatorResIdToLoad;

    private static final int CONNECTION_STATE_DISCONNECTED =
            MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED;
    private static final int CONNECTION_STATE_CONNECTING =
            MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTING;
    private static final int CONNECTION_STATE_CONNECTED =
            MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED;

    private int mConnectionState;

    private ColorStateList mButtonTint;
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
        super(MediaRouterThemeHelper.createThemedButtonContext(context), attrs, defStyleAttr);
        context = getContext();

        mRouter = MediaRouter.getInstance(context);
        mCallback = new MediaRouterCallback();

        if (sConnectivityReceiver == null) {
            sConnectivityReceiver = new ConnectivityReceiver(context.getApplicationContext());
        }

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MediaRouteButton, defStyleAttr, 0);
        mButtonTint = a.getColorStateList(R.styleable.MediaRouteButton_mediaRouteButtonTint);
        mMinWidth = a.getDimensionPixelSize(
                R.styleable.MediaRouteButton_android_minWidth, 0);
        mMinHeight = a.getDimensionPixelSize(
                R.styleable.MediaRouteButton_android_minHeight, 0);

        int remoteIndicatorStaticResId = a.getResourceId(
                R.styleable.MediaRouteButton_externalRouteEnabledDrawableStatic, 0);
        mRemoteIndicatorResIdToLoad = a.getResourceId(
                R.styleable.MediaRouteButton_externalRouteEnabledDrawable, 0);
        a.recycle();

        if (mRemoteIndicatorResIdToLoad != 0) {
            Drawable.ConstantState remoteIndicatorState =
                    sRemoteIndicatorCache.get(mRemoteIndicatorResIdToLoad);
            if (remoteIndicatorState != null) {
                setRemoteIndicatorDrawable(remoteIndicatorState.newDrawable());
            }
        }
        if (mRemoteIndicator == null && remoteIndicatorStaticResId != 0) {
            Drawable.ConstantState remoteIndicatorStaticState =
                    sRemoteIndicatorCache.get(remoteIndicatorStaticResId);
            if (remoteIndicatorStaticState != null) {
                setRemoteIndicatorDrawableInternal(remoteIndicatorStaticState.newDrawable());
            } else {
                mRemoteIndicatorLoader = new RemoteIndicatorLoader(remoteIndicatorStaticResId);
                mRemoteIndicatorLoader.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
            }
        }

        updateContentDescription();
        setClickable(true);
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
        if (route.isDefaultOrBluetooth() || !route.matchesSelector(mSelector)) {
            if (fm.findFragmentByTag(CHOOSER_FRAGMENT_TAG) != null) {
                Log.w(TAG, "showDialog(): Route chooser dialog already showing!");
                return false;
            }
            MediaRouteChooserDialogFragment f =
                    mDialogFactory.onCreateChooserDialogFragment();
            f.setRouteSelector(mSelector);

            FragmentTransaction transaction = fm.beginTransaction();
            transaction.add(f, CHOOSER_FRAGMENT_TAG);
            transaction.commitAllowingStateLoss();
        } else {
            if (fm.findFragmentByTag(CONTROLLER_FRAGMENT_TAG) != null) {
                Log.w(TAG, "showDialog(): Route controller dialog already showing!");
                return false;
            }
            MediaRouteControllerDialogFragment f =
                    mDialogFactory.onCreateControllerDialogFragment();
            f.setRouteSelector(mSelector);

            FragmentTransaction transaction = fm.beginTransaction();
            transaction.add(f, CONTROLLER_FRAGMENT_TAG);
            transaction.commitAllowingStateLoss();
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
        TooltipCompat.setTooltipText(this,
                enable ? getContext().getString(R.string.mr_button_content_description) : null);
    }

    @Override
    public boolean performClick() {
        // Send the appropriate accessibility events and call listeners
        boolean handled = super.performClick();
        if (!handled) {
            playSoundEffect(SoundEffectConstants.CLICK);
        }
        loadRemoteIndicatorIfNeeded();
        return showDialog() || handled;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);

        // Technically we should be handling this more completely, but these
        // are implementation details here. Checkable is used to express the connecting
        // drawable state and it's mutually exclusive with check for the purposes
        // of state selection here.
        switch (mConnectionState) {
            case CONNECTION_STATE_CONNECTING:
                mergeDrawableStates(drawableState, CHECKABLE_STATE_SET);
                break;
            case CONNECTION_STATE_CONNECTED:
                mergeDrawableStates(drawableState, CHECKED_STATE_SET);
                break;
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
        // to prevent overwriting user-set drawables
        mRemoteIndicatorResIdToLoad = 0;
        setRemoteIndicatorDrawableInternal(d);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mRemoteIndicator;
    }

    @Override
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
        mVisibility = visibility;
        refreshVisibility();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        mAttachedToWindow = true;
        if (!mSelector.isEmpty()) {
            mRouter.addCallback(mSelector, mCallback);
        }
        refreshRoute();

        sConnectivityReceiver.registerReceiver(this);
    }

    @Override
    public void onDetachedFromWindow() {
        mAttachedToWindow = false;
        if (!mSelector.isEmpty()) {
            mRouter.removeCallback(mCallback);
        }

        sConnectivityReceiver.unregisterReceiver(this);

        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        final int width = Math.max(mMinWidth, mRemoteIndicator != null ?
                mRemoteIndicator.getIntrinsicWidth() + getPaddingLeft() + getPaddingRight() : 0);
        final int height = Math.max(mMinHeight, mRemoteIndicator != null ?
                mRemoteIndicator.getIntrinsicHeight() + getPaddingTop() + getPaddingBottom() : 0);

        int measuredWidth;
        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                measuredWidth = widthSize;
                break;
            case MeasureSpec.AT_MOST:
                measuredWidth = Math.min(widthSize, width);
                break;
            default:
            case MeasureSpec.UNSPECIFIED:
                measuredWidth = width;
                break;
        }

        int measuredHeight;
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                measuredHeight = heightSize;
                break;
            case MeasureSpec.AT_MOST:
                measuredHeight = Math.min(heightSize, height);
                break;
            default:
            case MeasureSpec.UNSPECIFIED:
                measuredHeight = height;
                break;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
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

    private void loadRemoteIndicatorIfNeeded() {
        if (mRemoteIndicatorResIdToLoad > 0) {
            if (mRemoteIndicatorLoader != null) {
                mRemoteIndicatorLoader.cancel(false);
            }
            mRemoteIndicatorLoader = new RemoteIndicatorLoader(mRemoteIndicatorResIdToLoad);
            mRemoteIndicatorResIdToLoad = 0;
            mRemoteIndicatorLoader.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    }

    void setRemoteIndicatorDrawableInternal(Drawable d) {
        if (mRemoteIndicatorLoader != null) {
            mRemoteIndicatorLoader.cancel(false);
        }

        if (mRemoteIndicator != null) {
            mRemoteIndicator.setCallback(null);
            unscheduleDrawable(mRemoteIndicator);
        }
        if (d != null) {
            if (mButtonTint != null) {
                d = DrawableCompat.wrap(d.mutate());
                DrawableCompat.setTintList(d, mButtonTint);
            }
            d.setCallback(this);
            d.setState(getDrawableState());
            d.setVisible(getVisibility() == VISIBLE, false);
        }
        mRemoteIndicator = d;

        refreshDrawableState();
        if (mAttachedToWindow && mRemoteIndicator != null
                && mRemoteIndicator.getCurrent() instanceof AnimationDrawable) {
            AnimationDrawable curDrawable = (AnimationDrawable) mRemoteIndicator.getCurrent();
            if (mConnectionState == CONNECTION_STATE_CONNECTING) {
                if (!curDrawable.isRunning()) {
                    curDrawable.start();
                }
            } else if (mConnectionState == CONNECTION_STATE_CONNECTED) {
                if (curDrawable.isRunning()) {
                    curDrawable.stop();
                }
                curDrawable.selectDrawable(curDrawable.getNumberOfFrames() - 1);
            }
        }
    }

    void refreshVisibility() {
        super.setVisibility(mVisibility == VISIBLE && !sConnectivityReceiver.isConnected()
                ? INVISIBLE : mVisibility);
        if (mRemoteIndicator != null) {
            mRemoteIndicator.setVisible(getVisibility() == VISIBLE, false);
        }
    }

    void refreshRoute() {
        final MediaRouter.RouteInfo route = mRouter.getSelectedRoute();
        final boolean isRemote = !route.isDefaultOrBluetooth() && route.matchesSelector(mSelector);
        final int connectionState = (isRemote ? route.getConnectionState()
                : CONNECTION_STATE_DISCONNECTED);

        boolean needsRefresh = false;

        if (mConnectionState != connectionState) {
            mConnectionState = connectionState;
            needsRefresh = true;
        }

        if (needsRefresh) {
            updateContentDescription();
            refreshDrawableState();
        }
        if (connectionState == CONNECTION_STATE_CONNECTING) {
            loadRemoteIndicatorIfNeeded();
        }

        if (mAttachedToWindow) {
            setEnabled(mRouter.isRouteAvailable(mSelector,
                    MediaRouter.AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE));
        }
        if (mRemoteIndicator != null
                && mRemoteIndicator.getCurrent() instanceof AnimationDrawable) {
            AnimationDrawable curDrawable = (AnimationDrawable) mRemoteIndicator.getCurrent();
            if (mAttachedToWindow) {
                if ((needsRefresh || connectionState == CONNECTION_STATE_CONNECTING)
                        && !curDrawable.isRunning()) {
                    curDrawable.start();
                }
            } else if (connectionState == CONNECTION_STATE_CONNECTED) {
                // When the route is already connected before the view is attached, show the last
                // frame of the connected animation immediately.
                if (curDrawable.isRunning()) {
                    curDrawable.stop();
                }
                curDrawable.selectDrawable(curDrawable.getNumberOfFrames() - 1);
            }
        }
    }

    private void updateContentDescription() {
        int resId;
        switch (mConnectionState) {
            case CONNECTION_STATE_CONNECTING:
                resId = R.string.mr_cast_button_connecting;
                break;
            case CONNECTION_STATE_CONNECTED:
                resId = R.string.mr_cast_button_connected;
                break;
            default:
                resId = R.string.mr_cast_button_disconnected;
                break;
        }

        setContentDescription(getContext().getString(resId));
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        MediaRouterCallback() {
        }

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

    private final class RemoteIndicatorLoader extends AsyncTask<Void, Void, Drawable> {
        private final int mResId;

        RemoteIndicatorLoader(int resId) {
            mResId = resId;
        }

        @Override
        protected Drawable doInBackground(Void... params) {
            Drawable.ConstantState remoteIndicatorState = sRemoteIndicatorCache.get(mResId);
            if (remoteIndicatorState == null) {
                return getContext().getResources().getDrawable(mResId);
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Drawable remoteIndicator) {
            if (remoteIndicator != null) {
                cacheAndReset(remoteIndicator);
            } else {
                Drawable.ConstantState remoteIndicatorState = sRemoteIndicatorCache.get(mResId);
                if (remoteIndicatorState != null) {
                    remoteIndicator = remoteIndicatorState.newDrawable();
                }
                mRemoteIndicatorLoader = null;
            }

            setRemoteIndicatorDrawableInternal(remoteIndicator);
        }

        @Override
        protected void onCancelled(Drawable remoteIndicator) {
            cacheAndReset(remoteIndicator);
        }

        private void cacheAndReset(Drawable remoteIndicator) {
            if (remoteIndicator != null) {
                sRemoteIndicatorCache.put(mResId, remoteIndicator.getConstantState());
            }
            mRemoteIndicatorLoader = null;
        }
    }

    private static final class ConnectivityReceiver extends BroadcastReceiver {
        private final Context mContext;
        // If we have no information, assume that the device is connected
        private boolean mIsConnected = true;
        private List<MediaRouteButton> mButtons;

        ConnectivityReceiver(Context context) {
            mContext = context;
            mButtons = new ArrayList<MediaRouteButton>();
        }

        public void registerReceiver(MediaRouteButton button) {
            if (mButtons.size() == 0) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                mContext.registerReceiver(this, intentFilter);
            }
            mButtons.add(button);
        }

        public void unregisterReceiver(MediaRouteButton button) {
            mButtons.remove(button);

            if (mButtons.size() == 0) {
                mContext.unregisterReceiver(this);
            }
        }

        public boolean isConnected() {
            return mIsConnected;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                boolean isConnected = !intent.getBooleanExtra(
                        ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (mIsConnected != isConnected) {
                    mIsConnected = isConnected;
                    for (MediaRouteButton button: mButtons) {
                        button.refreshVisibility();
                    }
                }
            }
        }
    }
}
