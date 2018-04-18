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

package com.example.android.support.wear.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableRecyclerView;

import com.example.android.support.wear.app.drawers.WearableDrawersDemo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main activity for the wear demos.
 */
public class MainDemoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WearableRecyclerView demoList = new WearableRecyclerView(this);
        demoList.setPadding(30, 0, 30, 0);
        demoList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        demoList.setAdapter(new DemoAdapter(createContentMap()));
        demoList.setEdgeItemsCenteringEnabled(true);
        setContentView(demoList);
    }

    private Map<String, Intent> createContentMap() {
        Map<String, Intent> contentMap = new LinkedHashMap<>();
        contentMap.put("Wearable Recycler View", new Intent(
                this, SimpleWearableRecyclerViewDemo.class));
        contentMap.put("Wearable Switch", new Intent(
                this, WearableSwitchDemo.class));
        contentMap.put("Circular Progress Layout", new Intent(
                this, CircularProgressLayoutDemo.class));
        contentMap.put("Wearable Drawers", new Intent(
                this, WearableDrawersDemo.class));
        contentMap.put("Rounded Drawable", new Intent(
                this, RoundedDrawableDemo.class));
        contentMap.put("Ambient Fragment", new Intent(
                this, AmbientModeDemo.class));
        contentMap.put("Alert Dialog (v7)", new Intent(
                this, AlertDialogDemo.class));
        contentMap.put("Confirmation Overlay", new Intent(
                this, ConfirmationOverlayDemo.class));

        return contentMap;
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        Button mView;

        ViewHolder(Button itemView) {
            super(itemView);
            mView = itemView;
        }
    }

    private class DemoAdapter extends WearableRecyclerView.Adapter<ViewHolder> {
        private final Object[] mKeys;
        private final Map<String, Intent> mData;

        DemoAdapter(Map<String, Intent> dataMap) {
            mKeys = dataMap.keySet().toArray();
            mData = dataMap;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Button view = new Button(parent.getContext());
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            view.setPadding(10, 10, 10, 10);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            holder.mView.setText(mKeys[position].toString());
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent result = new Intent();
                    result.setClass(MainDemoActivity.this, SimpleWearableRecyclerViewDemo.class);
                    startActivity(mData.get(mKeys[position]));
                }
            });
        }


        @Override
        public int getItemCount() {
            return mKeys.length;
        }
    }
}
