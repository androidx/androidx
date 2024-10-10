/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Insets;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.SessionInfo;
import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IInsetsListener;
import androidx.car.app.activity.renderer.IRendererCallback;
import androidx.car.app.utils.ThreadUtils;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.view.DisplayCutoutCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * The view model to keep track of the CarAppActivity data.
 *
 * This main role of this class is to extent the life of a service connection beyond the regular
 * lifecycle of an activity. This is done by making sure the unbind happens when the view model
 * clears instead of when the activity calls onDestroy.
 *
 */
@RestrictTo(LIBRARY)
public class CarAppViewModel extends AndroidViewModel implements
        ServiceConnectionManager.ServiceConnectionListener {
    private final MutableLiveData<ErrorHandler.ErrorType> mError = new MutableLiveData<>();
    private final MutableLiveData<State> mState = new MutableLiveData<>(State.IDLE);
    private ServiceConnectionManager mServiceConnectionManager;

    private @Nullable IRendererCallback mIRendererCallback;
    private @Nullable IInsetsListener mIInsetsListener;
    private @Nullable Insets mInsets = Insets.NONE;
    private @Nullable DisplayCutoutCompat mDisplayCutout;

    private static WeakReference<Activity> sActivity = new WeakReference<>(null);

    /** Possible view states */
    public enum State {
        /** The activity hasn't yet started connecting to the host */
        IDLE,
        /** This activity is still in the process of connecting to the host */
        CONNECTING,
        /** The activity is connected to the host */
        CONNECTED,
        /** There has been an error in the communication with the host */
        ERROR,
    }

    public CarAppViewModel(@NonNull Application application,
            @NonNull ComponentName componentName, @NonNull SessionInfo sessionInfo) {
        super(application);

        mServiceConnectionManager = new ServiceConnectionManager(application, componentName,
                sessionInfo, this);
    }

    @VisibleForTesting
    @NonNull ServiceConnectionManager getServiceConnectionManager() {
        return mServiceConnectionManager;
    }

    @VisibleForTesting
    void setServiceConnectionManager(ServiceConnectionManager serviceConnectionManager) {
        mServiceConnectionManager = serviceConnectionManager;
    }

    @NonNull ServiceDispatcher getServiceDispatcher() {
        return mServiceConnectionManager.getServiceDispatcher();
    }

    /** Updates the renderer callback. */
    void setRendererCallback(@NonNull IRendererCallback rendererCallback) {
        mIRendererCallback = rendererCallback;
    }

    /** Updates the activity hosting this view model. */
    void setActivity(@Nullable Activity activity) {
        sActivity = new WeakReference<>(activity);
    }

    /** Resets the internal state of this view model. */
    @SuppressWarnings("NullAway")
    void resetState() {
        mState.setValue(State.IDLE);
        mError.setValue(null);
    }

    /**
     * Binds to the renderer service and initializes the service if not bound already.
     *
     * Initializes the renderer service with given properties if already bound to the renderer
     * service.
     */
    @SuppressWarnings("NullAway")
    void bind(@NonNull Intent intent, @NonNull ICarAppActivity iCarAppActivity,
            int displayId) {
        mState.setValue(State.CONNECTING);
        mError.setValue(null);
        mServiceConnectionManager.bind(intent, iCarAppActivity, displayId);
    }

    /** Closes the connection to the renderer service if any. */
    void unbind() {
        mServiceConnectionManager.unbind();
        mIInsetsListener = null;
        mIRendererCallback = null;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mIRendererCallback != null) {
            getServiceDispatcher().dispatch("onDestroyed", mIRendererCallback::onDestroyed);
        }
        mState.setValue(State.IDLE);
        unbind();
    }

    /**
     * Returns a {@link LiveData} of the current error, or null if no error is present at the
     * moment. Only relevant if {@link #getState()} is in state {@link State#ERROR}.
     */
    public @NonNull LiveData<ErrorHandler.ErrorType> getError() {
        return mError;
    }

    /**
     * Returns a {@link LiveData} of the state of the connection to the host
     */
    public @NonNull LiveData<State> getState() {
        return mState;
    }

    /**
     * Notifies of an error condition to be displayed to the user. While the error is presented,
     * the {@link CarAppActivity} will be disconnected from the host service.
     */
    @Override
    public void onError(ErrorHandler.@NonNull ErrorType errorCode) {
        ThreadUtils.runOnMain(() -> {
            ErrorHandler.ErrorType currentErrorCode = mError.getValue();
            if (currentErrorCode == ErrorHandler.ErrorType.HOST_CONNECTION_LOST
                    && errorCode == ErrorHandler.ErrorType.HOST_INCOMPATIBLE) {
                // Ignore. We receive this spurious report, which affects negatively the error
                // displayed to the user.
                return;
            }
            mState.setValue(State.ERROR);
            mError.setValue(errorCode);
        });
        unbind();
    }

    /**
     * Notifies that {@link CarAppActivity} is successfully bound to the host service.
     */
    @SuppressWarnings("NullAway")
    @Override
    public void onConnect() {
        mState.setValue(State.CONNECTED);
        mError.setValue(null);
    }

    /** Attempts to rebind to the host service */
    @SuppressWarnings("NullAway")
    public void retryBinding() {
        Activity activity = requireNonNull(sActivity.get());
        mError.setValue(null);
        activity.recreate();
    }

    /** Host update detected */
    public void onHostUpdated() {
        if (mError.getValue() != null) {
            retryBinding();
        }
    }

    /**
     * Update the result of the {@link androidx.car.app.activity.CarAppActivity} associated with
     * this view model.
     *
     * @see Activity#setResult(int, Intent)
     */
    public static void setActivityResult(int resultCode, @Nullable Intent data) {
        Activity activity = sActivity.get();
        if (activity != null) {
            activity.setResult(resultCode, data);
        }
    }

    /**
     * Returns the activity calling this {@link CarAppActivity}
     *
     * @see Activity#getCallingActivity()
     */
    public static @Nullable ComponentName getCallingActivity() {
        Activity activity = sActivity.get();
        if (activity != null) {
            return activity.getCallingActivity();
        }
        return null;
    }

    /**
     * Updates the insets for this {@link CarAppActivity}
     *
     * @param insets latest received {@link Insets}
     * @param displayCutout latest received {@link DisplayCutoutCompat}
     */
    public void updateWindowInsets(@NonNull Insets insets,
            @Nullable DisplayCutoutCompat displayCutout) {
        if (Objects.equals(mInsets, insets) && Objects.equals(mDisplayCutout, displayCutout)) {
            return;
        }
        mInsets = insets;
        mDisplayCutout = displayCutout;
        // If listener is not set yet, the inset will be dispatched once the listener is set.
        if (mIInsetsListener != null) {
            dispatchInsetsUpdates();
        }
    }

    /**
     * Dispatches the insets updates for this {@link CarAppActivity}
     */
    @SuppressWarnings({"NullAway", "deprecation"})
    private void dispatchInsetsUpdates() {
        if (mServiceConnectionManager.getHandshakeInfo().getHostCarAppApiLevel()
                >= CarAppApiLevels.LEVEL_5) {
            getServiceDispatcher().dispatch("onWindowInsetsChanged",
                    () -> requireNonNull(mIInsetsListener).onWindowInsetsChanged(mInsets,
                            getSafeInsets(mDisplayCutout))
            );
            return;
        }
        getServiceDispatcher().dispatch("onInsetsChanged",
                () -> requireNonNull(mIInsetsListener).onInsetsChanged(mInsets));
    }

    /**
     * Updates the listener that will handle insets changes. If a non-null listener is set, it will
     * be assumed that inset changes are handled by the host, and
     * {@link #updateWindowInsets(Insets, DisplayCutoutCompat)} will return <code>false</code>
     */
    public void setInsetsListener(@Nullable IInsetsListener listener) {
        mIInsetsListener = listener;
        dispatchInsetsUpdates();
    }

    /**
     * Returns the safe insets of a given {@link DisplayCutoutCompat}.
     *
     * @param displayCutout the {@link DisplayCutoutCompat} for which the safe insets is calculated.
     */
    private Insets getSafeInsets(@Nullable DisplayCutoutCompat displayCutout) {
        return displayCutout == null ? Insets.NONE :
                Insets.of(displayCutout.getSafeInsetLeft(), displayCutout.getSafeInsetTop(),
                        displayCutout.getSafeInsetRight(),
                        displayCutout.getSafeInsetBottom());
    }
}
