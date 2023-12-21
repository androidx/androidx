/*
 * Copyright 2023 The Android Open Source Project
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

package com.example.androidx.mediarouting.activities.systemrouting.source;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;

import java.util.ArrayList;
import java.util.List;

/** Implements {@link SystemRoutesSource} using {@link BluetoothManager}. */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public final class BluetoothManagerSystemRoutesSource implements SystemRoutesSource {

    @NonNull
    private final BluetoothManager mBluetoothManager;
    @NonNull
    private final BluetoothAdapter mBluetoothAdapter;

    /** Returns a new instance. */
    @NonNull
    public static BluetoothManagerSystemRoutesSource create(@NonNull Context context) {
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return new BluetoothManagerSystemRoutesSource(bluetoothManager);
    }

    BluetoothManagerSystemRoutesSource(@NonNull BluetoothManager bluetoothManager) {
        mBluetoothManager = bluetoothManager;
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    @NonNull
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public List<SystemRouteItem> fetchRoutes() {
        List<SystemRouteItem> out = new ArrayList<>();

        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            out.add(new SystemRouteItem.Builder(/* id= */ device.getAddress(),
                    /* type= */ SystemRouteItem.ROUTE_SOURCE_BLUETOOTH_MANAGER)
                    .setName(device.getName())
                    .setAddress(device.getAddress())
                    .build());
        }

        return out;
    }
}
