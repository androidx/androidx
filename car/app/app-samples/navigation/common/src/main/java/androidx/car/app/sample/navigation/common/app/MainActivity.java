/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.navigation.common.app;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.car.app.connection.CarConnection;
import androidx.car.app.sample.navigation.common.R;
import androidx.car.app.sample.navigation.common.nav.NavigationService;

/**
 * The main app activity.
 *
 * <p>See {@link androidx.car.app.sample.navigation.common.car.NavigationCarAppService} for the
 * app's entry point to the cat host.
 */
public class MainActivity extends ComponentActivity {
    static final String TAG = MainActivity.class.getSimpleName();

    // A reference to the navigation service used to get location updates and routing.
    NavigationService mService = null;

    // Tracks the bound state of the navigation service.
    boolean mIsBound = false;

    // Monitors the state of the connection to the navigation service.
    private final ServiceConnection mServiceConnection =
            new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.i(TAG, "In onServiceConnected() component:" + name);
                    NavigationService.LocalBinder binder = (NavigationService.LocalBinder) service;
                    mService = binder.getService();
                    mIsBound = true;
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.i(TAG, "In onServiceDisconnected() component:" + name);
                    mService = null;
                    mIsBound = false;
                }
            };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "In onCreate()");

        setContentView(R.layout.activity_main);

        // Hook up some manual navigation controls.
        Button startNavButton = findViewById(R.id.start_nav);
        startNavButton.setOnClickListener(this::startNavigation);
        Button stopNavButton = findViewById(R.id.stop_nav);
        stopNavButton.setOnClickListener(this::stopNavigation);

        new CarConnection(this).getType().observe(this,
                this::onConnectionStateUpdate);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "In onStart()");
        bindService(
                new Intent(this, NavigationService.class),
                mServiceConnection,
                Context.BIND_AUTO_CREATE);
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "In onStop(). bound" + mIsBound);
        if (mIsBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mIsBound = false;
            mService = null;
        }
        super.onStop();
    }

    private void onConnectionStateUpdate(Integer connectionState) {
        String message = connectionState > CarConnection.CONNECTION_TYPE_NOT_CONNECTED
                ? "Connected to a car head unit"
                : "Not Connected to a car head unit";
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void startNavigation(View view) {
        if (mService != null) {
            mService.startNavigation();
        }
    }

    private void stopNavigation(View view) {
        if (mService != null) {
            mService.stopNavigation();
        }
    }
}
