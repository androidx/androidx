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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.ICarHost;
import androidx.car.app.IStartCarApp;
import androidx.car.app.testing.navigation.TestNavigationManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * <p>Allows retrieving all {@link Intent}s sent via {@link CarContext#startCarApp(Intent)} and
 * {@link CarContext#startCarApp(Intent, Intent)}.
 */
public class TestCarContext extends CarContext {
    private static TestCarContext sLatestInstance;

    private final Map<String, Object> mOverriddenService = new HashMap<>();
    private final IStartCarApp mStartCarApp = new StartCarAppStub();

    private final FakeHost mFakeHost;
    private final TestLifecycleOwner mTestLifecycleOwner;
    private final TestAppManager mTestAppManager;
    private final TestNavigationManager mTestNavigationManager;
    private final TestScreenManager mTestScreenManager;

    private final List<Intent> mStartCarAppIntents = new ArrayList<>();
    private boolean mHasCalledFinishCarApp;

    /** Resets the values tracked by this {@link TestCarContext}. */
    public void reset() {
        mStartCarAppIntents.clear();
    }

    @NonNull
    @Override
    public <T> T getCarService(@NonNull Class<T> serviceClass) {
        requireNonNull(serviceClass);
        String serviceName;

        if (serviceClass.isInstance(mTestAppManager)) {
            serviceName = APP_SERVICE;
        } else if (serviceClass.isInstance(mTestNavigationManager)) {
            serviceName = NAVIGATION_SERVICE;
        } else if (serviceClass.isInstance(mTestScreenManager)) {
            serviceName = SCREEN_MANAGER_SERVICE;
        } else {
            serviceName = getCarServiceName(serviceClass);
        }

        return requireNonNull(requireNonNull(serviceClass).cast(getCarService(serviceName)));
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
            case CarContext.SCREEN_MANAGER_SERVICE:
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

    /** Creates a {@link TestCarContext} to use for testing. */
    @NonNull
    public static TestCarContext createCarContext(@NonNull Context testContext) {
        requireNonNull(testContext);

        sLatestInstance = new TestCarContext(new TestLifecycleOwner(), new HostDispatcher());
        sLatestInstance.attachBaseContext(testContext);

        try {
            Method method = CarContext.class.getDeclaredMethod("setCarHost", ICarHost.class);
            method.setAccessible(true);
            method.invoke(sLatestInstance, sLatestInstance.mFakeHost.getCarHost());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to attach the base context", e);
        }

        return sLatestInstance;
    }

    /**
     * Retrieves all {@link Intent}s sent via {@link CarContext#startCarApp}.
     *
     * <p>The {@link Intent}s are stored in order of calls.
     *
     * <p>The results will be stored until {@link #reset} is called.
     */
    @NonNull
    public List<Intent> getStartCarAppIntents() {
        return mStartCarAppIntents;
    }

    /** Verifies if {@link CarContext#finishCarApp} has been called. */
    public boolean hasCalledFinishCarApp() {
        return mHasCalledFinishCarApp;
    }

    /**
     * Sets the {@code service} as the service instance for the given {@code serviceClass}.
     *
     * <p>This can be used to mock a car service.
     *
     * <p>Internal use only.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    public void overrideCarService(@NonNull Class<?> serviceClass, @NonNull Object service) {
        requireNonNull(service);
        requireNonNull(serviceClass);

        String serviceName = getCarServiceName(serviceClass);
        if (serviceName == null) {
            throw new IllegalArgumentException(
                    "Not an expected car service class: " + serviceClass.getName());
        }
        mOverriddenService.put(serviceName, service);
    }

    /**
     * Retrieve the last instance of TestCarContext created for internal testing purposes.
     *
     * <p>Internal use only.
     *
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Nullable
    public static TestCarContext getLatestInstance() {
        return sLatestInstance;
    }

    TestLifecycleOwner getLifecycleOwner() {
        return mTestLifecycleOwner;
    }

    FakeHost getFakeHost() {
        return mFakeHost;
    }

    IStartCarApp getStartCarAppStub() {
        return mStartCarApp;
    }

    ICarHost getCarHostStub() {
        return mFakeHost.getCarHost();
    }

    /** Testing version of the start car app binder for notifications. */
    private class StartCarAppStub extends IStartCarApp.Stub {
        @Override
        public void startCarApp(Intent intent) {
            mStartCarAppIntents.add(intent);
        }
    }

    private TestCarContext(TestLifecycleOwner testLifecycleOwner, HostDispatcher hostDispatcher) {
        super(testLifecycleOwner.mRegistry, hostDispatcher);

        this.mFakeHost = new FakeHost(this);
        this.mTestLifecycleOwner = testLifecycleOwner;
        this.mTestAppManager = new TestAppManager(this, hostDispatcher);
        this.mTestNavigationManager = new TestNavigationManager(hostDispatcher);
        this.mTestScreenManager = new TestScreenManager(this);
    }
}
