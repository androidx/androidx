package com.android.flatfoot.apireviewdemo.lifecycle_04_shared_viewmodel;

import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.flatfoot.apireviewdemo.lifecycle_04_shared_viewmodel.internal.ShapesAdapter;
import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.ViewModelStore;

public class ShapeChooserFragment extends LifecycleFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        LifecycleActivity activity = (LifecycleActivity) getActivity();
        final SharedViewModel sharedViewModel = ViewModelStore.get(activity, "shared",
                SharedViewModel.class);

        RecyclerView rv = new RecyclerView(activity);
        rv.setLayoutManager(new GridLayoutManager(activity, 3));

        // adapter itself is implementation detail, ignore it
        ShapesAdapter adapter = new ShapesAdapter();
        adapter.setShapeListener(new ShapesAdapter.ShapeListener() {
            @Override
            public void onShapeChosen(ShapeDrawable shapeDrawable) {
                sharedViewModel.shapeDrawableData.setValue(shapeDrawable);
            }
        });
        rv.setAdapter(adapter);
        return rv;
    }
}
