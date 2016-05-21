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
import android.support.transition.Scene;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.example.android.support.transition.R;

/**
 * This demonstrates basic usage of the Transition API.
 */
public class BasicUsage extends AppCompatActivity {

    private final Scene[] mScenes = new Scene[2];
    private int mCurrentScene;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.basic_usage);

        // Retrieve the Toolbar from our content view, and set it as the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FrameLayout root = (FrameLayout) findViewById(R.id.root);
        mScenes[0] = Scene.getSceneForLayout(root, R.layout.scene0, this);
        mScenes[1] = Scene.getSceneForLayout(root, R.layout.scene1, this);
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
                if (mCurrentScene == 0) {
                    mCurrentScene = 1;
                } else {
                    mCurrentScene = 0;
                }
                TransitionManager.go(mScenes[mCurrentScene]);
                return true;
        }
        return false;
    }

}
