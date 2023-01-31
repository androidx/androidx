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

package androidx.window.layout.adapter.sidecar;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.os.IBinder;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.window.sidecar.SidecarDeviceState;
import androidx.window.sidecar.SidecarInterface.SidecarCallback;
import androidx.window.sidecar.SidecarWindowLayoutInfo;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A class to record the last calculated values from [SidecarInterface] and filter out
 * duplicates. This class uses [SidecarAdapter] to compute equality since the methods
 * [Object.equals] and [Object.hashCode] may not have been overridden.
 *
 * This is a Java class because in Kotlin we would not be able to ignore [null] values.
 *
 * Sidecar is currently deprecated and we do not plan on developing it further. Sidecar is still
 * supported for basic [FoldingFeature]. The deprecation lint warning is suppressed since it is
 * picking up the Sidecar deprecations.
 *
 * NOTE: If you change the name of this class, you must update the proguard file.
 * @hide
 */
@SuppressWarnings("deprecation") // Sidecar is deprecated but we still support it.
@RestrictTo(LIBRARY_GROUP)
public class DistinctElementSidecarCallback implements SidecarCallback {
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private SidecarDeviceState mLastDeviceState;
    @GuardedBy("mLock")
    private final Map<IBinder, SidecarWindowLayoutInfo> mActivityWindowLayoutInfo =
            new WeakHashMap<>();
    private final SidecarAdapter mAdapter;
    private final SidecarCallback mCallback;

    DistinctElementSidecarCallback(@NonNull SidecarAdapter adapter,
            @NonNull SidecarCallback callback) {
        mAdapter = adapter;
        mCallback = callback;
    }

    @VisibleForTesting
    public DistinctElementSidecarCallback(@NonNull SidecarCallback callback) {
        mAdapter = new SidecarAdapter();
        mCallback = callback;
    }

    @Override
    public void onDeviceStateChanged(@NonNull SidecarDeviceState newDeviceState) {
        //noinspection ConstantConditions
        if (newDeviceState == null) {
            return; // Fix for Sidecar implementations sending null values, see b/233458715
        }
        synchronized (mLock) {
            if (mAdapter.isEqualSidecarDeviceState(mLastDeviceState, newDeviceState)) {
                return;
            }
            mLastDeviceState = newDeviceState;
            mCallback.onDeviceStateChanged(mLastDeviceState);
        }
    }

    @Override
    public void onWindowLayoutChanged(@NonNull IBinder windowToken,
            @NonNull SidecarWindowLayoutInfo newLayout) {
        synchronized (mLock) {
            SidecarWindowLayoutInfo lastInfo = mActivityWindowLayoutInfo.get(windowToken);
            if (mAdapter.isEqualSidecarWindowLayoutInfo(lastInfo, newLayout)) {
                return;
            }
            mActivityWindowLayoutInfo.put(windowToken, newLayout);
            mCallback.onWindowLayoutChanged(windowToken, newLayout);
        }
    }
}
