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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * An {@link Activity} to exercise WebMessageCompat related functionality.
 */
public class WebMessageCompatActivity extends AppCompatActivity {
    private static final String TYPE_STRING = "String";
    private static final String TYPE_ARRAY_BUFFER = "ArrayBuffer";
    private static final String[] MESSAGE_TYPES = {TYPE_STRING, TYPE_ARRAY_BUFFER};
    private static final boolean ARRAY_BUFFER_FEATURE_ENABLED =
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER);

    private WebView mWebView;
    private TextView mTextView, mPerfTextView;
    private CheckBox mCheckBox;
    private WebMessagePortCompat mPort;
    private Spinner mSpinner;
    private int mMessageCount = 0;
    private int mExpectedCount = 0;
    private long mTimeStamp;

    static CharSequence createNativeTitle() {
        final String title = "Native View\n";
        SpannableString ss = new SpannableString(title);
        ss.setSpan(new AbsoluteSizeSpan(55, true), 0, title.length() - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    @SuppressLint("SetJavascriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_message_compat);
        setTitle(R.string.web_message_compat_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE)) {
            WebkitHelpers.showMessageInActivity(WebMessageCompatActivity.this,
                    R.string.webkit_api_not_available);
            return;
        }

        mWebView = findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new MyWebViewClient());
        mTextView = findViewById(R.id.textview);
        mPerfTextView = findViewById(R.id.textview_perf);
        mCheckBox = findViewById(R.id.checkbox_window_message);
        mSpinner = findViewById(R.id.message_type_spinner);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, MESSAGE_TYPES);
        mSpinner.setAdapter(adapter);
        // If GET_PAYLOAD feature is not supported, disable the type selection spinner.
        mSpinner.setEnabled(ARRAY_BUFFER_FEATURE_ENABLED);

        try (InputStream is = getAssets().open("www/web_message_compat.html")) {
            String webContent = new String(ByteStreams.toByteArray(is), Charsets.UTF_8);
            mWebView.loadDataWithBaseURL("https://example.com", webContent, "text/html", null,
                    null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        findViewById(R.id.button_send).setOnClickListener(view -> {
            mExpectedCount = mMessageCount + 5000;
            mTimeStamp = System.currentTimeMillis();
            mSpinner.setEnabled(false);
            sendMessage();
        });
    }

    private void sendMessage() {
        if (mMessageCount >= mExpectedCount) {
            return;
        }
        final String selectedType = (String) mSpinner.getSelectedItem();
        final WebMessageCompat message;
        switch (selectedType) {
            case TYPE_STRING:
                message = new WebMessageCompat(String.valueOf(mMessageCount + 1));
                break;
            case TYPE_ARRAY_BUFFER:
                byte[] bytes =
                        ByteBuffer.allocate(Integer.BYTES).putInt(mMessageCount + 1).array();
                message = new WebMessageCompat(bytes);
                break;
            default:
                // Should never happen.
                throw new RuntimeException("Invalid type.");
        }
        if (mCheckBox.isChecked()) {
            WebViewCompat.postWebMessage(mWebView, message, Uri.EMPTY);
        } else {
            mPort.postMessage(message);
        }
    }

    private void refreshNativeText() {
        mTextView.setText(TextUtils.concat(createNativeTitle(), String.valueOf(mMessageCount),
                " messages received"));
    }

    private void refreshPerfText() {
        mPerfTextView.setText("Average time over 5000 messages: "
                + (System.currentTimeMillis() - mTimeStamp) / 5000.0 + " ms");
    }

    private void setupMessagePort() {
        WebMessagePortCompat[] ports = WebViewCompat.createWebMessageChannel(mWebView);
        WebViewCompat.postWebMessage(mWebView,
                new WebMessageCompat("setup", new WebMessagePortCompat[]{ports[0]}), Uri.EMPTY);
        mPort = ports[1];
        mPort.setWebMessageCallback(new WebMessagePortCompat.WebMessageCallbackCompat() {
            @Override
            public void onMessage(@NonNull WebMessagePortCompat port,
                    @Nullable WebMessageCompat message) {
                switch (message.getType()) {
                    case WebMessageCompat.TYPE_STRING:
                        mMessageCount = Integer.parseInt(message.getData());
                        break;
                    case WebMessageCompat.TYPE_ARRAY_BUFFER:
                        mMessageCount = ByteBuffer.wrap(message.getArrayBuffer()).getInt();
                        break;
                    default:
                        // Should never happen
                        throw new RuntimeException("Invalid type: " + message.getType());
                }
                if (mMessageCount % 100 == 0) {
                    refreshNativeText();
                }
                if (mMessageCount == mExpectedCount) {
                    refreshPerfText();
                    mSpinner.setEnabled(ARRAY_BUFFER_FEATURE_ENABLED);
                }
                sendMessage();
            }
        });
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            setupMessagePort();
            view.setWebViewClient(null);
        }
    }
}
