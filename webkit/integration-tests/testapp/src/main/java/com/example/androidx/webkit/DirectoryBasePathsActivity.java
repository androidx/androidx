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

package com.example.androidx.webkit;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.webkit.ProcessGlobalConfig;
import androidx.webkit.WebViewFeature;

import java.io.File;

/**
 * An {@link Activity} which makes use of
 * {@link androidx.webkit.ProcessGlobalConfig#setDirectoryBasePaths(Context, File, File)}
 */
public class DirectoryBasePathsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.directory_base_path_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (!WebViewFeature.isStartupFeatureSupported(this,
                WebViewFeature.STARTUP_FEATURE_SET_DIRECTORY_BASE_PATHS)) {
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
            return;
        }

        File dataBasePath =
                new File(ContextCompat.getDataDir(this),
                        "data_dir");
        File cacheBasePath =
                new File(ContextCompat.getDataDir(this),
                        "cache_dir");
        ProcessGlobalConfig config = new ProcessGlobalConfig();
        config.setDirectoryBasePaths(this, dataBasePath, cacheBasePath);
        config.setDataDirectorySuffix(this, "directory_base_path_activity_suffix");
        ProcessGlobalConfig.apply(config);
        setContentView(R.layout.activity_data_directory_config);
        WebView wv = findViewById(R.id.data_directory_config_webview);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new WebViewClient());
        wv.loadUrl("www.google.com");
        TextView tv = findViewById(R.id.data_directory_config_textview);
        tv.setText("WebView Loaded!");
    }
}
