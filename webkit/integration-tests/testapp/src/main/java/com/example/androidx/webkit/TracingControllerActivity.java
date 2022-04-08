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

    @Override
    @SuppressWarnings("CatchAndPrintStackTrace")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracing_controller);
        setTitle(R.string.tracing_controller_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        final WebView webView = findViewById(R.id.tracing_controller_webview);
        webView.setWebViewClient(new WebViewClient());

        final EditText navigationBar = findViewById(R.id.tracing_controller_edittext);

        final TextView infoView = findViewById(R.id.tracing_controller_textview);
        infoView.setVisibility(View.GONE);

        final Button tracingButton = findViewById(R.id.tracing_controller_button);

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE)) {
            navigationBar.setVisibility(View.GONE);
            webView.setVisibility(View.GONE);
            tracingButton.setVisibility(View.GONE);
            infoView.setVisibility(View.GONE);
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
            return;
        }

        final TracingController tracingController = TracingController.getInstance();

        navigationBar.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                String url = navigationBar.getText().toString();
                if (!url.isEmpty()) {
                    if (!url.startsWith("http")) url = "http://" + url;
                    webView.loadUrl(url);
                    navigationBar.setText("");
                }
                return true;
            }
            return false;
        });

        tracingButton.setOnClickListener(v -> {
            if (tracingController.isTracing()) {
                try {
                    tracingButton.setEnabled(false);
                    final String logPath = getExternalFilesDir(null) + File.separator + "tc.json";
                    FileOutputStream os = new VerifyingFileOutputStream(logPath, infoView,
                            tracingButton);
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    tracingController.stop(os, executor);
                } catch (FileNotFoundException e) {

                    e.printStackTrace();
                }
            } else {
                TracingConfig config = new TracingConfig.Builder().addCategories(
                        TracingConfig.CATEGORIES_ANDROID_WEBVIEW).build();
                tracingController.start(config);
                tracingButton.setText(R.string.tracing_controller_stop_tracing);
            }
        });


    }

    private class VerifyingFileOutputStream extends FileOutputStream {

        private final String mLogPath;
        private final TextView mInfoView;
        private final Button mTracingButton;

        VerifyingFileOutputStream(String logPath, TextView infoView, Button tracingButton)
                throws FileNotFoundException {
            super(logPath);
            mLogPath = logPath;
            mInfoView = infoView;
            mTracingButton = tracingButton;
        }

        @Override
        public void close() throws IOException {
            super.close();
            runOnUiThread(() -> {
                mInfoView.setVisibility(View.VISIBLE);
                mTracingButton.setVisibility(View.GONE);
                mInfoView.setText(getString(R.string.tracing_controller_log_path, mLogPath));
                try {
                    verifyJSON(mLogPath);
                } catch (IOException | JSONException e) {
                    mInfoView.setText(R.string.tracing_controller_invalid_log);
                }
            });
        }

        private void verifyJSON(String logPath) throws IOException, JSONException {
            StringBuilder builder = new StringBuilder();
            FileInputStream fis = new FileInputStream(logPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, UTF_8));
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                builder.append(sCurrentLine).append("\n");
            }

            // Throw exception if JSON is incorrect
            new JSONObject(builder.toString());
        }

    }
}
