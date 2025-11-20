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

import static android.webkit.WebSettings.LOAD_DEFAULT;
import static android.widget.Toast.LENGTH_SHORT;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebStorageCompat;
import androidx.webkit.WebViewFeature;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class WebStorageCompatActivity extends AppCompatActivity {

    private static final String TAG = "WebStorageActivity";

    private WebView mWebView;
    private MockWebServer mMockWebServer;

    private String mPageUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_storage);
        WebkitHelpers.enableEdgeToEdge(this);
        setTitle(R.string.web_storage_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DELETE_BROWSING_DATA)) {
            WebkitHelpers.showMessageInActivity(WebStorageCompatActivity.this,
                    R.string.webkit_api_not_available);
            return;
        }


        mWebView = findViewById(R.id.web_storage_webview);
        mWebView.getSettings().setCacheMode(LOAD_DEFAULT);
        mWebView.setWebViewClient(new WebViewClient());

        Button loadButton = findViewById(R.id.web_storage_load_page_button);
        loadButton.setOnClickListener(this::onLoadButton);

        Button deleteButton = findViewById(R.id.web_storage_delete_data_button);
        deleteButton.setOnClickListener(this::onDeleteButton);


        mMockWebServer = new MockWebServer();

        mMockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                MockResponse response = new MockResponse();
                if (!request.getPath().equals("/")) {
                    response.setResponseCode(400);
                    return response;
                }

                try {
                    String template = readHtmlTemplate();
                    response.setHeader("Cache-Control", "max-age=604800");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z",
                            Locale.US);
                    response.setBody(String.format(template, dateFormat.format(new Date())));
                } catch (IOException e) {
                    Log.e(TAG, "Error loading html template", e);
                    response.setResponseCode(500);
                    response.setBody("Error loading html template");
                }
                return response;
            }
        });

        mPageUrl = startMockServerAndGetPageUrl();
        mWebView.loadUrl(mPageUrl);
    }

    private String startMockServerAndGetPageUrl() {
        // The mockWebServer accesses networking APIs during startup and URL construction that
        // are not allowed on the main thread.
        FutureTask<String> urlTask = new FutureTask<>(() -> {
            try {
                mMockWebServer.start();
                return mMockWebServer.url("/").toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Executors.newCachedThreadPool().execute(urlTask);
        try {
            return urlTask.get(1, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private String readHtmlTemplate() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getResources().openRawResource(R.raw.web_storage_html_template)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMockWebServer != null) {
            try {
                mMockWebServer.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing mock web server", e);
            }
        }
    }

    private void onLoadButton(View v) {
        mWebView.loadUrl(mPageUrl);
    }

    private void onDeleteButton(View v) {
        WebStorageCompat.deleteBrowsingData(WebStorage.getInstance(), this::onDeletionComplete);
    }

    private void onDeletionComplete() {
        Toast.makeText(this, R.string.web_storage_delete_complete, LENGTH_SHORT).show();
    }
}
