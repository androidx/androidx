/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.model.TemplateWrapper;
import androidx.car.app.utils.RemoteUtils;
import androidx.car.app.utils.ThreadUtils;

import java.util.Objects;

/** Manages the communication between the app and the host. */
public class AppManager {
    @NonNull
    private final CarContext mCarContext;
    @NonNull
    private final IAppManager.Stub mAppManager;
    @NonNull
    private final HostDispatcher mHostDispatcher;

    /**
     * Sets the {@link SurfaceCallback} to get changes and updates to the surface on which the
     * app can draw custom content, or {@code null} to reset the listener.
     *
     * <p>This call requires the {@code androidx.car.app.ACCESS_SURFACE}
     * permission to be declared.
     *
     * <p>The {@link Surface} can be used to draw custom content such as a navigation app's map.
     *
     * <p>Note that the listener relates to UI events and will be executed on the main thread
     * using {@link Looper#getMainLooper()}.
     *
     * @throws SecurityException if the app does not have the required permissions to access the
     *                           surface
     * @throws HostException     if the remote call fails
     */
    @SuppressLint("ExecutorRegistration")
    public void setSurfaceCallback(@Nullable SurfaceCallback surfaceCallback) {
        mHostDispatcher.dispatch(
                CarContext.APP_SERVICE,
                (IAppHost host) -> {
                    host.setSurfaceCallback(RemoteUtils.stubSurfaceCallback(surfaceCallback));
                    return null;
                },
                "setSurfaceListener");
    }

    /**
     * Requests the current template to be invalidated, which eventually triggers a call to {@link
     * Screen#onGetTemplate} to get the new template to display.
     *
     * @throws HostException if the remote call fails
     */
    public void invalidate() {
        mHostDispatcher.dispatch(
                CarContext.APP_SERVICE,
                (IAppHost host) -> {
                    host.invalidate();
                    return null;
                },
                "invalidate");
    }

    /**
     * Shows a toast on the car screen.
     *
     * @param text     the text to show
     * @param duration how long to display the message
     * @throws HostException if the remote call fails
     */
    public void showToast(@NonNull CharSequence text, int duration) {
        mHostDispatcher.dispatch(
                CarContext.APP_SERVICE,
                (IAppHost host) -> {
                    host.showToast(text, duration);
                    return null;
                },
                "showToast");
    }

    /** Returns the {@code IAppManager.Stub} binder. */
    IAppManager.Stub getIInterface() {
        return mAppManager;
    }

    /** Creates an instance of {@link AppManager}. */
    static AppManager create(@NonNull CarContext carContext,
            @NonNull HostDispatcher hostDispatcher) {
        Objects.requireNonNull(carContext);
        Objects.requireNonNull(hostDispatcher);

        return new AppManager(carContext, hostDispatcher);
    }

    // Strictly to avoid synthetic accessor.
    @NonNull
    CarContext getCarContext() {
        return mCarContext;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP) // Restrict to testing library
    protected AppManager(@NonNull CarContext carContext, @NonNull HostDispatcher hostDispatcher) {
        mCarContext = carContext;
        mHostDispatcher = hostDispatcher;
        mAppManager = new IAppManager.Stub() {
            @Override
            public void getTemplate(IOnDoneCallback callback) {
                ThreadUtils.runOnMain(
                        () -> {
                            TemplateWrapper templateWrapper;
                            try {
                                templateWrapper = getCarContext().getCarService(
                                        ScreenManager.class).getTopTemplate();
                            } catch (RuntimeException e) {
                                // Catch exceptions, notify the host of it, then rethrow it.
                                // This allows the host to log, and show an error to the user.
                                RemoteUtils.sendFailureResponse(callback,
                                        "getTemplate", e);
                                throw new RuntimeException(e);
                            }

                            RemoteUtils.sendSuccessResponse(callback, "getTemplate",
                                    templateWrapper);
                        });
            }

            @Override
            public void onBackPressed(IOnDoneCallback callback) {
                RemoteUtils.dispatchHostCall(
                        carContext.getOnBackPressedDispatcher()::onBackPressed, callback,
                        "onBackPressed");
            }
        };
    }
}
