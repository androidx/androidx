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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IRendererCallback;
import androidx.car.app.utils.ThreadUtils;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * The view model to keep track of the CarAppActivity data.
 *
 * This main role of this class is to extent the life of a service connection beyond the regular
 * lifecycle of an activity. This is done by making sure the unbind happens when the view model
 * clears instead of when the activity calls onDestroy.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class CarAppViewModel extends AndroidViewModel implements
        ServiceConnectionManager.ServiceConnectionListener {
    private final MutableLiveData<ErrorHandler.ErrorType> mError = new MutableLiveData<>();
    private final MutableLiveData<State> mState = new MutableLiveData<>(State.IDLE);
    private ServiceConnectionManager mServiceConnectionManager;
    @Nullable private IRendererCallback mIRendererCallback;
    @Nullable private Activity mActivity;

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

    public CarAppViewModel(@NonNull Application application, @NonNull ComponentName componentName) {
        super(application);

        mServiceConnectionManager = new ServiceConnectionManager(application, componentName, this);
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

    /** Updates the activity hosting this view model */
    void setActivity(@NonNull Activity activity) {
        mActivity = activity;
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
        mState.postValue(State.CONNECTING);
        mError.postValue(null);
        mServiceConnectionManager.bind(intent, iCarAppActivity, displayId);
    }

    /** Closes the connection to the renderer service if any. */
    void unbind() {
        mServiceConnectionManager.unbind();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mIRendererCallback != null) {
            getServiceDispatcher().dispatch("onDestroyed", mIRendererCallback::onDestroyed);
        }
        mState.postValue(State.IDLE);
        unbind();
    }

    /**
     * Returns a {@link LiveData} of the current error, or null if no error is present at the
     * moment. Only relevant if {@link #getState()} is in state {@link State#ERROR}.
     */
    @NonNull
    public LiveData<ErrorHandler.ErrorType> getError() {
        return mError;
    }

    /**
     * Returns a {@link LiveData} of the state of the connection to the host
     */
    @NonNull
    public LiveData<State> getState() {
        return mState;
    }

    /**
     * Notifies of an error condition to be displayed to the user. While the error is presented,
     * the {@link CarAppActivity} will be disconnected from the host service.
     */
    @Override
    public void onError(@NonNull ErrorHandler.ErrorType errorCode) {
        ThreadUtils.runOnMain(() -> {
            ErrorHandler.ErrorType currentErrorCode = mError.getValue();
            if (currentErrorCode == ErrorHandler.ErrorType.HOST_CONNECTION_LOST
                    && errorCode == ErrorHandler.ErrorType.HOST_INCOMPATIBLE) {
                // Ignore. We receive this spurious report, which affects negatively the error
                // displayed to the user.
                return;
            }
            mError.setValue(errorCode);
        });
        mState.postValue(State.ERROR);
        unbind();
    }

    /**
     * Notifies that {@link CarAppActivity} is successfully bound to the host service.
     */
    @SuppressWarnings("NullAway")
    @Override
    public void onConnect() {
        mState.postValue(State.CONNECTED);
        mError.postValue(null);
    }

    /** Attempts to rebind to the host service */
    @SuppressWarnings("NullAway")
    public void retryBinding() {
        requireNonNull(mActivity);
        mState.postValue(State.CONNECTING);
        mError.postValue(null);
        mActivity.recreate();
    }

    /** Host update detected */
    public void onHostUpdated() {
        if (mError.getValue() != null) {
            retryBinding();
        }
    }
}
