/*
 * Copyright 2025 The Android Open Source Project
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

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.CustomHeader;
import androidx.webkit.Profile;
import androidx.webkit.ProfileStore;
import androidx.webkit.WebViewFeature;

import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Demonstration of how to use the custom header API to set custom headers on requests.
 */
public class CustomHeaderActivity extends AppCompatActivity {

    private static final int SERVER_PORT = 17001;
    private static final String SERVER_URL = "http://localhost:" + SERVER_PORT;
    private WebView mWebView;
    private EditText mNameInput;
    private EditText mValueInput;
    private HttpServer mServer;
    private Profile mProfile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_header);
        setTitle(R.string.cookie_manager_activity_title);
        WebkitHelpers.enableEdgeToEdge(this);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.CUSTOM_REQUEST_HEADERS)) {
            WebkitHelpers.showMessageInActivity(CustomHeaderActivity.this,
                    R.string.webkit_api_not_available);
            return;
        }
        mServer = new HttpServer(SERVER_PORT, HttpServer.EchoRequestHandler::new, () -> {});
        mServer.start();

        mWebView = findViewById(R.id.custom_header_webview);
        mProfile = ProfileStore.getInstance().getProfile(Profile.DEFAULT_PROFILE_NAME);
        mNameInput = findViewById(R.id.custom_header_name);
        mValueInput = findViewById(R.id.custom_header_value);

        Button addButton = findViewById(R.id.custom_headers_add);
        addButton.setOnClickListener(this::addRequestHeader);

        Button clearButton = findViewById(R.id.custom_headers_clear);
        clearButton.setOnClickListener(this::clearCustomHeaders);

        Button loadButton = findViewById(R.id.custom_headers_load);
        loadButton.setOnClickListener(this::loadWebView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mServer != null) {
            mServer.shutdown();
        }
    }

    private void addRequestHeader(View v) {
        String name = mNameInput.getText().toString().trim();
        String value = mValueInput.getText().toString().trim();
        if (name.isEmpty() || value.isEmpty()) {
            Toast.makeText(this, R.string.custom_header_missing_input_warning, Toast.LENGTH_SHORT);
            return;
        }
        mProfile.addCustomHeader(new CustomHeader(name, value, Set.of(SERVER_URL)));
        mNameInput.getText().clear();
        mValueInput.getText().clear();
    }

    private void clearCustomHeaders(View v) {
        mProfile.clearAllCustomHeaders();
    }

    private void loadWebView(View v) {
        mWebView.loadUrl(SERVER_URL);
    }
}
