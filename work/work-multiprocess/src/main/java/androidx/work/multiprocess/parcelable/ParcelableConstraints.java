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

import static androidx.work.impl.model.WorkTypeConverters.byteArrayToContentUriTriggers;
import static androidx.work.impl.model.WorkTypeConverters.contentUriTriggersToByteArray;
import static androidx.work.impl.model.WorkTypeConverters.intToNetworkType;
import static androidx.work.impl.model.WorkTypeConverters.networkTypeToInt;
import static androidx.work.multiprocess.parcelable.ParcelUtils.readBooleanValue;
import static androidx.work.multiprocess.parcelable.ParcelUtils.writeBooleanValue;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Constraints;
import androidx.work.ContentUriTriggers;
import androidx.work.NetworkType;

import java.util.concurrent.TimeUnit;

/**
 * Constraints, but parcelable.
 *
 * @hide
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
        if (Build.VERSION.SDK_INT >= 23) {
            boolean requiresDeviceIdle = readBooleanValue(in);
            builder.setRequiresDeviceIdle(requiresDeviceIdle);
        }
        // ContentUriTriggers
        if (Build.VERSION.SDK_INT >= 24) {
            boolean hasTriggers = readBooleanValue(in);
            if (hasTriggers) {
                ContentUriTriggers contentUriTriggers =
                        byteArrayToContentUriTriggers(in.createByteArray());
                for (ContentUriTriggers.Trigger trigger : contentUriTriggers.getTriggers()) {
                    builder.addContentUriTrigger(trigger.getUri(),
                            trigger.shouldTriggerForDescendants());
                }
            }
            // triggerMaxContentDelay
            long triggerMaxContentDelay = in.readLong();
            builder.setTriggerContentMaxDelay(triggerMaxContentDelay, TimeUnit.MILLISECONDS);
            // triggerContentUpdateDelay
            long triggerContentUpdateDelay = in.readLong();
            builder.setTriggerContentUpdateDelay(triggerContentUpdateDelay, TimeUnit.MILLISECONDS);
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
        if (Build.VERSION.SDK_INT >= 23) {
            writeBooleanValue(parcel, mConstraints.requiresDeviceIdle());
        }
        // ContentUriTriggers
        if (Build.VERSION.SDK_INT >= 24) {
            boolean hasTriggers = mConstraints.hasContentUriTriggers();
            writeBooleanValue(parcel, hasTriggers);
            if (hasTriggers) {
                ContentUriTriggers contentUriTriggers = mConstraints.getContentUriTriggers();
                byte[] serializedTriggers = contentUriTriggersToByteArray(contentUriTriggers);
                parcel.writeByteArray(serializedTriggers);
            }
            // triggerMaxContentDelay
            parcel.writeLong(mConstraints.getTriggerMaxContentDelay());
            // triggerContentUpdateDelay
            parcel.writeLong(mConstraints.getTriggerContentUpdateDelay());
        }
    }

    @NonNull
    public Constraints getConstraints() {
        return mConstraints;
    }
}
