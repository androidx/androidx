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

package com.android.flatfoot.apireviewdemo.lifecycle_04_shared_viewmodel;

import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.flatfoot.apireviewdemo.R;

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
        SharedViewModel viewModel = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);
        viewModel.shapeDrawableData.observe(this, new Observer<ShapeDrawable>() {
            @Override
            public void onChanged(@Nullable ShapeDrawable shapeDrawable) {
                updateShape(shapeDrawable);
            }
        });
        return layout;
    }

    private void updateShape(ShapeDrawable shape) {
        mNoneShapeView.setVisibility(shape == null ? View.VISIBLE : View.GONE);
        mShapeView.setVisibility(shape != null ? View.VISIBLE : View.GONE);
        if (shape != null) {
            mShapeView.setBackground(shape);
        }
    }
}
