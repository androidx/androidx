/*
 * Copyright 2018 The Android Open Source Project
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

package com.example.androidx.webkit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.jspecify.annotations.Nullable;

/**
 * An {@link Activity} which lists features that make use of
 * {@link androidx.webkit.ProcessGlobalConfig} to set up process global configuration prior to
 * loading WebView.
 */
public class ProcessGlobalConfigActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebkitHelpers.enableEdgeToEdge(this);
        setTitle(R.string.process_global_config_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);
        final Context activityContext = this;
        MenuListView listView = findViewById(R.id.top_level_list);
        MenuListView.MenuItem[] menuItems = new MenuListView.MenuItem[] {
                new MenuListView.MenuItem(
                        getResources().getString(R.string.data_directory_suffix_activity_title),
                        new Intent(activityContext, DataDirectorySuffixActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.directory_base_path_activity_title),
                        new Intent(activityContext, DirectoryBasePathsActivity.class)),
                new MenuListView.MenuItem(
                        getResources().getString(R.string.ui_thread_startup_mode_activity_title),
                        new Intent(activityContext, UiThreadStartupModeActivity.class)),
        };
        listView.setItems(menuItems);
    }
}
