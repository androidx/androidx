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

package com.android.flatfoot.apireviewdemo.lifecycle_04_shared_viewmodel.internal;


import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.flatfoot.apireviewdemo.R;

import java.util.ArrayList;
import java.util.List;

// ignore it as an implementation detail
public class ShapesAdapter extends RecyclerView.Adapter<VHolder> {

    public interface ShapeListener {
        void onShapeChosen(ShapeDrawable shapeDrawable);
    }

    private static final float[] RADII =
            {16.0f, 16.0f, 16.0f, 16.0f, 16.0f, 16.0f, 16.0f, 16.0f};

    private static final Shape[] SHAPES = new Shape[]{
            new RectShape(),
            new RoundRectShape(RADII, null, null),
            new OvalShape(),
    };

    private static final int[] COLORS = new int[]{
            Color.BLUE,
            Color.CYAN,
            Color.YELLOW,
            Color.RED,
            Color.GREEN,
            Color.LTGRAY,
            Color.MAGENTA,
    };

    private final List<ShapeDrawable> mDrawables = new ArrayList<>();
    private ShapeListener mShapeListener;

    public ShapesAdapter() {
        for (int color : COLORS) {
            for (Shape shape : SHAPES) {
                ShapeDrawable drawable = new ShapeDrawable(shape);
                drawable.getPaint().setColor(color);
                mDrawables.add(drawable);
            }
        }
    }

    @Override
    public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shape_item,
                parent, false);
        final VHolder holder = new VHolder(view);
        holder.mShapeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mShapeListener != null) {
                    mShapeListener.onShapeChosen(holder.mShapeDrawable);
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(final VHolder holder, int position) {
        holder.mShapeView.setBackground(mDrawables.get(position));
        holder.mShapeDrawable = mDrawables.get(position);
    }

    @Override
    public int getItemCount() {
        return mDrawables.size();
    }

    public void setShapeListener(ShapeListener shapeListener) {
        mShapeListener = shapeListener;
    }
}

class VHolder extends RecyclerView.ViewHolder {
    View mShapeView;
    ShapeDrawable mShapeDrawable;

    public VHolder(View itemView) {
        super(itemView);
        mShapeView = itemView.findViewById(R.id.shape);
    }
}