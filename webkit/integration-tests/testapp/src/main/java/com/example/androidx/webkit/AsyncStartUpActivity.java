/*
 * Copyright 2024 The Android Open Source Project
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
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.StartUpLocation;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewStartUpConfig;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executors;

/**
 * An {@link Activity} that calls
 * {@link WebViewCompat#startUpWebView(android.content.Context, WebViewStartUpConfig, WebViewCompat.WebViewStartUpCallback)}
 * to startup WebView asynchronously and displays the summary of startup.
 */
public class AsyncStartUpActivity extends AppCompatActivity {
    private long mStartCaptureTime;

    @OptIn(markerClass = WebViewCompat.ExperimentalAsyncStartUp.class)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Take care not to startup WebView (including layout inflation) before the explicit call
        // to `startUpWebView()`.

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_async_startup);
        setTitle(R.string.async_startup_activity_title);
        WebkitHelpers.enableEdgeToEdge(this);
        WebkitHelpers.appendWebViewVersionToTitle(this);
        WebViewStartUpConfig config = new WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor()).build();
        WebViewCompat.WebViewStartUpCallback callback = result -> {
            long duration =
                    System.currentTimeMillis()
                            - mStartCaptureTime;
            TextView tv = findViewById(R.id.async_startup_textview);
            tv.setMovementMethod(new ScrollingMovementMethod());
            tv.setText("WebView Async StartUp onSuccess() called in " + duration + " ms. \n");
            tv.append("getTotalTimeInUiThreadMillis: "
                    + result.getTotalTimeInUiThreadMillis() + "\n");
            tv.append("getMaxTimePerTaskInUiThreadMillis: "
                    + result.getMaxTimePerTaskInUiThreadMillis() + "\n");
            tv.append("getUiThreadBlockingStartUpLocations: \n");
            if (result.getUiThreadBlockingStartUpLocations() == null) {
                tv.append("null \n");
            } else if (result.getUiThreadBlockingStartUpLocations().isEmpty()) {
                tv.append("empty list \n");
            } else {
                for (StartUpLocation location : result.getUiThreadBlockingStartUpLocations()) {
                    tv.append(location.getStackInformation() + "\n");
                }
            }
            tv.append("getNonUiThreadBlockingStartUpLocations: \n");
            if (result.getNonUiThreadBlockingStartUpLocations() == null) {
                tv.append("null \n");
            } else if (result.getNonUiThreadBlockingStartUpLocations().isEmpty()) {
                tv.append("empty list \n");
            } else {
                for (StartUpLocation location : result.getNonUiThreadBlockingStartUpLocations()) {
                    tv.append(location.getStackInformation() + "\n");
                }
            }
            //  Render content in a WebView similar to real-life usage.
            WebView wv = new WebView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            LinearLayout layout = (LinearLayout) findViewById(R.id.activity_async_startup);
            layout.addView(wv, params);
            wv.setWebViewClient(new WebViewClient());
            wv.loadUrl("www.example.com");
        };
        mStartCaptureTime = System.currentTimeMillis();
        WebViewCompat.startUpWebView(getApplicationContext(), config, callback);
    }
}
