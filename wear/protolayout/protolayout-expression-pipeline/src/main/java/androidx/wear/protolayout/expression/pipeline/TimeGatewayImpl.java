/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.collection.ArrayMap;

import java.util.Map;
import java.util.concurrent.Executor;

/** Default implementation of {@link TimeGateway} using Android's clock. */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class TimeGatewayImpl implements TimeGateway, AutoCloseable {
    private final Handler uiHandler;
    private final Map<TimeCallback, Executor> registeredCallbacks = new ArrayMap<>();
    private boolean updatesEnabled = false;
    private final Runnable onTick;

    private long lastScheduleTimeMillis = 0;

    // Suppress warning on "onTick = this::notifyNextSecond". This happens because notifyNextSecond
    // is @UnderInitialization here, but onTick needs to be @Initialized. This is safe though;  the
    // only time that onTick can be invoked is in the other methods on this class, which can only be
    // called after initialization is complete. This class is also final, so those methods cannot be
    // called from a sub-constructor either.
    @SuppressWarnings("methodref.receiver.bound")
    public TimeGatewayImpl(@NonNull Handler uiHandler) {
        this.uiHandler = uiHandler;

        this.onTick = this::notifyNextSecond;
    }

    /** See {@link TimeGateway#registerForUpdates(Executor, TimeCallback)}. */
    @Override
    public void registerForUpdates(@NonNull Executor executor, @NonNull TimeCallback callback) {
        registeredCallbacks.put(callback, executor);

        // If this was the first registration, _and_ we're enabled, then schedule the message on the
        // Handler (otherwise, another call has already scheduled the call).
        if (registeredCallbacks.size() == 1 && this.updatesEnabled) {
            lastScheduleTimeMillis = SystemClock.uptimeMillis() + 1000;
            uiHandler.postAtTime(this.onTick, this, lastScheduleTimeMillis);
        }

        // Send first update to initialize clients that are using this TimeGateway
        if (updatesEnabled) {
            callback.onPreUpdate();
            callback.onData();
        }
    }

    /** See {@link TimeGateway#unregisterForUpdates(TimeCallback)}. */
    @Override
    public void unregisterForUpdates(@NonNull TimeCallback callback) {
        registeredCallbacks.remove(callback);

        // If there are no more registered callbacks, stop the periodic call.
        if (registeredCallbacks.isEmpty() && this.updatesEnabled) {
            uiHandler.removeCallbacks(this.onTick, this);
        }
    }

    @UiThread
    public void enableUpdates() {
        setUpdatesEnabled(true);
    }

    @UiThread
    public void disableUpdates() {
        setUpdatesEnabled(false);
    }

    private void setUpdatesEnabled(boolean updatesEnabled) {
        if (updatesEnabled == this.updatesEnabled) {
            return;
        }

        this.updatesEnabled = updatesEnabled;

        if (!updatesEnabled) {
            uiHandler.removeCallbacks(this.onTick, this);
        } else if (!registeredCallbacks.isEmpty()) {
            lastScheduleTimeMillis = SystemClock.uptimeMillis() + 1000;

            uiHandler.postAtTime(this.onTick, this, lastScheduleTimeMillis);
        }
    }

    @SuppressWarnings("ExecutorTaskName")
    private void notifyNextSecond() {
        if (!this.updatesEnabled) {
            return;
        }

        for (Map.Entry<TimeCallback, Executor> callback : registeredCallbacks.entrySet()) {
            callback.getValue().execute(callback.getKey()::onPreUpdate);
        }

        for (Map.Entry<TimeCallback, Executor> callback : registeredCallbacks.entrySet()) {
            callback.getValue().execute(callback.getKey()::onData);
        }

        lastScheduleTimeMillis += 1000;

        // Ensure that the new time is actually in the future. If a call from uiHandler gets
        // significantly delayed for any reason, then without this, we'll reschedule immediately
        // (potentially multiple times), compounding the situation further.
        if (lastScheduleTimeMillis < SystemClock.uptimeMillis()) {
            // Skip the failed updates...
            long missedTime = SystemClock.uptimeMillis() - lastScheduleTimeMillis;

            // Round up to the nearest second...
            missedTime = ((missedTime / 1000) + 1) * 1000;
            lastScheduleTimeMillis += missedTime;
        }

        uiHandler.postAtTime(this.onTick, this, lastScheduleTimeMillis);
    }

    @Override
    @UiThread
    public void close() {
        setUpdatesEnabled(false);
        registeredCallbacks.clear();
    }
}
