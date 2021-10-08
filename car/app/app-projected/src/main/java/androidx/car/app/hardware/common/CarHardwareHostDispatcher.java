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
package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.util.Objects.requireNonNull;

import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.HostDispatcher;
import androidx.car.app.ICarHost;
import androidx.car.app.hardware.ICarHardwareHost;
import androidx.car.app.hardware.ICarHardwareResult;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.utils.RemoteUtils;

/**
 * Dispatcher of calls to the host and manages possible exceptions.
 *
 * <p>Since the standard {@link HostDispatcher} does not know about the {@link ICarHardwareHost}
 * this wrapper fetches it and then does direct dispatch.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class CarHardwareHostDispatcher {

    @NonNull
    private final HostDispatcher mHostDispatcher;

    @Nullable
    private ICarHardwareHost mICarHardwareHost;

    /**
     * Creates an instance of {@link CarHardwareHostDispatcher} with the given root dispatcher.
     *
     * @param hostDispatcher root dispatcher that is used to get the {@link ICarHardwareHost}
     *                       initially
     * @throws NullPointerException if {@code hostDispatcher} is {@code null}
     */
    public CarHardwareHostDispatcher(@NonNull HostDispatcher hostDispatcher) {
        mHostDispatcher = requireNonNull(hostDispatcher);
    }

    /**
     * Dispatches a call to {@link ICarHardwareHost#getCarHardwareResult} for the given {@code
     * resultType}.
     *
     * @param resultType the result type to fetch
     * @param bundle     parameters or additional info for the call
     * @param result     the callback where the result is returned
     * @throws NullPointerException if {@code result} is {@code null}
     */
    public void dispatchGetCarHardwareResult(int resultType, @Nullable Bundleable bundle,
            @NonNull ICarHardwareResult result) {
        requireNonNull(result);
        RemoteUtils.dispatchCallToHost("getCarHardwareResult",
                () -> {
                    getHost().getCarHardwareResult(
                            resultType,
                            bundle,
                            result);
                    return null;
                });
    }

    /**
     * Dispatches a call to {@link ICarHardwareHost#subscribeCarHardwareResult} for the given {@code
     * resultType}.
     *
     * @param resultType the result type to fetch
     * @param bundle     parameters or additional info for the call
     * @param callback   the callback where the result is returned
     * @throws NullPointerException if {@code callback} is {@code null}
     */
    public void dispatchSubscribeCarHardwareResult(int resultType, @Nullable Bundleable bundle,
            @NonNull ICarHardwareResult callback) {
        requireNonNull(callback);
        RemoteUtils.dispatchCallToHost("subscribeCarHardwareResult",
                () -> {
                    getHost().subscribeCarHardwareResult(
                            resultType,
                            bundle,
                            callback);
                    return null;
                });
    }

    /**
     * Dispatches a call to {@link ICarHardwareHost#unsubscribeCarHardwareResult} for the
     * {@code resultType}.
     *
     * @param resultType the result type to fetch
     * @throws NullPointerException if {@code callback} is {@code null}
     */
    public void dispatchUnsubscribeCarHardwareResult(int resultType, @Nullable Bundleable bundle) {
        RemoteUtils.dispatchCallToHost("unsubscribeCarHardwareResult",
                () -> {
                    getHost().unsubscribeCarHardwareResult(resultType, bundle);
                    return null;
                });
    }

    @NonNull
    private ICarHardwareHost getHost() throws RemoteException {
        ICarHardwareHost host = mICarHardwareHost;
        if (host == null) {
            host = requireNonNull(mHostDispatcher.dispatchForResult(CarContext.CAR_SERVICE,
                    "getHost(CarHardware)",
                    (ICarHost carHost) ->
                            ICarHardwareHost.Stub.asInterface(
                                    carHost.getHost(CarContext.HARDWARE_SERVICE))
            ));
            mICarHardwareHost = host;
        }
        return host;
    }
}
