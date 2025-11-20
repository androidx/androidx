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

package androidx.work.multiprocess.parcelable;

import static androidx.work.impl.model.WorkTypeConverters.byteArrayToSetOfTriggers;
import static androidx.work.impl.model.WorkTypeConverters.intToNetworkType;
import static androidx.work.impl.model.WorkTypeConverters.networkTypeToInt;
import static androidx.work.impl.model.WorkTypeConverters.setOfTriggersToByteArray;
import static androidx.work.impl.utils.NetworkRequestCompatKt.getCapabilitiesCompat;
import static androidx.work.impl.utils.NetworkRequestCompatKt.getTransportTypesCompat;
import static androidx.work.multiprocess.parcelable.ParcelUtils.readBooleanValue;
import static androidx.work.multiprocess.parcelable.ParcelUtils.writeBooleanValue;

import android.annotation.SuppressLint;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;
import androidx.work.Constraints;
import androidx.work.Constraints.ContentUriTrigger;
import androidx.work.NetworkType;
import androidx.work.impl.utils.NetworkRequest28;

import org.jspecify.annotations.NonNull;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Constraints, but parcelable.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableConstraints implements Parcelable {

    private final Constraints mConstraints;

    public ParcelableConstraints(@NonNull Constraints constraints) {
        mConstraints = constraints;
    }

    public ParcelableConstraints(@NonNull Parcel in) {
        Constraints.Builder builder = new Constraints.Builder();
        // networkType
        NetworkType networkType = intToNetworkType(in.readInt());
        builder.setRequiredNetworkType(networkType);
        // batteryNotLow
        boolean batteryNotLow = readBooleanValue(in);
        builder.setRequiresBatteryNotLow(batteryNotLow);
        // requiresCharging
        boolean requiresCharging = readBooleanValue(in);
        builder.setRequiresCharging(requiresCharging);
        // requiresStorageNotLow
        boolean requiresStorageNotLow = readBooleanValue(in);
        builder.setRequiresStorageNotLow(requiresStorageNotLow);
        // requiresDeviceIdle
        boolean requiresDeviceIdle = readBooleanValue(in);
        builder.setRequiresDeviceIdle(requiresDeviceIdle);
        // ContentUriTriggers
        if (Build.VERSION.SDK_INT >= 24) {
            boolean hasTriggers = readBooleanValue(in);
            if (hasTriggers) {
                Set<ContentUriTrigger> contentUriTriggers = byteArrayToSetOfTriggers(
                        in.createByteArray());
                for (ContentUriTrigger trigger : contentUriTriggers) {
                    builder.addContentUriTrigger(trigger.getUri(),
                            trigger.isTriggeredForDescendants());
                }
            }
            // triggerMaxContentDelay
            long triggerMaxContentDelay = in.readLong();
            builder.setTriggerContentMaxDelay(triggerMaxContentDelay, TimeUnit.MILLISECONDS);
            // triggerContentUpdateDelay
            long triggerContentUpdateDelay = in.readLong();
            builder.setTriggerContentUpdateDelay(triggerContentUpdateDelay, TimeUnit.MILLISECONDS);
        }
        if (Build.VERSION.SDK_INT >= 28) {
            boolean hasNetworkRequest = readBooleanValue(in);
            if (hasNetworkRequest) {
                //noinspection DataFlowIssue
                NetworkRequest request = NetworkRequest28.createNetworkRequest(
                        in.createIntArray(), in.createIntArray());
                builder.setRequiredNetworkRequest(request, NetworkType.NOT_REQUIRED);
            }
        }
        mConstraints = builder.build();
    }

    public static final Creator<ParcelableConstraints> CREATOR =
            new Creator<ParcelableConstraints>() {
                @Override
                public ParcelableConstraints createFromParcel(Parcel in) {
                    return new ParcelableConstraints(in);
                }

                @Override
                public ParcelableConstraints[] newArray(int size) {
                    return new ParcelableConstraints[size];
                }
            };

    @Override
    public int describeContents() {
        // No file descriptors being returned.
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        // networkType
        parcel.writeInt(networkTypeToInt(mConstraints.getRequiredNetworkType()));
        // batteryNotLow
        writeBooleanValue(parcel, mConstraints.requiresBatteryNotLow());
        // requiresCharging
        writeBooleanValue(parcel, mConstraints.requiresCharging());
        // requiresStorageNotLow
        writeBooleanValue(parcel, mConstraints.requiresStorageNotLow());
        // requiresDeviceIdle
        writeBooleanValue(parcel, mConstraints.requiresDeviceIdle());
        // ContentUriTriggers
        if (Build.VERSION.SDK_INT >= 24) {
            boolean hasTriggers = mConstraints.hasContentUriTriggers();
            writeBooleanValue(parcel, hasTriggers);
            if (hasTriggers) {
                byte[] serializedTriggers =
                        setOfTriggersToByteArray(mConstraints.getContentUriTriggers());
                parcel.writeByteArray(serializedTriggers);
            }
            // triggerMaxContentDelay
            parcel.writeLong(mConstraints.getContentTriggerMaxDelayMillis());
            // triggerContentUpdateDelay
            parcel.writeLong(mConstraints.getContentTriggerUpdateDelayMillis());
        }
        if (Build.VERSION.SDK_INT >= 28) {
            NetworkRequest networkRequest = mConstraints.getRequiredNetworkRequest();
            boolean hasNetworkRequest = networkRequest != null;
            writeBooleanValue(parcel, hasNetworkRequest);
            if (hasNetworkRequest) {
                parcel.writeIntArray(getCapabilitiesCompat(networkRequest));
                parcel.writeIntArray(getTransportTypesCompat(networkRequest));
            }
        }
    }

    public @NonNull Constraints getConstraints() {
        return mConstraints;
    }
}
