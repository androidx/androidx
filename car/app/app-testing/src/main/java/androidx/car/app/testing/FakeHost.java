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

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.IAppHost;
import androidx.car.app.ICarHost;
import androidx.car.app.ISurfaceCallback;
import androidx.car.app.Screen;
import androidx.car.app.navigation.INavigationHost;
import androidx.car.app.notification.CarAppNotificationBroadcastReceiver;
import androidx.car.app.serialization.Bundleable;

import org.robolectric.Shadows;

/**
 * A fake that simulates the behavior of the host of a car app.
 *
 * <p>This fake allows sending a {@link PendingIntent} as if the user clicked on a notification
 * action.
 *
 * <p>It will also perform expected host behaviors such as calling {@link Screen#onGetTemplate}
 * after {@link AppManager#invalidate} is called.
 */
public class FakeHost {
    private final ICarHost.Stub mCarHost = new TestCarHost();
    final IAppHost mAppHost = new TestAppHost();
    final INavigationHost mNavigationHost = new TestNavigationHost();
    final TestCarContext mTestCarContext;

    /**
     * Sends the given pending intent as if the user clicked on a notification action.
     *
     * @throws NullPointerException if {@code pendingIntent} is {@code null}
     */
    public void performNotificationActionClick(@NonNull PendingIntent pendingIntent) {
        requireNonNull(pendingIntent);

        Bundle extras = new Bundle(1);
        extras.putBinder(
                CarContext.EXTRA_START_CAR_APP_BINDER_KEY,
                mTestCarContext.getStartCarAppStub().asBinder());
        Intent extraData = new Intent().putExtras(extras);

        try {
            pendingIntent.send(mTestCarContext, /* code= */ 0, extraData);
        } catch (CanceledException e) {
            throw new IllegalStateException("Unable to broadcast intent " + pendingIntent, e);
        }

        Intent broadcastedIntent = Shadows.shadowOf(mTestCarContext).getBroadcastIntents().get(0);

        new CarAppNotificationBroadcastReceiver().onReceive(mTestCarContext, broadcastedIntent);
    }

    FakeHost(TestCarContext testCarContext) {
        this.mTestCarContext = testCarContext;
    }

    ICarHost getCarHost() {
        return mCarHost;
    }

    /**
     * A fake implementation of the host binder.
     *
     * <p>Mainly it provides the fake host services {@link TestAppHost} and
     * {@link TestNavigationHost}.
     */
    class TestCarHost extends ICarHost.Stub {
        @Override
        public void startCarApp(Intent intent) {
            // No-op.
        }

        @Override
        public IBinder getHost(String type) {
            switch (type) {
                case CarContext.APP_SERVICE:
                    return mAppHost.asBinder();
                case CarContext.NAVIGATION_SERVICE:
                    return mNavigationHost.asBinder();
                default: // Fall out
            }
            throw new IllegalArgumentException("Unknown host type: " + type);
        }

        @Override
        public void finish() {
            // No-op.
        }
    }

    /** Testing version of the app host. */
    class TestAppHost extends IAppHost.Stub {
        @Override
        public void invalidate() {
            Screen top = mTestCarContext.getCarService(TestScreenManager.class).getTop();
            mTestCarContext
                    .getCarService(TestAppManager.class)
                    .addTemplateReturned(top, top.onGetTemplate());
        }

        @Override
        public void showToast(CharSequence text, int duration) {
            // No-op.
        }

        @Override
        public void setSurfaceCallback(@Nullable ISurfaceCallback callback) {
            // No-op.
        }
    }

    /** Testing version of the navigation host. */
    static class TestNavigationHost extends INavigationHost.Stub {
        @Override
        public void navigationStarted() {
            // No-op.
        }

        @Override
        public void navigationEnded() {
            // No-op.
        }

        @Override
        public void updateTrip(Bundleable navState) {
            // No-op.
        }
    }
}
