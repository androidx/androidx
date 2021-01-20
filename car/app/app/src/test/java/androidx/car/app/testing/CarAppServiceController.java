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

import static java.util.Objects.requireNonNull;

import android.content.ComponentName;
import android.content.Intent;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppInfo;
import androidx.car.app.CarAppService;
import androidx.car.app.HostInfo;
import androidx.car.app.ICarApp;
import androidx.car.app.Session;
import androidx.lifecycle.Lifecycle.State;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * A controller that allows testing of a {@link CarAppService}.
 *
 * <p>This controller allows:
 *
 * <ul>
 *   <li>Sending different {@link Intent}s to the {@link CarAppService}'s {@link
 *       Session#onCreateScreen} and {@link Session#onNewIntent} methods.
 *   <li>Moving a {@link CarAppService} through its different {@link State}s.
 * </ul>
 */
public class CarAppServiceController {
    private final TestCarContext mTestCarContext;
    private final CarAppService mCarAppService;
    private final ICarApp mCarAppStub;

    /** Creates a {@link CarAppServiceController} to control the provided {@link CarAppService}. */
    public static CarAppServiceController of(
            @NonNull TestCarContext testCarContext,
            @NonNull Session session, @NonNull CarAppService carAppService) {
        return new CarAppServiceController(
                requireNonNull(carAppService), requireNonNull(session),
                requireNonNull(testCarContext));
    }

    /**
     * Initializes the {@link CarAppService} that is being controlled.
     *
     * <p>This will send an empty {@link Intent} to the {@link Session} returned from
     * {@link CarAppService#onCreateSession}.
     */
    public CarAppServiceController create() {
        return create(
                new Intent().setComponent(
                        new ComponentName(mTestCarContext, mCarAppService.getClass())));
    }

    /**
     * Initializes the {@link CarAppService} that is being controlled.
     *
     * <p>This will send the provided {@link Intent} to {@link Session#onCreateScreen}.
     */
    public CarAppServiceController create(@NonNull Intent intent) {
        Objects.requireNonNull(intent);

        try {
            mCarAppStub.onAppCreate(
                    mTestCarContext.getCarHostStub(),
                    intent,
                    mTestCarContext.getResources().getConfiguration(),
                    new TestOnDoneCallbackStub());
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to create the CarAppService", e);
        }

        return this;
    }

    /** Sends the provided {@link Intent} to the {@link CarAppService} that is being controlled. */
    public CarAppServiceController newIntent(@NonNull Intent intent) {
        Objects.requireNonNull(intent);

        try {
            mCarAppStub.onNewIntent(intent, new TestOnDoneCallbackStub());
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to send new intent to the CarAppService", e);
        }

        return this;
    }

    /**
     * Starts the {@link CarAppService} that is being controlled.
     *
     * @see Session#getLifecycle
     */
    public CarAppServiceController start() {
        try {
            mCarAppStub.onAppStart(new TestOnDoneCallbackStub());
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to start the CarAppService", e);
        }

        return this;
    }

    /**
     * Resumes the {@link CarAppService} that is being controlled.
     *
     * @see Session#getLifecycle
     */
    public CarAppServiceController resume() {
        try {
            mCarAppStub.onAppResume(new TestOnDoneCallbackStub());
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to resume the CarAppService", e);
        }

        return this;
    }

    /**
     * Pauses the {@link CarAppService} that is being controlled.
     *
     * @see Session#getLifecycle
     */
    public CarAppServiceController pause() {
        try {
            mCarAppStub.onAppPause(new TestOnDoneCallbackStub());
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to pause the CarAppService", e);
        }

        return this;
    }

    /**
     * Stops the {@link CarAppService} that is being controlled.
     *
     * @see Session#getLifecycle
     */
    public CarAppServiceController stop() {
        try {
            mCarAppStub.onAppStop(new TestOnDoneCallbackStub());
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to stop the CarAppService", e);
        }
        return this;
    }

    /**
     * Destroys the {@link CarAppService} that is being controlled.
     *
     * @see Session#getLifecycle
     */
    public CarAppServiceController destroy() {
        mCarAppService.onUnbind(new Intent());
        mCarAppService.onDestroy();
        return this;
    }

    public void setHostInfo(@Nullable HostInfo hostInfo) {
        try {
            Field hostInfoField = CarAppService.class.getDeclaredField("mHostInfo");
            hostInfoField.setAccessible(true);
            hostInfoField.set(mCarAppService, hostInfo);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to set CarAppService hostInfo value for testing", e);
        }
    }

    public void setAppInfo(@Nullable AppInfo appInfo) {
        try {
            Field appInfoField = CarAppService.class.getDeclaredField("mAppInfo");
            appInfoField.setAccessible(true);
            appInfoField.set(mCarAppService, appInfo);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to set CarAppService appInfo value for testing", e);
        }
    }

    /** Retrieves the {@link CarAppService} that is being controlled. */
    @NonNull
    public CarAppService get() {
        return mCarAppService;
    }

    private CarAppServiceController(
            CarAppService carAppService,
            @NonNull Session session,
            @NonNull TestCarContext testCarContext) {
        this.mCarAppService = carAppService;
        this.mTestCarContext = testCarContext;

        // Use reflection to inject the Session and TestCarContext into the CarAppService.
        try {
            Field currentSession = CarAppService.class.getDeclaredField("mCurrentSession");
            currentSession.setAccessible(true);
            currentSession.set(carAppService, session);

            Field registry = Session.class.getDeclaredField("mRegistry");
            registry.setAccessible(true);
            registry.set(session, testCarContext.getLifecycleOwner().mRegistry);

            Field carContext = Session.class.getDeclaredField("mCarContext");
            carContext.setAccessible(true);
            carContext.set(session, testCarContext);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to set internal CarAppService values for testing", e);
        }

        mCarAppStub = ICarApp.Stub.asInterface(carAppService.onBind(new Intent()));
    }
}
