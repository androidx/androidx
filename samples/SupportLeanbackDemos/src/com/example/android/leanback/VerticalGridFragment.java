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

        mAdapter = new ArrayObjectAdapter(new CardPresenter());
        for (int i = 0; i < NUM_ITEMS; i++) {
            mAdapter.add(Integer.toString(i));
        }
        setAdapter(mAdapter);

        setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(Object item, Row row) {
                Log.i(TAG, "item selected: " + item);
            }
        });

        setOnItemClickedListener(new OnItemClickedListener() {
            @Override
            public void onItemClicked(Object item, Row row) {
                Log.i(TAG, "item clicked: " + item);
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
}
