/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.widget.helper;

import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.support.v7.recyclerview.R;

/**
 * Internal class that is used to move views on both Gingerbread and Honeycomb
 */
interface ItemTouchUICompat {

    void onDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
            float dX,
            float dY, int actionState, boolean isCurrentlyActive);

    void onDrawOver(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
            float dX, float dY, int actionState, boolean isCurrentlyActive);

    void clearView(RecyclerView.ViewHolder viewHolder);

    void onSelected(RecyclerView.ViewHolder viewHolder);

    public static class LollipopImpl extends HoneycombImpl {

        @Override
        public void onDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            if (isCurrentlyActive) {
                Object originalElevation = viewHolder.itemView
                        .getTag(R.id.item_touch_helper_previous_elevation);
                if (originalElevation == null) {
                    originalElevation = ViewCompat.getElevation(viewHolder.itemView);
                    float newElevation = 1f + findMaxElevation(recyclerView, viewHolder.itemView);
                    ViewCompat.setElevation(viewHolder.itemView, newElevation);
                    viewHolder.itemView
                            .setTag(R.id.item_touch_helper_previous_elevation, originalElevation);
                }
            }
            super.onDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        private float findMaxElevation(RecyclerView recyclerView, View itemView) {
            final int childCount = recyclerView.getChildCount();
            float max = 0;
            for (int i = 0; i < childCount; i++) {
                final View child = recyclerView.getChildAt(i);
                if (child == itemView) {
                    continue;
                }
                final float elevation = ViewCompat.getElevation(child);
                if (elevation > max) {
                    max = elevation;
                }
            }
            return max;
        }

        @Override
        public void clearView(RecyclerView.ViewHolder viewHolder) {
            final Object tag = viewHolder.itemView
                    .getTag(R.id.item_touch_helper_previous_elevation);
            if (tag != null && tag instanceof Float) {
                ViewCompat.setElevation(viewHolder.itemView, (Float) tag);
            }
            viewHolder.itemView.setTag(R.id.item_touch_helper_previous_elevation, null);
            super.clearView(viewHolder);
        }

        @Override
        public void onSelected(RecyclerView.ViewHolder viewHolder) {

        }
    }

    public static class HoneycombImpl implements ItemTouchUICompat {

        @Override
        public void clearView(RecyclerView.ViewHolder viewHolder) {
            ViewCompat.setTranslationX(viewHolder.itemView, 0f);
            ViewCompat.setTranslationY(viewHolder.itemView, 0f);
        }

        @Override
        public void onSelected(RecyclerView.ViewHolder viewHolder) {

        }

        @Override
        public void onDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            final View view = viewHolder.itemView;
            ViewCompat.setTranslationX(view, dX);
            ViewCompat.setTranslationY(view, dY);
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView recyclerView,
                RecyclerView.ViewHolder viewHolder,
                float dX, float dY, int actionState, boolean isCurrentlyActive) {

        }
    }

    public static class GingerbreadImpl implements ItemTouchUICompat {

        private void draw(Canvas c, RecyclerView parent, RecyclerView.ViewHolder viewHolder,
                float dX, float dY) {
            c.save();
            c.translate(dX, dY);
            parent.drawChild(c, viewHolder.itemView, 0);
            c.restore();
        }

        @Override
        public void clearView(RecyclerView.ViewHolder viewHolder) {
            viewHolder.itemView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onSelected(RecyclerView.ViewHolder viewHolder) {
            viewHolder.itemView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            final View view = viewHolder.itemView;
            if (actionState != ItemTouchHelper.ACTION_STATE_DRAG) {
                draw(c, recyclerView, viewHolder, dX, dY);
            }
        }

        @Override
        public void onDrawOver(Canvas c, RecyclerView recyclerView,
                RecyclerView.ViewHolder viewHolder, float dX, float dY,
                int actionState, boolean isCurrentlyActive) {
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                draw(c, recyclerView, viewHolder, dX, dY);
            }
        }
    }
}

