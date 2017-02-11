package com.android.flatfoot.apireviewdemo.lifecycle_02_livedata;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.TextView;
import android.widget.Toast;

import com.android.flatfoot.apireviewdemo.R;
import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.Observer;

public class LiveLocationActivity extends LifecycleActivity {

    private static final int REQUEST_LOCATION_PERMISSION_CODE = 1;
    private Observer<Location> mObserver = new Observer<Location>() {
        @Override
        public void onChanged(@Nullable Location location) {
            TextView textView = (TextView) findViewById(R.id.location);
            textView.setText(location.getLatitude() + ", " + location.getLongitude());
        }
    };

    private LocationLiveData mLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_activity);
        mLiveData = LocationLiveData.getInstance();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_CODE);
        } else {
            mLiveData.observe(this, mObserver);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            mLiveData.observe(this, mObserver);
        } else {
            Toast.makeText(this, "This sample requires a location permission",
                    Toast.LENGTH_LONG).show();
        }
    }
}
