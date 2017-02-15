package com.android.flatfoot.apireviewdemo.lifecycle_02_livedata;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.TextView;
import android.widget.Toast;

import com.android.flatfoot.apireviewdemo.R;
import com.android.flatfoot.apireviewdemo.internal.PermissionUtils;
import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.Observer;

public class LiveLocationActivity extends LifecycleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_activity);
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
