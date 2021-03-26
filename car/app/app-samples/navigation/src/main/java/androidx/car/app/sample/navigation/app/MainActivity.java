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

package androidx.car.app.sample.navigation.app;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.car.app.sample.navigation.R;
import androidx.car.app.sample.navigation.nav.NavigationService;

/**
 * The main app activity.
 *
 * <p>See {@link NavigationCarAppService} for the app's entry point to Android Auto.
 */
public class MainActivity extends Activity {
    static final String TAG = MainActivity.class.getSimpleName();

    // A reference to the navigation service used to get location updates and routing.
    NavigationService mService = null;

    // Tracks the bound state of the navigation service.
    boolean mIsBound = false;

    private Button mStartNavButton;
    private Button mStopNavButton;

    // Monitors the state of the connection to the navigation service.
    private final ServiceConnection mServiceConnection =
            new ServiceConnection() {

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.i(TAG, String.format("In onServiceConnected() component:%s", name));
                    NavigationService.LocalBinder binder = (NavigationService.LocalBinder) service;
                    mService = binder.getService();
                    mIsBound = true;
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.i(TAG, String.format("In onServiceDisconnected() component:%s", name));
                    mService = null;
                    mIsBound = false;
                }
            };

    @Override
    protected void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "In onCreate()");

        setContentView(R.layout.activity_main);

        // Hook up some manual navigation controls.
        mStartNavButton = findViewById(R.id.start_nav);
        mStartNavButton.setOnClickListener(this::startNavigation);
        mStopNavButton = findViewById(R.id.stop_nav);
        mStopNavButton.setOnClickListener(this::stopNavigation);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "In onStart()");
        bindService(
                new Intent(this, NavigationService.class),
                mServiceConnection,
                Context.BIND_AUTO_CREATE);
        requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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
