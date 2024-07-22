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
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHearingAid;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;
import com.example.androidx.mediarouting.activities.systemrouting.SystemRoutesSourceItem;

import java.util.ArrayList;
import java.util.List;

/** Implements {@link SystemRoutesSource} using {@link BluetoothManager}. */
public final class BluetoothManagerSystemRoutesSource extends SystemRoutesSource {

    @NonNull
    private final Context mContext;
    @NonNull
    private final BluetoothManager mBluetoothManager;
    @NonNull
    private final BluetoothAdapter mBluetoothAdapter;
    @NonNull
    private final DeviceStateChangedReceiver mDeviceStateChangedReceiver =
            new DeviceStateChangedReceiver();

    /** Returns a new instance. */
    @NonNull
    public static BluetoothManagerSystemRoutesSource create(@NonNull Context context) {
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return new BluetoothManagerSystemRoutesSource(context, bluetoothManager);
    }

    BluetoothManagerSystemRoutesSource(@NonNull Context context,
            @NonNull BluetoothManager bluetoothManager) {
        mContext = context;
        mBluetoothManager = bluetoothManager;
        mBluetoothAdapter = mBluetoothManager.getAdapter();
    }

    @Override
    public void start() {
        IntentFilter deviceStateChangedIntentFilter = new IntentFilter();

        deviceStateChangedIntentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        deviceStateChangedIntentFilter.addAction(
                BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED);
        deviceStateChangedIntentFilter.addAction(
                BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);

        mContext.registerReceiver(mDeviceStateChangedReceiver, deviceStateChangedIntentFilter);
    }

    @Override
    public void stop() {
        mContext.unregisterReceiver(mDeviceStateChangedReceiver);
    }

    @NonNull
    @Override
    public SystemRoutesSourceItem getSourceItem() {
        return new SystemRoutesSourceItem(/* name= */ "BluetoothManager");
    }

    @NonNull
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    public List<SystemRouteItem> fetchSourceRouteItems() {
        List<SystemRouteItem> out = new ArrayList<>();

        for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
            out.add(createRouteItemFor(device));
        }

        return out;
    }

    @Override
    public boolean select(@NonNull SystemRouteItem item) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private SystemRouteItem createRouteItemFor(@NonNull BluetoothDevice device) {
        return new SystemRouteItem.Builder(getSourceId(), /* id= */ device.getAddress())
                .setName(device.getName())
                .setAddress(device.getAddress())
                .build();
    }

    private class DeviceStateChangedReceiver extends BroadcastReceiver {
        @Override
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                case BluetoothHearingAid.ACTION_CONNECTION_STATE_CHANGED:
                case BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED:
                    mOnRoutesChangedListener.run();
                    break;
                default:
                    // Do nothing.
            }
        }
    }
}
