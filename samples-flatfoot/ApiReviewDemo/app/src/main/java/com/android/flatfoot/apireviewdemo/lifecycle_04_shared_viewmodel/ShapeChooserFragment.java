package com.android.flatfoot.apireviewdemo.lifecycle_04_shared_viewmodel;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.flatfoot.apireviewdemo.R;
import com.android.support.lifecycle.LifecycleActivity;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.ViewModelStore;

import java.util.ArrayList;
import java.util.List;

public class ShapeChooserFragment extends LifecycleFragment {

    private static final float[] RADII =
            {16.0f, 16.0f, 16.0f, 16.0f, 16.0f, 16.0f, 16.0f, 16.0f};

    private Shape[] SHAPES = new Shape[]{
            new RectShape(),
            new RoundRectShape(RADII, null, null),
            new OvalShape(),
    };

    private int[] COLORS = new int[]{
            Color.BLUE,
            Color.CYAN,
            Color.YELLOW,
            Color.RED,
            Color.GREEN,
            Color.LTGRAY,
            Color.MAGENTA,
    };
    private SharedViewModel mViewModel;

    private class VHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        View mShapeView;
        ShapeDrawable mShapeDrawable;

        public VHolder(View itemView) {
            super(itemView);
            mShapeView = itemView.findViewById(R.id.shape);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mViewModel.shapeDrawableData.setValue(mShapeDrawable);
        }
    }

    private class RVAdapter extends RecyclerView.Adapter<VHolder> {
        private final List<ShapeDrawable> mDrawables;

        private RVAdapter(ArrayList<ShapeDrawable> drawables) {
            mDrawables = drawables;
        }

        @Override
        public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shape_item,
                    parent, false);
            return new VHolder(view);
        }

        @Override
        public void onBindViewHolder(VHolder holder, int position) {
            holder.mShapeView.setBackground(mDrawables.get(position));
            holder.mShapeDrawable = mDrawables.get(position);
        }

        @Override
        public int getItemCount() {
            return mDrawables.size();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        LifecycleActivity activity = (LifecycleActivity) getActivity();
        mViewModel = ViewModelStore.get(activity, "shared", SharedViewModel.class);

        Context context = inflater.getContext();
        RecyclerView rv = new RecyclerView(context);
        rv.setLayoutManager(new GridLayoutManager(context, 3));

        ArrayList<ShapeDrawable> drawables = new ArrayList<>();
        for (int color : COLORS) {
            for (Shape shape : SHAPES) {
                ShapeDrawable drawable = new ShapeDrawable(shape);
                drawable.getPaint().setColor(color);
                drawables.add(drawable);
            }
        }
        rv.setAdapter(new RVAdapter(drawables));
        return rv;
    }
}
