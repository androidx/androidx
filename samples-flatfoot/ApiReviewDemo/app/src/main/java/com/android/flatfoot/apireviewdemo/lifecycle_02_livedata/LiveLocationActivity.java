/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.flatfoot.apireviewdemo.lifecycle_02_livedata;

import android.arch.lifecycle.LifecycleActivity;
import android.arch.lifecycle.Observer;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.TextView;
import android.widget.Toast;

import com.android.flatfoot.apireviewdemo.R;
import com.android.flatfoot.apireviewdemo.internal.PermissionUtils;

public class LiveLocationActivity extends LifecycleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_activity);
        // ignore permission handling code as an implementation detail
        if (PermissionUtils.hasLocationPermission(this)) {
            startListening();
        } else {
            PermissionUtils.requestLocationPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // ignore permission handling code as an implementation detail
        if (PermissionUtils.hasLocationPermission(this)) {
            startListening();
        } else {
            Toast.makeText(this, "This sample requires a location permission",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void startListening() {
        LocationLiveData liveData = LocationLiveData.getInstance();
        liveData.observe(this, new Observer<Location>() {
            @Override
            public void onChanged(@Nullable Location location) {
                TextView textView = (TextView) findViewById(R.id.location);
                textView.setText(location.getLatitude() + ", " + location.getLongitude());
            }
        });
    }
}
