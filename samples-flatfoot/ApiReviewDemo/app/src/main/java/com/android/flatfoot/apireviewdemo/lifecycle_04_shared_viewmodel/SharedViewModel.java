package com.android.flatfoot.apireviewdemo.lifecycle_04_shared_viewmodel;

import android.graphics.drawable.ShapeDrawable;

import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    LiveData<ShapeDrawable> shapeDrawableData = createEmpty();

    private static LiveData<ShapeDrawable> createEmpty() {
        LiveData<ShapeDrawable> empty = new LiveData<>();
        empty.setValue(null);
        return empty;
    }
}
