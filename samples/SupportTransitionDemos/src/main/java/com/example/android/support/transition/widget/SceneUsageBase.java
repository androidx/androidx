/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.example.android.support.transition.widget;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.transition.Scene;
import androidx.transition.TransitionManager;

import com.example.android.support.transition.R;

abstract class SceneUsageBase extends TransitionUsageBase {

    private Scene[] mScenes;

    private int mCurrentScene;

    abstract Scene[] setUpScenes(ViewGroup root);

    abstract void go(Scene scene);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = findViewById(R.id.root);
        mScenes = setUpScenes(root);
        TransitionManager.go(mScenes[0]);
    }

    @Override
    int getLayoutResId() {
        return R.layout.scene_usage;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.basic_usage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggle:
                mCurrentScene = (mCurrentScene + 1) % mScenes.length;
                go(mScenes[mCurrentScene]);
                return true;
        }
        return false;
    }

}
