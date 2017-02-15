package com.android.flatfoot.apireviewdemo.internal;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.support.v4.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

// ignore it as implementation detail
public class PermissionUtils {

    public static boolean hasLocationPermission(Context context) {
        boolean fineLocationPermission = checkSelfPermission(context, ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocationPermission = checkSelfPermission(context,
                ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fineLocationPermission && coarseLocationPermission;
    }

    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
    }
}
