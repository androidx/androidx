/*
 * Copyright 2017 The Android Open Source Project
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

package com.example.androidx.car;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.DrawableRes;
import androidx.car.widget.ActionBar;
import androidx.fragment.app.FragmentActivity;

/**
 * Demo activity for ActionBar
 */
public class ActionBarActivity extends FragmentActivity {
    private ActionBar mActionPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_action_bar);
        mActionPanel = findViewById(R.id.action_panel);
        mActionPanel.setView(createButton(this, R.drawable.ic_play),
                ActionBar.SLOT_MAIN);
        ImageButton skpPrevious = createButton(this, R.drawable.ic_skip_next);
        skpPrevious.setVisibility(View.GONE);
        mActionPanel.setView(skpPrevious, ActionBar.SLOT_LEFT);
        mActionPanel.setView(createButton(this, R.drawable.ic_skip_next),
                ActionBar.SLOT_RIGHT);
        mActionPanel.setViews(new ImageButton[]{
                createButton(this, R.drawable.ic_queue_music),
                createButton(this, R.drawable.ic_overflow),
                createButton(this, R.drawable.ic_overflow),
                createButton(this, R.drawable.ic_overflow),
                createButton(this, R.drawable.ic_overflow),
                createButton(this, R.drawable.ic_overflow)
        });
    }

    private ImageButton createButton(Context context, @DrawableRes int iconResId) {
        ImageButton button = new ImageButton(context, null,
                androidx.car.R.style.Widget_Car_Button_ActionBar);
        button.setImageDrawable(context.getDrawable(iconResId));
        return button;
    }
}
