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

package androidx.car.app.connection;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.car.app.utils.CommonUtils.isAutomotiveOS;

import static java.util.Objects.requireNonNull;

import android.content.Context;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LiveData;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A class that allows retrieval of information about connection to a car head unit.
 */
public final class CarConnection {
    /**
     * Defines current car connection state.
     *
     * <p>This is used for communication with the car host's content provider on queries for
     * connection type.
     */
    public static final String CAR_CONNECTION_STATE = "CarConnectionState";

    /**
     * Broadcast action that notifies that the car connection has changed and needs to be updated.
     */
    public static final String ACTION_CAR_CONNECTION_UPDATED =
            "androidx.car.app.connection.action.CAR_CONNECTION_UPDATED";

    /**
     * Represents the types of connections that exist to a car head unit.
     *
     * @hide
     */
    @IntDef({CONNECTION_TYPE_NOT_CONNECTED, CONNECTION_TYPE_NATIVE, CONNECTION_TYPE_PROJECTION})
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.TYPE_USE})
    @RestrictTo(LIBRARY)
    public @interface ConnectionType {
    }

    /**
     * Not connected to any car head unit.z
     */
    public static final int CONNECTION_TYPE_NOT_CONNECTED = 0;

    /**
     * Natively running on a head unit (Android Automotive OS).
     */
    public static final int CONNECTION_TYPE_NATIVE = 1;

    /**
     * Connected to a car head unit by projecting to it.
     */
    public static final int CONNECTION_TYPE_PROJECTION = 2;

    private final LiveData<Integer> mConnectionTypeLiveData;

    /**
     * Constructs a {@link CarConnection} that can be used to get connection information.
     *
     * @throws NullPointerException if {@code context} is {@code null}
     */
    public CarConnection(@NonNull Context context) {
        requireNonNull(context);
        mConnectionTypeLiveData = isAutomotiveOS(context)
                ? new AutomotiveCarConnectionTypeLiveData()
                : new CarConnectionTypeLiveData(context);
    }

    /**
     * Returns a {@link LiveData} that can be observed to get current connection type.
     *
     * <p>The recommended pattern is to observe the {@link LiveData} with the activity's
     * lifecycle in order to get updates on the state change throughout the activity's lifetime.
     *
     * <p>Connection types are:
     * <ol>
     *     <li>{@link #CONNECTION_TYPE_NOT_CONNECTED}
     *     <li>{@link #CONNECTION_TYPE_NATIVE}
     *     <li>{@link #CONNECTION_TYPE_PROJECTION}
     * </ol>
     */
    @NonNull
    public LiveData<@ConnectionType Integer> getType() {
        return mConnectionTypeLiveData;
    }
}
