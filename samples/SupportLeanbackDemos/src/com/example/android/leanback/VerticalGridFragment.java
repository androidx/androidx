/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.leanback;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.VerticalGridPresenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.OnItemClickedListener;
import android.support.v17.leanback.widget.OnItemSelectedListener;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Random;

public class VerticalGridFragment extends android.support.v17.leanback.app.VerticalGridFragment {
    private static final String TAG = "leanback.VerticalGridFragment";

    private static final int NUM_COLUMNS = 3;
    private static final int NUM_ITEMS = 50;
    private static final int HEIGHT = 200;

    private ArrayObjectAdapter mAdapter;
    private Random mRandom;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mRandom = new Random();

        Params p = new Params();
        p.setBadgeImage(getActivity().getResources().getDrawable(R.drawable.ic_title));
        p.setTitle("Leanback Vertical Grid Demo");
        setParams(p);

        setupFragment();
    }

    private void setupFragment() {
        VerticalGridPresenter gridPresenter = new VerticalGridPresenter();
        gridPresenter.setNumberOfColumns(NUM_COLUMNS);
        setGridPresenter(gridPresenter);

        mAdapter = new ArrayObjectAdapter(new GridItemPresenter());
        for (int i = 0; i < NUM_ITEMS; i++) {
            mAdapter.add(new MyItem(i));
        }
        setAdapter(mAdapter);

        setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(Object item, Row row) {
                Log.i(TAG, "item selected: " + ((MyItem) item).id);
            }
        });

        setOnItemClickedListener(new OnItemClickedListener() {
            @Override
            public void onItemClicked(Object item, Row row) {
                Log.i(TAG, "item clicked: " + ((MyItem) item).id);
            }
        });
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });
    }

    private class GridItemPresenter extends Presenter {
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            // Choose a random height between HEIGHT and 1.5 * HEIGHT to
            // demonstrate the staggered nature of the grid.
            final int height = HEIGHT + mRandom.nextInt(HEIGHT / 2);
            view.setLayoutParams(new ViewGroup.LayoutParams(200, height));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(Color.DKGRAY);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText(Integer.toString(((MyItem) item).id));
        }

        public void onUnbindViewHolder(ViewHolder viewHolder) {}
    }

    static class MyItem {
        int id;
        MyItem(int id) {
            this.id = id;
        }
    }
}
