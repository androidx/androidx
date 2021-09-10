/*
 * Copyright 2019 The Android Open Source Project
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

import static java.nio.charset.StandardCharsets.UTF_8;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.TracingConfig;
import androidx.webkit.TracingController;
import androidx.webkit.WebViewFeature;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An {@link Activity} to exercise Tracing Controller functionality.
 */
public class TracingControllerActivity extends AppCompatActivity {
    TracingController mTracingController;
    private WebView mWebView;
    private TextView mInfo;
    private EditText mNavigationBar;
    private Button mButton;
    private String mLogPath;

    @Override
    @SuppressWarnings("CatchAndPrintStackTrace")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracing_controller);
        setTitle(R.string.tracing_controller_activity_title);

        mNavigationBar = findViewById(R.id.tracing_controller_edittext);
        mNavigationBar.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                String url = mNavigationBar.getText().toString();
                if (!url.isEmpty()) {
                    if (!url.startsWith("http")) url = "http://" + url;
                    mWebView.loadUrl(url);
                    mNavigationBar.setText("");
                }
                return true;
            }
            return false;
        });

        mInfo = findViewById(R.id.tracing_controller_textview);
        mInfo.setVisibility(View.GONE);

        mButton = findViewById(R.id.tracing_controller_button);
        mButton.setOnClickListener(v -> {
            if (mTracingController.isTracing()) {
                try {
                    mButton.setEnabled(false);
                    mLogPath = getExternalFilesDir(null) + File.separator + "tc.json";
                    FileOutputStream os = new FileOutputStream(new File(mLogPath)) {
                        @Override
                        public void close() throws IOException {
                            super.close();
                            runOnUiThread(() -> {
                                mInfo.setVisibility(View.VISIBLE);
                                mButton.setVisibility(View.GONE);
                                mInfo.setText(
                                        getString(R.string.tracing_controller_log_path, mLogPath));
                                try {
                                    verifyJSON();
                                } catch (IOException | JSONException e) {
                                    mInfo.setText(R.string.tracing_controller_invalid_log);
                                }
                            });
                        }
                    };
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    mTracingController.stop(os, executor);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                TracingConfig config = new TracingConfig.Builder()
                        .addCategories(TracingConfig.CATEGORIES_ANDROID_WEBVIEW)
                        .build();
                mTracingController.start(config);
                mButton.setText(R.string.tracing_controller_stop_tracing);
            }
        });

        mWebView = findViewById(R.id.tracing_controller_webview);
        mWebView.setWebViewClient(new WebViewClient());

        WebkitHelpers.appendWebViewVersionToTitle(this);
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE)) {
            mNavigationBar.setVisibility(View.GONE);
            mWebView.setVisibility(View.GONE);
            mButton.setVisibility(View.GONE);
            mInfo.setVisibility(View.GONE);
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
            return;
        }

        mTracingController = TracingController.getInstance();
    }

    private void verifyJSON() throws IOException, JSONException {
        StringBuilder builder = new StringBuilder();
        FileInputStream fis = new FileInputStream(mLogPath);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, UTF_8));
        String sCurrentLine;
        while ((sCurrentLine = br.readLine()) != null) {
            builder.append(sCurrentLine).append("\n");
        }

        // Throw exception if JSON is incorrect
        new JSONObject(builder.toString());
    }
}
