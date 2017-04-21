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

import android.arch.lifecycle.LifecycleActivity;
import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.flatfoot.apireviewdemo.lifecycle_04_shared_viewmodel.internal.ShapesAdapter;

public class ShapeChooserFragment extends LifecycleFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        LifecycleActivity activity = (LifecycleActivity) getActivity();
        final SharedViewModel sharedViewModel = ViewModelProviders.of(getActivity())
                .get(SharedViewModel.class);

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
