package com.android.flatfoot.apireviewdemo.lifecycle_04_shared_viewmodel;

import android.os.Bundle;

import com.android.flatfoot.apireviewdemo.R;
import com.android.support.lifecycle.LifecycleActivity;

public class ShapesActivity extends LifecycleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shape_activity);
    }
}
