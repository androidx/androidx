/*
 * Copyright 2022 The Android Open Source Project
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
package com.example.android.supportv4.widget;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.android.supportv4.R;
import com.google.android.material.appbar.CollapsingToolbarLayout;

/**
 * This activity demonstrates the use of nested scrolling in the v4 support library along with
 * a collapsing app bar. See the associated layout file for details.
 */
public class NestedScrollActivity3LevelsWithCollapsingToolbar extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.nested_scroll_3_levels_collapsing_toolbar);

        CollapsingToolbarLayout collapsingToolbar = findViewById(R.id.collapsing_toolbar_layout);
        collapsingToolbar.setTitle(
                getResources().getString(R.string.nested_scroll_3_levels_collapsing_appbar_title)
        );
        collapsingToolbar.setContentScrimColor(getResources().getColor(R.color.color1));
    }
}
