package com.android.flatfoot.apireviewdemo;

import android.app.Application;
import android.content.Context;

public class DemoApplication extends Application {

    private static DemoApplication sApplication;

    public static Context context() {
        return sApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
    }
}
