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

package androidx.car.app.testing;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.ICarHost;
import androidx.car.app.IStartCarApp;
import androidx.car.app.OnRequestPermissionsCallback;
import androidx.car.app.testing.navigation.TestNavigationManager;
import androidx.car.app.utils.CollectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * The {@link CarContext} that is used for testing.
 *
 * <p>This class will return the test version of car services for tracking calls during testing.
 *
 * <p>It also allows retrieving the car services already cast to the testing class by calling {@link
 * #getCarService} with the class name of the test services:
 *
 * <pre>{@code testCarContext.getCarService(TestAppManager.class)}</pre>
 *
 * <pre>{@code testCarContext.getCarService(TestNavigationManager.class)}</pre>
 *
 * <pre>{@code testCarContext.getCarService(TestScreenManager.class)}</pre>
 *
 * <p>Allows retrieving all {@link Intent}s sent via {@link CarContext#startCarApp(Intent)}.
 */
public class TestCarContext extends CarContext {
    private final Map<String, Object> mOverriddenService = new HashMap<>();
    private final IStartCarApp mStartCarApp = new StartCarAppStub();

    private final FakeHost mFakeHost;
    private final TestLifecycleOwner mTestLifecycleOwner;
    private final TestAppManager mTestAppManager;
    private final TestNavigationManager mTestNavigationManager;
    private final TestScreenManager mTestScreenManager;

    final List<Intent> mStartCarAppIntents = new ArrayList<>();
    @Nullable
    private PermissionRequestInfo mLastPermissionRequestInfo = null;
    private boolean mHasCalledFinishCarApp;

    /** Resets the values tracked by this {@link TestCarContext}. */
    public void reset() {
        mStartCarAppIntents.clear();
    }

    @NonNull
    @Override
    public <T> T getCarService(@NonNull Class<T> serviceClass) {
        String serviceName;

        if (serviceClass.isInstance(mTestAppManager)) {
            serviceName = APP_SERVICE;
        } else if (serviceClass.isInstance(mTestNavigationManager)) {
            serviceName = NAVIGATION_SERVICE;
        } else if (serviceClass.isInstance(mTestScreenManager)) {
            serviceName = SCREEN_SERVICE;
        } else {
            serviceName = getCarServiceName(serviceClass);
        }

        return requireNonNull(serviceClass.cast(getCarService(serviceName)));
    }

    @Override
    @NonNull
    public Object getCarService(@NonNull String name) {
        Object service = mOverriddenService.get(name);
        if (service != null) {
            return service;
        }

        switch (name) {
            case CarContext.APP_SERVICE:
                return mTestAppManager;
            case CarContext.NAVIGATION_SERVICE:
                return mTestNavigationManager;
            case CarContext.SCREEN_SERVICE:
                return mTestScreenManager;
            default:
                // Fall out
        }
        return super.getCarService(name);
    }

    @Override
    public void startCarApp(@NonNull Intent intent) {
        mStartCarAppIntents.add(intent);
    }

    @Override
    public void finishCarApp() {
        mHasCalledFinishCarApp = true;
    }

    @Override
    public void requestPermissions(@NonNull List<String> permissions, @NonNull Executor executor,
            @NonNull OnRequestPermissionsCallback callback) {
        mLastPermissionRequestInfo = new PermissionRequestInfo(requireNonNull(permissions),
                requireNonNull(callback));
        super.requestPermissions(permissions, executor, callback);
    }

    /**
     * Creates a {@link TestCarContext} to use for testing.
     *
     * @throws NullPointerException if {@code testContext} is null
     */
    @SuppressLint("BanUncheckedReflection")
    @NonNull
    public static TestCarContext createCarContext(@NonNull Context testContext) {
        requireNonNull(testContext);

        TestCarContext carContext = new TestCarContext(new TestLifecycleOwner(),
                new HostDispatcher());
        carContext.attachBaseContext(testContext);

        try {
            Method method = CarContext.class.getDeclaredMethod("setCarHost", ICarHost.class);
            method.setAccessible(true);
            method.invoke(carContext, carContext.mFakeHost.getCarHost());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to attach the base context", e);
        }

        return carContext;
    }

    /**
     * Returns all {@link Intent}s sent via {@link CarContext#startCarApp(Intent)}.
     *
     * <p>The {@link Intent}s are stored in the order of when they were sent, where the first
     * intent in the list, is the first intent sent.
     *
     * <p>The results will be stored until {@link #reset} is called.
     */
    @NonNull
    public List<Intent> getStartCarAppIntents() {
        return CollectionUtils.unmodifiableCopy(mStartCarAppIntents);
    }

    /**
     * Returns a {@link PermissionRequestInfo} including the information with the last call made to
     * {@link CarContext#requestPermissions}, or {@code null} if no call was made.
     */
    @Nullable
    public PermissionRequestInfo getLastPermissionRequestInfo() {
        return mLastPermissionRequestInfo;
    }

    /** Verifies if {@link CarContext#finishCarApp} has been called. */
    public boolean hasCalledFinishCarApp() {
        return mHasCalledFinishCarApp;
    }

    /**
     * Retrieve the {@link FakeHost} being used.
     */
    @NonNull
    public FakeHost getFakeHost() {
        return mFakeHost;
    }

    /**
     * Sets the {@code service} as the service instance for the given {@code serviceClass}.
     *
     * <p>This can be used to mock a car service.
     *
     * <p>Internal use only.
     *
     * @throws NullPointerException if either {@code serviceClass} or {@code service} are {@code
     *                              null}
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public void overrideCarService(@NonNull Class<?> serviceClass, @NonNull Object service) {
        requireNonNull(service);
        requireNonNull(serviceClass);

        String serviceName = getCarServiceName(serviceClass);
        mOverriddenService.put(serviceName, service);
    }

    /**
     * Returns the {@link TestLifecycleOwner} that is used for this CarContext.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public TestLifecycleOwner getLifecycleOwner() {
        return mTestLifecycleOwner;
    }

    /**
     * Returns the {@link IStartCarApp} instance that is being used by this CarContext.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public IStartCarApp getStartCarAppStub() {
        return mStartCarApp;
    }

    ICarHost getCarHostStub() {
        return mFakeHost.getCarHost();
    }

    /** Testing version of the start car app binder for notifications. */
    class StartCarAppStub extends IStartCarApp.Stub {
        @Override
        public void startCarApp(Intent intent) {
            mStartCarAppIntents.add(intent);
        }
    }

    /**
     * A representation of a permission request including the permissions that were requested as
     * well as the callback provided.
     */
    public static class PermissionRequestInfo {
        private final List<String> mPermissionsRequested;
        private final OnRequestPermissionsCallback mCallback;

        @SuppressWarnings("ExecutorRegistration")
        PermissionRequestInfo(List<String> permissionsRequested,
                OnRequestPermissionsCallback callback) {
            mPermissionsRequested = requireNonNull(permissionsRequested);
            mCallback = requireNonNull(callback);
        }

        /**
         * Returns the callback that was provided in the permission request.
         */
        @NonNull
        public OnRequestPermissionsCallback getCallback() {
            return mCallback;
        }

        /**
         * Returns the permissions that were requested.
         */
        @NonNull
        public List<String> getPermissionsRequested() {
            return mPermissionsRequested;
        }
    }

    private TestCarContext(TestLifecycleOwner testLifecycleOwner, HostDispatcher hostDispatcher) {
        super(testLifecycleOwner.mRegistry, hostDispatcher);

        this.mFakeHost = new FakeHost(this);
        this.mTestLifecycleOwner = testLifecycleOwner;
        this.mTestAppManager = new TestAppManager(this, hostDispatcher);
        this.mTestNavigationManager = new TestNavigationManager(this, hostDispatcher);
        this.mTestScreenManager = new TestScreenManager(this);
    }
}
