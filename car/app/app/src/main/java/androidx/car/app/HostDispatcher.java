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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import static java.util.Objects.requireNonNull;

import android.os.IInterface;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext.CarServiceType;
import androidx.car.app.constraints.IConstraintHost;
import androidx.car.app.navigation.INavigationHost;
import androidx.car.app.utils.LogTags;
import androidx.car.app.utils.RemoteUtils;
import androidx.car.app.utils.ThreadUtils;

import java.security.InvalidParameterException;

/**
 * Dispatches calls to the host and manages possible exceptions.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP) // Restrict to testing library
public final class HostDispatcher {
    @Nullable
    private ICarHost mCarHost;
    @Nullable
    private IAppHost mAppHost;
    @Nullable
    private IConstraintHost mConstraintHost;
    @Nullable
    private INavigationHost mNavigationHost;

    /**
     * Dispatches the {@code call} to the host for the given {@code hostType}.
     *
     * @param hostType the service to dispatch to
     * @param callName the name of the call for logging purposes
     * @param call     the request to dispatch
     * @throws SecurityException if the host has thrown it
     * @throws HostException     if the host throws any exception other than
     *                           {@link SecurityException}
     */
    @Nullable
    @SuppressWarnings({"unchecked", "cast.unsafe"}) // Cannot check if instanceof ServiceT
    public <ServiceT, ReturnT> ReturnT dispatchForResult(
            @CarServiceType @NonNull String hostType, @NonNull String callName,
            @NonNull HostCall<ServiceT, ReturnT> call) throws RemoteException {
        return RemoteUtils.dispatchCallToHostForResult(callName, () -> {
            IInterface service = getHost(hostType);
            if (service == null) {
                Log.e(LogTags.TAG_DISPATCH,
                        "Could not retrieve host while dispatching call " + callName);
                return null;
            }
            return call.dispatch((ServiceT) service);
        });
    }

    /**
     * Dispatches the {@code call} to the host for the given {@code hostType}.
     *
     * @param hostType the service to dispatch to
     * @param callName the name of the call for logging purposes
     * @param call     the request to dispatch
     * @throws SecurityException if the host has thrown it
     * @throws HostException     if the host throws any exception other than
     *                           {@link SecurityException}
     */
    @SuppressWarnings({"unchecked", "cast.unsafe"}) // Cannot check if instanceof ServiceT
    public <ServiceT, ReturnT> void dispatch(
            @CarServiceType @NonNull String hostType, @NonNull String callName,
            @NonNull HostCall<ServiceT, ReturnT> call) {
        RemoteUtils.dispatchCallToHost(callName, () -> {
            IInterface service = getHost(hostType);
            if (service == null) {
                Log.e(LogTags.TAG_DISPATCH,
                        "Could not retrieve host while dispatching call " + callName);
                return null;
            }
            call.dispatch((ServiceT) service);
            return null;
        });
    }

    @MainThread
    public void setCarHost(@NonNull ICarHost carHost) {
        ThreadUtils.checkMainThread();

        resetHosts();
        mCarHost = carHost;
    }

    /** Removes references to remote services which are no longer valid. */
    @MainThread
    void resetHosts() {
        ThreadUtils.checkMainThread();

        mCarHost = null;
        mAppHost = null;
        mNavigationHost = null;
    }

    /**
     * Retrieves the {@link IInterface} for the given {@code hostType}.
     *
     * @throws RemoteException if the host is unresponsive
     * @hide
     */
    @RestrictTo(LIBRARY)
    @Nullable
    IInterface getHost(@CarServiceType String hostType) throws RemoteException {
        if (mCarHost == null) {
            Log.e(LogTags.TAG_DISPATCH, "Host is not bound when attempting to retrieve host "
                    + "service");
            return null;
        }

        IInterface host = null;
        switch (hostType) {
            case CarContext.APP_SERVICE:
                if (mAppHost == null) {
                    mAppHost =
                            RemoteUtils.dispatchCallToHostForResult("getHost(App)", () ->
                                    IAppHost.Stub.asInterface(requireNonNull(mCarHost).getHost(
                                            CarContext.APP_SERVICE)));
                }
                host = mAppHost;
                break;
            case CarContext.CONSTRAINT_SERVICE:
                if (mConstraintHost == null) {
                    mConstraintHost =
                            RemoteUtils.dispatchCallToHostForResult("getHost(Constraints)", () ->
                                    IConstraintHost.Stub.asInterface(
                                            requireNonNull(mCarHost).getHost(
                                                    CarContext.CONSTRAINT_SERVICE)));
                }
                host = mConstraintHost;
                break;
            case CarContext.NAVIGATION_SERVICE:
                if (mNavigationHost == null) {
                    mNavigationHost =
                            RemoteUtils.dispatchCallToHostForResult(
                                    "getHost(Navigation)", () ->
                                            INavigationHost.Stub.asInterface(
                                                    requireNonNull(mCarHost).getHost(
                                                            CarContext.NAVIGATION_SERVICE))
                            );
                }
                host = mNavigationHost;
                break;
            case CarContext.CAR_SERVICE:
                host = mCarHost;
                break;
            default:
                throw new InvalidParameterException("Invalid host type: " + hostType);
        }
        return host;
    }
}
