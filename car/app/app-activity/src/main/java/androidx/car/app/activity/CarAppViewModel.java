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

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.car.app.activity.renderer.ICarAppActivity;
import androidx.car.app.activity.renderer.IRendererCallback;
import androidx.lifecycle.AndroidViewModel;
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
public class CarAppViewModel extends AndroidViewModel implements ErrorHandler {
    /** Holds the information about an error event. */
    public static class ErrorEvent {
        private final ErrorType mErrorType;
        private final Throwable mException;

        public ErrorEvent(@NonNull ErrorType errorType, @NonNull Throwable exception) {
            mErrorType = errorType;
            mException = exception;
        }

        /** Returns the type of error. */
        @NonNull ErrorType getErrorType() {
            return mErrorType;
        }

        /** Returns the exception associated with this error event. */
        @NonNull Throwable getException() {
            return mException;
        }
    }

    private final MutableLiveData<ErrorEvent> mErrorEvent = new MutableLiveData<>();
    private ServiceConnectionManager mServiceConnectionManager;
    @Nullable private IRendererCallback mIRendererCallback;

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

    /** Updates the rendeer callback. */
    void setRendererCallback(@NonNull IRendererCallback rendererCallback) {
        mIRendererCallback = rendererCallback;
    }

    /**
     * Binds to the renderer service and initializes the service if not bound already.
     *
     * Initializes the renderer service with given properties if already bound to the renderer
     * service.
     */
    void bind(@NonNull Intent intent, @NonNull ICarAppActivity iCarAppActivity,
            int displayId) {
        mServiceConnectionManager.bind(intent, iCarAppActivity, displayId);
    }

    /** Closes the connection to the renderer service if any. */
    void unbind() {
        mServiceConnectionManager.unbind();
    }

    @NonNull
    MutableLiveData<ErrorEvent> getErrorEvent() {
        return mErrorEvent;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mIRendererCallback != null) {
            mServiceConnectionManager.getServiceDispatcher()
                    .dispatch(mIRendererCallback::onDestroyed);
        }
        mServiceConnectionManager.unbind();
    }

    @Override
    public void onError(@NonNull ErrorHandler.ErrorType errorType, @NonNull Throwable exception) {
        mErrorEvent.setValue(new ErrorEvent(errorType, exception));
    }
}
