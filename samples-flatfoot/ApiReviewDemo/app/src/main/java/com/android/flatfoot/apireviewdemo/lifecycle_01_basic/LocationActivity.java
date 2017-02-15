package com.android.flatfoot.apireviewdemo.lifecycle_01_basic;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.TextView;
import android.widget.Toast;

import com.android.flatfoot.apireviewdemo.R;
import com.android.flatfoot.apireviewdemo.internal.PermissionUtils;
import com.android.flatfoot.apireviewdemo.internal.SimpleLocationListener;
import com.android.support.lifecycle.LifecycleActivity;

public class LocationActivity extends LifecycleActivity {

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // ignore permission handling code as an implementation detail
        if (PermissionUtils.hasLocationPermission(this)) {
            startListening();
        } else {
            Toast.makeText(this, "This sample requires Location access", Toast.LENGTH_LONG).show();
        }
    }

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

    private void startListening() {
        LocationListener.listenLocation(this, new SimpleLocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                TextView textView = (TextView) findViewById(R.id.location);
                textView.setText(location.getLatitude() + ", " + location.getLongitude());
            }
        });
    }
}
