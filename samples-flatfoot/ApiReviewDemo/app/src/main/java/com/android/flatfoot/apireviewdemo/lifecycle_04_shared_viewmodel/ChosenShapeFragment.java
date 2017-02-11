package com.android.flatfoot.apireviewdemo.lifecycle_04_shared_viewmodel;

import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.flatfoot.apireviewdemo.R;
import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

public class ChosenShapeFragment extends LifecycleFragment {

    private View mNoneShapeView;
    private View mShapeView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.chosen_shape_layout, container);
        mNoneShapeView = layout.findViewById(R.id.none);
        mShapeView = layout.findViewById(R.id.color);
        LifecycleActivity activity = (LifecycleActivity) getActivity();
        SharedViewModel viewModel = ViewModelStore.get(activity, "shared", SharedViewModel.class);
        viewModel.shapeDrawableData.observe(this, new Observer<ShapeDrawable>() {
            @Override
            public void onChanged(@Nullable ShapeDrawable shapeDrawable) {
                updateColor(shapeDrawable);
            }
        });
        return layout;
    }

    private void updateColor(ShapeDrawable shape) {
        mNoneShapeView.setVisibility(shape == null ? View.VISIBLE : View.GONE);
        mShapeView.setVisibility(shape != null ? View.VISIBLE : View.GONE);
        if (shape != null) {
            mShapeView.setBackground(shape);
        }
    }
}
